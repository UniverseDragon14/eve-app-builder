import { NextRequest, NextResponse } from "next/server";
import { execFile } from "child_process";
import { promisify } from "util";
import path from "path";

const execFileAsync = promisify(execFile);

const FORGE_DIR = path.join(process.env.HOME || "/home/aslam", "universal_dragon", "eve_forge");
const FORGE_FILE = path.join(FORGE_DIR, "forge.py");

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

export async function GET() {
  try {
    const { stdout } = await execFileAsync("python3", [FORGE_FILE, "list"], {
      cwd: FORGE_DIR,
      timeout: 10000,
    });

    return NextResponse.json({
      ok: true,
      output: stdout,
    });
  } catch (error) {
    return NextResponse.json({
      ok: false,
      output: "Forge list failed. Check EVE Forge path.",
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

    return NextResponse.json({
      ok: true,
      output: stdout,
      project: name,
      previewUrl: "http://192.168.70.117:3051",
    });
  } catch (error) {
    return NextResponse.json({
      ok: false,
      output: "Forge create failed. Project may already exist or Python error.",
    });
  }
}
