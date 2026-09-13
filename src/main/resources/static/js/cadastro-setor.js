function showForm() {
  const panel = document.getElementById('form-panel');
  if (panel) panel.style.display = 'block';
}

function hideForm() {
  const panel = document.getElementById('form-panel');
  if (panel) panel.style.display = 'none';
  limparFormularioSetor();
}

function limparFormularioSetor() {
  const form = document.getElementById('setor-form');
  if (form) form.reset();

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = '';

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';
}

function novoSetorForm() {
  const form = document.getElementById('setor-form');
  if (form) form.action = '/dashboard/setores';

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Novo Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = '';

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';

  // Ao criar um novo setor, já iniciamos com uma linha de etapa em branco opcional
  adicionarEtapaSetor();

  showForm();
}

function adicionarEtapaSetor(nomeEtapa = '', idEtapa = '', ordemEtapa = '') {
  const container = document.getElementById('etapas-setor-list');
  if (!container) return;

  const index = container.children.length;
  const ordemCalculada = ordemEtapa !== '' ? ordemEtapa : (index + 1);

  const row = document.createElement('div');
  row.className = 'etapa-item';
  row.style.display = 'flex';
  row.style.gap = '10px';
  row.style.marginBottom = '8px';
  row.style.alignItems = 'center';

  const inputId = document.createElement('input');
  inputId.type = 'hidden';
  inputId.name = `etapas[${index}].id`;
  inputId.value = idEtapa;

  const inputOrdem = document.createElement('input');
  inputOrdem.type = 'hidden';
  inputOrdem.name = `etapas[${index}].ordem`;
  inputOrdem.value = ordemCalculada;

  const inputNome = document.createElement('input');
  inputNome.type = 'text';
  inputNome.name = `etapas[${index}].nome`;
  inputNome.className = 'form-control';
  inputNome.placeholder = 'Nome da etapa (Ex: 1ª Dobra)';
  inputNome.value = nomeEtapa;
  inputNome.required = true;
  inputNome.style.flex = '1';
  inputNome.style.padding = '8px';
  inputNome.style.border = '1px solid var(--color-muted-3, #ccc)';
  inputNome.style.borderRadius = '4px';

  const btnRemover = document.createElement('button');
  btnRemover.type = 'button';
  btnRemover.className = 'btn-del';
  btnRemover.textContent = '✕';
  btnRemover.style.background = '#ff4d4d';
  btnRemover.style.color = 'white';
  btnRemover.style.border = 'none';
  btnRemover.style.padding = '6px 10px';
  btnRemover.style.borderRadius = '4px';
  btnRemover.style.cursor = 'pointer';
  btnRemover.addEventListener('click', () => removerLinhaEtapaSetor(btnRemover));

  row.appendChild(inputId);
  row.appendChild(inputOrdem);
  row.appendChild(inputNome);
  row.appendChild(btnRemover);

  container.appendChild(row);
  reindexarEtapasSetor();
}

function removerLinhaEtapaSetor(btn) {
  btn.closest('.etapa-item').remove();
  reindexarEtapasSetor();
}

// Reindexa os inputs para o Spring fazer o bind correto em etapas[0], etapas[1]...
function reindexarEtapasSetor() {
  document.querySelectorAll('#etapas-setor-list .etapa-item').forEach((item, index) => {
    const inputId = item.querySelector('input[name$=".id"]');
    const inputOrdem = item.querySelector('input[name$=".ordem"]');
    const inputNome = item.querySelector('input[type="text"]');

    if (inputId) inputId.name = `etapas[${index}].id`;
    if (inputOrdem) {
      inputOrdem.name = `etapas[${index}].ordem`;
      inputOrdem.value = index + 1; // Atualiza a ordem sequencial automaticamente
    }
    if (inputNome) inputNome.name = `etapas[${index}].nome`;
  });
}

async function editarSetor(id, nome) {
  const form = document.getElementById('setor-form');
  if (form) form.action = `/dashboard/setores/${id}/editar`;

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Editar Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = id;

  const setorNome = document.getElementById('nome');
  if (setorNome) setorNome.value = nome;

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';

  // Busca as etapas cadastradas do setor via API para edição
  try {
    const response = await fetch(`/dashboard/setores/${id}/etapas`);
    if (response.ok) {
      const etapas = await response.json();
      if (etapas && etapas.length > 0) {
        etapas.forEach(etapa => {
          adicionarEtapaSetor(etapa.nome, etapa.id, etapa.ordem);
        });
      } else {
        adicionarEtapaSetor(); // Garante uma linha vazia se o setor não tiver etapas cadastradas
      }
    }
  } catch (e) {
    console.error('Erro ao carregar etapas do setor para edição', e);
    adicionarEtapaSetor();
  }

  showForm();
}

function editarSetorDoBotao(button) {
  const id = button.getAttribute('data-id');
  const nome = button.getAttribute('data-nome');
  editarSetor(id, nome);
}

// ============================================================
// FILTRO DINÂMICO DE BUSCA DE SETORES VIA AJAX (DEBOUNCE)
// ============================================================
document.addEventListener('DOMContentLoaded', () => {
  'use strict';
  initFiltroBuscaSetores();
});

function initFiltroBuscaSetores() {
  const inputBusca = document.querySelector('.search');
  if (!inputBusca) return;

  const formBusca = inputBusca.closest('form');
  if (!formBusca) return;

  // Mantém o foco e a posição do cursor no input ao atualizar via AJAX
  if (inputBusca.value) {
    const val = inputBusca.value;
    inputBusca.focus();
    inputBusca.value = '';
    inputBusca.value = val;
  }

  let timeout = null;

  inputBusca.addEventListener('input', () => {
    clearTimeout(timeout);
    timeout = setTimeout(async () => {
      const formData = new FormData(formBusca);
      const params = new URLSearchParams(formData).toString();
      const url = `${formBusca.action}?${params}`;

      try {
        const response = await fetch(url, {
          headers: {
            'X-Requested-With': 'XMLHttpRequest'
          }
        });

        if (response.ok) {
          const htmlText = await response.text();
          const parser = new DOMParser();
          const doc = parser.parseFromString(htmlText, 'text/html');

          const novaLista = doc.querySelector('.lista');
          const listaAtual = document.querySelector('.lista');

          if (novaLista && listaAtual) {
            listaAtual.replaceWith(novaLista);
            initFiltroBuscaSetores(); // Reatacha o evento após substituir o DOM
          }
        }
      } catch (error) {
        console.error('Erro na busca dinâmica de setores, enviando via submit normal:', error);
        formBusca.submit();
      }
    }, 400); // 400ms de atraso (debounce)
  });
}