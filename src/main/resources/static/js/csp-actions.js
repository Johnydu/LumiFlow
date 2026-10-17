function executarAcaoCsp(element, tipo) {
  const action = element.dataset[`csp${tipo}`];
  if (!action) return;

  if (action === 'goBack') {
    window.history.back();
    return;
  }

  const handler = window[action];
  if (typeof handler !== 'function') {
    console.error(`Ação da interface não encontrada: ${action}`);
    return;
  }

  if (action === 'showTab') {
    handler(element.dataset.cspValue, element);
  } else if (action === 'mudarTema') {
    handler(element.dataset.cspValue);
  } else if (action === 'moverEtapa') {
    handler(element, Number(element.dataset.cspDirection));
  } else if (element.dataset.cspArgument === 'element') {
    handler(element);
  } else if (element.dataset.cspArgument === 'value') {
    handler(element.value);
  } else if (element.dataset.cspArgument === 'dataset') {
    handler(element.dataset.id, element.dataset.qtd, element.dataset.nome);
  } else {
    handler();
  }
}

document.addEventListener('click', event => {
  const element = event.target instanceof Element
    ? event.target.closest('[data-csp-action]')
    : null;
  if (element) executarAcaoCsp(element, 'Action');
});

document.addEventListener('change', event => {
  const element = event.target.closest('[data-csp-change]');
  if (element) executarAcaoCsp(element, 'Change');
});

document.addEventListener('submit', event => {
  const message = event.submitter?.dataset.cspConfirm;
  if (message && !window.confirm(message)) {
    event.preventDefault();
  }
});

document.addEventListener('keydown', event => {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;

  const currentTab = event.target instanceof Element
    ? event.target.closest('[role="tab"]')
    : null;
  const tabList = currentTab?.closest('[role="tablist"]');
  if (!currentTab || !tabList) return;

  const tabs = Array.from(tabList.querySelectorAll('[role="tab"]:not(:disabled)'));
  const currentIndex = tabs.indexOf(currentTab);
  if (currentIndex < 0 || tabs.length < 2) return;

  event.preventDefault();
  const nextIndex = event.key === 'Home'
    ? 0
    : event.key === 'End'
      ? tabs.length - 1
      : (currentIndex + (event.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length;

  tabs[nextIndex].focus();
  tabs[nextIndex].click();
});

document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.progress-value[data-progress]').forEach(element => {
    const rawPercent = Number(element.dataset.progress.replace(',', '.'));
    const percent = Math.max(0, Math.min(100, Math.round(rawPercent || 0)));
    element.classList.add(`progress-${percent}`);
  });
});
