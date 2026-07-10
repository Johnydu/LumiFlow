function showTab(tab, el) {
  document.getElementById('tab-consumo').style.display = tab === 'consumo' ? 'block' : 'none';
  document.getElementById('tab-entrada').style.display = tab === 'entrada' ? 'block' : 'none';
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}
