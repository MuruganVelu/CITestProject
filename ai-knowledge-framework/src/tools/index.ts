import { z, type ZodTypeAny } from "zod";
import { resolve } from "node:path";
import type { AgentTool } from "../framework/Tool.js";
import type { DocumentKind, GeneratedDocument } from "../framework/types.js";
import { ingestPath } from "../ingestion/ingest.js";
import {
  buildAutomationContextDoc,
  buildFunctionalOverviewDoc,
  buildOnboardingDoc,
} from "../generators/templates.js";

export function createDefaultTools(): AgentTool<ZodTypeAny>[] {
  return [
    {
      name: "ingest_sources",
      description:
        "Load Jira exports, functional test cases, requirements, and existing docs from file paths.",
      parameters: z.object({
        paths: z.array(z.string()).describe("Absolute or relative file/dir paths"),
      }),
      execute: async ({ paths }, ctx) => {
        let count = 0;
        for (const p of paths) {
          const artifacts = await ingestPath(resolve(p));
          for (const a of artifacts) {
            ctx.addArtifact(a);
            count++;
          }
        }
        ctx.log("ingest", `Ingested ${count} artifact(s)`);
        return { ingested: count };
      },
    },
    {
      name: "analyze_knowledge_gap",
      description:
        "Analyze ingested artifacts for onboarding gaps, missing test coverage, and stale documentation risks.",
      parameters: z.object({}),
      execute: async (_input, ctx) => {
        const byType = groupBy(ctx.artifacts, (a) => a.type);
        const gaps: string[] = [];
        if (!byType.jira?.length) {
          gaps.push("No Jira stories found — add exported issues JSON.");
        }
        if (!byType["test-case"]?.length) {
          gaps.push("No functional/automation test cases ingested.");
        }
        if (!byType.requirement?.length && !byType["existing-doc"]?.length) {
          gaps.push("No formal requirements or legacy onboarding docs found.");
        }
        const jiraKeys = new Set(
          ctx.artifacts
            .filter((a) => a.type === "jira")
            .map((a) => a.title.split(":")[0]),
        );
        const testMentions = ctx.artifacts
          .filter((a) => a.type === "test-case")
          .map((a) => a.summary + a.rawExcerpt)
          .join(" ");
        for (const key of jiraKeys) {
          if (key && !testMentions.includes(key)) {
            gaps.push(`Story ${key} may lack linked automated/functional tests.`);
          }
        }
        ctx.log("gap-analysis", JSON.stringify(gaps));
        return { gaps, artifactCount: ctx.artifacts.length };
      },
    },
    {
      name: "generate_document",
      description:
        "Generate markdown documentation for onboarding, functional overview, automation context, or full pack.",
      parameters: z.object({
        kind: z.enum([
          "onboarding",
          "functional-overview",
          "automation-context",
          "full-pack",
        ]),
      }),
      execute: async ({ kind }, ctx) => {
        const docs: GeneratedDocument[] = [];
        if (kind === "full-pack") {
          docs.push(
            buildOnboardingDoc(ctx),
            buildFunctionalOverviewDoc(ctx),
            buildAutomationContextDoc(ctx),
          );
        } else {
          docs.push(buildDoc(kind as DocumentKind, ctx));
        }
        for (const doc of docs) {
          ctx.log("document-generated", JSON.stringify(doc));
        }
        return docs.length === 1 ? docs[0] : { documents: docs };
      },
    },
  ];
}

function buildDoc(kind: DocumentKind, ctx: import("../framework/AgentContext.js").AgentContext) {
  switch (kind) {
    case "onboarding":
      return buildOnboardingDoc(ctx);
    case "functional-overview":
      return buildFunctionalOverviewDoc(ctx);
    case "automation-context":
      return buildAutomationContextDoc(ctx);
    default:
      return buildOnboardingDoc(ctx);
  }
}

function groupBy<T, K extends string>(
  items: T[],
  keyFn: (item: T) => K,
): Partial<Record<K, T[]>> {
  const out: Partial<Record<K, T[]>> = {};
  for (const item of items) {
    const k = keyFn(item);
    if (!out[k]) out[k] = [];
    out[k]!.push(item);
  }
  return out;
}
