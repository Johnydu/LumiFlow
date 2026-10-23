/**
 * LumiFlow - Filtro dinâmico (AJAX) para listagens.
 *
 * Os filtros reiniciam a página; o evento de paginação mantém a página escolhida.
 */
(function () {
    'use strict';

    function initFiltroAjax({ formSelector, listaSelector = '.lista', atraso = 400 }) {
        const form = document.querySelector(formSelector);
        if (!form) return;

        const campoTexto = form.querySelector('input.search');
        const campoPagina = form.querySelector('input[name="page"]');
        const voltarParaPrimeiraPagina = () => {
            if (campoPagina) campoPagina.value = '0';
        };

        if (campoTexto && campoTexto.value) {
            const fim = campoTexto.value.length;
            campoTexto.focus();
            campoTexto.setSelectionRange(fim, fim);
        }

        let temporizador = null;
        let controlador = null;

        async function atualizar() {
            if (controlador) controlador.abort();
            controlador = new AbortController();
            const requisicao = controlador;
            const params = new URLSearchParams(new FormData(form)).toString();

            try {
                const resposta = await fetch(`${form.action}?${params}`, {
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                    signal: requisicao.signal
                });
                if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);

                const documento = new DOMParser().parseFromString(await resposta.text(), 'text/html');
                const novaLista = documento.querySelector(listaSelector);
                const listaAtual = document.querySelector(listaSelector);
                if (!novaLista || !listaAtual) {
                    throw new Error(`Fragmento da lista não encontrado: ${listaSelector}`);
                }
                listaAtual.replaceWith(novaLista);
            } catch (erro) {
                if (erro.name === 'AbortError') return;
                console.error('Erro na busca dinâmica, enviando via submit normal:', erro);
                form.requestSubmit();
            }
        }

        if (campoTexto) {
            campoTexto.addEventListener('input', () => {
                voltarParaPrimeiraPagina();
                clearTimeout(temporizador);
                temporizador = setTimeout(atualizar, atraso);
            });
        }

        form.querySelectorAll('select').forEach(select => {
            select.addEventListener('change', () => {
                voltarParaPrimeiraPagina();
                clearTimeout(temporizador);
                atualizar();
            });
        });

        form.addEventListener('filtro:atualizar', evento => {
            evento.preventDefault();
            atualizar();
        });
    }

    window.initFiltroAjax = initFiltroAjax;
})();
