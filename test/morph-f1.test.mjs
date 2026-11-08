import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { runInNewContext } from "node:vm";
import { test } from "node:test";

function fixture() {
  const handlers = new Map();
  const fields = Object.fromEntries(
    ["[data-morph-eyebrow]", "[data-morph-heading]", "[data-morph-description]", "[data-morph-note]"]
      .map(key => [key, { textContent: "" }])
  );
  const stage = { hidden: true };
  const buttons = ["circuito", "estrategia", "analise"].map(id => ({
    id, active: false, pressed: "",
    getAttribute(name) { return name === "data-morph-tab" ? id : null; },
    classList: { toggle(_name, _value) {} },
    setAttribute(name, value) { if (name === "aria-pressed") this.pressed = value; }
  }));
  const panel = { querySelector: key => fields[key] };
  const document = {
    body: { dataset: {} },
    getElementById(id) { return id === "fluxMorphF1" ? stage : id === "morphF1Detail" ? panel : null; },
    querySelectorAll(selector) { return selector === "[data-morph-tab]" ? buttons : []; },
    addEventListener(name, callback) { handlers.set(name, callback); },
  };
  const window = {};
  runInNewContext(readFileSync(new URL("../web/flux-ath/morph-f1.js", import.meta.url), "utf8"), { document, window });
  return { document, stage, fields, buttons, handlers, morph: window.FluxMorph };
}

test("local intent router recognizes F1 but not unrelated requests", () => {
  const { morph } = fixture();
  assert.equal(morph.detectIntent("Flux, vamos falar do GP da F1"), "f1");
  assert.equal(morph.detectIntent("Me mostre a Fórmula 1"), "f1");
  assert.equal(morph.detectIntent("Quero conhecer Interlagos"), "f1");
  assert.equal(morph.detectIntent("Me ajuda com biologia"), null);
});

test("F1 command opens demo mode, updates panels, and returns home", () => {
  const { document, stage, fields, handlers, morph } = fixture();
  const reply = morph.handleCommand("Flux, mostre o GP da F1");
  assert.equal(reply.scene, "f1");
  assert.match(reply.text, /demonstra/i);
  assert.equal(stage.hidden, false);
  assert.equal(document.body.dataset.fluxScene, "f1");
  assert.equal(fields["[data-morph-heading]"].textContent, "Visão do traçado");
  handlers.get("click")({ target: { closest: selector => selector === "[data-morph-tab]" ? { getAttribute: () => "estrategia" } : null } });
  assert.equal(fields["[data-morph-heading]"].textContent, "Estratégia de corrida");
  morph.reset();
  assert.equal(stage.hidden, true);
  assert.equal(document.body.dataset.fluxScene, undefined);
});

test("scene HTML is embedded in the existing home with accessible actions", () => {
  const html = readFileSync(new URL("../web/flux-ath/index.html", import.meta.url), "utf8");
  const app = readFileSync(new URL("../web/flux-ath/app.js", import.meta.url), "utf8");
  assert.match(html, /id="fluxMorphF1"/);
  assert.match(html, /data-morph-launch/);
  assert.match(html, /data-morph-reset/);
  assert.match(html, /morph-f1\.css/);
  assert.match(html, /morph-f1\.js/);
  assert.match(app, /window\.FluxMorph\?\.handleCommand\(clean\)/);
});
