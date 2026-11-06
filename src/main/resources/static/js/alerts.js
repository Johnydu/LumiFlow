document.addEventListener('DOMContentLoaded', () => {
    const alerts = document.querySelectorAll('.alert-success, .alert-danger, .alert-info');

    alerts.forEach(alert => {
        // Região viva: leitores de tela anunciam a mensagem (erro é assertivo, o resto educado).
        const ehErro = alert.classList.contains('alert-danger');
        if (!alert.hasAttribute('role')) alert.setAttribute('role', ehErro ? 'alert' : 'status');
        alert.setAttribute('aria-live', ehErro ? 'assertive' : 'polite');
        alert.setAttribute('aria-atomic', 'true');

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

        // 4. Sucesso e informação somem após 4,5 s; erros ficam até o usuário fechar
        //    (o usuário precisa ter tempo de ler e agir sobre a falha).
        if (!ehErro) {
            setTimeout(closeAlert, 4500);
        }
    });
});
