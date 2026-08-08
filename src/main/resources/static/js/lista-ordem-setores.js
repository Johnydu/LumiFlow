/**
 * LumiFlow - ListaOrdemSetores JS
 * Controla os filtros de busca e salvamento assíncrono (AJAX) dos operadores por setor.
 */
document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    // 1. Controle dos Filtros de Pesquisa com Debounce
    const formFiltros = document.getElementById('formFiltros');
    const inputBusca = formFiltros ? formFiltros.querySelector('input[name="busca"]') : null;
    const inputData = formFiltros ? formFiltros.querySelector('input[name="data"]') : null;

    if (formFiltros) {
        const debounce = (func, delay = 500) => {
            let timeoutTimer;
            return (...args) => {
                clearTimeout(timeoutTimer);
                timeoutTimer = setTimeout(() => func.apply(this, args), delay);
            };
        };

        if (inputBusca) {
            inputBusca.addEventListener('input', debounce(() => formFiltros.submit(), 500));
        }

        if (inputData) {
            inputData.addEventListener('change', () => formFiltros.submit());
        }
    }

    // 2. Manipulação Segura da Edição dos Operadores por Setor
    const inputsOperadores = document.querySelectorAll('.input-operadores');

    inputsOperadores.forEach(input => {
        const btnSave = input.nextElementSibling;
        const valorOriginal = input.value;

        // Revela botão de salvar somente se alterou o texto
        input.addEventListener('input', () => {
            if (input.value.trim() !== valorOriginal.trim()) {
                btnSave.style.display = 'inline-flex';
            } else {
                btnSave.style.display = 'none';
            }
        });

        // Envio via Fetch API para salvar no backend
        btnSave.addEventListener('click', async () => {
            const setorId = input.dataset.setorId;
            const novosOperadores = input.value.trim();

            try {
                btnSave.disabled = true;
                btnSave.textContent = '...';

                const response = await fetch(`/api/setores/${setorId}/operadores`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ operadores: novosOperadores })
                });

                if (response.ok) {
                    btnSave.style.display = 'none';
                    btnSave.textContent = '✓';
                } else {
                    throw new Error('Erro na resposta do servidor');
                }
            } catch (err) {
                alert('Erro ao atualizar os operadores do setor.');
                btnSave.textContent = '✓';
            } finally {
                btnSave.disabled = false;
            }
        });
    });
});