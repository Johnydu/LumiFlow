function updateRoteiro(produtoId) {
  const pillsContainer = document.getElementById('roteiro-pills');
  const infoBox = document.getElementById('roteiro-info');

  if (!produtoId) {
    pillsContainer.replaceChildren();
    const empty = document.createElement('span');
    empty.className = 'r-empty';
    empty.textContent = 'Selecione um produto para ver o roteiro.';
    pillsContainer.appendChild(empty);
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
        pillsContainer.replaceChildren();
        const erro = document.createElement('span');
        erro.className = 'r-empty';
        erro.textContent = 'Erro ao carregar o roteiro deste produto.';
        pillsContainer.appendChild(erro);
        infoBox.style.display = 'none';
      });
}

function renderRoteiro(fluxo) {
  const pillsContainer = document.getElementById('roteiro-pills');
  const infoBox = document.getElementById('roteiro-info');

  pillsContainer.replaceChildren();

  if (!fluxo || fluxo.length === 0) {
    const empty = document.createElement('span');
    empty.className = 'r-empty';
    empty.textContent = 'Este produto ainda não possui roteiro cadastrado.';
    pillsContainer.appendChild(empty);
    infoBox.style.display = 'none';
    return;
  }

  fluxo.forEach((etapa, index) => {
    const pill = document.createElement('span');
    pill.className = 'r-pill';
    pill.textContent = etapa;
    pillsContainer.appendChild(pill);

    if (index < fluxo.length - 1) {
      const arrow = document.createElement('span');
      arrow.className = 'r-arr';
      arrow.textContent = '→';
      pillsContainer.appendChild(arrow);
    }
  });

  infoBox.style.display = 'block';
}