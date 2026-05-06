"use client";

import { useState } from "react";

export default function Home() {
  const [prompt, setPrompt] = useState("create curtain motor controller app");
  const [reply, setReply] = useState("Hi Aslam bro. EVE ready.");
  const [count, setCount] = useState(0);
  const [loading, setLoading] = useState(false);

  async function askEve() {
    const nextCount = count + 1;
    setCount(nextCount);
    setLoading(true);
    setReply("Button clicked ✅ EVE thinking... click #" + nextCount);

    try {
      const res = await fetch("/api/eve", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ prompt }),
      });

      const data = await res.json();
      setReply(data.reply || "API replied but message empty.");
    } catch {
      setReply("Button works ✅ but API failed. Check terminal error.");
    }

    setLoading(false);
  }

  return (
    <main className="min-h-screen bg-black text-white p-5">
      <section className="mx-auto max-w-4xl">
        <div className="rounded-3xl border border-orange-500/30 bg-zinc-950 p-6">
          <p className="text-xs tracking-[0.4em] text-orange-400">
            UNIVERSAL DRAGON
          </p>

          <h1 className="mt-3 text-4xl font-black">
            EVE App Builder
          </h1>

          <p className="mt-2 text-zinc-400">
            Button test + API connected version.
          </p>

          <div className="mt-5 rounded-2xl border border-green-500/30 bg-green-500/10 p-4">
            <p className="text-sm text-green-300">
              Click Count: {count}
            </p>
          </div>
        </div>

        <div className="mt-5 rounded-3xl border border-zinc-800 bg-zinc-950 p-5">
          <h2 className="text-2xl font-black text-orange-300">
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
            className="mt-4 h-36 w-full resize-none rounded-2xl border border-zinc-800 bg-black p-4 text-white outline-none focus:border-orange-500"
          />

          <button
            type="button"
            onClick={askEve}
            disabled={loading}
            className="mt-4 w-full rounded-2xl bg-orange-500 py-4 text-xl font-black text-black active:scale-95 disabled:opacity-50"
          >
            {loading ? "Thinking..." : "Ask EVE"}
          </button>
        </div>

        <div className="mt-5 rounded-3xl border border-zinc-800 bg-zinc-950 p-5">
          <h2 className="font-black text-orange-300">Status</h2>
          <pre className="mt-3 overflow-auto rounded-2xl bg-black p-4 text-sm text-green-300">
{`✓ Next.js running
✓ Client button test added
✓ API route added
✓ If click count increases, button works
✓ If API fails, terminal will show error`}
          </pre>
        </div>
      </section>
    </main>
  );
}
