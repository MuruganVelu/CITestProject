import { resolve } from "node:path";
import type { ZodTypeAny } from "zod";
import type { AgentContext } from "./AgentContext.js";
import type { AgentTool } from "./Tool.js";
import type { AgentRunInput, AgentRunResult } from "./types.js";
import {
  buildAutomationContextDoc,
  buildFunctionalOverviewDoc,
  buildOnboardingDoc,
} from "../generators/templates.js";
import { ingestPath } from "../ingestion/ingest.js";

/**
 * Offline / POC path: no LLM required. Runs the same tools in a fixed order.
 */
export async function runDeterministicPipeline(
  input: AgentRunInput,
  ctx: AgentContext,
  tools: AgentTool<ZodTypeAny>[],
  outputDir?: string,
): Promise<AgentRunResult> {
  const byName = new Map(tools.map((t) => [t.name, t]));

  const ingest = byName.get("ingest_sources");
  if (ingest) {
    await ingest.execute({ paths: input.sources }, ctx);
  } else {
    for (const p of input.sources) {
      const artifacts = await ingestPath(resolve(p));
      for (const a of artifacts) {
        ctx.addArtifact(a);
      }
    }
  }

  const analyze = byName.get("analyze_knowledge_gap");
  if (analyze) {
    await analyze.execute({}, ctx);
  }

  const generate = byName.get("generate_document");
  const documents: import("./types.js").GeneratedDocument[] = [];
  for (const kind of input.documentKinds) {
    if (generate) {
      const result = await generate.execute({ kind }, ctx);
      if (result && typeof result === "object" && "documents" in result) {
        documents.push(
          ...(result as { documents: import("./types.js").GeneratedDocument[] })
            .documents,
        );
      } else if (result && typeof result === "object" && "kind" in result) {
        documents.push(result as import("./types.js").GeneratedDocument);
      }
    }
  }

  if (input.documentKinds.includes("full-pack") && documents.length === 0) {
    documents.push(
      buildOnboardingDoc(ctx),
      buildFunctionalOverviewDoc(ctx),
      buildAutomationContextDoc(ctx),
    );
  }

  const out =
    outputDir ?? process.env.KNOWLEDGE_OUTPUT_DIR;
  if (out) {
    const { mkdir, writeFile } = await import("node:fs/promises");
    const { join } = await import("node:path");
    await mkdir(out, { recursive: true });
    for (const doc of documents) {
      await writeFile(join(out, `${doc.kind}.md`), doc.markdown, "utf8");
    }
    ctx.log("write", out);
  }

  ctx.log("complete", `Generated ${documents.length} document(s)`);

  return {
    applicationName: input.applicationName,
    artifacts: ctx.artifacts,
    documents,
    trace: ctx.trace,
  };
}
