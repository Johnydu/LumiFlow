(() => {
  const breakpoint = 1024;
  const storageKey = 'sidebar-collapsed';

  if (window.innerWidth <= breakpoint) return;

  try {
    if (window.localStorage.getItem(storageKey) !== 'true') return;
  } catch (error) {
    console.warn('Não foi possível ler a preferência da sidebar:', error);
    return;
  }

  const root = document.documentElement;
  root.classList.add('sidebar-collapsed-init');
  document.addEventListener('DOMContentLoaded', () => {
    const temSidebar = document.querySelector('.sidebar') !== null;
    if (temSidebar) {
      document.body.classList.add('sidebar-collapsed');
    }

    // Sem sidebar não há nada a restaurar: remove a classe de imediato.
    if (!temSidebar) {
      root.classList.remove('sidebar-collapsed-init');
      return;
    }

    // setTimeout como garantia: requestAnimationFrame fica pausado em aba em segundo plano.
    let removida = false;
    const remover = () => {
      if (removida) return;
      removida = true;
      root.classList.remove('sidebar-collapsed-init');
    };
    requestAnimationFrame(() => requestAnimationFrame(remover));
    setTimeout(remover, 300);
  }, { once: true });
})();
