/**
 * LumiFlow - Módulo de Cadastro e Gerenciamento de Produtos
 */

// ========================
// ESTADO GLOBAL
// ========================
let sequenciaAtual = 0;
const setoresSelecionados = new Map(); // id → { nome, seq }

document.addEventListener('DOMContentLoaded', () => {
  initFiltroBuscaProdutos();
});


// ========================
// CONTROLE DO PAINEL DE FORMULÁRIO
// ========================

function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}


// ========================
// CONTROLE DE ABAS (Dados / Roteiro)
// ========================

function showTab(tab, btnClicado) {
  const tabDados = document.getElementById('tab-dados');
  const tabRoteiro = document.getElementById('tab-roteiro');

  if (tabDados) tabDados.style.display = tab === 'dados' ? 'block' : 'none';
  if (tabRoteiro) tabRoteiro.style.display = tab === 'roteiro' ? 'block' : 'none';

  if (btnClicado) {
    document.querySelectorAll('.tab').forEach(b => b.classList.remove('active'));
    btnClicado.classList.add('active');
  }
}


// ========================
// FUNÇÕES DE FORMULÁRIO DE PRODUTO
// ========================

function novoProdutoForm() {
  resetarFormularios();

  document.getElementById('produto-form').action = '/dashboard/produtos';
  document.getElementById('form-title').textContent = 'Informações do produto';

  showTab('dados', document.getElementById('tab-btn-dados'));
  showForm();
}

function editarProduto(id, nome, codigo, descricao) {
  resetarFormularios();

  const form = document.getElementById('produto-form');
  form.action = '/dashboard/produtos/' + id + '/editar';

  document.getElementById('id').value = id;
  document.getElementById('nome').value = nome;
  document.getElementById('codigo').value = codigo;
  document.getElementById('descricao').value = descricao || '';
  document.getElementById('roteiro-produto-id').value = id;

  document.getElementById('form-title').textContent = 'Editar produto';

  showTab('dados', document.getElementById('tab-btn-dados'));
  showForm();
}

function resetarFormularios() {
  // Reseta aba de dados
  document.getElementById('id').value = '';
  document.getElementById('nome').value = '';
  document.getElementById('codigo').value = '';
  document.getElementById('descricao').value = '';

  // Reseta aba de roteiro
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
// FILTRO DE BUSCA DINÂMICA (AJAX)
// ========================
document.addEventListener('DOMContentLoaded', () => {
  initFiltroBuscaProdutos();
});

function initFiltroBuscaProdutos() {
  // Procura o input de busca pelo ID ou classe na página toda
  const inputBusca = document.getElementById('form-busca-produto')?.querySelector('input.search')
      || document.querySelector('input.search');

  if (!inputBusca) return;

  const formBusca = inputBusca.closest('form');

  // Mantém o foco e o cursor no final do texto digitado
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
      const termo = inputBusca.value;
      const url = `${formBusca.action}?nome=${encodeURIComponent(termo)}`;

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
            // Reinicializa o listener caso o DOM tenha sido substituído
            initFiltroBuscaProdutos();
          }
        }
      } catch (error) {
        console.error('Erro na busca dinâmica, enviando via submit normal:', error);
        formBusca.submit();
      }
    }, 300); // 300ms de delay (debounce)
  });
}



// ========================
// TOGGLE DE SETORES (Roteiro)
// ========================

function toggleSetor(label, setorId) {
  const checkbox = label.querySelector('input[type=checkbox]');
  const seqSpan = label.querySelector('.setor-seq');
  const nomeSetor = label.querySelector('.setor-nome').textContent.trim();

  if (checkbox.checked) {
    checkbox.checked = false;
    label.classList.remove('checked');
    seqSpan.textContent = '—';
    seqSpan.style.display = 'none';

    setoresSelecionados.delete(setorId);
    reordenarSequencias();
  } else {
    sequenciaAtual++;
    checkbox.checked = true;
    label.classList.add('checked');
    seqSpan.textContent = sequenciaAtual + 'º';
    seqSpan.style.display = 'inline';

    setoresSelecionados.set(setorId, { nome: nomeSetor, seq: sequenciaAtual });
  }

  atualizarPreview();
}

function reordenarSequencias() {
  const ordenados = [...setoresSelecionados.entries()]
      .sort((a, b) => a[1].seq - b[1].seq);

  setoresSelecionados.clear();
  sequenciaAtual = 0;

  ordenados.forEach(([id, info]) => {
    sequenciaAtual++;
    setoresSelecionados.set(id, { nome: info.nome, seq: sequenciaAtual });
  });

  document.querySelectorAll('.setor-check').forEach(label => {
    const id = obterSetorId(label);
    const seqSpan = label.querySelector('.setor-seq');

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
  if (!pills) return;

  const ordenados = [...setoresSelecionados.values()]
      .sort((a, b) => a.seq - b.seq);

  pills.replaceChildren();

  if (ordenados.length === 0) {
    const vazio = document.createElement('span');
    vazio.style.color = 'var(--color-muted-5)';
    vazio.style.fontSize = '12px';
    vazio.textContent = 'Nenhum setor selecionado.';
    pills.appendChild(vazio);
    return;
  }

  ordenados.forEach((s, i) => {
    const pill = document.createElement('span');
    pill.className = 'r-pill';
    pill.textContent = s.nome;
    pills.appendChild(pill);

    if (i < ordenados.length - 1) {
      const seta = document.createElement('span');
      seta.className = 'r-arr';
      seta.textContent = '›';
      pills.appendChild(seta);
    }
  });
}


// ========================
// ABERTURA AUTOMÁTICA DA ABA (via URL)
// ========================
const abaRoteiro = /*[[${param.abaRoteiro}]]*/ null;
const produtoIdParam = /*[[${param.produtoId}]]*/ null;

if (abaRoteiro && produtoIdParam) {
  showForm();
  showTab('roteiro', document.getElementById('tab-btn-roteiro'));
  const roteiroIdInput = document.getElementById('roteiro-produto-id');
  if (roteiroIdInput) roteiroIdInput.value = produtoIdParam;
}