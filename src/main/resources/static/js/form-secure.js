/**
 * Handlers de segurança para máquinas e setores
 * Evita XSS via data attributes
 */

function handleEditarMaquina(button) {
    const maquinaId = button.getAttribute('data-id');
    const maquinaNome = button.getAttribute('data-nome');
    const maquinaSetorId = button.getAttribute('data-setor-id');

    editarMaquina(maquinaId, maquinaNome, maquinaSetorId || null);
}

function handleFiltrarSetor(button) {
    const setorNome = button.getAttribute('data-setor-nome');
    filtrarSetor(button, setorNome);
}

function hideForm() {
    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.style.display = 'none';
        document.querySelectorAll('.form-card form').forEach(f => f.reset());
    }
}

function adicionarEtapaSetor() {
    // Implementação segura
    const container = document.getElementById('etapas-container');
    if (!container) return;

    const newRow = document.createElement('div');
    newRow.className = 'etapa-row';

    const input = document.createElement('input');
    input.type = 'text';
    input.name = 'etapas[]';
    input.placeholder = 'Nome da etapa';

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.textContent = 'Remove';
    removeButton.addEventListener('click', () => newRow.remove());

    newRow.append(input, removeButton);
    container.appendChild(newRow);
}

function editarSetorDoBotao(button) {
    const setorId = button.getAttribute('data-id');
    const setorNome = button.getAttribute('data-nome');
    const possuiEtapas = button.getAttribute('data-possui') === 'true';

    const formTitle = document.getElementById('form-title');
    if (formTitle) {
        formTitle.textContent = 'Editar Setor';
    }

    const nomeInput = document.querySelector('input[name="nome"]');
    if (nomeInput) {
        nomeInput.value = setorNome;
    }

    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.style.display = 'flex';
    }
}
