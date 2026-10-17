'use strict';

document.addEventListener('DOMContentLoaded', () => {
    const formFiltro = document.querySelector('.history-filter-form');
    if (!formFiltro) {
        return;
    }

    const selectSetor = formFiltro.querySelector('#setorId');
    if (!selectSetor) {
        return;
    }

    selectSetor.addEventListener('change', () => {
        const selectEtapa = formFiltro.querySelector('#etapaSetorId');
        if (selectEtapa) {
            selectEtapa.value = '';
        }
        formFiltro.requestSubmit();
    });
});

function voltarParaSetor() {
    const mainElement = document.querySelector('body[data-setor-id]');
    const setorId = mainElement ? mainElement.getAttribute('data-setor-id') : null;

    if (setorId) {
        window.location.href = `/dashboard/ordens-setor/${setorId}`;
    } else {
        window.history.back();
    }
}