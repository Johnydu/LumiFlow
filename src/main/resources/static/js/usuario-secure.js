/**
 * Handlers de segurança para formulários de usuário
 * Evita XSS ao não interpolar dados diretamente no onclick
 */

function handleEditarUsuario(button) {
    const usuarioId = button.getAttribute('data-id');
    const usuarioNome = button.getAttribute('data-nome');
    const usuarioLogin = button.getAttribute('data-login');
    const usuarioNivel = button.getAttribute('data-nivel');
    const usuarioSetor = button.getAttribute('data-setor');

    // Chamar função via data attributes (seguro)
    editarUsuario(usuarioId, usuarioNome, usuarioLogin, usuarioNivel, usuarioSetor);
}

/**
 * Mostrar formulário de edição com dados preenchidos
 */
function editarUsuario(id, nome, login, nivelId, setorId) {
    const formTitle = document.getElementById('form-title');
    const formPanel = document.getElementById('form-panel');

    if (!formTitle || !formPanel) return;

    // Atualizar título
    formTitle.textContent = 'Editar Usuário';

    // Preencher ID (Essencial para atualização no Spring Boot)
    const idInput = document.querySelector('input[name="id"]');
    if (idInput) idInput.value = id;

    // Preencher demais campos usando os atributos padrão name/id do HTML
    const nomeInput = document.querySelector('input[name="nome"]');
    const loginInput = document.querySelector('input[name="login"]');
    const senhaInput = document.querySelector('input[name="senha"]');
    const nivelSelect = document.querySelector('select[name="nivelAcessoId"]');
    const setorSelect = document.querySelector('select[name="setorId"]');

    if (nomeInput) nomeInput.value = nome || '';
    if (loginInput) loginInput.value = login || '';
    if (senhaInput) senhaInput.value = ''; // Senha em branco para manter a atual se não alterada
    if (nivelSelect) nivelSelect.value = nivelId || '';
    if (setorSelect) setorSelect.value = setorId || '';

    // Mostrar modal/painel
    formPanel.style.display = 'flex';
}

function hideForm() {
    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.style.display = 'none';

        // Limpar o formulário e resetar o campo de ID
        const form = document.querySelector('.form-card form');
        if (form) {
            form.reset();
            const idInput = form.querySelector('input[name="id"]');
            if (idInput) idInput.value = '';
        }
    }
}

function novoUsuarioForm() {
    const formTitle = document.getElementById('form-title');
    if (formTitle) {
        formTitle.textContent = 'Novo Usuário';
    }

    const form = document.querySelector('.form-card form');
    if (form) {
        form.reset();
        // Garante que o campo ID está vazio para criação de um NOVO usuário
        const idInput = form.querySelector('input[name="id"]');
        if (idInput) idInput.value = '';
    }

    const formPanel = document.getElementById('form-panel');
    if (formPanel) formPanel.style.display = 'flex';
}