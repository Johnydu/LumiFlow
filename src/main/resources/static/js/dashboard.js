document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.card-metric, .kpi-card').forEach((card, index) => {
    card.classList.add('card-entering');
    setTimeout(() => card.classList.remove('card-entering'), index * 80 + 400);
  });
});

function novaOrdemForm() {
  window.location.href = '/dashboard/ordens/novaOrdem';
}
