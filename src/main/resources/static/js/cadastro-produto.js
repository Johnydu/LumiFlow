/**
 * LumiFlow - Módulo de Cadastro e Gerenciamento de Produtos
 * Ficheiro unificado (Modais, Segurança data-*, Abas, AJAX e Roteiros)
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
// CONTROLE DO PAINEL DE FORMULÁRIO (MODAL)
// ========================

function showForm() {
  const panel = document.getElementById('form-panel');
  if (panel) {
    panel.style.display = 'flex'; // Exibe o overlay do modal centralizado
  }
}

function hideForm() {
  const panel = document.getElementById('form-panel');
  if (panel) {
    panel.style.display = 'none';
    const form = document.getElementById('produto-form');
    if (form) form.reset();
  }
}

// ========================
// CONTROLE DE ABAS (Dados / Roteiro)
// ========================

function showTab(tab, btnClicado) {
  const produtoForm = document.getElementById('produto-form');
  const tabRoteiro = document.getElementById('tab-roteiro');

  if (produtoForm) produtoForm.style.display = tab === 'dados' ? 'block' : 'none';
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

  const produtoForm = document.getElementById('produto-form');
  if (produtoForm) produtoForm.action = '/dashboard/produtos';

  const titleEl = document.getElementById('form-title');
  if (titleEl) titleEl.textContent = 'Novo Produto';

  showTab('dados');
  showForm();
}

/**
 * Handler seguro para edição de produto via atributos data-* (Evita XSS)
 */
function handleEditarProduto(button) {
  const produtoId = button.getAttribute('data-id');
  const produtoNome = button.getAttribute('data-nome');
  const produtoCodigo = button.getAttribute('data-codigo');
  const produtoDescricao = button.getAttribute('data-descricao');

  editarProduto(produtoId, produtoNome, produtoCodigo, produtoDescricao);
}

function editarProduto(id, nome, codigo, descricao) {
  resetarFormularios();

  const form = document.getElementById('produto-form');
  if (form) {
    form.action = '/dashboard/produtos/' + id + '/editar';
  }

  const idInput = document.getElementById('id');
  if (idInput) idInput.value = id;

  const nomeInput = document.getElementById('nome');
  if (nomeInput) nomeInput.value = nome;

  const codigoInput = document.getElementById('codigo');
  if (codigoInput) codigoInput.value = codigo;

  const descInput = document.getElementById('descricao');
  if (descInput) descInput.value = descricao || '';

  const roteiroIdInput = document.getElementById('roteiro-produto-id');
  if (roteiroIdInput) roteiroIdInput.value = id;

  const titleEl = document.getElementById('form-title');
  if (titleEl) titleEl.textContent = 'Editar Produto';

  showTab('dados');
  showForm();
}

function resetarFormularios() {
  // Reseta aba de dados
  const idInput = document.getElementById('id');
  if (idInput) idInput.value = '';
  const nomeInput = document.getElementById('nome');
  if (nomeInput) nomeInput.value = '';
  const codigoInput = document.getElementById('codigo');
  if (codigoInput) codigoInput.value = '';
  const descInput = document.getElementById('descricao');
  if (descInput) descInput.value = '';

  const form = document.getElementById('produto-form');
  if (form) form.reset();

  // Reseta aba de roteiro
  sequenciaAtual = 0;
  setoresSelecionados.clear();

  document.querySelectorAll('.setor-check').forEach(label => {
    label.classList.remove('checked');
    const checkbox = label.querySelector('input[type=checkbox]');
    if (checkbox) checkbox.checked = false;
    const seq = label.querySelector('.setor-seq');
    if (seq) {
      seq.textContent = '—';
      seq.style.display = 'none';
    }
  });

  atualizarPreview();
}

// ========================
// FILTRO DE BUSCA DINÂMICA (AJAX)
// ========================

function initFiltroBuscaProdutos() {
  const inputBusca = document.getElementById('form-busca-produto')?.querySelector('input.search')
      || document.querySelector('input.search');

  if (!inputBusca) return;

  const formBusca = inputBusca.closest('form');
  if (!formBusca) return;

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
            initFiltroBuscaProdutos();
          }
        }
      } catch (error) {
        console.error('Erro na busca dinâmica:', error);
        formBusca.submit();
      }
    }, 300);
  });
}

// ========================
// TOGGLE DE SETORES (Roteiro)
// ========================

function toggleSetor(label, setorId) {
  const checkbox = label.querySelector('input[type=checkbox]');
  const seqSpan = label.querySelector('.setor-seq');
  const nomeSetor = label.querySelector('.setor-nome').textContent.trim();

  if (!checkbox) return;

  if (checkbox.checked) {
    checkbox.checked = false;
    label.classList.remove('checked');
    if (seqSpan) {
      seqSpan.textContent = '—';
      seqSpan.style.display = 'none';
    }

    setoresSelecionados.delete(setorId);
    reordenarSequencias();
  } else {
    sequenciaAtual++;
    checkbox.checked = true;
    label.classList.add('checked');
    if (seqSpan) {
      seqSpan.textContent = sequenciaAtual + 'º';
      seqSpan.style.display = 'inline';
    }

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

    if (id && setoresSelecionados.has(id) && seqSpan) {
      seqSpan.textContent = setoresSelecionados.get(id).seq + 'º';
    }
  });
}

function obterSetorId(label) {
  const onclickAttr = label.getAttribute('onclick');
  if (!onclickAttr) return null;
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