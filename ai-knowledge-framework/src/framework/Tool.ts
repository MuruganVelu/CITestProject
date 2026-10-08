import type { z } from "zod";
import type { AgentContext } from "./AgentContext.js";

export interface AgentTool<TInput extends z.ZodTypeAny> {
  name: string;
  description: string;
  parameters: TInput;
  execute: (input: z.infer<TInput>, ctx: AgentContext) => Promise<unknown>;
}
