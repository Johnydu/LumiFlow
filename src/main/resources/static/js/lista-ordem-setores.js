document.addEventListener("DOMContentLoaded", function () {
    const formFiltros = document.getElementById("formFiltros");

    if (formFiltros) {
        const inputs = formFiltros.querySelectorAll("input");

        inputs.forEach(input => {
            // Dispara a busca ao confirmar o campo de busca (Enter ou ao sair do campo)
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
