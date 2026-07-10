const ordem = ['Corte', 'Dobra', 'Solda', 'Lixa', 'Pintura', 'Colagem', 'Acabamento', 'Montagem2'];
const nomes = {
  Corte: 'Corte',
  Dobra: 'Dobra',
  Solda: 'Solda',
  Lixa: 'Lixa',
  Pintura: 'Pintura',
  Colagem: 'Colagem',
  Acabamento: 'Acabamento',
  Montagem2: 'Montagem 2'
};

function showTab(tab, el) {
  document.getElementById('tab-dados').style.display = tab === 'dados' ? 'block' : 'none';
  document.getElementById('tab-roteiro').style.display = tab === 'roteiro' ? 'block' : 'none';
  document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
  el.classList.add('active');
}

function toggle(el, key) {
  el.classList.toggle('checked');
  updatePreview();
}

function updatePreview() {
  const selecionados = ordem.filter(k => {
    const el = document.querySelector(`[onclick*="'${k}'"]`);
    return el && el.classList.contains('checked');
  });

  let n = 1;
  ordem.forEach(k => {
    const seq = document.getElementById('seq-' + k);
    const el = document.querySelector(`[onclick*="'${k}'"]`);
    if (el && el.classList.contains('checked')) {
      seq.textContent = n + 'º';
      seq.style.display = '';
      n++;
    } else if (seq) {
      seq.style.display = 'none';
    }
  });

  const pills = document.getElementById('preview-pills');
  if (selecionados.length === 0) {
    pills.innerHTML = '<span class="r-empty">Nenhum setor selecionado.</span>';
    return;
  }
  pills.innerHTML = selecionados.map((k, i) =>
    (i > 0 ? '<span class="r-arr">›</span>' : '') + `<span class="r-pill">${nomes[k]}</span>`
  ).join('');
}
