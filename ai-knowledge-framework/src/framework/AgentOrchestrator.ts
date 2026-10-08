import { generateText, stepCountIs, tool } from "ai";
import { createOpenAI } from "@ai-sdk/openai";
import type { ZodTypeAny } from "zod";
import { AgentContext } from "./AgentContext.js";
import type { AgentTool } from "./Tool.js";
import type {
  AgentRunInput,
  AgentRunResult,
  DocumentKind,
  GeneratedDocument,
  KnowledgeAgentConfig,
} from "./types.js";
import { SYSTEM_PROMPT } from "../prompts/system.js";
import { runDeterministicPipeline } from "./DeterministicPipeline.js";

function wrapTools(ctx: AgentContext, tools: AgentTool<ZodTypeAny>[]) {
  const wrapped: Record<string, unknown> = {};
  for (const t of tools) {
    wrapped[t.name] = tool({
      description: t.description,
      inputSchema: t.parameters,
      execute: async (input: unknown) => t.execute(input, ctx),
    });
  }
  return wrapped as Parameters<typeof generateText>[0]["tools"];
}

export class AgentOrchestrator {
  constructor(
    private readonly agentTools: AgentTool<ZodTypeAny>[],
    private readonly config: KnowledgeAgentConfig = {},
  ) {}

  async run(input: AgentRunInput): Promise<AgentRunResult> {
    const ctx = new AgentContext(
      input.applicationName,
      input.audience ?? "new-joiner",
    );
    ctx.log("init", `Starting knowledge run for ${input.applicationName}`);

    if (this.config.mockLlm) {
      return runDeterministicPipeline(
        input,
        ctx,
        this.agentTools,
        this.config.outputDir,
      );
    }

    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) {
      ctx.log(
        "fallback",
        "OPENAI_API_KEY not set — using deterministic mock pipeline",
      );
      return runDeterministicPipeline(
        input,
        ctx,
        this.agentTools,
        this.config.outputDir,
      );
    }

    const openai = createOpenAI({ apiKey });
    const modelId =
      this.config.modelId ?? process.env.KNOWLEDGE_AGENT_MODEL ?? "gpt-4o-mini";

    const userGoal = buildUserGoal(input);

    const result = await generateText({
      model: openai(modelId),
      system: SYSTEM_PROMPT,
      prompt: userGoal,
      tools: wrapTools(ctx, this.agentTools),
      stopWhen: stepCountIs(this.config.maxSteps ?? 12),
    });

    ctx.log("llm-complete", result.text.slice(0, 500));

    const documents = await synthesizeDocuments(
      input.documentKinds,
      ctx,
      result.text,
    );

    if (this.config.outputDir) {
      await writeOutputs(this.config.outputDir, documents, ctx);
    }

    return {
      applicationName: input.applicationName,
      artifacts: ctx.artifacts,
      documents,
      trace: ctx.trace,
    };
  }
}

function buildUserGoal(input: AgentRunInput): string {
  const kinds = input.documentKinds.join(", ");
  return [
    `Application: ${input.applicationName}`,
    `Audience: ${input.audience ?? "new-joiner"}`,
    `Source paths to ingest (use ingest_sources tool): ${input.sources.join(", ")}`,
    `Generate document kinds: ${kinds}`,
    "Workflow:",
    "1. Ingest all sources.",
    "2. Call analyze_knowledge_gap to list gaps and risks.",
    "3. For each requested document kind, call generate_document.",
    "4. Summarize what was produced.",
  ].join("\n");
}

async function synthesizeDocuments(
  kinds: DocumentKind[],
  ctx: AgentContext,
  llmSummary: string,
): Promise<GeneratedDocument[]> {
  const fromTrace = ctx.trace
    .filter((t) => t.step === "document-generated")
    .map((t) => {
      const parsed = JSON.parse(t.detail) as GeneratedDocument;
      return parsed;
    });

  if (fromTrace.length > 0) {
    return fromTrace;
  }

  return kinds.map((kind) => ({
    kind,
    title: `${ctx.applicationName} — ${kind}`,
    markdown: `${llmSummary}\n\n_Auto-synthesized from agent run._`,
  }));
}

async function writeOutputs(
  dir: string,
  docs: GeneratedDocument[],
  ctx: AgentContext,
): Promise<void> {
  const { mkdir, writeFile } = await import("node:fs/promises");
  const { join } = await import("node:path");
  await mkdir(dir, { recursive: true });
  for (const doc of docs) {
    const file = join(dir, `${doc.kind}.md`);
    await writeFile(file, doc.markdown, "utf8");
    ctx.log("write", file);
  }
}
