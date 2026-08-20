document.addEventListener('DOMContentLoaded', () => {
    const alerts = document.querySelectorAll('.alert-success, .alert-danger, .alert-info');

    alerts.forEach(alert => {
        // 1. Força o navegador a processar o elemento e adiciona a animação de entrada
        requestAnimationFrame(() => {
            alert.classList.add('show');
        });

        // 2. Função para remover o card suavemente
        const closeAlert = () => {
            alert.classList.remove('show');
            // Aguarda a transição de saída terminar antes de remover do DOM
            setTimeout(() => alert.remove(), 400);
        };

        // 3. Permite fechar ao clicar no botão X (se existir)
        const closeBtn = alert.querySelector('.btn-close');
        if (closeBtn) {
            closeBtn.addEventListener('click', closeAlert);
        }

        // 4. Auto-remove após 4.5 segundos
        setTimeout(closeAlert, 4500);
    });
});