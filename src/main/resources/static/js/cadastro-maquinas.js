function showForm(nome, setor) {
  document.getElementById('form-panel').style.display = 'block';
  if (nome) {
    document.getElementById('form-title').textContent = 'Editar Máquina';
    document.getElementById('input-nome').value = nome;
    const sel = document.getElementById('input-setor');
    for (let opt of sel.options) {
      if (opt.text === setor) { opt.selected = true; break; }
    }
  } else {
    document.getElementById('form-title').textContent = 'Nova Máquina';
    document.getElementById('input-nome').value = '';
    document.getElementById('input-setor').selectedIndex = 0;
  }
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}

function setTab(el) {
  document.querySelectorAll('.setor-tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}
