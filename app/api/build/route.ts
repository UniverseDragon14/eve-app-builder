import { NextRequest, NextResponse } from "next/server";
import { execFile } from "child_process";
import { promisify } from "util";
import fs from "fs";
import path from "path";

const execFileAsync = promisify(execFile);

const HOME = process.env.HOME || "/home/aslam";
const PROJECTS_DIR = path.join(HOME, "universal_dragon", "eve_forge", "projects");
const BUILD_LOG_DIR = path.join(HOME, "universal_dragon", "eve_forge", "build_logs");

function safeName(input: string) {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 60);
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

    fs.mkdirSync(BUILD_LOG_DIR, { recursive: true });

    const startedAt = new Date().toISOString();

    const { stdout, stderr } = await execFileAsync(
      "bash",
      ["-lc", "npm install && npm run build"],
      {
        cwd: projectDir,
        timeout: 120000,
        maxBuffer: 1024 * 1024 * 5,
      }
    );

    const log = `BUILD SUCCESS
Project: ${project}
Time: ${startedAt}

STDOUT:
${stdout}

STDERR:
${stderr}
`;

    fs.writeFileSync(path.join(BUILD_LOG_DIR, `${project}.log`), log);

    return NextResponse.json({
      ok: true,
      output: `✅ Build success\nProject: ${project}`,
      log,
    });
  } catch (error: any) {
    return NextResponse.json({
      ok: false,
      output:
        "❌ Build failed. Check code errors.\n\n" +
        String(error?.stdout || "") +
        "\n" +
        String(error?.stderr || "") +
        "\n" +
        String(error?.message || ""),
    });
  }
}
