function showForm() {
  document.getElementById('form-panel').classList.add('is-open');
}

function hideForm() {
  document.getElementById('form-panel').classList.remove('is-open');
}

function novaMaquinaForm() {
  const form = document.querySelector('.form-card');

  form.reset();
  form.action = '/dashboard/maquinas';

  document.getElementById('id').value = '';
  document.getElementById('form-title').textContent = 'Nova Máquina';

  showForm();
}

// Ajustado para receber e preencher corretamente o id do select (setorId)
function handleEditarMaquina(button) {
  const id = button.getAttribute('data-id');
  const nome = button.getAttribute('data-nome');
  const setorId = button.getAttribute('data-setor-id');

  const form = document.querySelector('.form-card');
  form.action = '/dashboard/maquinas/' + id + '/editar';

  document.getElementById('id').value = id;
  document.getElementById('nome').value = nome;

  // Define o valor no select de setores do formulário
  const selectSetor = document.querySelector('select[th\\:field="*{setorId}"], select[id="setorId"], select[name="setorId"]');
  if (selectSetor) {
    selectSetor.value = setorId;
  }

  document.getElementById('form-title').textContent = 'Editar Máquina';

  showForm();
}

// Função atualizada para lidar com o novo filtro de Dropdown (Select)
function handleFiltrarSetorSelect(selectElement) {
  const setorSelecionado = selectElement.value.toLowerCase();
  const linhas = document.querySelectorAll('table tbody tr[data-setor]');

  linhas.forEach(linha => {
    const setorLinha = linha.getAttribute('data-setor').toLowerCase();

    if (setorSelecionado === 'todos' || setorLinha === setorSelecionado) {
      linha.hidden = false;
    } else {
      linha.hidden = true;
    }
  });
}