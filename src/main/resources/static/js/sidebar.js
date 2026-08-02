
// ========================
// SIDEBAR — HAMBÚRGUER
// ========================

function toggleSidebar() {
    const sidebar  = document.querySelector('.sidebar');
    const overlay  = document.querySelector('.sidebar-overlay');

    sidebar.classList.toggle('open');
    overlay.classList.toggle('open');
}

function closeSidebar() {
    const sidebar  = document.querySelector('.sidebar');
    const overlay  = document.querySelector('.sidebar-overlay');

    sidebar.classList.remove('open');
    overlay.classList.remove('open');
}

// ========================
// INICIALIZAÇÃO
// ========================

document.addEventListener('DOMContentLoaded', () => {
    const overlay = document.querySelector('.sidebar-overlay');

    // fecha o sidebar ao clicar no overlay
    if (overlay) {
        overlay.addEventListener('click', closeSidebar);
    }

    // fecha o sidebar ao pressionar ESC
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') closeSidebar();
    });
});