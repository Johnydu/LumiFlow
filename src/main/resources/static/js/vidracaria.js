/**
 * Módulo de Gestão da Vidraçaria - LumiFlow
 */
'use strict';

const VidracariaModule = (() => {

  /**
   * Alterna a visibilidade dos cards de formulário
   */
  const showTab = (tabName, targetButton) => {
    if (!tabName) return;

    // 1. Esconde todos os cards
    const formCards = document.querySelectorAll('.form-card');
    formCards.forEach(card => card.classList.add('is-hidden'));

    // 2. Exibe o card selecionado
    const selectedCard = document.getElementById(`tab-${tabName}`);
    if (selectedCard) {
      selectedCard.classList.remove('is-hidden');
      selectedCard.classList.add('is-visible');
    }

    // 3. Gerencia classe ativa dos botões
    const tabs = document.querySelectorAll('.tab');
    tabs.forEach(tab => tab.classList.remove('active'));

    if (targetButton && targetButton instanceof HTMLElement) {
      targetButton.classList.add('active');
    }
  };

  /**
   * Previne envio duplo de formulário no chão de fábrica
   */
  const initFormProtections = () => {
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
      // Ignora o formulário de filtros para não desabilitar o botão incorretamente
      if (form.id === 'filter-form') return;

      form.addEventListener('submit', (event) => {
        const submitBtn = form.querySelector('button[type="submit"]');
        if (submitBtn) {
          // Previne duplo clique
          submitBtn.disabled = true;
          submitBtn.innerText = 'Processando...';
        }
      });
    });
  };

  /**
   * Inicializa o comportamento dos filtros do histórico via AJAX
   */
  const initHistoricoFilters = () => {
    const selectTipoMov = document.getElementById("filtroTipoMov");
    const selectTipoVidro = document.getElementById("filtroTipoVidro");
    const btnLimpar = document.getElementById("btn-limpar"); // CORRIGIDO PARA O ID CORRETO
    const wrapper = document.getElementById("tabela-historico-wrapper");

    if (!wrapper) return;

    const atualizarTabela = () => {
      const tipoMov = selectTipoMov ? selectTipoMov.value : "";
      const tipoVidro = selectTipoVidro ? selectTipoVidro.value : "";

      const params = new URLSearchParams();
      if (tipoMov) params.append("tipoMovimentacao", tipoMov);
      if (tipoVidro) params.append("tipoVidroFiltro", tipoVidro);

      fetch(`/dashboard/vidracaria/filtrar-historico?${params.toString()}`)
          .then(response => {
            if (!response.ok) throw new Error("Erro na requisição dos filtros");
            return response.text();
          })
          .then(html => {
            const parsedDocument = new DOMParser().parseFromString(html, 'text/html');
            const content = Array.from(parsedDocument.body.childNodes, node =>
              document.importNode(node, true)
            );
            wrapper.replaceChildren(...content);
          })
          .catch(error => console.error("Erro ao atualizar histórico:", error));
    };

    // Eventos de alteração nos selects
    if (selectTipoMov) {
      selectTipoMov.addEventListener("change", atualizarTabela);
    }
    if (selectTipoVidro) {
      selectTipoVidro.addEventListener("change", atualizarTabela);
    }

    // Ação do botão limpar filtros
    if (btnLimpar) {
      btnLimpar.addEventListener("click", (e) => {
        e.preventDefault(); // Evita qualquer comportamento padrão de submit
        if (selectTipoMov) selectTipoMov.value = "";
        if (selectTipoVidro) selectTipoVidro.value = "";
        atualizarTabela();
      });
    }
  };

  // Inicialização ao carregar o DOM
  document.addEventListener('DOMContentLoaded', () => {
    initFormProtections();
    initHistoricoFilters();
  });

  return {
    showTab
  };

})();

// Exportação global segura
window.showTab = VidracariaModule.showTab;