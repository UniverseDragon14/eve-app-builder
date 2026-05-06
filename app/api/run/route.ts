import { NextRequest, NextResponse } from "next/server";
import { spawn } from "child_process";
import fs from "fs";
import path from "path";

const HOME = process.env.HOME || "/home/aslam";
const PROJECTS_DIR = path.join(HOME, "universal_dragon", "eve_forge", "projects");
const RUN_STATE = path.join(HOME, "universal_dragon", "eve_forge", "run_state.json");
const BASE_PORT = 3051;

function safeName(input: string) {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60);
}

function readState() {
  if (!fs.existsSync(RUN_STATE)) return {};
  try {
    return JSON.parse(fs.readFileSync(RUN_STATE, "utf8"));
  } catch {
    return {};
  }
}

function writeState(data: any) {
  fs.writeFileSync(RUN_STATE, JSON.stringify(data, null, 2));
}

function getPort(project: string, state: any) {
  if (state[project]?.port) return state[project].port;

  const usedPorts = Object.values(state)
    .map((item: any) => item.port)
    .filter(Boolean);

  let port = BASE_PORT;
  while (usedPorts.includes(port)) port += 1;
  return port;
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

    const state = readState();
    const port = getPort(project, state);

    if (state[project]?.pid) {
      return NextResponse.json({
        ok: true,
        output: `Project already running\n${project}\nPreview: http://192.168.70.117:${port}`,
        project,
        port,
        url: `http://192.168.70.117:${port}`,
        status: "running",
      });
    }

    const child = spawn(
      "bash",
      ["-lc", `npm install && npm run dev -- -H 0.0.0.0 -p ${port}`],
      {
        cwd: projectDir,
        detached: true,
        stdio: "ignore",
      }
    );

    child.unref();

    state[project] = {
      project,
      pid: child.pid,
      port,
      url: `http://192.168.70.117:${port}`,
      startedAt: new Date().toISOString(),
      status: "running",
    };

    writeState(state);

    return NextResponse.json({
      ok: true,
      output: `✅ Project run started\n${project}\nPort: ${port}\nPreview: ${state[project].url}`,
      ...state[project],
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
    const state = readState();
    const names = Object.keys(state);

    if (names.length === 0) {
      return NextResponse.json({
        ok: true,
        status: "idle",
        output: "No project running yet.",
        state: {},
      });
    }

    const output = names
      .map((name) => {
        const item = state[name];
        return `${name} → ${item.url} pid:${item.pid}`;
      })
      .join("\n");

    return NextResponse.json({
      ok: true,
      output,
      state,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      output: "Could not read run state.",
    });
  }
}
