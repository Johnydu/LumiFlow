function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}

// Abre o formulário em modo CRIAÇÃO: limpa os campos e aponta o
// action de volta para o endpoint de cadastro (POST /dashboard/usuario).
function novoUsuarioForm() {
  const form = document.querySelector('.form-card');
  form.reset();
  form.action = '/dashboard/usuario';

  document.getElementById('form-title').textContent = 'Novo Usuário';

  const senha = document.getElementById('senha');
  senha.placeholder = 'Mínimo 6 caracteres';

  showForm();
}

// Abre o formulário em modo EDIÇÃO: preenche os campos com os dados
// do usuário clicado e aponta o action para o endpoint de edição
// (POST /dashboard/usuario/{id}/editar).
function editarUsuario(id, nome, login, nivelAcessoId, setorId) {
  const form = document.querySelector('.form-card');

  form.action = '/dashboard/usuario/' + id + '/editar';

  document.getElementById('form-title').textContent = 'Editar Usuário';

  document.getElementById('nome').value = nome;
  document.getElementById('login').value = login;
  document.getElementById('nivelAcessoId').value = nivelAcessoId;
  document.getElementById('setorId').value = setorId ?? '';

  // Senha fica em branco de propósito: o usuário só digita algo aqui
  // se quiser trocar a senha atual. Backend trata campo vazio como
  // "manter a senha existente".
  const senha = document.getElementById('senha');
  senha.value = '';
  senha.placeholder = 'Deixe em branco para manter a senha atual';

  showForm();
}
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