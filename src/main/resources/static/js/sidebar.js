// ========================
// SIDEBAR — DESKTOP E MOBILE
// ========================

const SIDEBAR_STORAGE_KEY = 'sidebar-collapsed';
const DESKTOP_BREAKPOINT = 1024;

function atualizarControlesSidebar(aberta) {
    document.querySelectorAll('[data-csp-action="toggleSidebar"]').forEach(botao => {
        botao.setAttribute('aria-expanded', String(aberta));
    });
}

function aplicarEstadoDesktop() {
    const recolhida = localStorage.getItem(SIDEBAR_STORAGE_KEY) === 'true';
    document.body.classList.toggle('sidebar-collapsed', recolhida);
    document.body.classList.remove('sidebar-open');
    document.querySelector('.sidebar')?.classList.remove('open');
    document.querySelector('.sidebar-overlay')?.classList.remove('open');
    atualizarControlesSidebar(!recolhida);
}

function toggleSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    if (!sidebar) return;

    if (window.innerWidth > DESKTOP_BREAKPOINT) {
        const recolhida = document.body.classList.toggle('sidebar-collapsed');
        localStorage.setItem(SIDEBAR_STORAGE_KEY, String(recolhida));
        atualizarControlesSidebar(!recolhida);
        return;
    }

    const aberta = sidebar.classList.toggle('open');
    overlay?.classList.toggle('open', aberta);
    document.body.classList.toggle('sidebar-open', aberta);
    atualizarControlesSidebar(aberta);
}

function closeSidebar() {
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.querySelector('.sidebar-overlay');
    if (!sidebar) return;

    sidebar.classList.remove('open');
    overlay?.classList.remove('open');
    document.body.classList.remove('sidebar-open');
    atualizarControlesSidebar(false);
}

document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.querySelector('.sidebar');
    let overlay = document.querySelector('.sidebar-overlay');
    if (!sidebar) return;

    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        overlay.setAttribute('aria-hidden', 'true');
        document.body.appendChild(overlay);
    }

    overlay.addEventListener('click', closeSidebar);
    document.addEventListener('keydown', event => {
        if (event.key === 'Escape' && window.innerWidth <= DESKTOP_BREAKPOINT) {
            closeSidebar();
        }
    });

    const sincronizarViewport = () => {
        if (window.innerWidth > DESKTOP_BREAKPOINT) {
            aplicarEstadoDesktop();
        } else {
            document.body.classList.remove('sidebar-collapsed');
            closeSidebar();
        }
    };

    sincronizarViewport();
    window.addEventListener('resize', sincronizarViewport);
});
