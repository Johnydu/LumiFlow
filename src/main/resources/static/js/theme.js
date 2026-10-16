(function () {
  const savedTheme = localStorage.getItem('theme');
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
    localStorage.setItem('theme', 'light');
  } else if (tema === 'dark') {
    htmlEl.classList.add('dark');
    localStorage.setItem('theme', 'dark');
  } else {
    // Tema Identidade (Original - sem classes no html)
    localStorage.removeItem('theme'); // ou localStorage.setItem('theme', 'identity');
  }
}