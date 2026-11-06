// Idioma: o LocaleChangeInterceptor lê ?lang= e guarda o locale na sessão.
function mudarIdioma(lang) {
    const url = new URL(window.location.href);
    url.searchParams.set('lang', lang);
    window.location.assign(url);
}

// O tema em si é aplicado por mudarTema() (theme.js); aqui só marcamos o botão ativo.
function atualizarBotoesTema() {
    const atual = lerTemaSalvo() || 'identity';
    document.querySelectorAll('[data-csp-action="mudarTema"]').forEach(botao => {
        botao.setAttribute('aria-pressed', String(botao.dataset.cspValue === atual));
    });
}

document.addEventListener('DOMContentLoaded', atualizarBotoesTema);
document.addEventListener('click', event => {
    if (event.target instanceof Element && event.target.closest('[data-csp-action="mudarTema"]')) {
        atualizarBotoesTema();
    }
});
