document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.card-metric, .kpi-card').forEach((card, index) => {
    card.classList.add('card-entering');
    setTimeout(() => card.classList.remove('card-entering'), index * 80 + 400);
  });

  initFiltroOrdens();
});

/**
 * Filtro da tabela de ordens: é de SERVIDOR (a lista é paginada), então qualquer mudança
 * volta para a página 0 e envia o formulário.
 */
function initFiltroOrdens() {
  const form = document.getElementById('formFiltroOrdens');
  if (!form) return;

  const campoPagina = form.querySelector('input[name="page"]');
  const reiniciarPagina = () => {
    if (campoPagina) campoPagina.value = '0';
  };

  const busca = form.querySelector('input[name="busca"]');
  if (busca) {
    if (busca.value) {
      const fim = busca.value.length;
      busca.focus();
      busca.setSelectionRange(fim, fim);
    }

    let temporizador = null;
    busca.addEventListener('input', () => {
      clearTimeout(temporizador);
      temporizador = setTimeout(() => {
        reiniciarPagina();
        form.requestSubmit();
      }, 500);
    });
  }

  const status = form.querySelector('select[name="status"]');
  if (status) {
    status.addEventListener('change', () => {
      reiniciarPagina();
      form.requestSubmit();
    });
  }
}

function novaOrdemForm() {
  window.location.href = '/dashboard/ordens/novaOrdem';
}