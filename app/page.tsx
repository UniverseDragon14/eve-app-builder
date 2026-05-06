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

export default function Home() {
  const [projectName, setProjectName] = useState("my-first-app");
  const [template, setTemplate] = useState("booking");
  const [forgeOutput, setForgeOutput] = useState("EVE Forge ready.");
  const [projectList, setProjectList] = useState("Loading projects...");
  const [loading, setLoading] = useState(false);

  async function loadProjects() {
    try {
      const res = await fetch("/api/forge");
      const data = await res.json();
      setProjectList(data.output || "No output.");
    } catch {
      setProjectList("Failed to load projects.");
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

  useEffect(() => {
    loadProjects();
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
            Replit + Vercel style project factory. Created by Aslam.
          </p>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <div className="rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
              <p className="text-xs text-zinc-400">System</p>
              <p className="font-bold text-green-400">ONLINE</p>
            </div>

            <div className="rounded-2xl border border-orange-500/30 bg-orange-500/10 p-4">
              <p className="text-xs text-zinc-400">Engine</p>
              <p className="font-bold text-orange-300">EVE FORGE</p>
            </div>

            <div className="rounded-2xl border border-zinc-700 bg-black p-4">
              <p className="text-xs text-zinc-400">Domain</p>
              <p className="font-bold">universaldragon.com</p>
            </div>
          </div>
        </header>

        <div className="mt-5 grid gap-4 lg:grid-cols-[380px_1fr]">
          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-5">
            <h2 className="text-2xl font-black text-orange-300">
              Create Project
            </h2>

            <p className="mt-2 text-sm text-zinc-500">
              This creates real project files inside EVE Forge.
            </p>

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
              Refresh Project List
            </button>
          </section>

          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-5">
            <h2 className="text-2xl font-black text-orange-300">
              Forge Console
            </h2>

            <pre className="mt-4 min-h-[220px] overflow-auto rounded-2xl border border-orange-500/20 bg-orange-500/10 p-4 text-sm whitespace-pre-wrap text-orange-100">
{forgeOutput}
            </pre>

            <h2 className="mt-5 text-2xl font-black text-orange-300">
              Projects
            </h2>

            <pre className="mt-4 min-h-[260px] overflow-auto rounded-2xl border border-zinc-800 bg-black p-4 text-sm whitespace-pre-wrap text-green-300">
{projectList}
            </pre>

            <div className="mt-5 rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
              <p className="font-bold text-green-400">
                Next modules: Run Project, Preview URL, Build, Deploy, Rollback.
              </p>
            </div>
          </section>
        </div>
      </section>
    </main>
  );
}
