#!/usr/bin/env node
/**
 * FLUX / Sign in with ChatGPT — local, opt-in feasibility test.
 * Node.js >= 20. No credentials or API keys are shipped in this repository.
 * Only run on a computer you control. Not for deployment on Cloudflare Workers.
 */
import { createServer } from "node:http";
import { createHash, createPublicKey, randomBytes, randomUUID, verify as verifySignature, constants } from "node:crypto";
import { mkdir, readFile, writeFile, chmod } from "node:fs/promises";
import { homedir } from "node:os";
import { join } from "node:path";

const ISSUER = "https://auth.openai.com";
const TOKEN_URL = ISSUER + "/api/accounts/oauth/token";
const RESOURCE = "https://api.openai.com/v1";
const STORE = join(homedir(), ".flux", "siwc-local.json");
const random = () => randomBytes(32).toString("base64url");
const sha256 = (text) => createHash("sha256").update(text).digest("base64url");

async function getJson(url, init = {}) {
  const res = await fetch(url, { ...init, signal: AbortSignal.timeout(30000) });
  if (!res.ok) {
    // Do not print responses from authorization/token endpoints, as they can contain sensitive data.
    throw new Error("Requisição não autorizada/concluída: HTTP " + res.status + " (" + new URL(url).hostname + ")");
  }
  return res.json();
}

async function readState() {
  try { return JSON.parse(await readFile(STORE, "utf8")); }
  catch (err) {
    if (err.code === "ENOENT") return null;
    throw err;
  }
}

async function saveState(value) {
  await mkdir(join(homedir(), ".flux"), { recursive: true, mode: 0o700 });
  await writeFile(STORE, JSON.stringify(value, null, 2), { mode: 0o600 });
  await chmod(STORE, 0o600);
}

function parseJwt(jwt) {
  const parts = jwt?.split(".");
  if (parts?.length !== 3) throw new Error("ID token inválido.");
  const [header, body, signature] = parts;
  return {
    header: JSON.parse(Buffer.from(header, "base64url").toString("utf8")),
    claims: JSON.parse(Buffer.from(body, "base64url").toString("utf8")),
    signed: Buffer.from(header + "." + body),
    signature: Buffer.from(signature, "base64url"),
  };
}

async function validateIdToken(jwt, expectedClientId, nonce) {
  const { header, claims, signed, signature } = parseJwt(jwt);
  if (!["RS256", "PS256", "ES256"].includes(header.alg)) throw new Error("Algoritmo ID token não permitido.");
  const discovery = await getJson(ISSUER + "/.well-known/openid-configuration");
  if (discovery.issuer !== ISSUER || !discovery.jwks_uri?.startsWith(ISSUER + "/")) {
    throw new Error("Metadados OIDC não confiáveis.");
  }
  const jwks = await getJson(discovery.jwks_uri);
  const jwk = jwks.keys?.find(k => k.kid === header.kid && (!k.alg || k.alg === header.alg) && (!k.use || k.use === "sig"));
  if (!jwk) throw new Error("Chave de assinatura OIDC não encontrada.");
  const key = createPublicKey({ key: jwk, format: "jwk" });
  const algorithm = header.alg === "ES256" ? "sha256" : "RSA-SHA256";
  const options = header.alg === "ES256" ? { key, dsaEncoding: "ieee-p1363" }
    : header.alg === "PS256" ? { key, padding: constants.RSA_PKCS1_PSS_PADDING, saltLength: 32 } : key;
  if (!verifySignature(algorithm, signed, options, signature)) throw new Error("Assinatura do ID token inválida.");
  const now = Math.floor(Date.now() / 1000);
  const audience = Array.isArray(claims.aud) ? claims.aud : [claims.aud];
  if (claims.iss !== ISSUER || !audience.includes(expectedClientId) ||
      claims.nonce !== nonce || !claims.sub || claims.exp <= now ||
      (claims.iat && claims.iat > now + 300)) {
    throw new Error("Validação de identidade OIDC falhou.");
  }
  return claims;
}

async function login() {
  let previous = await readState();
  const hostId = previous?.host_id || "urn:uuid:" + randomUUID();
  // Persist the stable host ID before registration, even if sign-in is cancelled.
  if (!previous) { previous = { host_id: hostId }; await saveState(previous); }
  const state = random(), nonce = random(), verifier = random();
  // Open loopback listener and preserve it until authorization returns.
  let done, fail;
  const authorization = new Promise((resolve, reject) => { done = resolve; fail = reject; });
  const server = createServer((req, res) => {
    const uri = new URL(req.url || "/", "http://127.0.0.1");
    if (uri.pathname !== "/auth/callback") { res.writeHead(404); res.end(); return; }
    const match = uri.searchParams.get("state") === state;
    const code = uri.searchParams.get("code");
    res.writeHead(match && code ? 200 : 400, { "content-type": "text/plain; charset=utf-8", "cache-control": "no-store" });
    res.end(match && code ? "FLUX conectado. Volte ao terminal." : "Autorização não concluída.");
    if (!match) fail(new Error("OAuth state inválido."));
    else if (uri.searchParams.get("error")) fail(new Error("Autorização recusada."));
    else if (!code) fail(new Error("Código ausente."));
    else done({ code, issuedClientId: uri.searchParams.get("client_id") });
  });
  await new Promise((resolve, reject) => {
    server.once("error", reject);
    server.listen(0, "127.0.0.1", resolve);
  });
  const redirectUri = "http://127.0.0.1:" + server.address().port + "/auth/callback";
  const isNew = !previous.client_id;
  const params = new URLSearchParams({
    client_id: isNew ? "dynamic_agent_client" : previous.client_id,
    ext_agent_host_id: hostId,
    response_type: "code", redirect_uri: redirectUri,
    scope: "openid profile email offline_access resource.invoke chatgpt.tokens.use.direct",
    resource: RESOURCE, state, nonce,
    code_challenge_method: "S256", code_challenge: sha256(verifier),
  });
  if (isNew) params.set("agent_name_hint", "FLUX");
  console.log("\nAbra este link no navegador deste mesmo computador e autorize o FLUX:\n");
  console.log(ISSUER + "/api/accounts/authorize?" + params.toString());
  console.log("\nO link expira em 3 minutos. Nunca envie tokens a terceiros.\n");
  let timeoutId;
  try {
    const result = await Promise.race([
      authorization,
      new Promise((_, reject) => { timeoutId = setTimeout(() => reject(new Error("Tempo de autorização esgotado.")), 180000); }),
    ]);
    const clientId = isNew ? result.issuedClientId : previous.client_id;
    if (!clientId || (isNew && clientId === "dynamic_agent_client") ||
        (!isNew && result.issuedClientId && result.issuedClientId !== clientId)) {
      throw new Error("Registro OAuth sem client_id válido.");
    }
    const tok = await getJson(TOKEN_URL, {
      method: "POST",
      headers: { "content-type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code", client_id: clientId,
        code: result.code, code_verifier: verifier,
        redirect_uri: redirectUri, resource: RESOURCE,
      }),
    });
    const claims = await validateIdToken(tok.id_token, clientId, nonce);
    if (previous.subject && previous.subject !== claims.sub) throw new Error("Conta diferente da autorizada anteriormente.");
    const scopes = (tok.scope || "").split(" ");
    if (!scopes.includes("chatgpt.tokens.use.direct")) throw new Error("A permissão de uso do plano não foi concedida.");
    if (!tok.access_token || !tok.refresh_token) throw new Error("Tokens incompletos.");
    await saveState({
      host_id: hostId, client_id: clientId, subject: claims.sub, email: claims.email || "",
      id_token: tok.id_token, access_token: tok.access_token,
      refresh_token: tok.refresh_token, scope: tok.scope,
      expires_at: Date.now() + (tok.expires_in || 3600) * 1000,
    });
    console.log("Autenticação confirmada. Conta: " + (claims.email || "conta validada"));
    console.log("Agora execute: node experiments/siwc-local/flux-siwc.mjs models");
  } finally {
    clearTimeout(timeoutId);
    server.close();
  }
}

async function authorized() {
  const saved = await readState();
  if (!saved?.client_id || !saved?.access_token || !saved?.refresh_token) {
    throw new Error("Execute o comando login antes de usar o plano.");
  }
  if (Date.now() < saved.expires_at - 120000) return saved;
  const tok = await getJson(TOKEN_URL, {
    method: "POST", headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "refresh_token", client_id: saved.client_id,
      refresh_token: saved.refresh_token, resource: RESOURCE,
    }),
  });
  if (!tok.access_token || !tok.refresh_token) throw new Error("Renovação do acesso falhou: faça login novamente.");
  const updated = {
    ...saved, access_token: tok.access_token, refresh_token: tok.refresh_token,
    expires_at: Date.now() + (tok.expires_in || 3600) * 1000,
  };
  await saveState(updated);
  return updated;
}

async function listModels(token) {
  const result = await getJson(RESOURCE + "/models", { headers: { authorization: "Bearer " + token } });
  return (result.models || []).filter(m => m.visibility === "list" && m.slug);
}

async function ask(question) {
  const session = await authorized();
  const models = await listModels(session.access_token);
  const selected = process.env.FLUX_SIWC_MODEL || models[0]?.slug;
  if (!selected || !models.some(m => m.slug === selected)) throw new Error("Modelo indisponível. Execute models e selecione um da lista.");
  const res = await fetch(RESOURCE + "/responses", {
    method: "POST",
    headers: { authorization: "Bearer " + session.access_token, "content-type": "application/json" },
    body: JSON.stringify({
      model: selected, instructions: "Você é o FLUX, assistente pessoal em português brasileiro. Responda com precisão e clareza.",
      input: [{ role: "user", content: question }], store: false, stream: true,
    }),
    signal: AbortSignal.timeout(120000),
  });
  if (!res.ok || !res.body) throw new Error("Falha na inferência: HTTP " + res.status);
  console.log("Modelo: " + selected + "\n");
  const decoder = new TextDecoder();
  let buffer = "", completed = false;
  const reader = res.body.getReader();
  for (;;) {
    const { value, done } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, "\n");
    let end;
    while ((end = buffer.indexOf("\n\n")) !== -1) {
      const frame = buffer.slice(0, end);
      buffer = buffer.slice(end + 2);
      const payload = frame.split("\n").filter(x => x.startsWith("data:")).map(x => x.slice(5).trimStart()).join("\n");
      if (!payload || payload === "[DONE]") continue;
      const e = JSON.parse(payload);
      if (e.type === "response.output_text.delta") process.stdout.write(e.delta || "");
      else if (e.type === "response.completed") completed = true;
      else if (e.type === "response.failed" || e.type === "response.incomplete" || e.type === "error") {
        throw new Error("Resposta não concluída: " + (e.response?.error?.code || e.type));
      }
    }
  }
  if (!completed) throw new Error("Stream terminou sem response.completed; resultado não confirmado.");
  console.log("\n");
}

const [command, ...args] = process.argv.slice(2);
try {
  if (command === "login") await login();
  else if (command === "models") {
    const session = await authorized();
    const models = await listModels(session.access_token);
    if (!models.length) throw new Error("Nenhum modelo elegível retornado pela conta.");
    for (const m of models) console.log(m.slug + " — " + (m.display_name || m.slug));
  } else if (command === "ask" && args.length) await ask(args.join(" "));
  else {
    console.log("FLUX teste local: node experiments/siwc-local/flux-siwc.mjs login | models | ask \"Olá Flux\"");
  }
} catch (error) {
  console.error("FLUX:", error.message);
  process.exitCode = 1;
}
