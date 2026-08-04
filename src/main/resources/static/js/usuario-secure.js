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
 * Mostrar formulário de edição com dados sanitizados
 */
function editarUsuario(id, nome, login, nivelId, setorId) {
    // Sanitizar antes de usar
    const formTitle = document.getElementById('form-title');
    const formPanel = document.getElementById('form-panel');
    
    if (!formTitle || !formPanel) return;

    // Atualizar título
    formTitle.textContent = 'Editar Usuário';
    
    // Preencher form (valores já escapados pelo Thymeleaf)
    const nomeInput = document.querySelector('[th\\:field="*{nome}"]') || document.querySelector('input[name="nome"]');
    const loginInput = document.querySelector('[th\\:field="*{login}"]') || document.querySelector('input[name="login"]');
    const senhaInput = document.querySelector('input[name="senha"]');
    const nivelSelect = document.querySelector('[th\\:field="*{nivelAcessoId}"]') || document.querySelector('select[name="nivelAcessoId"]');
    const setorSelect = document.querySelector('[th\\:field="*{setorId}"]') || document.querySelector('select[name="setorId"]');

    if (nomeInput) nomeInput.value = nome;
    if (loginInput) loginInput.value = login;
    if (senhaInput) senhaInput.value = '';
    if (nivelSelect) nivelSelect.value = nivelId || '';
    if (setorSelect) setorSelect.value = setorId || '';

    // Mostrar form
    formPanel.style.display = 'flex';
}

function hideForm() {
    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.style.display = 'none';
        // Limpar formulário
        document.querySelector('.form-card form')?.reset();
    }
}

function novoUsuarioForm() {
    const formTitle = document.getElementById('form-title');
    if (formTitle) {
        formTitle.textContent = 'Novo Usuário';
    }
    
    const form = document.querySelector('.form-card form');
    if (form) form.reset();
    
    const formPanel = document.getElementById('form-panel');
    if (formPanel) formPanel.style.display = 'flex';
}
