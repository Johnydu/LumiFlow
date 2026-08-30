/**
 * LumiFlow - Módulo da Dashboard Principal
 */
document.addEventListener('DOMContentLoaded', () => {
    initDashboard();
});

/**
 * Inicializa todos os módulos e eventos da Dashboard
 */
function initDashboard() {
    initCardsMetricAnimation();
    initCharts();
    initQuickActions();
}

/**
 * Anima a transição ou entrada dos cards numéricos
 */
function initCardsMetricAnimation() {
    const metricCards = document.querySelectorAll('.card-metric, .kpi-card');

    metricCards.forEach((card, index) => {
        card.style.opacity = '0';
        card.style.transform = 'translateY(15px)';

        setTimeout(() => {
            card.style.transition = 'all 0.4s ease-out';
            card.style.opacity = '1';
            card.style.transform = 'translateY(0)';
        }, index * 80);
    });
}

/**
 * Inicialização dos gráficos da Dashboard (Chart.js)
 * Descomente e ajuste os IDs conforme seu HTML, se utilizar gráficos.
 */
function initCharts() {
    const canvasProgresso = document.getElementById('chartProgresso');
    if (canvasProgresso && typeof Chart !== 'undefined') {
        new Chart(canvasProgresso, {
            type: 'bar',
            data: {
                labels: ['Corte', 'Usinagem', 'Pintura', 'Montagem', 'Qualidade'],
                datasets: [{
                    label: 'Ordens por Setor',
                    data: [12, 19, 8, 15, 5],
                    backgroundColor: 'rgba(59, 130, 246, 0.85)',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                }
            }
        });
    }
}

/**
 * Delegação de eventos para botões ou ações rápidas na Dashboard
 */
function initQuickActions() {
    // Exemplo: Permite clicar em cards de status para ir direto à lista filtrada
    const statusCards = document.querySelectorAll('[data-status-filter]');

    statusCards.forEach(card => {
        card.addEventListener('click', () => {
            const status = card.getAttribute('data-status-filter');
            if (status) {
                window.location.href = `/dashboard/ordens?status=${status}`;
            }
        });
    });
}

/**
 * Redireciona para o formulário de criação de Nova Ordem
 * Esta função deve estar em escopo global para ser chamada no Topbar via inline JS
 */
function novaOrdemForm() {
    window.location.href = '/dashboard/ordens/novaOrdem';
}