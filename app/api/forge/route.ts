import { NextRequest, NextResponse } from "next/server";
import { execFile } from "child_process";
import { promisify } from "util";
import fs from "fs";
import path from "path";

const execFileAsync = promisify(execFile);

const HOME = process.env.HOME || "/home/aslam";
const FORGE_DIR = path.join(HOME, "universal_dragon", "eve_forge");
const FORGE_FILE = path.join(FORGE_DIR, "forge.py");
const PROJECTS_DIR = path.join(FORGE_DIR, "projects");

const allowedTemplates = [
  "booking",
  "portfolio",
  "shop",
  "dashboard",
  "assistant",
  "api",
];

function safeName(input: string) {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60);
}

function getProjects() {
  if (!fs.existsSync(PROJECTS_DIR)) return [];

  return fs
    .readdirSync(PROJECTS_DIR, { withFileTypes: true })
    .filter((item) => item.isDirectory())
    .map((item) => item.name)
    .filter((name) => /^[a-z0-9][a-z0-9-]*$/.test(name))
    .sort();
}

export async function GET() {
  try {
    const projects = getProjects();

    return NextResponse.json({
      ok: true,
      projects,
      output: projects.length
        ? projects.map((name) => `- ${name}`).join("\n")
        : "No projects yet.",
    });
  } catch {
    return NextResponse.json({
      ok: false,
      projects: [],
      output: "Forge list failed.",
    });
  }
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();

    const template = String(body.template || "booking").trim();
    const name = safeName(String(body.name || "eve-project"));

    if (!allowedTemplates.includes(template)) {
      return NextResponse.json({
        ok: false,
        output: "Template not allowed.",
      });
    }

    if (!name) {
      return NextResponse.json({
        ok: false,
        output: "Project name required.",
      });
    }

    const { stdout } = await execFileAsync(
      "python3",
      [FORGE_FILE, "create", template, name],
      {
        cwd: FORGE_DIR,
        timeout: 20000,
      }
    );

    const projects = getProjects();

    return NextResponse.json({
      ok: true,
      output: stdout,
      projects,
      project: name,
    });
  } catch {
    const projects = getProjects();

    return NextResponse.json({
      ok: false,
      output: "Forge create failed. Project may already exist.",
      projects,
    });
  }
}
