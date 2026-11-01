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
    document.body.classList.add('sidebar-collapsed');
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        root.classList.remove('sidebar-collapsed-init');
      });
    });
  }, { once: true });
})();
