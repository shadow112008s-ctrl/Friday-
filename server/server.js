const express = require("express");
const cors = require("cors");
const app = express();

app.use(cors());
app.use(express.json());

const ANTHROPIC_API_KEY = process.env.ANTHROPIC_API_KEY;
const MODEL = "claude-sonnet-4-6";

if (!ANTHROPIC_API_KEY) {
  console.warn("WARNING: ANTHROPIC_API_KEY is not set. Requests will fail until you set it.");
}

async function callClaude({ system, messages, useSearch }) {
  const body = {
    model: MODEL,
    max_tokens: 1000,
    system,
    messages,
  };
  if (useSearch) body.tools = [{ type: "web_search_20250305", name: "web_search" }];

  const res = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "x-api-key": ANTHROPIC_API_KEY,
      "anthropic-version": "2023-06-01",
    },
    body: JSON.stringify(body),
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(`Claude API error ${res.status}: ${errText}`);
  }
  const data = await res.json();
  const text = (data.content || [])
    .filter((b) => b.type === "text")
    .map((b) => b.text)
    .join("\n\n");
  return text || "(no response)";
}

app.post("/chat", async (req, res) => {
  try {
    const { message, history = [] } = req.body;
    const messages = [...history, { role: "user", content: message }];
    const reply = await callClaude({
      system:
        "You are Friday, a concise personal assistant answering from a phone widget. Keep replies short — a few sentences at most, since this is read on a small glance view.",
      messages,
      useSearch: true,
    });
    res.json({ reply });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

app.post("/monitor", async (req, res) => {
  try {
    const { topic } = req.body;
    if (!topic) return res.status(400).json({ error: "Missing 'topic'." });
    const reply = await callClaude({
      system:
        "You are Friday in Monitor mode, writing for a small home-screen widget. Search the web and return the single most important new development on this topic in one short sentence, plus one supporting sentence max. If nothing meaningfully new, say so plainly.",
      messages: [{ role: "user", content: `Topic to check: ${topic}` }],
      useSearch: true,
    });
    res.json({ summary: reply });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: err.message });
  }
});

app.get("/health", (req, res) => res.json({ ok: true }));

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`Friday backend running on port ${PORT}`));
