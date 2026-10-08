import { readFile, readdir, stat } from "node:fs/promises";
import { extname, join } from "node:path";
import type { IngestedArtifact } from "../framework/types.js";

export async function ingestPath(absolutePath: string): Promise<IngestedArtifact[]> {
  const st = await stat(absolutePath);
  if (st.isDirectory()) {
    const entries = await readdir(absolutePath);
    const all: IngestedArtifact[] = [];
    for (const name of entries) {
      all.push(...(await ingestPath(join(absolutePath, name))));
    }
    return all;
  }

  const raw = await readFile(absolutePath, "utf8");
  const ext = extname(absolutePath).toLowerCase();

  if (ext === ".json") {
    return parseJsonFile(absolutePath, raw);
  }
  if (ext === ".xml") {
    return parseTestNgXml(absolutePath, raw);
  }
  if (ext === ".md" || ext === ".txt") {
    return [
      {
        source: absolutePath,
        type: "existing-doc",
        title: basenameTitle(absolutePath),
        summary: raw.slice(0, 400).replace(/\s+/g, " "),
        rawExcerpt: raw.slice(0, 2000),
        metadata: { format: ext.slice(1) },
      },
    ];
  }

  return [
    {
      source: absolutePath,
      type: "requirement",
      title: basenameTitle(absolutePath),
      summary: raw.slice(0, 300),
      rawExcerpt: raw.slice(0, 1500),
      metadata: { format: "text" },
    },
  ];
}

function basenameTitle(path: string): string {
  const parts = path.split(/[/\\]/);
  return parts[parts.length - 1] ?? path;
}

function parseJsonFile(path: string, raw: string): IngestedArtifact[] {
  try {
    const data = JSON.parse(raw) as Record<string, unknown>;
    if (Array.isArray(data.issues)) {
      return (data.issues as JiraIssue[]).map((issue) => jiraToArtifact(path, issue));
    }
    if (data.key && data.fields) {
      return [jiraToArtifact(path, data as JiraIssue)];
    }
    if (Array.isArray(data)) {
      return data.map((item, i) => ({
        source: `${path}#${i}`,
        type: "test-case" as const,
        title: String((item as { name?: string }).name ?? `Case ${i + 1}`),
        summary: JSON.stringify(item).slice(0, 400),
        rawExcerpt: JSON.stringify(item, null, 2).slice(0, 2000),
        metadata: { format: "json-test-case" },
      }));
    }
  } catch {
    /* fall through */
  }
  return [
    {
      source: path,
      type: "requirement",
      title: basenameTitle(path),
      summary: raw.slice(0, 300),
      rawExcerpt: raw.slice(0, 1500),
      metadata: { format: "json" },
    },
  ];
}

interface JiraIssue {
  key?: string;
  fields?: {
    summary?: string;
    description?: string;
    issuetype?: { name?: string };
    status?: { name?: string };
  };
}

function jiraToArtifact(path: string, issue: JiraIssue): IngestedArtifact {
  const key = issue.key ?? "UNKNOWN";
  const summary = issue.fields?.summary ?? key;
  const desc = issue.fields?.description ?? "";
  return {
    source: path,
    type: "jira",
    title: `${key}: ${summary}`,
    summary: `${issue.fields?.issuetype?.name ?? "Story"} — ${summary}`,
    rawExcerpt: `${summary}\n\n${desc}`.slice(0, 2000),
    metadata: {
      status: issue.fields?.status?.name ?? "",
      issueType: issue.fields?.issuetype?.name ?? "",
    },
  };
}

function parseTestNgXml(path: string, raw: string): IngestedArtifact[] {
  const methods = [...raw.matchAll(/<include\s+name="([^"]+)"/g)].map((m) => m[1]);
  const classes = [...raw.matchAll(/<class\s+name="([^"]+)"/g)].map((m) => m[1]);
  return [
    {
      source: path,
      type: "test-case",
      title: `TestNG suite: ${basenameTitle(path)}`,
      summary: `Classes: ${classes.join(", ") || "n/a"}. Methods: ${methods.join(", ") || "n/a"}`,
      rawExcerpt: raw.slice(0, 2000),
      metadata: {
        classes: classes.join(";"),
        methods: methods.join(";"),
      },
    },
  ];
}
