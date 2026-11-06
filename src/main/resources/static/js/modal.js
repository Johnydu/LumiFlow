/**
 * LumiFlow - Comportamento comum dos modais (.modal-overlay).
 *
 * Não abre nem fecha nada por conta própria: cada tela continua alternando a classe
 * "is-open" no overlay. Este módulo apenas reage a essa classe para:
 *  - mover o foco para o primeiro campo ao abrir e devolvê-lo a quem abriu ao fechar;
 *  - fechar com Esc (clicando no botão ".modal-close" do próprio modal);
 *  - manter o Tab dentro do modal aberto.
 * Os atributos role="dialog", aria-modal e aria-labelledby ficam no HTML (.modal-box).
 */
(function () {
  'use strict';

  const SELETOR_FOCAVEL =
    'input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled]), ' +
    'button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])';

  const abertoPor = new WeakMap();

  function modaisAbertos() {
    return Array.from(document.querySelectorAll('.modal-overlay.is-open'));
  }

  function elementosFocaveis(modal) {
    return Array.from(modal.querySelectorAll(SELETOR_FOCAVEL)).filter(el => el.offsetParent !== null);
  }

  function aoAbrir(modal) {
    abertoPor.set(modal, document.activeElement);
    // Espera o navegador aplicar o display do overlay antes de focar.
    requestAnimationFrame(() => {
      const candidatos = elementosFocaveis(modal);
      const alvo = candidatos.find(el => !el.classList.contains('modal-close')) || candidatos[0];
      if (alvo) alvo.focus();
    });
  }

  function aoFechar(modal) {
    const origem = abertoPor.get(modal);
    abertoPor.delete(modal);
    if (origem instanceof HTMLElement && document.contains(origem)) {
      origem.focus();
    }
  }

  new MutationObserver(registros => {
    registros.forEach(registro => {
      const alvo = registro.target;
      if (!(alvo instanceof HTMLElement) || !alvo.classList.contains('modal-overlay')) return;

      const estavaAberto = /(^|\s)is-open(\s|$)/.test(registro.oldValue || '');
      const estaAberto = alvo.classList.contains('is-open');
      if (!estavaAberto && estaAberto) aoAbrir(alvo);
      else if (estavaAberto && !estaAberto) aoFechar(alvo);
    });
  }).observe(document.documentElement, {
    subtree: true,
    attributes: true,
    attributeFilter: ['class'],
    attributeOldValue: true
  });

  document.addEventListener('keydown', event => {
    const abertos = modaisAbertos();
    if (abertos.length === 0) return;
    const modal = abertos[abertos.length - 1];

    if (event.key === 'Escape') {
      const fechar = modal.querySelector('.modal-close');
      if (fechar) {
        event.preventDefault();
        fechar.click();
      }
      return;
    }

    if (event.key === 'Tab') {
      const focaveis = elementosFocaveis(modal);
      if (focaveis.length === 0) return;
      const primeiro = focaveis[0];
      const ultimo = focaveis[focaveis.length - 1];
      if (!modal.contains(document.activeElement)) {
        event.preventDefault();
        primeiro.focus();
      } else if (event.shiftKey && document.activeElement === primeiro) {
        event.preventDefault();
        ultimo.focus();
      } else if (!event.shiftKey && document.activeElement === ultimo) {
        event.preventDefault();
        primeiro.focus();
      }
    }
  });
})();
