// localStorage pode lançar (modo privado, cookies bloqueados): a tela deve funcionar sem ele.
function lerTemaSalvo() {
  try {
    return window.localStorage.getItem('theme');
  } catch (error) {
    return null;
  }
}

function salvarTema(tema) {
  try {
    if (tema) {
      window.localStorage.setItem('theme', tema);
    } else {
      window.localStorage.removeItem('theme');
    }
  } catch (error) {
    // Armazenamento indisponível: o tema vale só para esta página.
  }
}

(function () {
  const savedTheme = lerTemaSalvo();
  if (savedTheme === 'dark') {
    document.documentElement.classList.add('dark');
  } else if (savedTheme === 'light') {
    document.documentElement.classList.add('theme-light');
  }
})();

/**
 * Função global chamada pelo csp-actions.js ao clicar nos botões de tema
 * @param {string} tema - 'identity', 'light' ou 'dark'
 */
function mudarTema(tema) {
  const htmlEl = document.documentElement;

  // Remove as classes de tema anteriores
  htmlEl.classList.remove('theme-light', 'dark');

  if (tema === 'light') {
    htmlEl.classList.add('theme-light');
    salvarTema('light');
  } else if (tema === 'dark') {
    htmlEl.classList.add('dark');
    salvarTema('dark');
  } else {
    // Tema Identidade (Original - sem classes no html)
    salvarTema(null);
  }
}
