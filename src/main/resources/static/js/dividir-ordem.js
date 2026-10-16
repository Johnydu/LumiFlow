'use strict';

let contadorLinhas = 0;
let qtdTotalDivisao = 0;

document.addEventListener('click', event => {
    if (!(event.target instanceof Element)) {
        return;
    }

    const botaoFechar = event.target.closest('[data-divisao-fechar]');

    if (botaoFechar) {
        fecharModalDivisao();
        return;
    }

    const botaoAdicionar = event.target.closest('[data-divisao-adicionar]');

    if (botaoAdicionar) {
        adicionarLinhaDivisao();
        return;
    }

    const botaoDividir = event.target.closest('.btn-dividir');

    if (!botaoDividir) {
        return;
    }

    abrirModalDivisao(
        botaoDividir.dataset.id,
        botaoDividir.dataset.qtd,
        botaoDividir.dataset.nome,
        botaoDividir.dataset.setor,
        botaoDividir.dataset.ordem
    );
});

function abrirModalDivisao(
    ordemSetorId,
    quantidadeTotal,
    produtoNome,
    setorId,
    ordemId
) {
    const modal = document.getElementById('modalDivisao');
    const form = document.getElementById('formDivisao');

    if (!modal || !form) {
        console.error('Modal de divisão ou formulário não encontrados no DOM.');
        return;
    }

    // Fallback inteligente caso o setorId não venha direto do botão
    let resolvedSetorId = setorId;
    if (!resolvedSetorId || Number(resolvedSetorId) <= 0) {
        const bodySetor = document.body.dataset.setorId;
        if (bodySetor) {
            resolvedSetorId = bodySetor;
        } else {
            const match = window.location.pathname.match(/\/ordens-setor\/(\d+)/);
            if (match) {
                resolvedSetorId = match[1];
            }
        }
    }

    const idEtapa = Number(ordemSetorId);
    const idSetor = Number(resolvedSetorId);
    const quantidade = Number(quantidadeTotal);

    // Validação robusta com log detalhado para facilitar depuração (F12)
    if (
        !Number.isSafeInteger(idEtapa) ||
        idEtapa <= 0 ||
        !Number.isSafeInteger(idSetor) ||
        idSetor <= 0 ||
        !Number.isSafeInteger(quantidade) ||
        quantidade <= 0
    ) {
        console.error('Dados inválidos para divisão da etapa:', {
            ordemSetorId,
            quantidadeTotal,
            setorId: resolvedSetorId
        });
        return;
    }

    qtdTotalDivisao = quantidade;
    contadorLinhas = 0;

    const elProduto = document.getElementById('modalProdutoNome');
    if (elProduto) {
        elProduto.textContent = produtoNome || '';
    }

    const elQtdTotal = document.getElementById('modalQtdTotal');
    if (elQtdTotal) {
        elQtdTotal.textContent = String(quantidade);
    }

    const elQtdEsperada = document.getElementById('qtdEsperada');
    if (elQtdEsperada) {
        elQtdEsperada.textContent = String(quantidade);
    }

    const linhasContainer = document.getElementById('linhasDivisao');
    if (linhasContainer) {
        linhasContainer.replaceChildren();
    }

    form.action =
        `/dashboard/ordens-setor/${idSetor}` +
        `/ordens/${idEtapa}/dividir`;

    const campoOrdem = document.getElementById('divisaoOrdemId');
    if (campoOrdem) {
        campoOrdem.value = ordemId || '';
    }

    adicionarLinhaDivisao();
    adicionarLinhaDivisao();
    atualizarSoma();

    modal.style.display = 'flex';
    modal.classList.add('is-open');
}

function fecharModalDivisao() {
    const modal = document.getElementById('modalDivisao');

    if (!modal) {
        return;
    }

    modal.classList.remove('is-open');
    modal.style.display = 'none';
}

function adicionarLinhaDivisao() {
    const container = document.getElementById('linhasDivisao');

    if (!container) {
        return;
    }

    const index = contadorLinhas++;

    const linha = document.createElement('div');
    linha.className = 'linha-divisao';
    linha.id = `linha-${index}`;

    const inputVariante = document.createElement('input');
    inputVariante.type = 'text';
    inputVariante.name = `divisoes[${index}].variante`;
    inputVariante.placeholder = 'Ex.: Branco, PT...';
    inputVariante.maxLength = 100;
    inputVariante.required = true;
    inputVariante.autocomplete = 'off';
    inputVariante.className = 'input-variante';

    const inputQuantidade = document.createElement('input');
    inputQuantidade.type = 'number';
    inputQuantidade.name = `divisoes[${index}].quantidade`;
    inputQuantidade.placeholder = 'Qtd';
    inputQuantidade.min = '1';
    inputQuantidade.step = '1';
    inputQuantidade.required = true;
    inputQuantidade.className = 'input-qtd';

    inputQuantidade.addEventListener('input', atualizarSoma);

    const botaoRemover = document.createElement('button');
    botaoRemover.type = 'button';
    botaoRemover.className = 'btn-remove-linha';
    botaoRemover.textContent = '×';
    botaoRemover.setAttribute('aria-label', 'Remover variante');

    botaoRemover.addEventListener('click', () => {
        linha.remove();
        atualizarSoma();
    });

    linha.append(inputVariante, inputQuantidade, botaoRemover);
    container.appendChild(linha);

    atualizarSoma();
}

function atualizarSoma() {
    const inputs = document.querySelectorAll('#linhasDivisao .input-qtd');

    let soma = 0;
    let quantidadesValidas = true;

    inputs.forEach(input => {
        const valor = input.valueAsNumber;

        if (
            input.value.trim() === '' ||
            !Number.isSafeInteger(valor) ||
            valor <= 0
        ) {
            quantidadesValidas = false;
            return;
        }

        soma += valor;
    });

    const somaAtual = document.getElementById('somaAtual');
    const restanteInfo = document.getElementById('restanteInfo');
    const botaoConfirmar = document.getElementById('btnConfirmarDivisao');

    if (!somaAtual || !botaoConfirmar) {
        return;
    }

    somaAtual.textContent = String(soma);

    const somaPermitida =
        soma > 0 &&
        soma <= qtdTotalDivisao &&
        Number.isSafeInteger(soma);

    botaoConfirmar.disabled =
        !somaPermitida || !quantidadesValidas;

    somaAtual.style.color = somaPermitida
        ? 'var(--color-success)'
        : 'var(--color-danger)';

    if (restanteInfo) {
        // CORRIGIDO: Adicionado o operador de subtração (-) que estava em falta
        const restante = qtdTotalDivisao - soma;

        restanteInfo.textContent =
            restante > 0 && somaPermitida
                ? `Restante (${restante}) ficará sem variante definida`
                : '';
    }
}

document.addEventListener('submit', event => {
    if (event.target.id !== 'formDivisao') {
        return;
    }

    atualizarSoma();

    const botaoConfirmar = document.getElementById('btnConfirmarDivisao');

    if (!botaoConfirmar || botaoConfirmar.disabled) {
        event.preventDefault();
        return;
    }

    // Reindexa as linhas sequencialmente (0, 1, 2...) antes de enviar para garantir o bind correto no Spring
    const linhas = document.querySelectorAll('#linhasDivisao .linha-divisao');
    linhas.forEach((linha, novoIndex) => {
        const inputVariante = linha.querySelector('.input-variante');
        const inputQtd = linha.querySelector('.input-qtd');

        if (inputVariante) {
            inputVariante.name = `divisoes[${novoIndex}].variante`;
        }
        if (inputQtd) {
            inputQtd.name = `divisoes[${novoIndex}].quantidade`;
        }
    });
});