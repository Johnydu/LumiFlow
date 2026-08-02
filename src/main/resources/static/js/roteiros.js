let etapaIndex = 0;

function showForm() {
    document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
    document.getElementById('form-panel').style.display = 'none';
}

function limparEtapas() {
    const container = document.getElementById('etapas-list');
    container.innerHTML = '';
    etapaIndex = 0;
}

function novoRoteiroForm() {
    const form = document.getElementById('roteiro-form');
    form.action = '/dashboard/roteiros';

    document.getElementById('form-title').textContent = 'Novo Roteiro';
    document.getElementById('produtoId').value = '';

    limparEtapas();
    adicionarEtapa();

    showForm();
}

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
    selectSetor.value = setorId ? String(setorId) : '';

    if (setorId) {
        carregarEtapasDoSetor(selectSetor, etapaSetorId);
    }

    return row;
}

async function adicionarEtapa(setorId = '', etapaSetorId = '') {
    const container = document.getElementById('etapas-list');
    const row = criarLinhaEtapa(setorId, etapaSetorId);
    container.appendChild(row);

    // Se veio um setor pré-selecionado (como na edição), busca e seleciona a etapa correspondente
    if (setorId) {
        const selectSetor = row.querySelector('.setor-select');
        await carregarEtapasDoSetor(selectSetor, etapaSetorId);
    }

    reindexarPassos();
}

function removerEtapa(btn) {
    const container = document.getElementById('etapas-list');
    btn.closest('.etapa-item').remove();

    if (container.children.length === 0) {
        adicionarEtapa();
    } else {
        reindexarPassos();
    }
}

function moverEtapa(btn, direcao) {
    const item = btn.closest('.etapa-item');
    const container = document.getElementById('etapas-list');

    if (direcao < 0 && item.previousElementSibling) {
        container.insertBefore(item, item.previousElementSibling);
    } else if (direcao > 0 && item.nextElementSibling) {
        container.insertBefore(item.nextElementSibling, item);
    }

    reindexarPassos();
}

function reindexarPassos() {
    document.querySelectorAll('.etapa-item').forEach((item, index) => {
        item.querySelector('.etapa-num').textContent = `${index + 1}º`;

        const selectSetor = item.querySelector('.setor-select');
        const selectEtapa = item.querySelector('.etapa-setor-select');

        if (selectSetor) selectSetor.name = `passos[${index}].setorId`;
        if (selectEtapa) selectEtapa.name = `passos[${index}].etapaSetorId`;
    });
}

// Disparado quando o usuário troca o setor na linha
async function aoMudarSetor(selectSetor) {
    await carregarEtapasDoSetor(selectSetor, '');
}

async function carregarEtapasDoSetor(selectSetor, etapaSetorIdSelecionada) {
    const setorId = selectSetor.value;
    const row = selectSetor.closest('.etapa-item');
    const selectEtapa = row.querySelector('.etapa-setor-select');

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
        console.error('Erro ao buscar processos do setor', e);
    }
}

async function editarRoteiro(produtoId) {
    const response = await fetch(`/dashboard/roteiros/${produtoId}`);

    if (!response.ok) {
        alert('Não foi possível carregar o roteiro.');
        return;
    }

    const roteiro = await response.json();

    const form = document.getElementById('roteiro-form');
    form.action = `/dashboard/roteiros/${produtoId}/editar`;

    document.getElementById('form-title').textContent = 'Editar Roteiro';
    document.getElementById('produtoId').value = roteiro.produtoId;

    limparEtapas();

    if (roteiro.passos && roteiro.passos.length > 0) {
        // Cria todas as linhas primeiro de forma síncrona para fixar os selects e índices corretos
        for (const passo of roteiro.passos) {
            await adicionarEtapa(passo.setorId, passo.etapaSetorId);
        }
    } else {
        adicionarEtapa();
    }

    showForm();
}