function showTab(nome) {
  document.querySelectorAll('.form-card').forEach(card => card.style.display = 'none');
  document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));

  document.getElementById('tab-' + nome).style.display = 'block';
  document.getElementById('tab-btn-' + (nome === 'producao' ? 'prod' : nome === 'refugo' ? 'ref' : 'ret')).classList.add('active');
}

document.addEventListener('DOMContentLoaded', () => {
  if (typeof abaComErro !== 'undefined' && abaComErro) {
    showTab(abaComErro);
  }
});

