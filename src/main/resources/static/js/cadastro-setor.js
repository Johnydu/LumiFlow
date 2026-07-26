function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}

function novoSetorForm() {

  const form = document.getElementById('setor-form');

  form.action = '/dashboard/setores';
  form.method = 'post';

  document.getElementById('id').value = '';
  document.getElementById('nome').value = '';
  document.getElementById('possuiEtapas').value = 'false';

  document.getElementById('form-title').textContent = 'Novo Setor';

  showForm();
}

function editarSetor(id, nome, possuiEtapas) {

  const form = document.getElementById('setor-form');

  // rota do seu controller
  form.action = '/dashboard/setores/' + id + '/editar';
  form.method = 'post';

  document.getElementById('id').value = id;
  document.getElementById('nome').value = nome;
  document.getElementById('possuiEtapas').value = String(possuiEtapas);

  document.getElementById('form-title').textContent = 'Editar Setor';

  showForm();
}