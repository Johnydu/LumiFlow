(function () {
    'use strict';

    function initFiltroAjax({ formSelector, listaSelector = '.lista', atraso = 400 }) {
        const form = document.querySelector(formSelector);
        if (!form) return;

        const campoTexto = form.querySelector('input.search');
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

            const params = new URLSearchParams(new FormData(form)).toString();
            try {
                const resposta = await fetch(`${form.action}?${params}`, {
                    headers: { 'X-Requested-With': 'XMLHttpRequest' },
                    signal: controlador.signal
                });
                if (!resposta.ok) throw new Error(`HTTP ${resposta.status}`);

                const documento = new DOMParser().parseFromString(await resposta.text(), 'text/html');
                const novaLista = documento.querySelector(listaSelector);
                const listaAtual = document.querySelector(listaSelector);
                if (novaLista && listaAtual) {
                    listaAtual.replaceWith(novaLista);
                }
            } catch (erro) {
                if (erro.name === 'AbortError') return;
                console.error('Dynamic search failed; using normal submit:', erro);
                form.submit();
            }
        }

        if (campoTexto) {
            campoTexto.addEventListener('input', () => {
                clearTimeout(temporizador);
                temporizador = setTimeout(atualizar, atraso);
            });
        }

        form.querySelectorAll('select').forEach(select => {
            select.addEventListener('change', () => {
                clearTimeout(temporizador);
                atualizar();
            });
        });
    }

    window.initFiltroAjax = initFiltroAjax;
})();
