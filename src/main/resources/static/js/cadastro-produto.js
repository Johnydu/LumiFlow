/**
 * LumiFlow - Módulo de Cadastro e Gerenciamento de Produtos
 * Ficheiro unificado (Modais, Segurança data-*, Abas, AJAX e Roteiros)
 */

// ========================
// ESTADO GLOBAL
// ========================
document.addEventListener('DOMContentLoaded', () => {
  initFiltroAjax({ formSelector: '#form-busca-produto' });
});

// ========================
// CONTROLE DO PAINEL DE FORMULÁRIO (MODAL)
// ========================

function showForm() {
  const panel = document.getElementById('form-panel');
  if (panel) {
    panel.classList.add('is-open');
  }
}

function hideForm() {
  const panel = document.getElementById('form-panel');
  if (panel) {
    panel.classList.remove('is-open');
    const form = document.getElementById('produto-form');
    if (form) form.reset();
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

  const nomeInput = document.getElementById('nomeProduto');
  if (nomeInput) nomeInput.value = nome;

  const codigoInput = document.getElementById('codigoProduto');
  if (codigoInput) codigoInput.value = codigo;

  const descInput = document.getElementById('descricaoProduto');
  if (descInput) descInput.value = descricao || '';

  const titleEl = document.getElementById('form-title');
  if (titleEl) titleEl.textContent = 'Editar Produto';

  showForm();
}

function resetarFormularios() {
  // Reseta aba de dados
  const idInput = document.getElementById('id');
  if (idInput) idInput.value = '';
  const nomeInput = document.getElementById('nomeProduto');
  if (nomeInput) nomeInput.value = '';
  const codigoInput = document.getElementById('codigoProduto');
  if (codigoInput) codigoInput.value = '';
  const descInput = document.getElementById('descricaoProduto');
  if (descInput) descInput.value = '';

  const form = document.getElementById('produto-form');
  if (form) form.reset();

}

// ========================
// FILTRO DE BUSCA DINÂMICA - ver filtro-ajax.js
// ========================
