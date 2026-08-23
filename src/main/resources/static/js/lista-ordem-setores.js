document.addEventListener("DOMContentLoaded", () => {
    const operatorInputs = document.querySelectorAll(".input-operadores");

    operatorInputs.forEach(input => {
        const wrapper = input.closest(".operator-edit-wrapper");
        const saveBtn = wrapper.querySelector(".btn-save-operator");

        // Mostra o botão de salvar quando o operador digitar algo
        input.addEventListener("input", () => {
            saveBtn.style.display = "inline-flex";
        });

        // Ação ao clicar no botão de salvar (✓)
        saveBtn.addEventListener("click", () => {
            const setorId = input.getAttribute("data-setor-id");
            const operadores = input.value;

            // Exemplo de requisição Fetch para o seu backend
            fetch(`/dashboard/ordens/setor/${setorId}/operadores`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    // Se estiver usando Spring Security com CSRF ativado, lembre-se do token aqui se necessário
                },
                body: JSON.stringify({ operadores: operadores })
            })
                .then(response => {
                    if (response.ok) {
                        saveBtn.style.display = "none";
                        // Feedback visual rápido de sucesso (opcional)
                        input.style.borderColor = "#28a745";
                        setTimeout(() => input.style.borderColor = "", 2000);
                    } else {
                        alert("Erro ao salvar operadores.");
                    }
                })
                .catch(error => {
                    console.error("Erro:", error);
                    alert("Erro de conexão ao salvar.");
                });
        });
    });
});