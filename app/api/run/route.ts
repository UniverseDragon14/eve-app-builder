import { NextRequest, NextResponse } from "next/server";
import { spawn } from "child_process";
import fs from "fs";
import path from "path";

const HOME = process.env.HOME || "/home/aslam";
const PROJECTS_DIR = path.join(HOME, "universal_dragon", "eve_forge", "projects");
const RUN_STATE = path.join(HOME, "universal_dragon", "eve_forge", "run_state.json");

function safeName(input: string) {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60);
}

function writeState(data: any) {
  fs.writeFileSync(RUN_STATE, JSON.stringify(data, null, 2));
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const project = safeName(String(body.project || ""));

    if (!project) {
      return NextResponse.json({
        ok: false,
        output: "Project name required.",
      });
    }

    const projectDir = path.join(PROJECTS_DIR, project);

    if (!fs.existsSync(projectDir)) {
      return NextResponse.json({
        ok: false,
        output: `Project not found: ${project}`,
      });
    }

    const child = spawn("bash", ["-lc", "npm install && npm run dev"], {
      cwd: projectDir,
      detached: true,
      stdio: "ignore",
    });

    child.unref();

    const state = {
      project,
      port: 3051,
      url: "http://192.168.70.117:3051",
      startedAt: new Date().toISOString(),
      status: "running",
    };

    writeState(state);

    return NextResponse.json({
      ok: true,
      output: `✅ Project run started\n${project}\nPreview: ${state.url}`,
      ...state,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      output: "Run failed. Check project files or npm install.",
    });
  }
}

export async function GET() {
  try {
    if (!fs.existsSync(RUN_STATE)) {
      return NextResponse.json({
        ok: true,
        status: "idle",
        output: "No project running yet.",
      });
    }

    const state = JSON.parse(fs.readFileSync(RUN_STATE, "utf8"));

    return NextResponse.json({
      ok: true,
      output: `Running: ${state.project}\nPreview: ${state.url}`,
      ...state,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      output: "Could not read run state.",
    });
  }
}
