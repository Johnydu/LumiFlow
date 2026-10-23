/**
 * Módulo de Gestão da Vidraçaria - LumiFlow
 */
'use strict';

const VidracariaModule = (() => {
  const showTab = (tabName, targetButton) => {
    if (!tabName) return;
    document.querySelectorAll('.content > .form-card').forEach(card => {
      card.classList.add('is-hidden');
      card.classList.remove('is-visible');
    });

    const selectedCard = document.getElementById(`tab-${tabName}`);
    if (selectedCard) {
      selectedCard.classList.remove('is-hidden');
      selectedCard.classList.add('is-visible');
    }

    document.querySelectorAll('.tab').forEach(tab => {
      tab.classList.remove('active');
      tab.setAttribute('aria-selected', 'false');
      tab.tabIndex = -1;
    });
    if (targetButton instanceof HTMLElement) {
      targetButton.classList.add('active');
      targetButton.setAttribute('aria-selected', 'true');
      targetButton.tabIndex = 0;
    }
  };

  const initFormProtections = () => {
    document.querySelectorAll('form:not(#filter-form)').forEach(form => {
      form.addEventListener('submit', () => {
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.innerText = 'Processando...';
        }
      });
    });
  };

  const initHistoricoFilters = () => {
    const form = document.getElementById('filter-form');
    const wrapper = document.getElementById('tabela-historico-wrapper');
    const btnLimpar = document.getElementById('btn-limpar');
    if (!form || !wrapper) return;

    const campoPagina = form.querySelector('input[name="page"]');

    async function atualizarTabela() {
      const params = new URLSearchParams(new FormData(form)).toString();
      try {
        const response = await fetch(`${form.action}?${params}`, {
          headers: { 'X-Requested-With': 'XMLHttpRequest' }
        });
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const html = await response.text();
        const parsedDocument = new DOMParser().parseFromString(html, 'text/html');
        const content = Array.from(parsedDocument.body.childNodes, node =>
          document.importNode(node, true)
        );
        wrapper.replaceChildren(...content);
      } catch (error) {
        console.error('Erro ao atualizar histórico:', error);
        window.location.href = `/dashboard/vidracaria?${params}`;
      }
    }

    form.addEventListener('change', event => {
      if (event.target.matches('select')) {
        if (campoPagina) campoPagina.value = '0';
        atualizarTabela();
      }
    });

    form.addEventListener('filtro:atualizar', event => {
      event.preventDefault();
      atualizarTabela();
    });

    if (btnLimpar) {
      btnLimpar.addEventListener('click', event => {
        event.preventDefault();
        form.querySelectorAll('select').forEach(select => {
          select.value = '';
        });
        if (campoPagina) campoPagina.value = '0';
        atualizarTabela();
      });
    }
  };

  document.addEventListener('DOMContentLoaded', () => {
    initFormProtections();
    initHistoricoFilters();
  });

  return { showTab };
})();

window.showTab = VidracariaModule.showTab;
