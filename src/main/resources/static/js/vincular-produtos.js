/**
 * LumiFlow - Modal "Produtos do roteiro" preparado para muitos produtos.
 *
 * Em vez de carregar todos os produtos na página, busca no servidor (paginado) e guarda
 * só as MUDANÇAS feitas pelo usuário: ids a vincular e ids a desvincular.
 */
(function () {
    'use strict';

    const ATRASO_BUSCA_MS = 300;

    const estado = {
        roteiroId: null,
        pagina: 0,
        temMais: false,
        adicionar: new Set(),
        remover: new Set(),
        controlador: null,
        temporizador: null
    };

    const el = {};

    function iniciarElementos() {
        el.form = document.getElementById('vincular-form');
        el.origem = document.getElementById('vincular-origem');
        el.busca = document.getElementById('vincular-busca');
        el.somenteVinculados = document.getElementById('vincular-so-vinculados');
        el.lista = document.getElementById('vincular-lista');
        el.maisBotao = document.getElementById('vincular-mais');
        el.resumo = document.getElementById('vincular-resumo');
        el.salvar = document.getElementById('vincular-salvar');
        el.hidden = document.getElementById('vincular-hidden');
        return Object.values(el).every(Boolean);
    }

    function marcado(item) {
        return estado.adicionar.has(item.id) || (item.vinculadoAqui && !estado.remover.has(item.id));
    }

    function alternar(item, marcar) {
        if (item.vinculadoAqui) {
            if (marcar) estado.remover.delete(item.id); else estado.remover.add(item.id);
        } else {
            if (marcar) estado.adicionar.add(item.id); else estado.adicionar.delete(item.id);
        }
        atualizarResumo();
    }

    function atualizarResumo() {
        const a = estado.adicionar.size;
        const r = estado.remover.size;
        el.resumo.textContent = (a === 0 && r === 0)
            ? 'Nenhuma alteração pendente.'
            : `${a} a vincular · ${r} a desvincular`;
        el.salvar.disabled = a === 0 && r === 0;
    }


    function criarLinha(item) {
        const label = document.createElement('label');
        label.className = 'produto-check';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = marcado(item);
        checkbox.addEventListener('change', () => alternar(item, checkbox.checked));

        const codigo = document.createElement('span');
        codigo.className = 'produto-codigo';
        codigo.textContent = item.codigo;

        const nome = document.createElement('span');
        nome.className = 'produto-check-nome';
        nome.textContent = item.nome;

        label.append(checkbox, codigo, nome);

        if (item.roteiroCodigo && !item.vinculadoAqui) {
            const outro = document.createElement('span');
            outro.className = 'tag-maq';
            outro.textContent = `Hoje em ${item.roteiroCodigo}`;
            outro.title = 'Ao marcar, o produto passa para este roteiro (vale só para novas ordens).';
            label.appendChild(outro);
        }
        return label;
    }

    function mensagemVazia(texto) {
        const p = document.createElement('p');
        p.className = 'hint';
        p.textContent = texto;
        return p;
    }

    async function carregar({ limpar }) {
        if (estado.controlador) estado.controlador.abort(); // descarta resposta antiga
        estado.controlador = new AbortController();
        if (limpar) estado.pagina = 0;

        const params = new URLSearchParams({
            busca: el.busca.value.trim(),
            vinculados: String(el.somenteVinculados.checked),
            pagina: String(estado.pagina)
        });

        try {
            const resposta = await fetch(
                `/dashboard/roteiros/${encodeURIComponent(estado.roteiroId)}/produtos/busca?${params}`,
                { signal: estado.controlador.signal });
            if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);
            const pagina = await resposta.json();

            if (limpar) el.lista.replaceChildren();
            if (limpar && pagina.itens.length === 0) {
                el.lista.appendChild(mensagemVazia('Nenhum produto encontrado.'));
            }
            pagina.itens.forEach(item => el.lista.appendChild(criarLinha(item)));

            estado.temMais = pagina.temMais;
            el.maisBotao.hidden = !pagina.temMais;
        } catch (erro) {
            if (erro.name === 'AbortError') return;
            console.error('Erro ao buscar produtos do roteiro:', erro);
            el.lista.replaceChildren(mensagemVazia('Não foi possível carregar os produtos. Tente novamente.'));
            el.maisBotao.hidden = true;
        }
    }

    function abrir({ roteiroId, roteiroCodigo, quantidadeProdutos }) {
        if (!iniciarElementos()) return;

        estado.roteiroId = roteiroId;
        estado.adicionar.clear();
        estado.remover.clear();

        el.form.action = `/dashboard/roteiros/${encodeURIComponent(roteiroId)}/produtos`;
        el.origem.textContent = roteiroCodigo;
        el.busca.value = '';
        // Roteiro sem produtos: já abre mostrando todos para facilitar o primeiro vínculo.
        el.somenteVinculados.checked = quantidadeProdutos > 0;

        atualizarResumo();
        carregar({ limpar: true });
    }

    document.addEventListener('DOMContentLoaded', () => {
        if (!iniciarElementos()) return;

        el.busca.addEventListener('input', () => {
            clearTimeout(estado.temporizador);
            estado.temporizador = setTimeout(() => carregar({ limpar: true }), ATRASO_BUSCA_MS);
        });
        el.somenteVinculados.addEventListener('change', () => carregar({ limpar: true }));
        el.maisBotao.addEventListener('click', () => {
            estado.pagina += 1;
            carregar({ limpar: false });
        });

        // Envia só o delta (ids a vincular e a desvincular) como campos ocultos.
        el.form.addEventListener('submit', () => {
            el.hidden.replaceChildren();
            const adicionarCampo = (nome, id) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = nome;
                input.value = id;
                el.hidden.appendChild(input);
            };
            estado.adicionar.forEach(id => adicionarCampo('adicionar', id));
            estado.remover.forEach(id => adicionarCampo('remover', id));
        });
    });

    window.VincularProdutos = { abrir };
})();