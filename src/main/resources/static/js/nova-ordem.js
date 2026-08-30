function updateRoteiro(produtoId) {
  const pillsContainer = document.getElementById('roteiro-pills');
  const infoBox = document.getElementById('roteiro-info');

  if (!produtoId) {
    pillsContainer.innerHTML = '<span class="r-empty">Selecione um produto para ver o roteiro.</span>';
    infoBox.style.display = 'none';
    return;
  }

  fetch(`/api/roteiros/produto/${produtoId}`)
      .then(response => {
        if (!response.ok) throw new Error('Erro ao buscar roteiro');
        return response.json();
      })
      .then(fluxo => renderRoteiro(fluxo))
      .catch(error => {
        console.error(error);
        pillsContainer.innerHTML = '<span class="r-empty">Erro ao carregar o roteiro deste produto.</span>';
        infoBox.style.display = 'none';
      });
}

function renderRoteiro(fluxo) {
  const pillsContainer = document.getElementById('roteiro-pills');
  const infoBox = document.getElementById('roteiro-info');

  if (!fluxo || fluxo.length === 0) {
    pillsContainer.innerHTML = '<span class="r-empty">Este produto ainda não possui roteiro cadastrado.</span>';
    infoBox.style.display = 'none';
    return;
  }

  pillsContainer.innerHTML = fluxo
      .map(etapa => `<span class="r-pill">${etapa}</span>`)
      .join('<span class="r-arr">→</span>');

  infoBox.style.display = 'block';
}

