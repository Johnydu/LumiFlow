/**
 * LumiFlow - Módulo da Lista de Ordens de Produção (por setor)
 *
 * Os filtros são aplicados no servidor; ao mudar qualquer filtro, volta à primeira página.
 */
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('formFiltros');
    if (form) {
        const campoPagina = form.querySelector('input[name="page"]');
        const reiniciarPagina = () => {
            if (campoPagina) campoPagina.value = '0';
        };

        const inputBusca = form.querySelector('input[name="busca"]');
        if (inputBusca) {
            if (inputBusca.value) {
                inputBusca.focus();
                inputBusca.setSelectionRange(inputBusca.value.length, inputBusca.value.length);
            }

            let timeout = null;
            inputBusca.addEventListener('input', () => {
                clearTimeout(timeout);
                timeout = setTimeout(() => {
                    reiniciarPagina();
                    form.requestSubmit();
                }, 500);
            });
        }

        const selectStatus = document.getElementById('filtroStatus');
        if (selectStatus) {
            selectStatus.addEventListener('change', () => {
                reiniciarPagina();
                form.requestSubmit();
            });
        }
    }

    document.querySelectorAll('.prog-fill').forEach(fill => {
        fill.classList.add('progress-animated');
    });
});

function novaOrdemForm() {
    window.location.href = '/dashboard/ordens/novaOrdem';
}

function voltarPagina() {
    window.location.href = '/dashboard/ordens-setor';
}
