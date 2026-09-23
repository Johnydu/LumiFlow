function showForm() {
  const panel = document.getElementById('form-panel');
  if (panel) panel.style.display = 'flex'; // Exibe o overlay do modal centralizado
}

function hideForm() {
  const panel = document.getElementById('form-panel');
  if (panel) panel.style.display = 'none';
  limparFormularioSetor();
}

function limparFormularioSetor() {
  const form = document.getElementById('setor-form');
  if (form) form.action = '/dashboard/setores';

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Novo Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = '';

  const setorNome = document.getElementById('nome');
  if (setorNome) setorNome.value = '';

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';
}

function novoSetorForm() {
  limparFormularioSetor();
  // Cria uma primeira linha em branco por padrão
  adicionarEtapaSetor();
  showForm();
}

function adicionarEtapaSetor(nomeEtapa = '', idEtapa = '', ordemEtapa = '') {
  const container = document.getElementById('etapas-setor-list');
  if (!container) return;

  const index = container.children.length;

  const row = document.createElement('div');
  row.className = 'etapa-item';
  row.style.display = 'flex';
  row.style.gap = '10px';
  row.style.alignItems = 'center';

  // Input de ID da etapa (Mantém o ID real recebido do banco, ou vazio se for nova)
  const inputId = document.createElement('input');
  inputId.type = 'hidden';
  inputId.name = `etapas[${index}].id`;
  inputId.value = idEtapa !== undefined && idEtapa !== null ? idEtapa : '';

  // Input da Ordem da etapa
  const inputOrdem = document.createElement('input');
  inputOrdem.type = 'hidden';
  inputOrdem.name = `etapas[${index}].ordem`;
  inputOrdem.value = ordemEtapa || (index + 1);

  // Input do Nome da etapa
  const inputNome = document.createElement('input');
  inputNome.type = 'text';
  inputNome.name = `etapas[${index}].nome`;
  inputNome.className = 'form-control input-etapa-nome';
  inputNome.placeholder = 'Nome da etapa (Ex: 1ª Dobra)';
  inputNome.value = nomeEtapa || '';
  inputNome.required = true;
  inputNome.style.flex = '1';
  inputNome.style.height = 'var(--control-height, 38px)';
  inputNome.style.padding = '0 var(--space-3)';
  inputNome.style.border = '1px solid var(--border-strong)';
  inputNome.style.borderRadius = 'var(--radius-md)';
  inputNome.style.background = 'var(--color-field)';
  inputNome.style.color = 'var(--color-text)';
  inputNome.style.fontSize = 'var(--font-size-sm)';

  // Botão de remover
  const btnRemover = document.createElement('button');
  btnRemover.type = 'button';
  btnRemover.className = 'btn-del';
  btnRemover.textContent = '✕';
  btnRemover.style.padding = '6px 12px';
  btnRemover.addEventListener('click', () => removerLinhaEtapaSetor(btnRemover));

  row.appendChild(inputId);
  row.appendChild(inputOrdem);
  row.appendChild(inputNome);
  row.appendChild(btnRemover);

  container.appendChild(row);
  reindexarEtapasSetor();
}

function removerLinhaEtapaSetor(btn) {
  const row = btn.closest('.etapa-item');
  if (row) {
    row.remove();
    reindexarEtapasSetor();
  }
}

// Reindexa os inputs para o Spring realizar o binding correto no List<EtapaSetorDTO>
function reindexarEtapasSetor() {
  document.querySelectorAll('#etapas-setor-list .etapa-item').forEach((item, index) => {
    const inputId = item.querySelector('input[name*=".id"]');
    const inputOrdem = item.querySelector('input[name*=".ordem"]');
    const inputNome = item.querySelector('input[name*=".nome"]');

    if (inputId) inputId.name = `etapas[${index}].id`;
    if (inputOrdem) {
      inputOrdem.name = `etapas[${index}].ordem`;
      inputOrdem.value = index + 1;
    }
    if (inputNome) inputNome.name = `etapas[${index}].nome`;
  });
}

async function editarSetor(id, nome, buttonRef) {
  limparFormularioSetor();

  const form = document.getElementById('setor-form');
  if (form) form.action = `/dashboard/setores/${id}/editar`;

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Editar Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = id;

  const setorNome = document.getElementById('nome');
  if (setorNome) setorNome.value = nome;

  let etapasCarregadas = false;

  // 1. Tenta carregar via API (fetch)
  try {
    const response = await fetch(`/dashboard/setores/${id}/etapas`);
    if (response.ok) {
      const etapas = await response.json();
      if (Array.isArray(etapas) && etapas.length > 0) {
        etapas.forEach(etapa => {
          adicionarEtapaSetor(etapa.nome, etapa.id, etapa.ordem);
        });
        etapasCarregadas = true;
      }
    }
  } catch (e) {
    console.warn('Falha no fetch de etapas, tentando fallback HTML...', e);
  }

  // 2. Fallback: Se o fetch falhar ou retornar vazio, extrai direto da tabela HTML
  if (!etapasCarregadas && buttonRef) {
    const tr = buttonRef.closest('tr');
    if (tr) {
      const tagsEtapas = tr.querySelectorAll('.tag-etapa');
      if (tagsEtapas.length > 0) {
        tagsEtapas.forEach((tag, idx) => {
          const nomeTexto = tag.textContent.trim();
          if (nomeTexto) {
            adicionarEtapaSetor(
                nomeTexto,
                tag.getAttribute('data-id'),
                tag.getAttribute('data-ordem') || idx + 1
            );
          }
        });
        etapasCarregadas = true;
      }
    }
  }

  // 3. Se não tiver nenhuma etapa, adiciona 1 linha vazia
  if (!etapasCarregadas) {
    adicionarEtapaSetor();
  }

  showForm();
}

function editarSetorDoBotao(button) {
  const id = button.getAttribute('data-id');
  const nome = button.getAttribute('data-nome');
  editarSetor(id, nome, button);
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
            initFiltroBuscaSetores();
          }
        }
      } catch (error) {
        console.error('Erro na busca dinâmica:', error);
      }
    }, 400);
  });
}