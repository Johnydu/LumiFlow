/**
 - LumiFlow - Gerenciamento de Usuários (Modais Otimizados)
 */

function showForm() {
  const formPanel = document.getElementById('form-panel');
  if (formPanel) {
    formPanel.classList.add('is-open');
  }
}

function hideForm() {
  const formPanel = document.getElementById('form-panel');
  if (formPanel) {
    formPanel.classList.remove('is-open');

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

  const senha = document.getElementById('senhaUsuario');
  if (senha) {
    senha.placeholder = 'Mínimo 8 caracteres (maiúscula, minúscula e número)...';
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
  const nomeInput = document.getElementById('nomeUsuario');
  const loginInput = document.getElementById('loginUsuario');
  const nivelSelect = document.getElementById('nivelAcessoUsuarioId');
  const setorSelect = document.getElementById('setorUsuarioId');
  const senhaInput = document.getElementById('senhaUsuario');

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
