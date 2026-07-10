function setSetor(el) {
  document.querySelectorAll('.setor-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}
