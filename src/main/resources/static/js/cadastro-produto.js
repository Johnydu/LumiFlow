// ========================
// ESTADO GLOBAL
// ========================

let sequenciaAtual = 0;
const setoresSelecionados = new Map(); // id → { nome, seq }


// ========================
// CONTROLE DO PAINEL
// ========================

function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}


// ========================
// CONTROLE DE ABAS
// ========================

function showTab(tab, btnClicado) {
  const tabDados = document.getElementById('tab-dados');
  const tabRoteiro = document.getElementById('tab-roteiro');

  if (tabDados) tabDados.style.display = tab === 'dados' ? 'block' : 'none';
  if (tabRoteiro) tabRoteiro.style.display = tab === 'roteiro' ? 'block' : 'none';

  // Se houver botões de aba na tela, atualiza a classe active
  if (btnClicado) {
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btnClicado.classList.add('active');
  }
}


// ========================
// FUNÇÕES DE FORMULÁRIO
// ========================

function novoProdutoForm() {
  resetarFormularios();

  document.getElementById('produto-form').action = '/dashboard/produtos';
  document.getElementById('form-title').textContent  = 'Informações do produto';

  showTab('dados', document.getElementById('tab-btn-dados'));
  showForm();
}

function editarProduto(id, nome, codigo, descricao) {

  resetarFormularios();

  const form = document.getElementById('produto-form');

  form.action =
      '/dashboard/produtos/' + id + '/editar';

  document.getElementById('id').value = id;
  document.getElementById('nome').value = nome;
  document.getElementById('codigo').value = codigo;
  document.getElementById('descricao').value = descricao || '';

  document.getElementById('roteiro-produto-id').value = id;

  document.getElementById('form-title').textContent =
      'Editar produto';

  showTab(
      'dados',
      document.getElementById('tab-btn-dados')
  );

  showForm();
}

function resetarFormularios() {
  // reseta aba de dados
  document.getElementById('id').value        = '';
  document.getElementById('nome').value      = '';
  document.getElementById('codigo').value    = '';
  document.getElementById('descricao').value = '';

  // reseta aba de roteiro
  sequenciaAtual = 0;
  setoresSelecionados.clear();

  document.querySelectorAll('.setor-check').forEach(label => {
    label.classList.remove('checked');
    label.querySelector('input[type=checkbox]').checked = false;
    const seq = label.querySelector('.setor-seq');
    seq.textContent = '—';
    seq.style.display = 'none';
  });

  atualizarPreview();
}


// ========================
// TOGGLE DE SETORES
// ========================

function toggleSetor(label, setorId) {
  const checkbox  = label.querySelector('input[type=checkbox]');
  const seqSpan   = label.querySelector('.setor-seq');
  const nomeSetor = label.querySelector('.setor-nome').textContent.trim();

  if (checkbox.checked) {
    // desmarca
    checkbox.checked = false;
    label.classList.remove('checked');
    seqSpan.textContent    = '—';
    seqSpan.style.display  = 'none';

    setoresSelecionados.delete(setorId);
    reordenarSequencias();
  } else {
    // marca
    sequenciaAtual++;
    checkbox.checked = true;
    label.classList.add('checked');
    seqSpan.textContent   = sequenciaAtual + 'º';
    seqSpan.style.display = 'inline';

    setoresSelecionados.set(setorId, { nome: nomeSetor, seq: sequenciaAtual });
  }

  atualizarPreview();
}

function reordenarSequencias() {
  // reordena o Map por seq e reatribui números contíguos
  const ordenados = [...setoresSelecionados.entries()]
      .sort((a, b) => a[1].seq - b[1].seq);

  setoresSelecionados.clear();
  sequenciaAtual = 0;

  ordenados.forEach(([id, info]) => {
    sequenciaAtual++;
    setoresSelecionados.set(id, { nome: info.nome, seq: sequenciaAtual });
  });

  // atualiza os spans na tela
  document.querySelectorAll('.setor-check').forEach(label => {
    const id       = obterSetorId(label);
    const seqSpan  = label.querySelector('.setor-seq');

    if (setoresSelecionados.has(id)) {
      seqSpan.textContent = setoresSelecionados.get(id).seq + 'º';
    }
  });
}

function obterSetorId(label) {
  const onclickAttr = label.getAttribute('onclick');
  const match = onclickAttr.match(/toggleSetor\(this,\s*(\d+)\)/);
  return match ? parseInt(match[1]) : null;
}


// ========================
// PREVIEW DO ROTEIRO
// ========================

function atualizarPreview() {
  const pills = document.getElementById('preview-pills');

  const ordenados = [...setoresSelecionados.values()]
      .sort((a, b) => a.seq - b.seq);

  if (ordenados.length === 0) {
    pills.innerHTML = `
            <span style="color:var(--color-muted-5);font-size:12px">
                Nenhum setor selecionado.
            </span>`;
    return;
  }

  pills.innerHTML = ordenados.map((s, i) => {
    const seta = i < ordenados.length - 1 ? '<span class="r-arr">›</span>' : '';
    return `<span class="r-pill">${s.nome}</span>${seta}`;
  }).join('');
}
<!-- ================================ -->
<!-- ABERTURA AUTOMÁTICA DA ABA       -->
<!-- Detecta parâmetro abaRoteiro=true-->
<!-- na URL após salvar produto       -->
<!-- ================================ -->
const abaRoteiro = /*[[${param.abaRoteiro}]]*/ null;
const produtoIdParam = /*[[${param.produtoId}]]*/ null;

if (abaRoteiro && produtoIdParam) {
  showForm();
  showTab('roteiro', document.getElementById('tab-btn-roteiro'));
  document.getElementById('roteiro-produto-id').value = produtoIdParam;
}