import assert from "node:assert/strict";
import { afterEach, test } from "node:test";
import worker, { FluxState } from "../src/index.ts";

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

function core() {
  const values = new Map();
  const storage = {
    get: async key => values.get(key),
    put: async (key, value) => { values.set(key, value); },
    delete: async key => values.delete(key),
    transaction: async callback => callback(storage),
  };
  return new FluxState({ storage }, {
    FLUX_AUTH_TOKEN: "a".repeat(32),
    INWORLD_API_KEY: "test-base64-credential",
  });
}

function request(path, body, type = "application/json") {
  return new Request(`https://flux.example${path}`, {
    method: body === undefined ? "GET" : "POST",
    headers: { authorization: `Bearer ${"a".repeat(32)}`, "x-flux-device-id": "test-device", "content-type": type },
    body: body === undefined ? undefined : type === "application/json" ? JSON.stringify(body) : body,
  });
}

test("chat uses the Inworld router without exposing the credential", async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "https://api.inworld.ai/v1/chat/completions");
    assert.equal(options.headers.authorization, "Basic test-base64-credential");
    const body = JSON.parse(options.body);
    assert.equal(body.model, "openai/gpt-4o-mini");
    assert.equal(body.messages.at(-1).content, "Bom dia");
    return Response.json({ choices: [{ message: { content: "Bom dia, Mauricio." } }] });
  };
  const response = await core().fetch(request("/v1/chat", {
    requestId: "123e4567-e89b-42d3-a456-426614174000",
    conversationId: "conversation-test",
    message: "Bom dia",
  }));
  assert.equal(response.status, 200);
  assert.equal((await response.json()).content, "Bom dia, Mauricio.");
});

test("TTS returns playable MP3 bytes from the selected voice", async () => {
  globalThis.fetch = async (url, options) => {
    assert.equal(url, "https://api.inworld.ai/tts/v1/voice");
    assert.equal(options.headers.authorization, "Basic test-base64-credential");
    const body = JSON.parse(options.body);
    assert.equal(body.voiceId, "keen-koala-9724__design-voice-90827709");
    assert.equal(body.modelId, "inworld-tts-2-flash");
    return Response.json({ audioContent: Buffer.from("test-mp3").toString("base64") });
  };
  const response = await core().fetch(request("/v1/tts", { text: "Olá, Mauricio." }));
  assert.equal(response.status, 200);
  assert.equal(response.headers.get("content-type"), "audio/mpeg");
  assert.equal(await response.text(), "test-mp3");
});

test("realtime proxy authenticates the device and keeps the API key server-side", async () => {
  const seen = [];
  globalThis.fetch = async (url, options) => {
    seen.push([url, options]);
    if (url.endsWith("ice-servers")) return Response.json({ ice_servers: [{ urls: "stun:test.example" }] });
    return new Response("v=0\r\no=answer", { headers: { "content-type": "application/sdp" } });
  };
  const instance = core();
  const denied = await instance.fetch(new Request("https://flux.example/v1/live/ice-servers"));
  assert.equal(denied.status, 401);
  const ice = await instance.fetch(request("/v1/live/ice-servers"));
  const config = await ice.json();
  assert.equal(config.voice, "keen-koala-9724__design-voice-90827709");
  assert.equal(config.iceServers[0].urls, "stun:test.example");
  assert.equal(JSON.stringify(config).includes("test-base64-credential"), false);
  const offer = await instance.fetch(request("/v1/live/offer", "v=0\r\no=offer", "application/sdp"));
  assert.equal(offer.status, 200);
  assert.equal(await offer.text(), "v=0\r\no=answer");
  assert.equal(seen[1][1].headers.authorization, "Bearer test-base64-credential");
});

test("Worker serves the current dynamic interface before stale static assets", async () => {
  const env = { ASSETS: { fetch: () => new Response("old site") } };
  const home = await worker.fetch(new Request("https://flux.example/"), env);
  const script = await worker.fetch(new Request("https://flux.example/app.js"), env);
  assert.match(await home.text(), /id="sceneFeed"/);
  assert.match(await script.text(), /weatherPlaceFromQuery/);
});
