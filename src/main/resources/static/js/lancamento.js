function showTab(nome) {
  document.querySelectorAll('.form-card').forEach(card => card.style.display = 'none');
  document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));

  document.getElementById('tab-' + nome).style.display = 'block';
  document.getElementById('tab-btn-' + (nome === 'producao' ? 'prod' : nome === 'refugo' ? 'ref' : 'ret'))
      .classList.add('active');
}

document.addEventListener('DOMContentLoaded', () => {
  if (typeof abaComErro !== 'undefined' && abaComErro) {
    showTab(abaComErro);
  }
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

    selectEtapa.innerHTML = '<option value="">Selecione a etapa</option>';
    campoEtapa.style.display = 'none';
    selectEtapa.required = false;

    if (!setorId) return;

    try {
      const response = await fetch(`/dashboard/roteiros/${produtoId}/setor/${setorId}/etapas`);
      if (!response.ok) return;

      const etapas = await response.json();

      // Só pergunta quando existe ambiguidade real (mais de uma etapa no roteiro)
      if (etapas.length > 1) {
        etapas.forEach(etapa => {
          const option = document.createElement('option');
          option.value = etapa.id;
          option.textContent = etapa.nome;
          selectEtapa.appendChild(option);
        });
        campoEtapa.style.display = 'flex';
        selectEtapa.required = true;
      }
    } catch (e) {
      console.error('Erro ao buscar etapas do setor de origem:', e);
    }
  });
}
