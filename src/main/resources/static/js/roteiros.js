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

// Funções auxiliares para manipulação de campos (Evita falhas de preenchimento)
function definirTexto(id, texto) {
    const el = document.getElementById(id);
    if (el) el.textContent = texto;
}

function definirValor(id, valor) {
    const el = document.getElementById(id);
    if (el) el.value = valor;
}

// ============================================================
// AÇÕES DA LISTA E MODAIS: EDITAR / DUPLICAR / PRODUTOS / ETC
// ============================================================
document.addEventListener('click', (event) => {
    // Trata tanto data-acao quanto data-csp-action para evitar falhas em qualquer botão
    const alvo = event.target.closest('[data-acao], [data-csp-action]');
    if (!alvo) return;

    const acao = alvo.dataset.acao || alvo.dataset.cspAction;

    switch (acao) {
        case 'editar':
            editarRoteiro(alvo.dataset.roteiroId);
            break;
        case 'duplicar':
            abrirModalDuplicar(alvo);
            break;
        case 'produtos':
            abrirModalVincular(alvo);
            break;
        case 'fechar-modal':
            // Fecha dinamicamente o modal informado no atributo data-modal
            fecharModal(alvo.dataset.modal);
            break;
        case 'hideForm':
            hideForm();
            break;
        case 'adicionarEtapa':
            adicionarEtapa();
            break;
        case 'novoRoteiroForm':
            novoRoteiroForm();
            break;
    }
});

// ============================================================
// NOVO ROTEIRO
// ============================================================
function novoRoteiroForm() {
    const form = document.getElementById('roteiro-form');
    if (form) {
        form.action = '/dashboard/roteiros';
    }

    definirTexto('form-title', 'Novo Roteiro');
    definirValor('codigo', '');
    definirValor('descricao', '');

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

    const labelSetor = document.createElement('label');
    labelSetor.htmlFor = `setorEtapa-${index}`;
    labelSetor.className = 'visually-hidden';
    labelSetor.textContent = `Setor da etapa ${index + 1}`;

    const selectSetor = document.createElement('select');
    selectSetor.id = labelSetor.htmlFor;
    selectSetor.name = `passos[${index}].setorId`;
    selectSetor.className = 'form-control setor-select';
    selectSetor.classList.add('etapa-row-select');
    selectSetor.addEventListener('change', () => aoMudarSetor(selectSetor));
    if (templateSetor) {
        Array.from(templateSetor.options).forEach(option => {
            selectSetor.appendChild(option.cloneNode(true));
        });
    }

    const labelEtapa = document.createElement('label');
    labelEtapa.htmlFor = `processoEtapa-${index}`;
    labelEtapa.className = 'visually-hidden';
    labelEtapa.textContent = `Processo da etapa ${index + 1}`;

    const selectEtapa = document.createElement('select');
    selectEtapa.id = labelEtapa.htmlFor;
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

    actions.append(moveUp, moveDown, remove);
    row.append(numeroEtapa, labelSetor, selectSetor, labelEtapa, selectEtapa, actions);

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

        if (selectSetor) {
            selectSetor.id = `setorEtapa-${index}`;
            selectSetor.name = `passos[${index}].setorId`;
            const label = item.querySelector(`label[for^="setorEtapa-"]`);
            if (label) {
                label.htmlFor = selectSetor.id;
                label.textContent = `Setor da etapa ${index + 1}`;
            }
        }
        if (selectEtapa) {
            selectEtapa.id = `processoEtapa-${index}`;
            selectEtapa.name = `passos[${index}].etapaSetorId`;
            const label = item.querySelector(`label[for^="processoEtapa-"]`);
            if (label) {
                label.htmlFor = selectEtapa.id;
                label.textContent = `Processo da etapa ${index + 1}`;
            }
        }
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

async function editarRoteiro(roteiroId) {
    try {
        const response = await fetch(`/dashboard/roteiros/${roteiroId}`);

        if (!response.ok) {
            alert('Não foi possível carregar as informações do roteiro.');
            return;
        }

        const roteiro = await response.json();

        const form = document.getElementById('roteiro-form');
        if (form) {
            form.action = `/dashboard/roteiros/${roteiroId}/editar`;
        }

        definirTexto('form-title', 'Editar Roteiro');
        definirValor('codigo', roteiro.codigo || '');
        definirValor('descricao', roteiro.descricao || '');

        const produtoInput = document.getElementById('produtoId');
        if (produtoInput) {
            produtoInput.value = roteiro.produtoId || '';
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

// Funções auxiliares para modais específicos (Duplicar e Vincular Produtos)
function abrirModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.add('is-open');
}

function fecharModal(id) {
    const modal = document.getElementById(id);
    if (modal) modal.classList.remove('is-open');
}

function abrirModalDuplicar(botao) {
    const form = document.getElementById('duplicar-form');
    if (!form) return;

    form.action = `/dashboard/roteiros/${botao.dataset.roteiroId}/duplicar`;
    const origemEl = document.getElementById('duplicar-origem');
    if (origemEl) origemEl.textContent = botao.dataset.roteiroCodigo;

    const campo = document.getElementById('duplicar-codigo');
    if (campo) {
        campo.value = '';
        abrirModal('modal-duplicar');
        campo.focus();
    }
}

function abrirModalVincular(botao) {
    const form = document.getElementById('vincular-form');
    if (!form) return;

    form.action = `/dashboard/roteiros/${botao.dataset.roteiroId}/produtos`;
    const origemEl = document.getElementById('vincular-origem');
    if (origemEl) origemEl.textContent = botao.dataset.roteiroCodigo;

    const vinculados = new Set((botao.dataset.produtoIds || '').split(',').filter(Boolean));
    document.querySelectorAll('#vincular-lista input[name="produtoIds"]').forEach(checkbox => {
        checkbox.checked = vinculados.has(checkbox.value);
    });

    const buscaInput = document.getElementById('vincular-busca');
    if (buscaInput) buscaInput.value = '';
    filtrarProdutosVinculo('');
    abrirModal('modal-vincular');
}

function filtrarProdutosVinculo(termo) {
    const busca = termo.trim().toLowerCase();
    document.querySelectorAll('#vincular-lista .produto-check').forEach(item => {
        const visivel = busca === '' || item.dataset.busca.includes(busca);
        item.style.display = visivel ? '' : 'none';
    });
}

document.addEventListener('input', (event) => {
    if (event.target.id === 'vincular-busca') {
        filtrarProdutosVinculo(event.target.value);
    }
});

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