import { AgentOrchestrator } from "./framework/AgentOrchestrator.js";
import type { AgentRunInput, KnowledgeAgentConfig } from "./framework/types.js";
import { createDefaultTools } from "./tools/index.js";

/**
 * Facade for Use Case 3 — AI-Based Induction & Knowledge Documentation Generation.
 */
export class KnowledgeDocAgent {
  private readonly orchestrator: AgentOrchestrator;

  constructor(config: KnowledgeAgentConfig = {}) {
    this.orchestrator = new AgentOrchestrator(createDefaultTools(), config);
  }

  run(input: AgentRunInput) {
    return this.orchestrator.run(input);
  }
}
