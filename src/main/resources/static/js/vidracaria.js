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
    formCards.forEach(card => card.style.display = 'none');

    // 2. Exibe o card selecionado
    const selectedCard = document.getElementById(`tab-${tabName}`);
    if (selectedCard) {
      selectedCard.style.display = 'block';
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

  // Inicialização ao carregar o DOM
  document.addEventListener('DOMContentLoaded', () => {
    initFormProtections();
  });

  return {
    showTab
  };

})();

// Exportação global segura
window.showTab = VidracariaModule.showTab;