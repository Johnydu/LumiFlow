function showTab(nome) {
  document.querySelectorAll('.tabs .tab').forEach(tab => {
    tab.classList.remove('active');
    tab.setAttribute('aria-selected', 'false');
    tab.tabIndex = -1;
  });
  document.querySelectorAll('.content > .form-card').forEach(card => {
    card.classList.add('is-hidden');
    card.classList.remove('is-visible');
  });

  const selectedCard = document.getElementById(`tab-${nome}`);
  const tabId = `tab-btn-${nome === 'producao' ? 'prod' : nome === 'refugo' ? 'ref' : 'ret'}`;
  const selectedTab = document.getElementById(tabId);
  if (selectedCard && selectedTab) {
    selectedCard.classList.remove('is-hidden');
    selectedCard.classList.add('is-visible');
    selectedTab.classList.add('active');
    selectedTab.setAttribute('aria-selected', 'true');
    selectedTab.tabIndex = 0;
  }
}

function voltarPagina() {
  const url = document.body.dataset.urlVoltar;
  if (url) {
    window.location.href = url;
  }
}

function filtrarMaquinasRetrabalho() {
  const selectRefugo = document.getElementById('selectRefugoRetrabalho');
  const selectMaquina = document.getElementById('selectMaquinaRetrabalho');
  if (!selectRefugo || !selectMaquina) return;

  const opcaoSelecionada = selectRefugo.options[selectRefugo.selectedIndex];
  const setorOrigemId = opcaoSelecionada ? opcaoSelecionada.getAttribute('data-setor-origem') : null;

  Array.from(selectMaquina.options).forEach(opt => {
    if (!opt.value) {
      opt.hidden = false;
      return;
    }
    opt.hidden = opt.getAttribute('data-setor') !== setorOrigemId;
  });

  selectMaquina.value = '';
}

function initEtapaOrigemRefugo(produtoId) {
  const selectSetor = document.getElementById('selectSetorOrigemRefugo');
  const campoEtapa = document.getElementById('campoEtapaOrigemRefugo');
  const selectEtapa = document.getElementById('selectEtapaOrigemRefugo');

  if (!selectSetor || !campoEtapa || !selectEtapa) return;

  selectSetor.addEventListener('change', async () => {
    const setorId = selectSetor.value;

    selectEtapa.replaceChildren(new Option('Selecione a etapa', ''));
    campoEtapa.classList.remove('is-visible');
    campoEtapa.classList.add('is-hidden');
    selectEtapa.required = false;

    if (!setorId) return;

    try {
      const response = await fetch(`/api/roteiros/produto/${produtoId}/setor/${setorId}/etapas`);
      if (!response.ok) {
        console.warn('Falha ao buscar etapas do setor de origem. Status:', response.status);
        return;
      }

      const etapas = await response.json();

      if (etapas.length > 1) {
        etapas.forEach(etapa => selectEtapa.appendChild(new Option(etapa.nome, etapa.id)));
        campoEtapa.classList.remove('is-hidden');
        campoEtapa.classList.add('is-visible');
        selectEtapa.required = true;
      }
    } catch (e) {
      console.error('Erro ao buscar etapas do setor de origem:', e);
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  const { abaErro, produtoId } = document.body.dataset;

  if (abaErro) showTab(abaErro);
  if (produtoId) initEtapaOrigemRefugo(produtoId);

  // Lógica da Modal de Confirmação e Resumo
  const modal = document.getElementById('modal-confirmacao-lancamento');
  const resumoConteudo = document.getElementById('modal-resumo-conteudo');
  const btnFechar = document.getElementById('btn-modal-fechar');
  const btnCancelar = document.getElementById('btn-modal-cancelar');
  const btnConfirmarEnvio = document.getElementById('btn-modal-confirmar-envio');

  let formParaSubmeter = null;

  document.querySelectorAll('.form-card form').forEach(form => {
    form.addEventListener('submit', function (e) {
      if (form.dataset.confirmado === 'true') {
        return;
      }

      e.preventDefault();

      if (!form.checkValidity()) {
        form.reportValidity();
        return;
      }

      formParaSubmeter = form;
      gerarResumo(form);
      abrirModal();
    });
  });

  function gerarResumo(form) {
    if (!resumoConteudo) return;
    resumoConteudo.innerHTML = '';
    const abaId = form.closest('.form-card').id;
    let htmlResumo = '';

    const produtoNome = document.querySelector('.produto-nome')?.textContent.trim() || '';
    const produtoOP = document.querySelector('.produto-op')?.textContent.trim() || '';

    htmlResumo += `
        <div class="resumo-item">
            <span class="resumo-label">Ordem de Produção:</span>
            <span class="resumo-val">${produtoOP} (${produtoNome})</span>
        </div>
    `;

    if (abaId === 'tab-producao') {
      const qtd = form.querySelector('#quantidadeProducao')?.value || '0';
      const maquinaSelect = form.querySelector('#maquinaProducaoId');
      const maquina = maquinaSelect?.options[maquinaSelect.selectedIndex]?.text || '-';
      const operadorSelect = form.querySelector('#operadorProducaoId');
      const operador = operadorSelect?.options[operadorSelect.selectedIndex]?.text || '-';
      const obs = form.querySelector('#observacaoProducao')?.value || '-';

      htmlResumo += `
            <div class="resumo-item"><span class="resumo-label">Tipo:</span><span class="resumo-val destaque">Lançamento de Produção</span></div>
            <div class="resumo-item"><span class="resumo-label">Qtd. Produzida:</span><span class="resumo-val destaque">${qtd} unid.</span></div>
            <div class="resumo-item"><span class="resumo-label">Máquina:</span><span class="resumo-val">${maquina}</span></div>
            <div class="resumo-item"><span class="resumo-label">Operador:</span><span class="resumo-val">${operador}</span></div>
            <div class="resumo-item"><span class="resumo-label">Observação:</span><span class="resumo-val">${obs}</span></div>
        `;
    } else if (abaId === 'tab-refugo') {
      const qtd = form.querySelector('#quantidadeRefugo')?.value || '0';
      const setorSelect = form.querySelector('#selectSetorOrigemRefugo');
      const setor = setorSelect?.options[setorSelect.selectedIndex]?.text || '-';
      const operadorSelect = form.querySelector('#operadorRefugoId');
      const operador = operadorSelect?.options[operadorSelect.selectedIndex]?.text || '-';
      const destinoSelect = form.querySelector('#destinoRefugo');
      const destino = destinoSelect?.options[destinoSelect.selectedIndex]?.text || '-';
      const motivo = form.querySelector('#motivoRefugo')?.value || '-';

      htmlResumo += `
            <div class="resumo-item"><span class="resumo-label">Tipo:</span><span class="resumo-val destaque" style="color: #ef4444;">Lançamento de Refugo</span></div>
            <div class="resumo-item"><span class="resumo-label">Qtd. Refugada:</span><span class="resumo-val destaque">${qtd} unid.</span></div>
            <div class="resumo-item"><span class="resumo-label">Setor de Origem:</span><span class="resumo-val">${setor}</span></div>
            <div class="resumo-item"><span class="resumo-label">Operador:</span><span class="resumo-val">${operador}</span></div>
            <div class="resumo-item"><span class="resumo-label">Destino:</span><span class="resumo-val">${destino}</span></div>
            <div class="resumo-item"><span class="resumo-label">Motivo:</span><span class="resumo-val">${motivo}</span></div>
        `;
    } else if (abaId === 'tab-retrabalho') {
      const refugoSelect = form.querySelector('#selectRefugoRetrabalho');
      const refugo = refugoSelect?.options[refugoSelect.selectedIndex]?.text || '-';
      const maquinaSelect = form.querySelector('#selectMaquinaRetrabalho');
      const maquina = maquinaSelect?.value ? maquinaSelect.options[maquinaSelect.selectedIndex].text : 'Nenhuma';
      const operadorSelect = form.querySelector('#operadorRetrabalhoId');
      const operador = operadorSelect?.options[operadorSelect.selectedIndex]?.text || '-';
      const qtd = form.querySelector('#quantidadeRetrabalho')?.value || '0';
      const obs = form.querySelector('#observacaoRetrabalho')?.value || '-';

      htmlResumo += `
            <div class="resumo-item"><span class="resumo-label">Tipo:</span><span class="resumo-val destaque" style="color: #f59e0b;">Lançamento de Retrabalho</span></div>
            <div class="resumo-item"><span class="resumo-label">Refugo Vinculado:</span><span class="resumo-val">${refugo}</span></div>
            <div class="resumo-item"><span class="resumo-label">Qtd. Refeita:</span><span class="resumo-val destaque">${qtd} unid.</span></div>
            <div class="resumo-item"><span class="resumo-label">Máquina:</span><span class="resumo-val">${maquina}</span></div>
            <div class="resumo-item"><span class="resumo-label">Operador:</span><span class="resumo-val">${operador}</span></div>
            <div class="resumo-item"><span class="resumo-label">Observação:</span><span class="resumo-val">${obs}</span></div>
        `;
    }

    resumoConteudo.innerHTML = htmlResumo;
  }

  function abrirModal() {
    if (!modal) return;
    modal.classList.remove('is-hidden');
    modal.classList.add('is-open');
  }

  function fecharModal() {
    if (modal) {
      modal.classList.remove('is-open');
      modal.classList.add('is-hidden');
    }
    formParaSubmeter = null;
  }

  if (btnFechar) btnFechar.addEventListener('click', fecharModal);
  if (btnCancelar) btnCancelar.addEventListener('click', fecharModal);

  if (btnConfirmarEnvio) {
    btnConfirmarEnvio.addEventListener('click', () => {
      if (formParaSubmeter) {
        formParaSubmeter.dataset.confirmado = 'true';
        formParaSubmeter.submit();
      }
    });
  }
});