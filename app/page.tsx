"use client";

import { useEffect, useState } from "react";

const templates = [
  "booking",
  "portfolio",
  "shop",
  "dashboard",
  "assistant",
  "api",
];

function cleanProjectList(raw: string) {
  return raw
    .split("\n")
    .map((line) => line.replace("-", "").trim())
    .filter((line) => line && !line.toLowerCase().includes("no projects"));
}

export default function Home() {
  const [projectName, setProjectName] = useState("my-first-app");
  const [template, setTemplate] = useState("booking");
  const [selectedProject, setSelectedProject] = useState("my-first-app");
  const [forgeOutput, setForgeOutput] = useState("EVE Forge ready.");
  const [projectList, setProjectList] = useState("Loading projects...");
  const [projects, setProjects] = useState<string[]>([]);
  const [runOutput, setRunOutput] = useState("No project running yet.");
  const [previewUrl, setPreviewUrl] = useState("");
  const [loading, setLoading] = useState(false);
  const [running, setRunning] = useState(false);

  async function loadProjects() {
    try {
      const res = await fetch("/api/forge");
      const data = await res.json();
      const output = data.output || "No output.";
      const names = Array.isArray(data.projects)
        ? data.projects
        : cleanProjectList(output);

      setProjectList(output);
      setProjects(names);

      if (names.length > 0 && !names.includes(selectedProject)) {
        setSelectedProject(names[0]);
      }
    } catch {
      setProjectList("Failed to load projects.");
    }
  }

  async function loadRunState() {
    try {
      const res = await fetch("/api/run");
      const data = await res.json();

      setRunOutput(data.output || "No run output.");
      if (data.url) {
        setPreviewUrl(data.url);
      }
    } catch {
      setRunOutput("Failed to load run state.");
    }
  }

  async function createProject() {
    setLoading(true);
    setForgeOutput("EVE Forge creating project...");

    try {
      const res = await fetch("/api/forge", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          template,
          name: projectName,
        }),
      });

      const data = await res.json();
      setForgeOutput(data.output || "No output.");
      await loadProjects();
    } catch {
      setForgeOutput("EVE Forge API failed.");
    }

    setLoading(false);
  }

  async function runProject() {
    setRunning(true);
    setRunOutput("Starting project... first run may take time because npm install runs.");

    try {
      const res = await fetch("/api/run", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          project: selectedProject,
        }),
      });

      const data = await res.json();
      setRunOutput(data.output || "No output.");
      if (data.url) {
        setPreviewUrl(data.url);
      }
    } catch {
      setRunOutput("Run API failed.");
    }

    setRunning(false);
  }


  async function stopProject() {
    setRunning(true);
    setRunOutput("Stopping project...");

    try {
      const res = await fetch("/api/stop", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          project: selectedProject,
        }),
      });

      const data = await res.json();
      setRunOutput(data.output || "No output.");
      await loadRunState();
    } catch {
      setRunOutput("Stop API failed.");
    }

    setRunning(false);
  }

  useEffect(() => {
    loadProjects();
    loadRunState();
  }, []);

  return (
    <main className="min-h-screen bg-black text-white">
      <div className="fixed inset-0 bg-[radial-gradient(circle_at_top_left,#ff5a0028,transparent_35%),radial-gradient(circle_at_bottom_right,#ffb00020,transparent_35%)]" />

      <section className="relative z-10 mx-auto max-w-7xl p-4 md:p-6">
        <header className="rounded-[2rem] border border-orange-500/30 bg-zinc-950/90 p-6 shadow-2xl">
          <p className="text-xs tracking-[0.45em] text-orange-400">
            UNIVERSAL DRAGON
          </p>

          <h1 className="mt-3 text-4xl font-black md:text-6xl">
            EVE Studio
          </h1>

          <p className="mt-2 text-zinc-400">
            Project factory + local preview runner. Created by Aslam.
          </p>

          <div className="mt-5 grid gap-3 md:grid-cols-4">
            <div className="rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
              <p className="text-xs text-zinc-400">System</p>
              <p className="font-bold text-green-400">ONLINE</p>
            </div>

            <div className="rounded-2xl border border-orange-500/30 bg-orange-500/10 p-4">
              <p className="text-xs text-zinc-400">Engine</p>
              <p className="font-bold text-orange-300">EVE FORGE</p>
            </div>

            <div className="rounded-2xl border border-zinc-700 bg-black p-4">
              <p className="text-xs text-zinc-400">Preview</p>
              <p className="font-bold">Port 3051</p>
            </div>

            <div className="rounded-2xl border border-zinc-700 bg-black p-4">
              <p className="text-xs text-zinc-400">Domain</p>
              <p className="font-bold">universaldragon.com</p>
            </div>
          </div>
        </header>

        <div className="mt-5 grid gap-4 xl:grid-cols-[360px_1fr_360px]">
          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-5">
            <h2 className="text-2xl font-black text-orange-300">
              Create Project
            </h2>

            <label className="mt-5 block text-sm font-bold text-zinc-300">
              Project Name
            </label>

            <input
              value={projectName}
              onChange={(e) => setProjectName(e.target.value)}
              className="mt-2 w-full rounded-2xl border border-zinc-800 bg-black p-4 outline-none focus:border-orange-500"
            />

            <label className="mt-5 block text-sm font-bold text-zinc-300">
              Template
            </label>

            <select
              value={template}
              onChange={(e) => setTemplate(e.target.value)}
              className="mt-2 w-full rounded-2xl border border-zinc-800 bg-black p-4 outline-none focus:border-orange-500"
            >
              {templates.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>

            <button
              onClick={createProject}
              disabled={loading}
              className="mt-6 w-full rounded-2xl bg-orange-500 py-4 text-xl font-black text-black active:scale-95 disabled:opacity-50"
            >
              {loading ? "Creating..." : "Create Project"}
            </button>

            <button
              onClick={loadProjects}
              className="mt-3 w-full rounded-2xl border border-zinc-700 py-4 font-bold text-zinc-300"
            >
              Refresh Projects
            </button>
          </section>

          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-5">
            <h2 className="text-2xl font-black text-orange-300">
              Run / Preview
            </h2>

            <label className="mt-5 block text-sm font-bold text-zinc-300">
              Select Project
            </label>

            <select
              value={selectedProject}
              onChange={(e) => setSelectedProject(e.target.value)}
              className="mt-2 w-full rounded-2xl border border-zinc-800 bg-black p-4 outline-none focus:border-orange-500"
            >
              {projects.map((item) => (
                <option key={item}>{item}</option>
              ))}
            </select>

            <button
              onClick={runProject}
              disabled={running}
              className="mt-6 w-full rounded-2xl bg-orange-500 py-4 text-xl font-black text-black active:scale-95 disabled:opacity-50"
            >
              {running ? "Working..." : "Run Selected Project"}
            </button>

            <button
              onClick={stopProject}
              disabled={running}
              className="mt-3 w-full rounded-2xl border border-red-500/40 bg-red-500/10 py-4 font-bold text-red-300 disabled:opacity-50"
            >
              Stop Selected Project
            </button>

            {previewUrl ? (
              <a
                href={previewUrl}
                target="_blank"
                className="mt-3 block rounded-2xl border border-green-500/30 bg-green-500/10 p-4 text-center font-bold text-green-400"
              >
                Open Preview: {previewUrl}
              </a>
            ) : (
              <div className="mt-3 rounded-2xl border border-zinc-800 bg-black p-4 text-center text-zinc-500">
                Preview URL waiting...
              </div>
            )}

            <pre className="mt-4 min-h-[160px] overflow-auto rounded-2xl border border-green-500/20 bg-green-500/10 p-4 text-sm whitespace-pre-wrap text-green-200">
{runOutput}
            </pre>
          </section>

          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-5">
            <h2 className="text-2xl font-black text-orange-300">
              Forge Console
            </h2>

            <pre className="mt-4 min-h-[170px] overflow-auto rounded-2xl border border-orange-500/20 bg-orange-500/10 p-4 text-sm whitespace-pre-wrap text-orange-100">
{forgeOutput}
            </pre>

            <h2 className="mt-5 text-2xl font-black text-orange-300">
              Projects
            </h2>

            <pre className="mt-4 min-h-[220px] overflow-auto rounded-2xl border border-zinc-800 bg-black p-4 text-sm whitespace-pre-wrap text-green-300">
{projectList}
            </pre>

            <div className="mt-5 rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
              <p className="font-bold text-green-400">
                Next: Stop Project, Build, Deploy, Rollback.
              </p>
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
