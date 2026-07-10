const roteiros = {
  arandela12: ['Corte', 'Dobra', 'Solda', 'Pintura', 'Colagem', 'Acabamento'],
  arandela20: ['Corte', 'Dobra', 'Solda', 'Lixa', 'Pintura', 'Colagem', 'Acabamento'],
  '222pt': ['Corte', 'Dobra', 'Pintura', 'Montagem 2'],
  '327mr': ['Corte', 'Dobra', 'Solda', 'Pintura', 'Acabamento'],
};

function updateRoteiro(val) {
  const pills = document.getElementById('roteiro-pills');
  const info = document.getElementById('roteiro-info');
  if (!val || !roteiros[val]) {
    pills.innerHTML = '<span class="r-empty">Selecione um produto para ver o roteiro.</span>';
    info.style.display = 'none';
    return;
  }
  const setores = roteiros[val];
  pills.innerHTML = setores.map((s, i) =>
    (i > 0 ? '<span class="r-arr">›</span>' : '') + `<span class="r-pill">${s}</span>`
  ).join('');
  info.style.display = 'flex';
}
