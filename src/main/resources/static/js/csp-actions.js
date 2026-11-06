// Somente estas funções globais podem ser acionadas por data-csp-action/data-csp-change.
// Ao criar uma nova ação na interface, inclua o nome aqui.
const ACOES_CSP_PERMITIDAS = new Set([
  'adicionarEtapa',
  'adicionarEtapaSetor',
  'closeSidebar',
  'editarSetorDoBotao',
  'filtrarMaquinasRetrabalho',
  'handleEditarMaquina',
  'handleEditarOperador',
  'handleEditarProduto',
  'handleEditarUsuario',
  'handleFiltrarSetorSelect',
  'hideForm',
  'mudarIdioma',
  'mudarTema',
  'novaMaquinaForm',
  'novaOrdemForm',
  'novoOperadorForm',
  'novoProdutoForm',
  'novoRoteiroForm',
  'novoSetorForm',
  'novoUsuarioForm',
  'showTab',
  'togglePasswordVisibility',
  'toggleSidebar',
  'voltarPagina',
  'voltarParaSetor'
]);

function executarAcaoCsp(element, tipo) {
  const action = element.dataset[`csp${tipo}`];
  if (!action) return;

  if (action === 'goBack') {
    window.history.back();
    return;
  }

  if (!ACOES_CSP_PERMITIDAS.has(action)) {
    console.error(`Ação da interface não permitida: ${action}`);
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
    return;
  }

  // Proteção contra envio duplo (Liberar, Pausar, Dividir etc.): depois que todos os
  // listeners rodaram, se o envio seguiu em frente, desabilita os botões de submit.
  // O adiamento garante que name/value do botão clicado ainda entrem no POST.
  const form = event.target;
  if (!(form instanceof HTMLFormElement) || form.method.toLowerCase() !== 'post') return;
  setTimeout(() => {
    if (event.defaultPrevented) return;
    form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach(botao => {
      botao.disabled = true;
      botao.dataset.bloqueadoEnvio = 'true';
    });
  }, 0);
});

// Voltar pelo histórico (bfcache) restaura a página; reabilita os botões travados acima.
window.addEventListener('pageshow', event => {
  if (!event.persisted) return;
  document.querySelectorAll('[data-bloqueado-envio]').forEach(botao => {
    botao.disabled = false;
    delete botao.dataset.bloqueadoEnvio;
  });
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
