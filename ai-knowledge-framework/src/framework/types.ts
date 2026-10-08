export type DocumentKind =
  | "onboarding"
  | "functional-overview"
  | "automation-context"
  | "full-pack";

export interface IngestedArtifact {
  source: string;
  type: "jira" | "test-case" | "requirement" | "existing-doc";
  title: string;
  summary: string;
  rawExcerpt: string;
  metadata: Record<string, string>;
}

export interface AgentRunInput {
  /** Paths to JSON/XML/Markdown files or directories */
  sources: string[];
  documentKinds: DocumentKind[];
  applicationName: string;
  audience?: "new-joiner" | "automation-engineer" | "business-analyst";
}

export interface GeneratedDocument {
  kind: DocumentKind;
  title: string;
  markdown: string;
}

export interface AgentRunResult {
  applicationName: string;
  artifacts: IngestedArtifact[];
  documents: GeneratedDocument[];
  trace: AgentTraceStep[];
}

export interface AgentTraceStep {
  step: string;
  detail: string;
  timestamp: string;
}

export interface KnowledgeAgentConfig {
  mockLlm?: boolean;
  modelId?: string;
  maxSteps?: number;
  outputDir?: string;
}
