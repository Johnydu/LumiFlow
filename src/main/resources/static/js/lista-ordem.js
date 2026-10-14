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
            linha.hidden = false;
            contadorVisiveis++;
        } else {
            linha.hidden = true;
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
        fill.classList.add('progress-animated');
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
    document.getElementById('modalDivisao').classList.add('is-open');
    document.getElementById('modalProdutoNome').textContent = produtoNome;
    document.getElementById('modalQtdTotal').textContent = quantidadeTotal;
    document.getElementById('qtdEsperada').textContent = quantidadeTotal;

    // Conversão garantida para número inteiro para evitar falha de comparação
    qtdTotalDivisao = parseInt(quantidadeTotal) || 0;

    contadorLinhas = 0;
    document.getElementById('linhasDivisao').replaceChildren();

    const formDivisao = document.getElementById('formDivisao');
    const actionTemplate = formDivisao.getAttribute('action');
    const action = actionTemplate.replace(
        /\/0\/dividir$/,
        `/${encodeURIComponent(ordemSetorId)}/dividir`
    );
    if (action === actionTemplate) {
        throw new Error('Não foi possível definir a URL para dividir a ordem.');
    }
    formDivisao.action = action;

    adicionarLinhaDivisao();
    adicionarLinhaDivisao();

    // Executa a validação inicial com as linhas vazias (0)
    atualizarSoma();
}

function fecharModalDivisao() {
    document.getElementById('modalDivisao').classList.remove('is-open');
}

function adicionarLinhaDivisao() {
    const container = document.getElementById('linhasDivisao');
    const index = contadorLinhas++;

    const linha = document.createElement('div');
    linha.className = 'linha-divisao';
    linha.id = 'linha-' + index;

    const inputVariante = document.createElement('input');
    inputVariante.type = 'text';
    inputVariante.name = `divisoes[${index}].variante`;
    inputVariante.placeholder = 'Ex: Branco, PT...';
    inputVariante.required = true;
    inputVariante.className = 'input-variante';

    const inputQuantidade = document.createElement('input');
    inputQuantidade.type = 'number';
    inputQuantidade.name = `divisoes[${index}].quantidade`;
    inputQuantidade.placeholder = 'Qtd';
    inputQuantidade.min = '1';
    inputQuantidade.required = true;
    inputQuantidade.className = 'input-qtd';
    inputQuantidade.addEventListener('input', atualizarSoma);

    const botaoRemover = document.createElement('button');
    botaoRemover.type = 'button';
    botaoRemover.className = 'btn-remove-linha';
    botaoRemover.textContent = '×';
    botaoRemover.addEventListener('click', () => removerLinha(index));

    linha.append(inputVariante, inputQuantidade, botaoRemover);

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
        document.getElementById('somaAtual').classList.add('text-success');
        document.getElementById('somaAtual').classList.remove('text-danger');
    } else {
        btnConfirmar.disabled = true;
        document.getElementById('somaAtual').classList.add('text-danger');
        document.getElementById('somaAtual').classList.remove('text-success');
    }
}

function voltarPagina() {
    window.location.href = '/dashboard/ordens-setor';
}