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
// FILTRO DINÂMICO DE BUSCA E SETOR VIA AJAX
// ========================
document.addEventListener('DOMContentLoaded', () => {
    'use strict';
    initFiltroOperadores();
});

function initFiltroOperadores() {
    const inputBusca = document.querySelector('.search');
    const selectSetor = document.querySelector('select[name="setorId"]');

    if (!inputBusca && !selectSetor) return;

    const formBusca = inputBusca ? inputBusca.closest('form') : selectSetor.closest('form');
    if (!formBusca) return;

    // Mantém o foco e a posição do cursor no input de busca se houver valor preenchido
    if (inputBusca && inputBusca.value) {
        const val = inputBusca.value;
        inputBusca.focus();
        inputBusca.value = '';
        inputBusca.value = val;
    }

    let timeout = null;

    // Função central que executa a requisição AJAX considerando os filtros atuais do formulário
    const dispararBuscaAsync = (isDebounced = true) => {
        const executar = async () => {
            const formData = new FormData(formBusca);
            const params = new URLSearchParams(formData).toString();
            const url = `${formBusca.action}?${params}`;

            try {
                const response = await fetch(url, {
                    headers: {
                        'X-Requested-With': 'XMLHttpRequest'
                    }
                });

                if (response.ok) {
                    const htmlText = await response.text();
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(htmlText, 'text/html');

                    const novaLista = doc.querySelector('.lista');
                    const listaAtual = document.querySelector('.lista');

                    if (novaLista && listaAtual) {
                        listaAtual.replaceWith(novaLista);
                    }
                }
            } catch (error) {
                console.error('Erro na busca dinâmica, enviando via submit normal:', error);
                formBusca.submit();
            }
        };

        if (isDebounced) {
            clearTimeout(timeout);
            timeout = setTimeout(executar, 400); // 400ms de atraso para o campo de texto
        } else {
            clearTimeout(timeout);
            executar(); // Execução imediata para a seleção do dropdown de setor
        }
    };

    // Evento de digitação no campo de nome (com debounce)
    if (inputBusca) {
        inputBusca.addEventListener('input', () => dispararBuscaAsync(true));
    }

    // Evento de alteração no select de setores (sem atraso, resposta imediata)
    if (selectSetor) {
        selectSetor.addEventListener('change', () => dispararBuscaAsync(false));
    }
}