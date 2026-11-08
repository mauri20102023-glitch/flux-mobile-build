/* FLUX Morph — primeira cena contextual (Fórmula 1).
   Toda a cena é local, ilustrativa, sem telemetria nem API paga. */
(function () {
  "use strict";
  var SCENE = "f1";
  var tabs = {
    circuito: {
      eyebrow: "01 / CIRCUITO",
      heading: "Visão do traçado",
      description: "Um esquema interativo de pista. O desenho é ilustrativo: não representa o traçado oficial de um GP.",
      note: "A próxima evolução poderá carregar mapas de circuitos verificados."
    },
    estrategia: {
      eyebrow: "02 / ESTRATÉGIA",
      heading: "Estratégia de corrida",
      description: "Pneus, ritmo de prova, entradas nos boxes e condições da pista influenciam cada decisão.",
      note: "Nenhuma estratégia real de uma corrida foi consultada nesta demonstração."
    },
    analise: {
      eyebrow: "03 / CONVERSA",
      heading: "Análise do GP",
      description: "Este painel foi montado pelo comando sobre F1. A análise factual dependerá do Core e de fontes verificadas.",
      note: "Sem resultados, classificação ou previsão inventados."
    }
  };

  function detectIntent(message) {
    if (typeof message !== "string") return null;
    var text = message.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    return /\b(f1|formula[\s-]*1|formula[\s-]*um|grand[\s-]*prix|grande[\s-]*premio|interlagos|gp\s+(da|de|do|em)\s+(f1|formula|sao paulo|brasil|monaco|italia|japao|australia))\b/.test(text) ? SCENE : null;
  }

  function tab(name) {
    var data = tabs[name] || tabs.circuito;
    var panel = document.getElementById("morphF1Detail");
    if (!panel) return;
    var eyebrow = panel.querySelector("[data-morph-eyebrow]");
    var heading = panel.querySelector("[data-morph-heading]");
    var description = panel.querySelector("[data-morph-description]");
    var note = panel.querySelector("[data-morph-note]");
    if (eyebrow) eyebrow.textContent = data.eyebrow;
    if (heading) heading.textContent = data.heading;
    if (description) description.textContent = data.description;
    if (note) note.textContent = data.note;
    document.querySelectorAll("[data-morph-tab]").forEach(function (button) {
      var selected = button.getAttribute("data-morph-tab") === name;
      button.classList.toggle("is-active", selected);
      button.setAttribute("aria-pressed", String(selected));
    });
  }

  function activateF1() {
    var stage = document.getElementById("fluxMorphF1");
    if (!stage) return false;
    stage.hidden = false;
    document.body.dataset.fluxScene = SCENE;
    tab("circuito");
    return true;
  }

  function reset() {
    var stage = document.getElementById("fluxMorphF1");
    if (stage) stage.hidden = true;
    delete document.body.dataset.fluxScene;
  }

  function handleCommand(message) {
    if (detectIntent(message) !== SCENE || !activateF1()) return null;
    return {
      scene: SCENE,
      text: "Modo GP ativado. A interface se reorganizou para explorar circuito, estratégia e análise. Esta é uma visualização local demonstrativa; não consultei resultados ou telemetria ao vivo."
    };
  }

  document.addEventListener("click", function (event) {
    var start = event.target.closest("[data-morph-launch]");
    if (start) { activateF1(); return; }
    var exit = event.target.closest("[data-morph-reset]");
    if (exit) { reset(); return; }
    var selected = event.target.closest("[data-morph-tab]");
    if (selected) tab(selected.getAttribute("data-morph-tab"));
  });

  document.addEventListener("keydown", function (event) {
    if (event.key !== "Escape" || document.body.dataset.fluxScene !== SCENE) return;
    if (event.target.closest("input,textarea,dialog")) return;
    reset();
  });

  // O chat chama este contrato local antes de tentar uma API.
  // No futuro, o Intelligence Router pode retornar a mesma cena com dados verificados.
  window.FluxMorph = Object.freeze({ detectIntent: detectIntent, handleCommand: handleCommand, activateF1: activateF1, reset: reset });
})();
