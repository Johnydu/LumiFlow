/**
 * LumiFlow - Gerenciamento de Operadores
 * JS assíncrono para controle do modal, submissão AJAX e filtros.
 */
document.addEventListener('DOMContentLoaded', () => {
    'use strict';

    // Elements
    const modalOperador = document.getElementById('modalOperador');
    const modalTitle = document.getElementById('modalTitle');
    const formOperador = document.getElementById('formOperador');

    const inputId = document.getElementById('operadorId');
    const inputNome = document.getElementById('inputNome');
    const inputFuncao = document.getElementById('inputFuncao');
    const selectSetorPadrao = document.getElementById('selectSetorPadrao');

    const btnFecharModal = document.getElementById('btnFecharModal');
    const btnCancelarModal = document.getElementById('btnCancelarModal');
    const formFiltros = document.getElementById('formFiltros');
    const inputBusca = formFiltros ? formFiltros.querySelector('input[name="busca"]') : null;

    // --- 1. Abertura e Fechamento do Modal ---
    window.abrirModalOperador = () => {
        formOperador.reset();
        inputId.value = '';
        modalTitle.textContent = 'Novo Operador';
        modalOperador.style.display = 'flex';
        inputNome.focus();
    };

    const fecharModal = () => {
        modalOperador.style.display = 'none';
    };

    btnFecharModal?.addEventListener('click', fecharModal);
    btnCancelarModal?.addEventListener('click', fecharModal);

    modalOperador?.addEventListener('click', (e) => {
        if (e.target === modalOperador) fecharModal();
    });

    // --- 2. Edição de Operador ---
    document.querySelectorAll('.btn-edit').forEach(btn => {
        btn.addEventListener('click', () => {
            const id = btn.dataset.id;
            const nome = btn.dataset.nome;
            const funcao = btn.dataset.funcao;
            const setorId = btn.dataset.setor;

            inputId.value = id;
            inputNome.value = nome;
            inputFuncao.value = funcao || '';
            selectSetorPadrao.value = setorId || '';

            modalTitle.textContent = 'Editar Operador';
            modalOperador.style.display = 'flex';
        });
    });

    // --- 3. Submissão do Formulário (Salvar / Atualizar via REST) ---
    formOperador?.addEventListener('submit', async (e) => {
        e.preventDefault();

        const id = inputId.value;
        const isEdit = Boolean(id);

        const payload = {
            nome: inputNome.value.trim(),
            funcao: inputFuncao.value.trim(),
            setorPadraoId: selectSetorPadrao.value ? Number(selectSetorPadrao.value) : null
        };

        const url = isEdit ? `/api/operadores/${id}` : '/api/operadores';
        const method = isEdit ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (response.ok) {
                fecharModal();
                window.location.reload(); // Recarrega para exibir a lista atualizada
            } else {
                alert('Erro ao salvar operador. Verifique os dados digitados.');
            }
        } catch (err) {
            alert('Falha na comunicação com o servidor.');
        }
    });

    // --- 4. Exclusão de Operador ---
    document.querySelectorAll('.btn-delete').forEach(btn => {
        btn.addEventListener('click', async () => {
            const id = btn.dataset.id;
            const nome = btn.dataset.nome;

            if (confirm(`Deseja realmente excluir o operador "${nome}"?`)) {
                try {
                    const response = await fetch(`/api/operadores/${id}`, { method: 'DELETE' });

                    if (response.ok) {
                        window.location.reload();
                    } else {
                        alert('Não foi possível excluir o operador. Ele pode estar vinculado a ordens ativas.');
                    }
                } catch (err) {
                    alert('Erro de conexão ao tentar excluir.');
                }
            }
        });
    });

    // --- 5. Debounce no filtro de busca ---
    if (inputBusca) {
        let timer;
        inputBusca.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(() => formFiltros.submit(), 500);
        });
    }
});