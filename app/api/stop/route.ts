import { NextRequest, NextResponse } from "next/server";
import fs from "fs";
import path from "path";
import { execFile } from "child_process";
import { promisify } from "util";

const execFileAsync = promisify(execFile);

const HOME = process.env.HOME || "/home/aslam";
const RUN_STATE = path.join(HOME, "universal_dragon", "eve_forge", "run_state.json");

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

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const project = safeName(String(body.project || ""));
    const state = readState();

    if (!project || !state[project]) {
      return NextResponse.json({
        ok: false,
        output: "Project is not running.",
      });
    }

    const pid = String(state[project].pid);

    try {
      await execFileAsync("bash", ["-lc", `kill -TERM -${pid} 2>/dev/null || kill -TERM ${pid} 2>/dev/null || true`], {
        timeout: 5000,
      });
    } catch {}

    delete state[project];
    writeState(state);

    return NextResponse.json({
      ok: true,
      output: `✅ Project stopped\n${project}`,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      output: "Stop failed.",
    });
  }
}
