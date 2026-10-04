function showTab(nome) {
  document.querySelectorAll('.form-card').forEach(card => card.style.display = 'none');
  document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));

  document.getElementById('tab-' + nome).style.display = 'block';
  document.getElementById('tab-btn-' + (nome === 'producao' ? 'prod' : nome === 'refugo' ? 'ref' : 'ret'))
      .classList.add('active');
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
      opt.style.display = '';
      return;
    }
    opt.style.display = (opt.getAttribute('data-setor') === setorOrigemId) ? '' : 'none';
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
    campoEtapa.style.display = 'none';
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
        campoEtapa.style.display = 'flex';
        selectEtapa.required = true;
      }
    } catch (e) {
      console.error('Erro ao buscar etapas do setor de origem:', e);
    }
  });
}