"use client";

import { useState } from "react";

const starterIdeas = [
  "create booking app",
  "create portfolio website",
  "create shop website",
  "create restaurant menu app",
  "create invoice tracker",
  "create AI assistant website",
];

export default function Home() {
  const [prompt, setPrompt] = useState("create booking app");
  const [reply, setReply] = useState(
    "Hi Aslam bro. EVE Studio ready. Type any app idea, press Ask EVE, then type ok build."
  );
  const [appType, setAppType] = useState("Waiting");
  const [buildReady, setBuildReady] = useState(false);
  const [loading, setLoading] = useState(false);

  async function askEve(text?: string) {
    const finalPrompt = text || prompt;
    setPrompt(finalPrompt);
    setLoading(true);
    setReply("EVE thinking...");

    try {
      const res = await fetch("/api/eve", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ prompt: finalPrompt }),
      });

      const data = await res.json();
      setReply(data.reply || "No reply from EVE.");
      setAppType(data.appType || "Unknown");
      setBuildReady(Boolean(data.buildReady));
    } catch {
      setReply("EVE API connection failed.");
    }

    setLoading(false);
  }

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
            Comment → Plan → Build Preview. Created by Aslam for universaldragon.com
          </p>

          <div className="mt-5 grid gap-3 md:grid-cols-3">
            <div className="rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
              <p className="text-xs text-zinc-400">System</p>
              <p className="font-bold text-green-400">ONLINE</p>
            </div>

            <div className="rounded-2xl border border-orange-500/30 bg-orange-500/10 p-4">
              <p className="text-xs text-zinc-400">Mode</p>
              <p className="font-bold text-orange-300">SAFE BUILDER</p>
            </div>

            <div className="rounded-2xl border border-zinc-700 bg-black p-4">
              <p className="text-xs text-zinc-400">Domain</p>
              <p className="font-bold">universaldragon.com</p>
            </div>
          </div>
        </header>

        <div className="mt-5 grid gap-4 xl:grid-cols-[330px_1fr_360px]">
          <aside className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-4">
            <h2 className="text-xl font-black text-orange-300">
              App Ideas
            </h2>

            <div className="mt-4 space-y-2">
              {starterIdeas.map((idea) => (
                <button
                  key={idea}
                  onClick={() => askEve(idea)}
                  className="w-full rounded-2xl border border-zinc-800 bg-black p-3 text-left text-sm text-zinc-300 hover:border-orange-500"
                >
                  {idea}
                </button>
              ))}
            </div>

            <div className="mt-4 rounded-2xl border border-zinc-800 bg-black p-4">
              <p className="text-xs text-zinc-500">Detected App</p>
              <p className="mt-1 font-bold text-orange-300">{appType}</p>
            </div>

            <div className="mt-4 rounded-2xl border border-zinc-800 bg-black p-4">
              <p className="text-xs text-zinc-500">Rollback</p>
              <p className="mt-1 font-bold text-green-400">Ready</p>
            </div>
          </aside>

          <section className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h2 className="text-xl font-black">Generated Preview</h2>
                <p className="text-xs text-zinc-500">
                  Safe browser concept. Real files later with approval.
                </p>
              </div>

              <span className="rounded-full bg-orange-500 px-3 py-1 text-xs font-bold text-black">
                PREVIEW
              </span>
            </div>

            {!buildReady ? (
              <div className="mt-5 rounded-[2rem] border border-dashed border-zinc-700 bg-black p-8 text-center">
                <p className="text-5xl">🐉</p>
                <h3 className="mt-4 text-3xl font-black">
                  Waiting for ok build
                </h3>
                <p className="mt-2 text-zinc-500">
                  Ask EVE for a plan, then type ok build to generate preview.
                </p>
              </div>
            ) : (
              <div className="mt-5 rounded-[2rem] border border-orange-500/30 bg-black p-5">
                <p className="text-xs tracking-[0.35em] text-orange-400">
                  GENERATED APP
                </p>

                <h3 className="mt-3 text-3xl font-black">{appType}</h3>

                <p className="mt-2 text-zinc-400">
                  Public-ready app concept generated by EVE Studio.
                </p>

                <div className="mt-5 grid gap-3 md:grid-cols-3">
                  <button className="rounded-2xl bg-orange-500 p-4 font-black text-black">
                    Main Action
                  </button>
                  <button className="rounded-2xl border border-zinc-700 p-4 font-bold">
                    Secondary
                  </button>
                  <button className="rounded-2xl border border-red-500/40 bg-red-500/10 p-4 font-bold text-red-300">
                    Stop / Cancel
                  </button>
                </div>

                <div className="mt-5 grid gap-3 md:grid-cols-2">
                  <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
                    <p className="text-xs text-zinc-500">Input Form</p>
                    <div className="mt-3 space-y-2">
                      <div className="h-10 rounded-xl bg-zinc-900" />
                      <div className="h-10 rounded-xl bg-zinc-900" />
                      <div className="h-10 rounded-xl bg-zinc-900" />
                    </div>
                  </div>

                  <div className="rounded-2xl border border-zinc-800 bg-zinc-950 p-4">
                    <p className="text-xs text-zinc-500">Dashboard</p>
                    <div className="mt-3 space-y-2">
                      <div className="rounded-xl bg-zinc-900 p-3 text-sm text-zinc-400">
                        Item / record 01
                      </div>
                      <div className="rounded-xl bg-zinc-900 p-3 text-sm text-zinc-400">
                        Item / record 02
                      </div>
                      <div className="rounded-xl bg-zinc-900 p-3 text-sm text-zinc-400">
                        Item / record 03
                      </div>
                    </div>
                  </div>
                </div>

                <div className="mt-5 rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
                  <p className="font-bold text-green-400">
                    Ready for GitHub / Vercel public release path
                  </p>
                </div>
              </div>
            )}
          </section>

          <aside className="rounded-[2rem] border border-zinc-800 bg-zinc-950/90 p-4">
            <h2 className="text-xl font-black text-orange-300">
              EVE Assistant
            </h2>

            <div className="mt-4 rounded-2xl border border-orange-500/20 bg-orange-500/10 p-4">
              <p className="whitespace-pre-wrap text-sm text-orange-100">
                {reply}
              </p>
            </div>

            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Type app idea or ok build..."
              className="mt-4 h-36 w-full resize-none rounded-2xl border border-zinc-800 bg-black p-4 text-white outline-none focus:border-orange-500"
            />

            <button
              type="button"
              onClick={() => askEve()}
              disabled={loading}
              className="mt-4 w-full rounded-2xl bg-orange-500 py-4 text-xl font-black text-black active:scale-95 disabled:opacity-50"
            >
              {loading ? "Thinking..." : "Ask EVE"}
            </button>

            <div className="mt-4 rounded-2xl border border-zinc-800 bg-black p-4">
              <p className="text-xs font-bold uppercase tracking-widest text-zinc-500">
                Flow
              </p>

              <div className="mt-3 space-y-2 text-sm text-zinc-400">
                <p>1. Type app idea</p>
                <p>2. Ask EVE</p>
                <p>3. Type ok build</p>
                <p>4. Preview generated</p>
                <p>5. Real file build later</p>
              </div>
            </div>
          </aside>
        </div>
      </section>
    </main>
  );
}
