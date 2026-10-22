/**
 * LumiFlow - Gerenciamento de Operadores
 * Funções para controle do modal centralizado de cadastro e edição.
 */

/**
 * Abre o modal centralizado para cadastrar um novo operador.
 */
window.novoOperadorForm = function() {
    const formPanel = document.getElementById('form-panel');
    const formTitle = document.getElementById('form-title');
    const form = document.getElementById('operador-form');

    if (form) {
        form.reset();
        // Reseta o action para a rota de criação
        form.action = '/dashboard/operadores';
    }

    // Limpa o ID oculto
    const idInput = document.getElementById('id');
    if (idInput) idInput.value = '';

    if (formTitle) formTitle.textContent = 'Novo operador';

    if (formPanel) {
        formPanel.classList.add('is-open');
    }

    // Foco automático no primeiro campo
    const inputNome = document.getElementById('nome');
    if (inputNome) inputNome.focus();
};

/**
 * Preenche e abre o modal centralizado para editar um operador existente.
 * @param {HTMLElement} button - O botão clicado da tabela contendo os atributos data-*
 */
window.handleEditarOperador = function(button) {
    const formPanel = document.getElementById('form-panel');
    const formTitle = document.getElementById('form-title');
    const form = document.getElementById('operador-form');

    // Recupera os valores dos atributos data- do botão da tabela
    const id = button.getAttribute('data-id') || '';
    const nome = button.getAttribute('data-nome') || '';
    const funcao = button.getAttribute('data-funcao') || '';
    const setorId = button.getAttribute('data-setor-id') || '';

    // ATUALIZA A ACTION PARA A ROTA DE EDIÇÃO: /dashboard/operadores/{id}/editar
    if (form && id) {
        form.action = `/dashboard/operadores/${id}/editar`;
    }

    // Preenche os campos do formulário
    const idInput = document.getElementById('id');
    const nomeInput = document.getElementById('nome');
    const funcaoInput = document.getElementById('funcao');
    const setorSelect = document.getElementById('setorPadraoId');

    if (idInput) idInput.value = id;
    if (nomeInput) nomeInput.value = nome;
    if (funcaoInput) funcaoInput.value = funcao;
    if (setorSelect) setorSelect.value = setorId;

    if (formTitle) formTitle.textContent = 'Editar operador';

    if (formPanel) {
        formPanel.classList.add('is-open');
    }
};

/**
 * Oculta o modal centralizado do formulário.
 */
window.hideForm = function() {
    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.classList.remove('is-open');
    }
};

// ========================
// FILTRO DINÂMICO (busca por nome e setor) - ver filtro-ajax.js
// ========================
document.addEventListener('DOMContentLoaded', () => {
    initFiltroAjax({ formSelector: 'form.filtros' });
});
