document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.card-metric, .kpi-card').forEach((card, index) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(15px)';

    setTimeout(() => {
      card.style.transition = 'opacity 0.4s ease-out, transform 0.4s ease-out';
      card.style.opacity = '1';
      card.style.transform = 'translateY(0)';
    }, index * 80);
  });
});

function novaOrdemForm() {
  window.location.href = '/dashboard/ordens/novaOrdem';
}
