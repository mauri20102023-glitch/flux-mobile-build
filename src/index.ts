type FluxMode = "FAST" | "STANDARD" | "DEEP";
type StoredRole = "user" | "assistant";

interface StoredMessage {
  role: StoredRole;
  content: string;
  createdAt: string;
}

interface ChatRequest {
  requestId: string;
  conversationId: string;
  message: string;
  deviceId?: string;
  modality?: "TEXT" | "VOICE";
}

interface ChatResponse {
  content: string;
  mode: FluxMode;
  outputDeviceId: string;
}

interface PairRequest {
  deviceId: string;
  deviceName?: string;
  timestamp: number;
  nonce: string;
  signature: string;
}

interface DurableObjectStorage {
  get<T>(key: string): Promise<T | undefined>;
  put<T>(key: string, value: T): Promise<void>;
}

interface DurableObjectState {
  storage: DurableObjectStorage;
}

interface DurableObjectStub {
  fetch(request: Request): Promise<Response>;
}

interface DurableObjectNamespace {
  getByName(name: string): DurableObjectStub;
}

interface WorkersAi {
  run(
    model: string,
    input: Record<string, unknown>,
    options?: { returnRawResponse?: boolean },
  ): Promise<unknown>;
}

export interface Env {
  FLUX_STATE: DurableObjectNamespace;
  AI?: WorkersAi;
  FLUX_AUTH_TOKEN?: string;
  FLUX_PAIRING_PUBLIC_KEY?: string;
  OPENAI_API_KEY?: string;
  ELEVENLABS_API_KEY?: string;
  ELEVENLABS_AGENT_ID?: string;
  OPENAI_BASE_URL?: string;
  AI_FAST_MODEL?: string;
  AI_STANDARD_MODEL?: string;
  AI_DEEP_MODEL?: string;
  WORKERS_AI_TEXT_MODEL?: string;
  FLUX_SYSTEM_PROMPT?: string;
}

const DEFAULT_INSTRUCTIONS = `Você é o FLUX, uma inteligência pessoal avançada.

Identidade e estilo:
- Responda sempre em português do Brasil, salvo quando o usuário pedir outro idioma.
- Seja inteligente, confiante, calmo, expressivo e natural; sofisticado sem parecer robótico ou corporativo.
- Dê o resultado primeiro. Seja breve no simples e detalhado em decisões, projetos, estudos e análises.
- Use humor sutil quando combinar, pouca formatação e nenhuma gíria forçada.
- Entenda fala ditada, repetições, interrupções e autocorreções pela intenção.
- Quando errar, reconheça, explique a correção e continue sem desculpas repetidas.
- Quando houver base suficiente, escolha a melhor alternativa e justifique. Se faltar um dado decisivo, faça uma pergunta curta.

Comportamento:
- Tenha proatividade moderada: avise o importante e sugira melhorias úteis sem interromper demais.
- Tenha autonomia conservadora: peça confirmação antes de enviar, comprar, publicar, apagar, ligar dispositivos ou realizar outra ação externa.
- Nunca finja que pesquisou, abriu um aplicativo, controlou um dispositivo ou verificou informação atual quando isso não aconteceu.
- Diferencie conhecimento geral de informação atual. Preços, taxas, estoque, regras, notícias e disponibilidade precisam de verificação atual.
- Não diga que funciona offline: a inteligência principal e a FLUX Voice dependem da conexão com o FLUX Core.
- Use apenas memórias e preferências fornecidas de forma segura pelo sistema. Nunca peça senhas ou credenciais em conversa.

Qualidade:
- Responda à intenção real, não apenas às palavras literais.
- Revise mentalmente fatos, contas e contradições antes de responder.
- Não invente detalhes para parecer útil. Declare limitações com clareza e ofereça o próximo passo concreto.`;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

class FluxHttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}

function json(value: unknown, status = 200, extraHeaders: HeadersInit = {}): Response {
  return new Response(JSON.stringify(value), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...extraHeaders,
    },
  });
}

function corsHeaders(): Record<string, string> {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-headers": "authorization, content-type, x-flux-user-id, x-flux-device-id",
    "access-control-allow-methods": "GET, POST, PATCH, DELETE, OPTIONS",
  };
}

function isAuthorized(request: Request, env: Env): boolean {
  if (!env.FLUX_AUTH_TOKEN || env.FLUX_AUTH_TOKEN.length < 32) return false;
  const authorization = request.headers.get("authorization") ?? "";
  return authorization.replace(/^Bearer\s+/i, "") === env.FLUX_AUTH_TOKEN;
}

function safeError(error: unknown): Response {
  if (error instanceof FluxHttpError) {
    return json({ error: error.status === 400 ? "INVALID_REQUEST" : "SERVICE_UNAVAILABLE", message: error.message }, error.status, corsHeaders());
  }
  return json({ error: "INTERNAL_ERROR", message: "O FLUX Core não conseguiu concluir a solicitação." }, 500, corsHeaders());
}

const worker = {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: corsHeaders() });
    if (url.pathname === "/health") {
      return json({ status: "ok", service: "flux-core-edge", version: "1.4.0-agent" }, 200, corsHeaders());
    }
    try {
      const stub = env.FLUX_STATE.getByName("primary-owner");
      const response = await stub.fetch(request);
      const headers = new Headers(response.headers);
      for (const [name, value] of Object.entries(corsHeaders())) headers.set(name, value);
      return new Response(response.body, { status: response.status, headers });
    } catch (error) {
      return safeError(error);
    }
  },
};

export default worker;

export class FluxState {
  constructor(private readonly state: DurableObjectState, private readonly env: Env) {}

  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);
    try {
      if (request.method === "POST" && url.pathname === "/v1/pair") return await this.pair(request);
      if (!(await this.isAuthorized(request))) return json({ error: "UNAUTHORIZED" }, 401);
      if (request.method === "GET" && url.pathname === "/v1/diagnostics") return this.diagnostics();
      if (request.method === "POST" && url.pathname === "/v1/devices/register") return await this.registerDevice(request);
      if (request.method === "POST" && url.pathname === "/v1/chat") return await this.chat(request);
      if (request.method === "POST" && url.pathname === "/v1/voice/session") return await this.voiceSession();
      return json({ error: "NOT_FOUND" }, 404);
    } catch (error) {
      return safeError(error);
    }
  }

  private async pair(request: Request): Promise<Response> {
    if (!this.env.FLUX_PAIRING_PUBLIC_KEY) {
      throw new FluxHttpError(503, "O pareamento seguro ainda não foi ativado.");
    }
    const raw = await this.readObject(request);
    const input: PairRequest = {
      deviceId: this.requiredString(raw.deviceId, "deviceId", 120),
      ...(typeof raw.deviceName === "string" ? { deviceName: raw.deviceName.slice(0, 120) } : {}),
      timestamp: typeof raw.timestamp === "number" ? raw.timestamp : Number.NaN,
      nonce: this.requiredString(raw.nonce, "nonce", 120),
      signature: this.requiredString(raw.signature, "signature", 800),
    };
    if (!Number.isSafeInteger(input.timestamp) || Math.abs(Date.now() - input.timestamp) > 5 * 60 * 1_000) {
      throw new FluxHttpError(400, "Solicitação de pareamento expirada.");
    }
    if (!/^[A-Za-z0-9_-]{24,120}$/.test(input.nonce)) {
      throw new FluxHttpError(400, "nonce inválido.");
    }
    const hour = new Date().toISOString().slice(0, 13);
    const remote = (request.headers.get("cf-connecting-ip") ?? "unknown").slice(0, 80);
    const rateKey = `pair-rate:${remote}:${hour}`;
    const attempts = (await this.state.storage.get<number>(rateKey) ?? 0) + 1;
    await this.state.storage.put(rateKey, attempts);
    if (attempts > 12) return json({ error: "PAIRING_RATE_LIMITED" }, 429);

    const nonceKey = `pair-nonce:${input.nonce}`;
    if (await this.state.storage.get<number>(nonceKey)) {
      return json({ error: "PAIRING_REPLAY_DENIED" }, 401);
    }
    const signedPayload = `flux-pair-v1\n${input.deviceId}\n${input.timestamp}\n${input.nonce}`;
    if (!(await this.verifyPairingSignature(signedPayload, input.signature))) {
      return json({ error: "PAIRING_DENIED" }, 401);
    }
    await this.state.storage.put(nonceKey, input.timestamp);

    const tokenBytes = new Uint8Array(32);
    crypto.getRandomValues(tokenBytes);
    const deviceToken = this.base64Url(tokenBytes);
    const pairedAt = new Date().toISOString();
    await Promise.all([
      this.state.storage.put(`device-auth:${input.deviceId}`, await this.sha256(deviceToken)),
      this.state.storage.put(`device:${input.deviceId}`, {
        deviceId: input.deviceId,
        name: input.deviceName ?? "FLUX Mobile",
        deviceType: "MOBILE",
        platform: "Android",
        online: true,
        pairedAt,
        lastSeen: pairedAt,
        trustLevel: "PAIRED",
      }),
    ]);
    return json({ deviceToken, deviceId: input.deviceId, pairedAt }, 201);
  }

  private async isAuthorized(request: Request): Promise<boolean> {
    const authorization = request.headers.get("authorization") ?? "";
    const token = authorization.replace(/^Bearer\s+/i, "").trim();
    if (token.length < 32) return false;
    if (isAuthorized(request, this.env)) return true;
    const deviceId = (request.headers.get("x-flux-device-id") ?? "").trim();
    if (!deviceId || deviceId.length > 120) return false;
    const expected = await this.state.storage.get<string>(`device-auth:${deviceId}`);
    return Boolean(expected) && this.constantTimeEquals(await this.sha256(token), expected!);
  }

  private diagnostics(): Response {
    const aiReady = Boolean(this.env.OPENAI_API_KEY || this.env.AI);
    const voiceReady = Boolean(this.env.ELEVENLABS_API_KEY && this.env.ELEVENLABS_AGENT_ID);
    return json({
      diagnostics: {
        core: "OK",
        ai: aiReady ? "OK" : "DEGRADED",
        voice: voiceReady ? "OK" : "NOT_CONFIGURED",
        glasses: "NOT_CONFIGURED",
        desktop: "NOT_CONFIGURED",
        tv: "NOT_CONFIGURED",
        realtime: "LIMITED",
        memory: "OK",
        authentication: "DEVICE_PAIRED",
        version: "1.4.0-agent",
        checkedAt: new Date().toISOString(),
      },
      aiProfile: {
        provider: this.env.OPENAI_API_KEY ? "openai" : this.env.AI ? "cloudflare-workers-ai" : "unavailable",
        ready: aiReady,
        endpoint: "secure-cloud",
        usageBased: true,
        models: {
          FAST: this.model("FAST"),
          STANDARD: this.model("STANDARD"),
          DEEP: this.model("DEEP"),
        },
      },
      voiceProfile: {
        provider: voiceReady ? "elevenlabs-agent" : "unavailable",
        official: voiceReady,
        name: voiceReady ? "FLUX ATH" : "unavailable",
      },
    });
  }

  private async registerDevice(request: Request): Promise<Response> {
    const body = await this.readObject(request);
    const deviceId = this.requiredString(body.deviceId, "deviceId", 120);
    const registered = {
      ...body,
      deviceId,
      online: true,
      lastSeen: new Date().toISOString(),
      trustLevel: "PAIRED",
    };
    await this.state.storage.put(`device:${deviceId}`, registered);
    return json(registered, 201);
  }

  private async chat(request: Request): Promise<Response> {
    if (!this.env.OPENAI_API_KEY && !this.env.AI) {
      throw new FluxHttpError(503, "A inteligência do FLUX ainda não foi ativada.");
    }
    const raw = await this.readObject(request);
    const input: ChatRequest = {
      requestId: this.requiredString(raw.requestId, "requestId", 80),
      conversationId: this.requiredString(raw.conversationId, "conversationId", 120),
      message: this.requiredString(raw.message, "message", 20_000),
      ...(typeof raw.deviceId === "string" ? { deviceId: raw.deviceId.slice(0, 120) } : {}),
      ...(raw.modality === "VOICE" || raw.modality === "TEXT" ? { modality: raw.modality } : {}),
    };
    if (!UUID.test(input.requestId)) throw new FluxHttpError(400, "requestId inválido.");

    const idempotencyKey = `request:${input.requestId}`;
    const cached = await this.state.storage.get<ChatResponse>(idempotencyKey);
    if (cached) return json(cached);

    const historyKey = `conversation:${input.conversationId}`;
    const history = (await this.state.storage.get<StoredMessage[]>(historyKey) ?? []).slice(-24);
    const mode = this.selectMode(input.message);
    const content = await this.generate(mode, history, input.message, input.requestId);
    const response: ChatResponse = {
      content,
      mode,
      outputDeviceId: input.deviceId ?? "mobile-primary",
    };
    const now = new Date().toISOString();
    const nextHistory: StoredMessage[] = [
      ...history,
      { role: "user" as const, content: input.message, createdAt: now },
      { role: "assistant" as const, content, createdAt: now },
    ].slice(-24);
    await Promise.all([
      this.state.storage.put(historyKey, nextHistory),
      this.state.storage.put(idempotencyKey, response),
    ]);
    return json(response);
  }

  private async voiceSession(): Promise<Response> {
    const agentId = this.env.ELEVENLABS_AGENT_ID?.trim();
    const apiKey = this.env.ELEVENLABS_API_KEY?.trim();
    if (!agentId || !apiKey) {
      throw new FluxHttpError(503, "O agente de voz FLUX ATH ainda não foi ativado.");
    }
    const endpoint = new URL("https://api.elevenlabs.io/v1/convai/conversation/token");
    endpoint.searchParams.set("agent_id", agentId);
    endpoint.searchParams.set("participant_name", "mauricio");
    const response = await fetch(endpoint, {
      method: "GET",
      headers: { "xi-api-key": apiKey, accept: "application/json" },
    });
    if (!response.ok) {
      const reason = response.status === 401
        ? "A chave da ElevenLabs foi recusada."
        : response.status === 429
          ? "O limite de uso da ElevenLabs foi atingido."
          : `A ElevenLabs respondeu com erro ${response.status}.`;
      throw new FluxHttpError(503, reason);
    }
    const payload = await response.json() as { token?: string; conversation_id?: string };
    if (!payload.token) throw new FluxHttpError(503, "A ElevenLabs não forneceu um token de conversa.");
    return json({
      token: payload.token,
      conversationId: payload.conversation_id ?? "",
      agentId,
      expiresInSeconds: 600,
    });
  }

  private async generate(
    mode: FluxMode,
    history: StoredMessage[],
    message: string,
    requestId: string,
  ): Promise<string> {
    if (!this.env.OPENAI_API_KEY && this.env.AI) {
      return await this.generateWithWorkersAi(mode, history, message);
    }
    const apiUrl = `${(this.env.OPENAI_BASE_URL ?? "https://api.openai.com/v1").replace(/\/$/, "")}/responses`;
    const body = JSON.stringify({
      model: this.model(mode),
      instructions: this.env.FLUX_SYSTEM_PROMPT?.trim() || DEFAULT_INSTRUCTIONS,
      input: [
        ...history.map(({ role, content }) => ({ role, content })),
        { role: "user", content: message },
      ],
      store: false,
      reasoning: { effort: mode === "FAST" ? "low" : mode === "STANDARD" ? "medium" : "high" },
      text: { verbosity: mode === "FAST" ? "low" : mode === "STANDARD" ? "medium" : "high" },
      max_output_tokens: mode === "FAST" ? 1_200 : mode === "STANDARD" ? 3_500 : 8_000,
    });

    let lastError = "falha temporária";
    for (let attempt = 0; attempt < 4; attempt += 1) {
      try {
        const response = await fetch(apiUrl, {
          method: "POST",
          headers: {
            authorization: `Bearer ${this.env.OPENAI_API_KEY}`,
            "content-type": "application/json",
            "x-client-request-id": requestId,
          },
          body,
        });
        if (response.ok) {
          const data = await response.json() as {
            output_text?: string;
            output?: Array<{ content?: Array<{ text?: string; refusal?: string }> }>;
          };
          const content = data.output_text?.trim() ?? data.output
            ?.flatMap((item) => item.content ?? [])
            .map((part) => part.text ?? part.refusal ?? "")
            .join("")
            .trim();
          if (!content) throw new Error("resposta vazia");
          return content;
        }
        lastError = `HTTP ${response.status}`;
        const retryable = response.status === 408 || response.status === 409 || response.status === 429 || response.status >= 500;
        if (!retryable || attempt === 3) break;
        await this.pause(this.retryAfterMs(response.headers.get("retry-after")) ?? this.backoffMs(attempt));
      } catch (error) {
        lastError = error instanceof Error ? error.message : "falha de rede";
        if (attempt === 3) break;
        await this.pause(this.backoffMs(attempt));
      }
    }
    throw new FluxHttpError(503, `A inteligência do FLUX está temporariamente indisponível (${lastError}).`);
  }

  private async generateWithWorkersAi(
    mode: FluxMode,
    history: StoredMessage[],
    message: string,
  ): Promise<string> {
    const model = this.env.WORKERS_AI_TEXT_MODEL ?? "@cf/openai/gpt-oss-20b";
    const messages = [
      { role: "system", content: this.env.FLUX_SYSTEM_PROMPT?.trim() || DEFAULT_INSTRUCTIONS },
      ...history.map(({ role, content }) => ({ role, content })),
      { role: "user", content: message },
    ];
    let lastError = "falha temporária";
    for (let attempt = 0; attempt < 3; attempt += 1) {
      try {
        const raw = await this.env.AI!.run(model, {
          messages,
          max_tokens: mode === "FAST" ? 700 : mode === "STANDARD" ? 1_800 : 3_500,
          temperature: mode === "FAST" ? 0.25 : 0.45,
        }) as {
          response?: string;
          output_text?: string;
          result?: { response?: string };
          choices?: Array<{ message?: { content?: string } }>;
          output?: Array<{ content?: Array<{ text?: string; refusal?: string }> }>;
        };
        const content = (
          raw?.response
          ?? raw?.output_text
          ?? raw?.result?.response
          ?? raw?.choices?.[0]?.message?.content
          ?? raw?.output
            ?.flatMap((item) => item.content ?? [])
            .map((part) => part.text ?? part.refusal ?? "")
            .join("")
          ?? ""
        ).trim();
        if (content) return content;
        lastError = "resposta vazia";
      } catch (error) {
        lastError = error instanceof Error ? error.message : "falha de rede";
      }
      if (attempt < 2) await this.pause(this.backoffMs(attempt));
    }
    throw new FluxHttpError(503, `A inteligência do FLUX está temporariamente indisponível (${lastError}).`);
  }

  private selectMode(message: string): FluxMode {
    const normalized = message.toLocaleLowerCase("pt-BR");
    const deepSignals = [
      "analise", "análise", "estratégia", "compare", "comparação", "decisão", "planeje",
      "projeto", "investigue", "calcule", "orçamento", "contrato", "arquitetura", "diagnostique",
    ];
    if (message.length > 700 || deepSignals.some((signal) => normalized.includes(signal))) return "DEEP";
    const fastSignals = ["abra ", "ligue ", "desligue ", "que horas", "lembre", "adicione", "toque "];
    if (message.length < 100 && fastSignals.some((signal) => normalized.startsWith(signal))) return "FAST";
    return "STANDARD";
  }

  private model(mode: FluxMode): string {
    if (mode === "FAST") return this.env.AI_FAST_MODEL ?? "gpt-5.6-luna";
    if (mode === "STANDARD") return this.env.AI_STANDARD_MODEL ?? "gpt-5.6-terra";
    return this.env.AI_DEEP_MODEL ?? "gpt-5.6-sol";
  }

  private async readObject(request: Request): Promise<Record<string, unknown>> {
    let value: unknown;
    try {
      value = await request.json();
    } catch {
      throw new FluxHttpError(400, "Corpo JSON inválido.");
    }
    if (!value || typeof value !== "object" || Array.isArray(value)) {
      throw new FluxHttpError(400, "Objeto JSON esperado.");
    }
    return value as Record<string, unknown>;
  }

  private requiredString(value: unknown, field: string, maximum: number): string {
    if (typeof value !== "string" || value.trim().length === 0 || value.length > maximum) {
      throw new FluxHttpError(400, `${field} inválido.`);
    }
    return value.trim();
  }

  private backoffMs(attempt: number): number {
    return 350 * (2 ** attempt) + Math.floor(Math.random() * 150);
  }

  private retryAfterMs(value: string | null): number | undefined {
    if (!value) return undefined;
    const seconds = Number(value);
    if (Number.isFinite(seconds)) return Math.max(0, seconds * 1_000);
    const date = Date.parse(value);
    return Number.isNaN(date) ? undefined : Math.max(0, date - Date.now());
  }

  private async pause(milliseconds: number): Promise<void> {
    await new Promise((resolve) => setTimeout(resolve, milliseconds));
  }

  private async sha256(value: string): Promise<string> {
    const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
    return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
  }

  private async verifyPairingSignature(payload: string, signature: string): Promise<boolean> {
    try {
      const publicKey = await crypto.subtle.importKey(
        "spki",
        this.decodeBase64(this.env.FLUX_PAIRING_PUBLIC_KEY!),
        { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
        false,
        ["verify"],
      );
      return await crypto.subtle.verify(
        "RSASSA-PKCS1-v1_5",
        publicKey,
        this.decodeBase64(signature),
        new TextEncoder().encode(payload),
      );
    } catch {
      return false;
    }
  }

  private decodeBase64(value: string): Uint8Array {
    const binary = atob(value.replace(/-/g, "+").replace(/_/g, "/"));
    return Uint8Array.from(binary, (character) => character.charCodeAt(0));
  }

  private constantTimeEquals(left: string, right: string): boolean {
    if (left.length !== right.length) return false;
    let difference = 0;
    for (let index = 0; index < left.length; index += 1) {
      difference |= left.charCodeAt(index) ^ right.charCodeAt(index);
    }
    return difference === 0;
  }

  private base64Url(value: Uint8Array): string {
    let binary = "";
    for (const byte of value) binary += String.fromCharCode(byte);
    return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }
}
