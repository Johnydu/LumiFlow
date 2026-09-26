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
