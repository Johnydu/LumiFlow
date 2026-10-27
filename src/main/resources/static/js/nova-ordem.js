/**
 * LumiFlow - Seleção de produto por busca (combobox) para a Nova Ordem.
 * Versão limpa e unificada.
 */
(function () {
  'use strict';

  window.updateRoteiro = function(produtoId) {
    const pillsContainer = document.getElementById('roteiro-pills');
    const infoBox = document.getElementById('roteiro-info');

    if (!pillsContainer || !infoBox) return;

    if (!produtoId) {
      pillsContainer.replaceChildren();
      const empty = document.createElement('span');
      empty.className = 'r-empty';
      empty.textContent = 'Selecione um produto para ver o roteiro.';
      pillsContainer.appendChild(empty);
      infoBox.classList.add('is-hidden');
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
          infoBox.classList.add('is-hidden');
        });
  };

  function renderRoteiro(fluxo) {
    const pillsContainer = document.getElementById('roteiro-pills');
    const infoBox = document.getElementById('roteiro-info');
    if (!pillsContainer || !infoBox) return;

    pillsContainer.replaceChildren();

    if (!fluxo || fluxo.length === 0) {
      const empty = document.createElement('span');
      empty.className = 'r-empty';
      empty.textContent = 'Este produto ainda não possui roteiro cadastrado.';
      pillsContainer.appendChild(empty);
      infoBox.classList.add('is-hidden');
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

    infoBox.classList.remove('is-hidden');
    infoBox.classList.add('is-visible');
  }

  document.addEventListener('DOMContentLoaded', () => {
    const combo = document.getElementById('produto-combo');
    const busca = document.getElementById('produtoBusca');
    const produtoId = document.getElementById('produtoId');
    const resultados = document.getElementById('produtoResultados');
    const ajuda = document.getElementById('produtoComboAjuda');
    const form = document.getElementById('formNovaOrdem');
    if (!combo || !busca || !produtoId || !resultados || !ajuda || !form) return;

    let paginaAtual = 0;
    let temMais = false;
    let temporizador = null;
    let controlador = null;
    let itensPorId = new Map();

    const abrirResultados = () => {
      resultados.classList.remove('is-hidden');
      busca.setAttribute('aria-expanded', 'true');
    };
    const fecharResultados = () => {
      resultados.classList.add('is-hidden');
      busca.setAttribute('aria-expanded', 'false');
    };

    function selecionarProduto(produto) {
      produtoId.value = produto.id;
      busca.value = [produto.codigo, produto.nome].filter(Boolean).join(' - ');
      fecharResultados();
      window.updateRoteiro(produto.id);
    }

    function renderizarItens(itens, substituir) {
      if (substituir) {
        resultados.replaceChildren();
        itensPorId = new Map();
      } else {
        resultados.querySelector('[data-carregar-mais]')?.parentElement.remove();
      }

      itens.forEach(produto => {
        itensPorId.set(String(produto.id), produto);
        const linha = document.createElement('li');
        const botao = document.createElement('button');
        botao.type = 'button';
        botao.className = 'combo-item';
        botao.setAttribute('role', 'option');
        botao.dataset.produtoId = produto.id;
        botao.disabled = !produto.roteiroCodigo;

        const codigo = document.createElement('span');
        codigo.className = 'produto-codigo';
        codigo.textContent = produto.codigo;

        const nome = document.createElement('span');
        nome.textContent = produto.nome;

        botao.append(codigo, nome);

        if (!produto.roteiroCodigo) {
          botao.classList.add('desabilitado');
          const semRoteiro = document.createElement('span');
          semRoteiro.textContent = 'Sem roteiro';
          botao.appendChild(semRoteiro);
        }
        linha.appendChild(botao);
        resultados.appendChild(linha);
      });

      if (temMais) {
        const linhaMais = document.createElement('li');
        const carregarMais = document.createElement('button');
        carregarMais.type = 'button';
        carregarMais.className = 'combo-item';
        carregarMais.dataset.carregarMais = 'true';
        carregarMais.textContent = 'Carregar mais produtos';
        linhaMais.appendChild(carregarMais);
        resultados.appendChild(linhaMais);
      }

      if (resultados.childElementCount === 0) {
        const vazio = document.createElement('li');
        vazio.className = 'combo-vazio';
        vazio.textContent = 'Nenhum produto encontrado.';
        vazio.setAttribute('role', 'presentation');
        resultados.appendChild(vazio);
      }
      abrirResultados();
    }

    async function buscarProdutos(pagina, acumular) {
      if (controlador) controlador.abort();
      controlador = new AbortController();
      const requisicao = controlador;
      ajuda.textContent = 'Carregando produtos...';

      try {
        const params = new URLSearchParams({ busca: busca.value.trim(), pagina: String(pagina) });
        const resposta = await fetch(`/api/produtos/busca?${params}`, { signal: requisicao.signal });
        if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
        const dados = await resposta.json();
        if (requisicao !== controlador) return;

        paginaAtual = dados.pagina;
        temMais = dados.temMais;
        renderizarItens(dados.itens, !acumular);
        ajuda.textContent = `${dados.total} produto(s) encontrado(s).`;
      } catch (erro) {
        if (erro.name === 'AbortError') return;
        console.error('Erro ao buscar produtos para a nova ordem:', erro);
        ajuda.textContent = 'Não foi possível carregar os produtos. Tente novamente.';
        resultados.replaceChildren();
        abrirResultados();
      }
    }

    busca.addEventListener('input', () => {
      produtoId.value = '';
      window.updateRoteiro('');
      clearTimeout(temporizador);
      temporizador = setTimeout(() => buscarProdutos(0, false), 300);
    });

    busca.addEventListener('focus', () => {
      if (!produtoId.value && resultados.classList.contains('is-hidden')) {
        buscarProdutos(0, false);
      }
    });

    resultados.addEventListener('click', event => {
      const botao = event.target.closest('button');
      if (!botao) return;
      if (botao.dataset.carregarMais) {
        buscarProdutos(paginaAtual + 1, true);
        return;
      }
      const produto = itensPorId.get(botao.dataset.produtoId);
      if (produto && produto.roteiroCodigo) selecionarProduto(produto);
    });

    busca.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        fecharResultados();
      } else if (event.key === 'ArrowDown') {
        const primeiro = resultados.querySelector('button:not(:disabled)');
        if (primeiro) {
          event.preventDefault();
          primeiro.focus();
        }
      }
    });

    resultados.addEventListener('keydown', event => {
      if (event.key === 'Escape') {
        fecharResultados();
        busca.focus();
      }
    });

    document.addEventListener('click', event => {
      if (!combo.contains(event.target)) fecharResultados();
    });

    form.addEventListener('submit', event => {
      if (produtoId.value) return;
      event.preventDefault();
      ajuda.textContent = 'Selecione um produto com roteiro para criar a ordem.';
      busca.focus();
    });

    const idInicial = combo.dataset.inicialId;
    const roteiroInicial = combo.dataset.inicialRoteiro;
    if (idInicial && roteiroInicial) {
      produtoId.value = idInicial;
      busca.value = [combo.dataset.inicialCodigo, combo.dataset.inicialNome].filter(Boolean).join(' - ');
      window.updateRoteiro(idInicial);
    } else {
      produtoId.value = '';
    }
  });
})();