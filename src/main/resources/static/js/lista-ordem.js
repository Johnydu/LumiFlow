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

    // Submete o formulário com Debounce para evitar submits a cada tecla digitada
    if (inputBusca) {
        let timeout = null;
        inputBusca.addEventListener('input', () => {
            clearTimeout(timeout);
            timeout = setTimeout(() => {
                formFiltros.submit();
            }, 450); // Aguarda 450ms após a última digitação
        });
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
    window.location.href = '/dashboard/ordens/nova';
}