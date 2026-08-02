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

  const containerEtapas = document.getElementById('container-etapas-setor');
  if (containerEtapas) containerEtapas.style.display = 'none';
}

function novoSetorForm() {
  const form = document.getElementById('setor-form');
  if (form) form.action = '/dashboard/setores';

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Novo Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = '';

  const selectPossui = document.getElementById('possuiEtapas');
  if (selectPossui) selectPossui.value = 'false';

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';

  const containerEtapas = document.getElementById('container-etapas-setor');
  if (containerEtapas) containerEtapas.style.display = 'none';

  showForm();
}

function toggleEtapasContainer() {
  const select = document.getElementById('possuiEtapas');
  const container = document.getElementById('container-etapas-setor');

  if (select && select.value === 'true') {
    if (container) container.style.display = 'block';
  } else {
    if (container) container.style.display = 'none';
    const etapasList = document.getElementById('etapas-setor-list');
    if (etapasList) etapasList.innerHTML = '';
  }
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

  row.innerHTML = `
        <input type="hidden" name="etapas[${index}].id" value="${idEtapa}">
        <input type="hidden" name="etapas[${index}].ordem" value="${ordemCalculada}">
        <input type="text" name="etapas[${index}].nome" class="form-control" placeholder="Nome da etapa (Ex: 1ª Dobra)" value="${nomeEtapa}" required style="flex:1; padding: 8px; border: 1px solid var(--color-muted-3, #ccc); border-radius: 4px;">
        <button type="button" class="btn-del" onclick="removerLinhaEtapaSetor(this)" style="background: #ff4d4d; color: white; border: none; padding: 6px 10px; border-radius: 4px; cursor: pointer;">✕</button>
    `;
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

async function editarSetor(id, nome, possuiEtapas) {
  const form = document.getElementById('setor-form');
  if (form) form.action = `/dashboard/setores/${id}/editar`;

  const formTitle = document.getElementById('form-title');
  if (formTitle) formTitle.textContent = 'Editar Setor';

  const setorId = document.getElementById('id');
  if (setorId) setorId.value = id;

  const setorNome = document.getElementById('nome');
  if (setorNome) setorNome.value = nome;

  // 1. Buscamos o select pelo ID correto do HTML
  const selectPossui = document.getElementById('possui-etapas-select');

  // 2. Normalizamos para string "true" ou "false"
  const possuiEtapasStr = (possuiEtapas === true || possuiEtapas === 'true') ? 'true' : 'false';

  // 3. Log de depuração seguro (usando variáveis que já existem)
  console.log("Valor recebido do banco (possuiEtapas):", possuiEtapas, typeof possuiEtapas);
  console.log("Valor convertido para string:", possuiEtapasStr);

  // 4. Aplicamos o valor no select e disparamos a mudança
  if (selectPossui) {
    selectPossui.value = possuiEtapasStr;
    selectPossui.dispatchEvent(new Event('change'));
  }

  const etapasList = document.getElementById('etapas-setor-list');
  if (etapasList) etapasList.innerHTML = '';

  const containerEtapas = document.getElementById('container-etapas-setor');

  if (possuiEtapasStr === 'true') {
    if (containerEtapas) containerEtapas.style.display = 'block';

    try {
      const response = await fetch(`/dashboard/setores/${id}/etapas`);
      if (response.ok) {
        const etapas = await response.json();
        if (etapas && etapas.length > 0) {
          etapas.forEach(etapa => {
            adicionarEtapaSetor(etapa.nome, etapa.id);
          });
        }
      }
    } catch (e) {
      console.error('Erro ao carregar etapas do setor para edição', e);
    }
  } else {
    if (containerEtapas) containerEtapas.style.display = 'none';
  }

  showForm();
}

function editarSetorDoBotao(button) {
  const id = button.getAttribute('data-id');
  const nome = button.getAttribute('data-nome');

  // Captura o valor bruto do atributo data-possui
  const dataPossuiAttr = button.getAttribute('data-possui');

  // Converte com segurança para booleano real (true ou false)
  const possuiEtapas = (dataPossuiAttr === 'true' || dataPossuiAttr === true);

  editarSetor(id, nome, possuiEtapas);
}

