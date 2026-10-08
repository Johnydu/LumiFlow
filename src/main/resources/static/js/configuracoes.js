// Lógica de Idioma
function mudarIdioma(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.assign(url);
}

// Lógica de Tema Global
function mudarTema(tema) {
    const root = document.documentElement;
    root.classList.remove('dark', 'theme-light');

    if (tema === 'dark') {
        localStorage.setItem('theme', 'dark');
        root.classList.add('dark');
    } else if (tema === 'light') {
        localStorage.setItem('theme', 'light');
        root.classList.add('theme-light');
    } else {
        localStorage.removeItem('theme');
    }

    document.querySelectorAll('[data-theme]').forEach(button => {
        button.setAttribute('aria-pressed', String(button.dataset.theme === tema));
    });
}

document.addEventListener('DOMContentLoaded', () => {
    const savedTheme = localStorage.getItem('theme');
    const currentTheme = savedTheme || 'identity';
    document.querySelectorAll('[data-theme]').forEach(button => {
        button.setAttribute('aria-pressed', String(button.dataset.theme === currentTheme));
    });
});