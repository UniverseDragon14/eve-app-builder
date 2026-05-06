import { NextRequest, NextResponse } from "next/server";

function detectAppType(prompt: string) {
  const p = prompt.toLowerCase();

  if (p.includes("booking") || p.includes("appointment")) return "Booking System";
  if (p.includes("shop") || p.includes("store") || p.includes("product")) return "Shop / Store";
  if (p.includes("portfolio") || p.includes("personal")) return "Portfolio Website";
  if (p.includes("restaurant") || p.includes("menu")) return "Restaurant Menu";
  if (p.includes("invoice") || p.includes("expense")) return "Invoice / Expense App";
  if (p.includes("chatbot") || p.includes("assistant") || p.includes("ai")) return "AI Assistant App";
  if (p.includes("dashboard") || p.includes("admin")) return "Admin Dashboard";

  return "Custom Web App";
}

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const prompt = String(body.prompt || "").trim();
    const mode = String(body.mode || "plan").trim();

    if (!prompt) {
      return NextResponse.json({
        ok: false,
        reply: "Tell me one app idea bro.",
        appType: "None",
        buildReady: false,
      });
    }

    const appType = detectAppType(prompt);

    if (prompt.toLowerCase().includes("ok build") || mode === "build") {
      return NextResponse.json({
        ok: true,
        appType,
        buildReady: true,
        reply: `EVE BUILD READY 🔥

App Type:
${appType}

Generated Concept:
- Landing screen
- Main app dashboard
- Form / input section
- Data list / result section
- Admin or control area
- Mobile-first layout
- Safe backend placeholder
- Rollback ready
- GitHub/Vercel deploy path

Status:
This is safe browser preview generation.
Real file creation will need approval later.`,
      });
    }

    return NextResponse.json({
      ok: true,
      appType,
      buildReady: false,
      reply: `EVE PLAN READY 🔥

App Idea:
${prompt}

Detected:
${appType}

Build Plan:
1. Understand exact app purpose
2. Create clean public landing page
3. Create main app screen
4. Add input forms and action buttons
5. Add dashboard/list section
6. Keep safe backend placeholder
7. Prepare GitHub/Vercel public release path
8. Keep rollback before file changes

Next:
Type "ok build" to generate preview concept.`,
    });
  } catch {
    return NextResponse.json({
      ok: false,
      reply: "EVE API error. Check terminal logs.",
      appType: "Error",
      buildReady: false,
    });
  }
}
