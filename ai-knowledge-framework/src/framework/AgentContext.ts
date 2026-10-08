import type { AgentTraceStep, IngestedArtifact } from "./types.js";

/**
 * Shared mutable state for a single agent run (ingestion → reasoning → output).
 */
export class AgentContext {
  readonly applicationName: string;
  readonly audience: string;
  artifacts: IngestedArtifact[] = [];
  trace: AgentTraceStep[] = [];

  constructor(applicationName: string, audience = "new-joiner") {
    this.applicationName = applicationName;
    this.audience = audience;
  }

  addArtifact(artifact: IngestedArtifact): void {
    this.artifacts.push(artifact);
  }

  log(step: string, detail: string): void {
    this.trace.push({
      step,
      detail,
      timestamp: new Date().toISOString(),
    });
  }
}
