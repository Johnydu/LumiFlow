/**
 * LumiFlow - Módulo da Lista de Ordens de Produção
 */

document.addEventListener('DOMContentLoaded', () => {
    initFiltros();
    initProgressBarAnimation();
});

/**
 * Inicializa e otimiza o comportamento dos filtros
 */
function initFiltros() {
    const formFiltros = document.getElementById('formFiltros');
    if (!formFiltros) return;

    const inputBusca = formFiltros.querySelector('input[name="busca"]');

    if (inputBusca) {
        // Se houver busca prévia na URL, posiciona o cursor ao final do texto
        if (inputBusca.value) {
            inputBusca.focus();
            const val = inputBusca.value;
            inputBusca.value = '';
            inputBusca.value = val;
        }

        let timeout = null;
        inputBusca.addEventListener('input', () => {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                formFiltros.submit();
            }, 500);
        });
    }

    // Configura o filtro de status para rodar via JS de forma instantânea
    const selectStatus = document.getElementById('filtroStatus');
    if (selectStatus) {
        selectStatus.addEventListener('change', () => {
            filtrarPorStatus(selectStatus.value);
        });
    }
}

/**
 * Filtra as linhas da tabela instantaneamente pelo status selecionado (Resolve o bug do "Todos os status")
 */
function filtrarPorStatus(statusSelecionado) {
    const linhas = document.querySelectorAll('table tbody tr[data-status]');
    let contadorVisiveis = 0;

    linhas.forEach(linha => {
        const statusLinha = linha.getAttribute('data-status');

        if (statusSelecionado === '' || statusLinha === statusSelecionado) {
            linha.style.display = '';
            contadorVisiveis++;
        } else {
            linha.style.display = 'none';
        }
    });

    // Atualiza o contador de ordens no cabeçalho do painel de forma dinâmica
    const painelCount = document.querySelector('.panel-count');
    if (painelCount) {
        painelCount.textContent = `${contadorVisiveis} ordens encontradas`;
    }
}

/**
 * Anima as barras de progresso no carregamento da tabela
 */
function initProgressBarAnimation() {
    const progressFills = document.querySelectorAll('.prog-fill');

    progressFills.forEach(fill => {
        const targetWidth = fill.style.width;
        fill.style.width = '0%';

        requestAnimationFrame(() => {
            setTimeout(() => {
                fill.style.transition = 'width 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
                fill.style.width = targetWidth;
            }, 50);
        });
    });
}

/**
 * Ação chamada pelo botão do Topbar "+ Nova Ordem"
 */
function novaOrdemForm() {
    window.location.href = '/dashboard/ordens/novaOrdem';
}

let contadorLinhas = 0;
let qtdTotalDivisao = 0;

function abrirModalDivisao(ordemSetorId, quantidadeTotal, produtoNome) {
    document.getElementById('modalDivisao').style.display = 'flex';
    document.getElementById('modalProdutoNome').textContent = produtoNome;
    document.getElementById('modalQtdTotal').textContent = quantidadeTotal;
    document.getElementById('qtdEsperada').textContent = quantidadeTotal;

    // Conversão garantida para número inteiro para evitar falha de comparação
    qtdTotalDivisao = parseInt(quantidadeTotal) || 0;

    contadorLinhas = 0;
    document.getElementById('linhasDivisao').innerHTML = '';

    // Pega o ID do setor direto do atributo data-setor-id do body
    const setorId = document.body.getAttribute('data-setor-id');

    document.getElementById('formDivisao').action =
        `/dashboard/ordens-setor/${setorId}/ordens/${ordemSetorId}/dividir`;

    adicionarLinhaDivisao();
    adicionarLinhaDivisao();

    // Executa a validação inicial com as linhas vazias (0)
    atualizarSoma();
}

function fecharModalDivisao() {
    document.getElementById('modalDivisao').style.display = 'none';
}

function adicionarLinhaDivisao() {
    const container = document.getElementById('linhasDivisao');
    const index = contadorLinhas++;

    const linha = document.createElement('div');
    linha.className = 'linha-divisao';
    linha.id = 'linha-' + index;
    linha.innerHTML = `
        <input type="text" name="divisoes[${index}].variante" placeholder="Ex: Branco, PT..." 
        required class="input-variante"/>
        <input type="number" name="divisoes[${index}].quantidade" placeholder="Qtd" min="1" 
        required class="input-qtd" oninput="atualizarSoma()"/>
        <button type="button" class="btn-remove-linha" onclick="removerLinha(${index})">&times;</button>
    `;

    container.appendChild(linha);
}

function removerLinha(index) {
    const linha = document.getElementById('linha-' + index);
    if (linha) linha.remove();
    atualizarSoma();
}

function atualizarSoma() {
    const inputs = document.querySelectorAll('.input-qtd');
    let soma = 0;
    inputs.forEach(input => soma += parseInt(input.value) || 0);

    document.getElementById('somaAtual').textContent = soma;

    const restanteInfo = document.getElementById('restanteInfo');
    if (restanteInfo) {
        restanteInfo.textContent = soma < qtdTotalDivisao
            ? `Restante (${qtdTotalDivisao - soma}) ficará sem variante definida`
            : '';
    }

    const btnConfirmar = document.getElementById('btnConfirmarDivisao');
    if (soma > 0 && soma <= qtdTotalDivisao) {
        btnConfirmar.disabled = false;
        document.getElementById('somaAtual').style.color = 'var(--color-success)';
    } else {
        btnConfirmar.disabled = true;
        document.getElementById('somaAtual').style.color = 'var(--color-danger)';
    }
}

function voltarPagina() {
    window.location.href = '/dashboard/ordens-setor';
}