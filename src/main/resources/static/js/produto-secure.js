/**
 * Handlers de segurança para formulários de produto
 * Evita XSS ao não interpolar dados diretamente no onclick
 */

function handleEditarProduto(button) {
    const produtoId = button.getAttribute('data-id');
    const produtoNome = button.getAttribute('data-nome');
    const produtoCodigo = button.getAttribute('data-codigo');
    const produtoDescricao = button.getAttribute('data-descricao');

    // Chamar função via data attributes (seguro)
    editarProduto(produtoId, produtoNome, produtoCodigo, produtoDescricao);
}

/**
 * Mostrar formulário de edição com dados sanitizados
 */
function editarProduto(id, nome, codigo, descricao) {
    const formTitle = document.getElementById('form-title');
    const formPanel = document.getElementById('form-panel');
    
    if (!formTitle || !formPanel) return;

    formTitle.textContent = 'Editar Produto';
    
    // Preencher form (valores já escapados pelo Thymeleaf)
    const nomeInput = document.querySelector('input[name="nome"]');
    const codigoInput = document.querySelector('input[name="codigo"]');
    const descricaoInput = document.querySelector('textarea[name="descricao"]');

    if (nomeInput) nomeInput.value = nome;
    if (codigoInput) codigoInput.value = codigo;
    if (descricaoInput) descricaoInput.value = descricao;

    formPanel.style.display = 'flex';
}

function hideForm() {
    const formPanel = document.getElementById('form-panel');
    if (formPanel) {
        formPanel.style.display = 'none';
        document.querySelector('.form-card form')?.reset();
    }
}

function novoProdutoForm() {
    const formTitle = document.getElementById('form-title');
    if (formTitle) {
        formTitle.textContent = 'Novo Produto';
    }
    
    const form = document.querySelector('.form-card form');
    if (form) form.reset();
    
    const formPanel = document.getElementById('form-panel');
    if (formPanel) formPanel.style.display = 'flex';
}
