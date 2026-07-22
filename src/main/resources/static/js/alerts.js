document.addEventListener('DOMContentLoaded', () => {
    const alert = document.querySelector('.alert-success, .alert-error');

    if (alert) {
        setTimeout(() => {
            alert.style.opacity = '0';

            setTimeout(() => {
                alert.remove();
            }, 300);
        }, 3000);
    }
});