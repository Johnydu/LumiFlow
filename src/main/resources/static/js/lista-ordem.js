/**
 * LumiFlow - Módulo da Lista de Ordens de Produção
 */

document.addEventListener('DOMContentLoaded', () => {
    initFiltros();
    initProgressBarAnimation();
    initPauseConfirmation();
});

function initPauseConfirmation() {
    document.querySelectorAll('form[data-confirm]').forEach(form => {
        form.addEventListener('submit', event => {
            if (!window.confirm(form.dataset.confirm)) {
                event.preventDefault();
            }
        });
    });
}

/**
 * Inicializa e otimiza o comportamento dos filtros
 */
function initFiltros() {
    const formFiltros = document.getElementById('formFiltros');
    if (!formFiltros) return;

    const listaOrdens = document.getElementById('listaOrdens');
    const tabelaOrdens = document.getElementById('tabelaOrdens');
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
    if (selectStatus && listaOrdens && tabelaOrdens) {
        selectStatus.addEventListener('change', () => {
            filtrarPorStatus(selectStatus.value, tabelaOrdens, listaOrdens);
        });
    }
}

/**
 * Filtra as linhas da tabela instantaneamente pelo status selecionado (Resolve o bug do "Todos os status")
 */
function filtrarPorStatus(statusSelecionado, tabela, listaOrdens) {
    const linhas = tabela.querySelectorAll('tbody tr[data-status]');
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
    const painelCount = listaOrdens.querySelector('.panel-count');
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


function voltarPagina() {
    window.location.href = '/dashboard/ordens-setor';
}