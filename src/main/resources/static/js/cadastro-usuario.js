/**
 * LumiFlow - Gerenciamento de Usuários (Modais Otimizados)
 */

function showForm() {
  const formPanel = document.getElementById('form-panel');
  if (formPanel) {
    formPanel.style.display = 'flex'; // Exibe o overlay do modal centralizado
  }
}

function hideForm() {
  const formPanel = document.getElementById('form-panel');
  if (formPanel) {
    formPanel.style.display = 'none';

    const form = document.querySelector('.modal-box form');
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

  const form = document.querySelector('.modal-box form');
  if (form) {
    form.reset();
    form.action = '/dashboard/usuario';
    const idInput = form.querySelector('input[name="id"]');
    if (idInput) idInput.value = '';
  }

  const senha = document.getElementById('senha');
  if (senha) {
    senha.placeholder = 'Mínimo 12 caracteres...';
    senha.value = '';
  }

  showForm();
}

function handleEditarUsuario(button) {
  const usuarioId = button.getAttribute('data-id');
  const usuarioNome = button.getAttribute('data-nome');
  const usuarioLogin = button.getAttribute('data-login');
  const usuarioNivel = button.getAttribute('data-nivel') || button.getAttribute('data-nivelacessoid');
  const usuarioSetor = button.getAttribute('data-setor') || button.getAttribute('data-setorid');

  editarUsuario(usuarioId, usuarioNome, usuarioLogin, usuarioNivel, usuarioSetor);
}

function editarUsuario(id, nome, login, nivelAcessoId, setorId) {
  const formTitle = document.getElementById('form-title');
  if (formTitle) {
    formTitle.textContent = 'Editar Usuário';
  }

  const form = document.querySelector('.modal-box form');
  if (form) {
    form.action = '/dashboard/usuario/' + id + '/editar';
  }

  // Preencher ID
  const idInput = document.querySelector('input[name="id"]');
  if (idInput) {
    idInput.value = id || '';
  }

  // Preencher campos
  const nomeInput = document.getElementById('nome');
  const loginInput = document.getElementById('login');
  const nivelSelect = document.getElementById('nivelAcessoId');
  const setorSelect = document.getElementById('setorId');
  const senhaInput = document.getElementById('senha');

  if (nomeInput) nomeInput.value = nome || '';
  if (loginInput) loginInput.value = login || '';
  if (nivelSelect) nivelSelect.value = nivelAcessoId || '';
  if (setorSelect) setorSelect.value = setorId || '';

  if (senhaInput) {
    senhaInput.value = '';
    senhaInput.placeholder = 'Deixe em branco para manter a senha atual';
  }

  showForm();
}

// Tratamento de fade-out para alertas do sistema
document.addEventListener('DOMContentLoaded', () => {
  const alert = document.querySelector('.alert-success, .alert-error');

  if (alert) {
    setTimeout(() => {
      alert.style.opacity = '0';

      setTimeout(() => {
        alert.remove();
      }, 300);
    }, 3000);
  }
});