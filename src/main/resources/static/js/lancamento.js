function showTab(nome) {
  document.querySelectorAll('.tabs .tab').forEach(tab => {
    tab.classList.remove('active');
    tab.setAttribute('aria-selected', 'false');
    tab.tabIndex = -1;
  });
  document.querySelectorAll('.content > .form-card').forEach(card => {
    card.classList.add('is-hidden');
    card.classList.remove('is-visible');
  });

  const selectedCard = document.getElementById(`tab-${nome}`);
  const tabId = `tab-btn-${nome === 'producao' ? 'prod' : nome === 'refugo' ? 'ref' : 'ret'}`;
  const selectedTab = document.getElementById(tabId);
  if (selectedCard && selectedTab) {
    selectedCard.classList.remove('is-hidden');
    selectedCard.classList.add('is-visible');
    selectedTab.classList.add('active');
    selectedTab.setAttribute('aria-selected', 'true');
    selectedTab.tabIndex = 0;
  }
}

document.addEventListener('DOMContentLoaded', () => {
  const { abaErro, produtoId } = document.body.dataset;

  if (abaErro) showTab(abaErro);
  if (produtoId) initEtapaOrigemRefugo(produtoId);
});

function voltarPagina() {
  const url = document.body.dataset.urlVoltar;
  if (url) {
    window.location.href = url;
  }
}

function filtrarMaquinasRetrabalho() {
  const selectRefugo = document.getElementById('selectRefugoRetrabalho');
  const selectMaquina = document.getElementById('selectMaquinaRetrabalho');
  const opcaoSelecionada = selectRefugo.options[selectRefugo.selectedIndex];
  const setorOrigemId = opcaoSelecionada.getAttribute('data-setor-origem');

  Array.from(selectMaquina.options).forEach(opt => {
    if (!opt.value) {
      opt.hidden = false;
      return;
    }
    opt.hidden = opt.getAttribute('data-setor') !== setorOrigemId;
  });

  selectMaquina.value = '';
}

function initEtapaOrigemRefugo(produtoId) {
  const selectSetor = document.getElementById('selectSetorOrigemRefugo');
  const campoEtapa = document.getElementById('campoEtapaOrigemRefugo');
  const selectEtapa = document.getElementById('selectEtapaOrigemRefugo');

  if (!selectSetor || !campoEtapa || !selectEtapa) return;

  selectSetor.addEventListener('change', async () => {
    const setorId = selectSetor.value;

    selectEtapa.replaceChildren(new Option('Selecione a etapa', ''));
    campoEtapa.classList.remove('is-visible');
    campoEtapa.classList.add('is-hidden');
    selectEtapa.required = false;

    if (!setorId) return;

    try {
      const response = await fetch(`/api/roteiros/produto/${produtoId}/setor/${setorId}/etapas`);
      if (!response.ok) {
        console.warn('Falha ao buscar etapas do setor de origem. Status:', response.status);
        return;
      }

      const etapas = await response.json();

      // Só pergunta quando existe ambiguidade real (mais de uma etapa no roteiro)
      if (etapas.length > 1) {
        etapas.forEach(etapa => selectEtapa.appendChild(new Option(etapa.nome, etapa.id)));
        campoEtapa.classList.remove('is-hidden');
        campoEtapa.classList.add('is-visible');
        selectEtapa.required = true;
      }
    } catch (e) {
      console.error('Erro ao buscar etapas do setor de origem:', e);
    }
  });
}