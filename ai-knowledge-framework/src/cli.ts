#!/usr/bin/env node
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { KnowledgeDocAgent } from "./KnowledgeDocAgent.js";
import type { DocumentKind } from "./framework/types.js";

const __dirname = dirname(fileURLToPath(import.meta.url));
const samplesRoot = resolve(__dirname, "../samples");

async function main(): Promise<void> {
  const args = process.argv.slice(2);
  const command = args[0] ?? "help";

  if (command === "help" || command === "--help") {
    printHelp();
    return;
  }

  if (command === "run") {
    const mock = args.includes("--mock");
    const outIdx = args.indexOf("--out");
    const outputDir =
      outIdx >= 0 ? args[outIdx + 1] : resolve(process.cwd(), "knowledge-output");

    const appName =
      args.find((a) => a.startsWith("--app="))?.split("=")[1] ?? "Sample Application";

    const sourceArgs = args.filter(
      (a) =>
        !a.startsWith("--") &&
        a !== "run" &&
        (outIdx < 0 || args.indexOf(a) !== outIdx + 1),
    );
    const sources =
      sourceArgs.length > 0
        ? sourceArgs.map((p) => resolve(p))
        : [resolve(samplesRoot)];

    const kinds: DocumentKind[] = ["full-pack"];

    const agent = new KnowledgeDocAgent({
      mockLlm: mock,
      outputDir,
    });

    const result = await agent.run({
      applicationName: appName,
      sources,
      documentKinds: kinds,
      audience: "new-joiner",
    });

    console.log(`\n✓ Generated ${result.documents.length} document(s) for "${result.applicationName}"`);
    for (const doc of result.documents) {
      console.log(`  - ${doc.kind}: ${doc.title}`);
    }
    console.log(`\nArtifacts ingested: ${result.artifacts.length}`);
    if (result.trace.length) {
      console.log("\nTrace:");
      for (const step of result.trace) {
        console.log(`  [${step.step}] ${step.detail.slice(0, 120)}`);
      }
    }
    return;
  }

  printHelp();
  process.exit(1);
}

function printHelp(): void {
  console.log(`
AI Knowledge Documentation Agent (Use Case 3)

Usage:
  npm run agent -- [--mock] [--app=Name] [--out dir] [source paths...]

Examples:
  npm run agent -- --mock
  npm run agent -- --mock --app="Customer Portal" --out ./docs ../MavenAutoTest/testng.xml

Environment:
  OPENAI_API_KEY          Enable live agentic LLM loop (optional)
  KNOWLEDGE_AGENT_MODEL   Model id (default: gpt-4o-mini)
`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
