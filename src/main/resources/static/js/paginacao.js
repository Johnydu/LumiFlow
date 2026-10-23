/**
 * LumiFlow - Paginação reutilizável para formulários de filtros.
 */
document.addEventListener('click', event => {
    const botao = event.target instanceof Element ? event.target.closest('[data-pagina]') : null;
    if (!botao || botao.disabled) return;

    const form = document.getElementById(botao.dataset.form);
    const campoPagina = form && form.querySelector('input[name="page"]');
    if (!campoPagina) return;

    campoPagina.value = botao.dataset.pagina;
    const evento = new CustomEvent('filtro:atualizar', { cancelable: true });
    if (form.dispatchEvent(evento)) {
        form.requestSubmit();
    }
});
