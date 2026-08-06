/**
 * ============================================================
 * GESTÃO DE ROTEIROS — LumiFlow
 * ============================================================
 */

let etapaIndex = 0;

// ============================================================
// PAINEL / VISIBILIDADE DO FORMULÁRIO
// ============================================================
function showForm() {
    const panel = document.getElementById('form-panel');
    if (panel) {
        panel.style.display = 'block';
    }
}

function hideForm() {
    const panel = document.getElementById('form-panel');
    if (panel) {
        panel.style.display = 'none';

        // Limpa o formulário de forma segura
        const form = document.getElementById('roteiro-form');
        if (form) form.reset();

        limparEtapas();
    }
}

function limparEtapas() {
    const container = document.getElementById('etapas-list');
    if (container) {
        container.innerHTML = '';
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
    row.className = 'etapa-item';
    row.style.display = 'flex';
    row.style.gap = '10px';
    row.style.marginBottom = '10px';
    row.style.alignItems = 'center';

    row.innerHTML = `
        <span class="etapa-num" style="font-weight:bold">${index + 1}º</span>

        <!-- Select do Setor -->
        <select name="passos[${index}].setorId" class="etapa-select setor-select" onchange="aoMudarSetor(this)">
            ${templateSetor ? templateSetor.innerHTML : ''}
        </select>

        <!-- Select do Processo / Etapa do Setor -->
        <select name="passos[${index}].etapaSetorId" class="etapa-select etapa-setor-select">
            <option value="">Selecione o processo</option>
        </select>

        <div class="etapa-actions">
            <button type="button" class="etapa-move" onclick="moverEtapa(this, -1)">↑</button>
            <button type="button" class="etapa-move" onclick="moverEtapa(this, 1)">↓</button>
            <button type="button" class="etapa-del" onclick="removerEtapa(this)">✕</button>
        </div>
    `;

    const selectSetor = row.querySelector('.setor-select');
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

    // Se veio um setor pré-selecionado (ex: na edição), carrega e seleciona as etapas correspondentes
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

// Disparado quando o usuário altera o setor na linha
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

    selectEtapa.innerHTML = '<option value="">Selecione o processo</option>';

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
// HANDLER E EDIÇÃO DE ROTEIRO (SEGURANÇA CONTRA XSS)
// ============================================================

/**
 * Handler seguro para acionamento via botão com data-attribute
 * Uso no HTML: <button type="button" th:data-produto-id="${roteiro.produtoId}" onclick="handleEditarRoteiro(this)">Editar</button>
 */
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