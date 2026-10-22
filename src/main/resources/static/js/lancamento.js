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

function initEtapaOrigemRefugo(roteiroId) {
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
      const response = await fetch(`/api/roteiros/${roteiroId}/setor/${setorId}/etapas`);
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

  // Monta uma linha do resumo sem innerHTML: textContent trata o valor como texto puro.
  function criarLinhaResumo(rotulo, valor, classeExtra = '') {
    const item = document.createElement('div');
    item.className = 'resumo-item';

    const label = document.createElement('span');
    label.className = 'resumo-label';
    label.textContent = rotulo;

    const val = document.createElement('span');
    val.className = `resumo-val ${classeExtra}`.trim();
    val.textContent = valor;

    item.append(label, val);
    return item;
  }

  function valorDoCampo(form, seletor, padrao = '-') {
    const valor = form.querySelector(seletor)?.value.trim();
    return valor || padrao;
  }

  function textoDoSelect(form, seletor, padrao = '-') {
    const select = form.querySelector(seletor);
    if (!select || !select.value) return padrao;
    return select.options[select.selectedIndex].text;
  }

  function gerarResumo(form) {
    if (!resumoConteudo) return;
    const abaId = form.closest('.form-card').id;

    const produtoCodigo = document.querySelector('.produto-codigo')?.textContent.trim() || '';
    const produtoNome = document.querySelector('.produto-nome')?.textContent.trim() || '';
    const produtoOP = document.querySelector('.produto-op')?.textContent.trim() || '';

    const linhas = [criarLinhaResumo(
      'Ordem de Produção:',
      `${produtoOP} (${produtoCodigo} · ${produtoNome})`
    )];

    if (abaId === 'tab-producao') {
      linhas.push(
        criarLinhaResumo('Tipo:', 'Lançamento de Produção', 'destaque'),
        criarLinhaResumo('Qtd. Produzida:', `${valorDoCampo(form, '#quantidadeProducao', '0')} unid.`, 'destaque'),
        criarLinhaResumo('Máquina:', textoDoSelect(form, '#maquinaProducaoId')),
        criarLinhaResumo('Operador:', textoDoSelect(form, '#operadorProducaoId')),
        criarLinhaResumo('Observação:', valorDoCampo(form, '#observacaoProducao'))
      );
    } else if (abaId === 'tab-refugo') {
      linhas.push(
        criarLinhaResumo('Tipo:', 'Lançamento de Refugo', 'destaque refugo'),
        criarLinhaResumo('Qtd. Refugada:', `${valorDoCampo(form, '#quantidadeRefugo', '0')} unid.`, 'destaque'),
        criarLinhaResumo('Setor de Origem:', textoDoSelect(form, '#selectSetorOrigemRefugo')),
        criarLinhaResumo('Operador:', textoDoSelect(form, '#operadorRefugoId')),
        criarLinhaResumo('Destino:', textoDoSelect(form, '#destinoRefugo')),
        criarLinhaResumo('Motivo:', valorDoCampo(form, '#motivoRefugo'))
      );
    } else if (abaId === 'tab-retrabalho') {
      linhas.push(
        criarLinhaResumo('Tipo:', 'Lançamento de Retrabalho', 'destaque retrab'),
        criarLinhaResumo('Refugo Vinculado:', textoDoSelect(form, '#selectRefugoRetrabalho')),
        criarLinhaResumo('Qtd. Refeita:', `${valorDoCampo(form, '#quantidadeRetrabalho', '0')} unid.`, 'destaque'),
        criarLinhaResumo('Máquina:', textoDoSelect(form, '#selectMaquinaRetrabalho', 'Nenhuma')),
        criarLinhaResumo('Operador:', textoDoSelect(form, '#operadorRetrabalhoId')),
        criarLinhaResumo('Observação:', valorDoCampo(form, '#observacaoRetrabalho'))
      );
    }

    resumoConteudo.replaceChildren(...linhas);
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
