/**
 * ============================================================
 * GESTÃO DE ROTEIROS — LumiFlow (Modais Otimizados)
 * ============================================================
 */

let etapaIndex = 0;

// ============================================================
// PAINEL / VISIBILIDADE DO MODAL
// ============================================================
function showForm() {
    const panel = document.getElementById('form-panel');
    if (panel) {
        panel.classList.add('is-open');
    }
}

function hideForm() {
    const panel = document.getElementById('form-panel');
    if (panel) {
        panel.classList.remove('is-open');

        // Limpa o formulário de forma segura
        const form = document.getElementById('roteiro-form');
        if (form) form.reset();

        limparEtapas();
    }
}

function limparEtapas() {
    const container = document.getElementById('etapas-list');
    if (container) {
        container.replaceChildren();
    }
    etapaIndex = 0;
}

// ============================================================
// NOVO ROTEIRO
// ============================================================
function novoRoteiroForm() {
    const form = document.getElementById('roteiro-form');
    if (form) {
        form.action = '/dashboard/roteiros';
    }

    const titleEl = document.getElementById('form-title');
    if (titleEl) {
        titleEl.textContent = 'Novo Roteiro';
    }

    const produtoInput = document.getElementById('produtoId');
    if (produtoInput) {
        produtoInput.value = '';
    }

    limparEtapas();
    adicionarEtapa();
    showForm();
}

// ============================================================
// ESTRUTURA E MANIPULAÇÃO DAS ETAPAS (DOM)
// ============================================================
function criarLinhaEtapa(setorId = '', etapaSetorId = '') {
    const index = etapaIndex++;
    const templateSetor = document.getElementById('setor-options-template');

    const row = document.createElement('div');
    row.className = 'etapa-item etapa-row';

    const numeroEtapa = document.createElement('span');
    numeroEtapa.className = 'etapa-num';
    numeroEtapa.textContent = `${index + 1}º`;

    const selectSetor = document.createElement('select');
    selectSetor.name = `passos[${index}].setorId`;
    selectSetor.className = 'form-control setor-select';
    selectSetor.classList.add('etapa-row-select');
    selectSetor.addEventListener('change', () => aoMudarSetor(selectSetor));
    if (templateSetor) {
        Array.from(templateSetor.options).forEach(option => {
            selectSetor.appendChild(option.cloneNode(true));
        });
    }

    const selectEtapa = document.createElement('select');
    selectEtapa.name = `passos[${index}].etapaSetorId`;
    selectEtapa.className = 'form-control etapa-setor-select';
    selectEtapa.classList.add('etapa-row-select');
    const optionEtapa = document.createElement('option');
    optionEtapa.value = '';
    optionEtapa.textContent = 'Selecione o processo...';
    selectEtapa.appendChild(optionEtapa);

    const actions = document.createElement('div');
    actions.className = 'etapa-actions';

    const createActionButton = (label, className, handler) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.className = className;
        button.textContent = label;
        button.classList.add('etapa-action-button');
        button.addEventListener('click', () => handler(button));
        return button;
    };

    const moveUp = createActionButton('↑', 'etapa-move', button => moverEtapa(button, -1));
    const moveDown = createActionButton('↓', 'etapa-move', button => moverEtapa(button, 1));
    const remove = createActionButton('✕', 'btn-del', removerEtapa);
    [moveUp, moveDown].forEach(button => {
    });

    actions.append(moveUp, moveDown, remove);
    row.append(numeroEtapa, selectSetor, selectEtapa, actions);

    if (selectSetor && setorId) {
        selectSetor.value = String(setorId);
    }

    return row;
}

async function adicionarEtapa(setorId = '', etapaSetorId = '') {
    const container = document.getElementById('etapas-list');
    if (!container) return;

    const row = criarLinhaEtapa(setorId, etapaSetorId);
    container.appendChild(row);

    // Se veio um setor pré-selecionado, carrega e seleciona as etapas correspondentes
    if (setorId) {
        const selectSetor = row.querySelector('.setor-select');
        await carregarEtapasDoSetor(selectSetor, etapaSetorId);
    }

    reindexarPassos();
}

function removerEtapa(btn) {
    const container = document.getElementById('etapas-list');
    const item = btn.closest('.etapa-item');

    if (item) {
        item.remove();
    }

    if (container && container.children.length === 0) {
        adicionarEtapa();
    } else {
        reindexarPassos();
    }
}

function moverEtapa(btn, direcao) {
    const item = btn.closest('.etapa-item');
    const container = document.getElementById('etapas-list');

    if (!item || !container) return;

    if (direcao < 0 && item.previousElementSibling) {
        container.insertBefore(item, item.previousElementSibling);
    } else if (direcao > 0 && item.nextElementSibling) {
        container.insertBefore(item.nextElementSibling, item);
    }

    reindexarPassos();
}

function reindexarPassos() {
    document.querySelectorAll('.etapa-item').forEach((item, index) => {
        const numEl = item.querySelector('.etapa-num');
        if (numEl) numEl.textContent = `${index + 1}º`;

        const selectSetor = item.querySelector('.setor-select');
        const selectEtapa = item.querySelector('.etapa-setor-select');

        if (selectSetor) selectSetor.name = `passos[${index}].setorId`;
        if (selectEtapa) selectEtapa.name = `passos[${index}].etapaSetorId`;
    });
}

// ============================================================
// REQUISIÇÕES ASYNC / FETCH
// ============================================================

async function aoMudarSetor(selectSetor) {
    await carregarEtapasDoSetor(selectSetor, '');
}

async function carregarEtapasDoSetor(selectSetor, etapaSetorIdSelecionada) {
    if (!selectSetor) return;

    const setorId = selectSetor.value;
    const row = selectSetor.closest('.etapa-item');
    if (!row) return;

    const selectEtapa = row.querySelector('.etapa-setor-select');
    if (!selectEtapa) return;

    const optionInicial = document.createElement('option');
    optionInicial.value = '';
    optionInicial.textContent = 'Selecione o processo...';
    selectEtapa.replaceChildren(optionInicial);

    if (!setorId) return;

    try {
        const response = await fetch(`/dashboard/setores/${setorId}/etapas`);
        if (response.ok) {
            const etapas = await response.json();
            etapas.forEach(etapa => {
                const option = document.createElement('option');
                option.value = etapa.id;
                option.textContent = etapa.nome;
                if (etapaSetorIdSelecionada && etapa.id == etapaSetorIdSelecionada) {
                    option.selected = true;
                }
                selectEtapa.appendChild(option);
            });
        }
    } catch (e) {
        console.error('Erro ao buscar processos do setor:', e);
    }
}

// ============================================================
// HANDLER E EDIÇÃO DE ROTEIRO
// ============================================================

function handleEditarRoteiro(button) {
    const produtoId = button.getAttribute('data-produto-id');
    if (produtoId) {
        editarRoteiro(produtoId);
    } else {
        console.warn('ID do produto não encontrado no botão de edição.');
    }
}

async function editarRoteiro(produtoId) {
    try {
        const response = await fetch(`/dashboard/roteiros/${produtoId}`);

        if (!response.ok) {
            alert('Não foi possível carregar as informações do roteiro.');
            return;
        }

        const roteiro = await response.json();

        const form = document.getElementById('roteiro-form');
        if (form) {
            form.action = `/dashboard/roteiros/${produtoId}/editar`;
        }

        const titleEl = document.getElementById('form-title');
        if (titleEl) {
            titleEl.textContent = 'Editar Roteiro';
        }

        const produtoInput = document.getElementById('produtoId');
        if (produtoInput) {
            produtoInput.value = roteiro.produtoId;
        }

        limparEtapas();

        if (roteiro.passos && roteiro.passos.length > 0) {
            for (const passo of roteiro.passos) {
                await adicionarEtapa(passo.setorId, passo.etapaSetorId);
            }
        } else {
            await adicionarEtapa();
        }

        showForm();
    } catch (error) {
        console.error('Erro ao editar roteiro:', error);
        alert('Ocorreu um erro ao carregar o formulário de edição.');
    }
}

// ============================================================
// FILTRO DINÂMICO DE BUSCA DE ROTEIROS VIA AJAX (DEBOUNCE)
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
    'use strict';
    initFiltroBuscaRoteiros();
});

function initFiltroBuscaRoteiros() {
    const inputBusca = document.querySelector('.search');
    if (!inputBusca) return;

    const formBusca = inputBusca.closest('form');
    if (!formBusca) return;

    if (inputBusca.value) {
        const val = inputBusca.value;
        inputBusca.focus();
        inputBusca.value = '';
        inputBusca.value = val;
    }

    let timeout = null;

    inputBusca.addEventListener('input', () => {
        clearTimeout(timeout);
        timeout = setTimeout(async () => {
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
                        initFiltroBuscaRoteiros();
                    }
                }
            } catch (error) {
                console.error('Erro na busca dinâmica de roteiros:', error);
                formBusca.submit();
            }
        }, 400);
    });
}