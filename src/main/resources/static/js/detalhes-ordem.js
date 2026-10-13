/**
 * Abre o modal de divisão padrão e configura os dados da etapa.
 */
function abrirModalDivisao(ordemSetorId, quantidadeTotal, produtoNome) {
    const modalElement = document.getElementById('modalDivisao');
    if (!modalElement) {
        console.error("Modal #modalDivisao não encontrado no DOM!");
        return;
    }

    document.getElementById('modalDivisao').style.display = 'flex';
    document.getElementById('modalProdutoNome').textContent = produtoNome;
    document.getElementById('modalQtdTotal').textContent = quantidadeTotal;
    document.getElementById('qtdEsperada').textContent = quantidadeTotal;

    // Remove pontos, vírgulas ou outros caracteres não numéricos caso o número venha formatado (ex: "3.000")
    const qtdString = String(quantidadeTotal).replace(/\D/g, '');

    // Conversão garantida para número inteiro com o valor limpo
    qtdTotalDivisao = parseInt(qtdString) || 0;

    contadorLinhas = 0;
    document.getElementById('linhasDivisao').replaceChildren();

    const formDivisao = document.getElementById('formDivisao');
    if (formDivisao) {
        const actionTemplate = formDivisao.getAttribute('action') || '/dashboard/ordens-setor/0/ordens/0/dividir';
        const action = actionTemplate.replace(
            /\/0\/dividir$/,
            `/${encodeURIComponent(ordemSetorId)}/dividir`
        );
        formDivisao.action = action;
    }

    if (typeof adicionarLinhaDivisao === 'function') {
        adicionarLinhaDivisao();
        adicionarLinhaDivisao();
    }

    // Executa a validação inicial com as linhas vazias (0)
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
        modalElement.style.display = 'none';
    }
}

/**
 * Adapta o modal de divisão padrão para o contexto da página de detalhes (utilizando /dashboard/ordens/0/ordens/{id}/dividir)[cite: 3].
 */
function abrirModalDivisaoDetalhes(ordemSetorId, qtdPendente, produtoNome) {
    if (typeof abrirModalDivisao === 'function') {
        abrirModalDivisao(ordemSetorId, qtdPendente, produtoNome);

        // Ajusta a rota do formulário para o endpoint correto de detalhes no OrdemDetalheController[cite: 3]
        const form = document.getElementById('formDivisao');
        if (form) {
            form.action = `/dashboard/ordens/0/ordens/${ordemSetorId}/dividir`;
        }
    }
}