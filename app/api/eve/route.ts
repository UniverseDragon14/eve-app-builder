import { NextRequest, NextResponse } from "next/server";

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const prompt = String(body.prompt || "").trim();

    if (!prompt) {
      return NextResponse.json({
        ok: false,
        reply: "Tell me one app idea bro.",
      });
    }

    return NextResponse.json({
      ok: true,
      reply: `EVE PLAN READY 🔥

App:
${prompt}

Plan:
1. Mobile-first dashboard
2. Curtain open / close / stop buttons
3. Room / window selection
4. Timer and schedule
5. Safe backend later
6. Rollback before changes

Next approval:
Type: ok build`,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      reply: "EVE API error.",
    });
  }
}
