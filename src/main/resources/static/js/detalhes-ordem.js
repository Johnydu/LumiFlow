/**
 * Abre o modal de divisão padrão e configura os dados da etapa.
 */
function abrirModalDivisao(ordemSetorId, quantidadeTotal, produtoNome) {
    const modalElement = document.getElementById('modalDivisao');
    if (!modalElement) {
        console.error("Modal #modalDivisao não encontrado no DOM!");
        return;
    }

    document.getElementById('modalDivisao').classList.add('is-open');
    document.getElementById('modalProdutoNome').textContent = produtoNome;
    document.getElementById('modalQtdTotal').textContent = quantidadeTotal;
    document.getElementById('qtdEsperada').textContent = quantidadeTotal;

    const qtdString = String(quantidadeTotal).replace(/\D/g, '');
    qtdTotalDivisao = parseInt(qtdString) || 0;

    contadorLinhas = 0;
    document.getElementById('linhasDivisao').replaceChildren();

    const formDivisao = document.getElementById('formDivisao');
    if (formDivisao) {
        const actionTemplate = formDivisao.getAttribute('action') || '';
        const action = actionTemplate.replace(
            /\/etapas\/0\/dividir$/,
            `/etapas/${encodeURIComponent(ordemSetorId)}/dividir`
        );
        formDivisao.action = action;
    }

    if (typeof adicionarLinhaDivisao === 'function') {
        adicionarLinhaDivisao();
        adicionarLinhaDivisao();
    }

    if (typeof atualizarSoma === 'function') {
        atualizarSoma();
    }
}

/**
 * Fecha o modal de divisão.
 */
function fecharModalDivisao() {
    const modalElement = document.getElementById('modalDivisao');
    if (modalElement) {
        modalElement.classList.remove('is-open');
    }
}

/**
 * Adapta o modal de divisão padrão para o contexto da página de detalhes.
 */
function abrirModalDivisaoDetalhes(ordemSetorId, qtdPendente, produtoNome) {
    if (typeof abrirModalDivisao === 'function') {
        abrirModalDivisao(ordemSetorId, qtdPendente, produtoNome);
    }
}

// Interceção AJAX padrão para envio assíncrono mantendo o controller intacto
document.addEventListener('DOMContentLoaded', () => {
    document.addEventListener('submit', function(e) {
        const form = e.target;
        if (form && (form.classList.contains('form-liberar') || form.id === 'formDivisao')) {
            e.preventDefault();

            fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            })
                .then(response => {
                    if (response.ok) {
                        if (form.id === 'formDivisao') {
                            fecharModalDivisao();
                        }
                        window.location.reload();
                    } else {
                        alert('Ocorreu um erro ao processar a operação.');
                    }
                })
                .catch(err => {
                    console.error('Erro na requisição AJAX:', err);
                    alert('Erro de comunicação com o servidor.');
                });
        }
    });
});