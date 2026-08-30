document.addEventListener("DOMContentLoaded", function () {
    const formFiltros = document.getElementById("formFiltros");

    if (formFiltros) {
        const inputs = formFiltros.querySelectorAll("input");

        inputs.forEach(input => {
            // Dispara a busca automaticamente ao alterar a data ou digitar no campo de busca
            input.addEventListener("change", () => {
                formFiltros.submit();
            });

            // Opcional: Se quiser que a busca por texto filtre ao digitar (com pequeno atraso),
            // mas o "change" ou "blur" já resolve bem para datas e inputs.
        });
    }


});

function novaOrdemForm() {
    window.location.href = '/dashboard/ordens/novaOrdem';
}