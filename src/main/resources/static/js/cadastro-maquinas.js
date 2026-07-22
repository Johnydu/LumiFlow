function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}

function novaMaquinaForm() {

  const form = document.querySelector('.form-card');

  form.reset();
  form.action = '/dashboard/maquina';

  document.getElementById('id').value = '';
  document.getElementById('form-title').textContent = 'Nova Máquina';

  showForm();
}

function editarMaquina(id, nome, setorId) {

  const form = document.querySelector('.form-card');

  form.action = '/dashboard/maquina/' + id + '/editar';

  document.getElementById('id').value = id;
  document.getElementById('nome').value = nome;
  document.getElementById('setorId').value = setorId;

  document.getElementById('form-title').textContent =
      'Editar Máquina';

  showForm();
}

function filtrarSetor(botao, setor) {

  document
      .querySelectorAll('.setor-tab')
      .forEach(t => t.classList.remove('active'));

  botao.classList.add('active');

  const linhas = document.querySelectorAll('tbody tr');

  linhas.forEach(linha => {

    const setorLinha = linha.dataset.setor;

    if (setor === 'todos' || setorLinha === setor) {
      linha.style.display = '';
    } else {
      linha.style.display = 'none';
    }
  });
}