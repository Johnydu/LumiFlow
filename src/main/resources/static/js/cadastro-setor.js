let etapaCount = 2;

function showForm() {
  document.getElementById('form-panel').style.display = 'block';
}

function hideForm() {
  document.getElementById('form-panel').style.display = 'none';
}

function removeEtapa(btn) {
  btn.parentElement.remove();
  updateNums();
}

function addEtapa() {
  etapaCount++;
  const div = document.createElement('div');
  div.className = 'etapa-item';
  div.innerHTML = `
    <span class="etapa-num">${etapaCount}º</span>
    <input 
      type="text" 
      placeholder="Nome da etapa..." 
      style="flex:1;background:var(--color-surface-2);border:1px solid rgba(255,255,255,0.1);border-radius:6px;color:var(--color-text);font-size:12px;padding:4px 8px;outline:none;">
    <button class="etapa-del" onclick="removeEtapa(this)">✕</button>
  `;
  document.getElementById('etapas-list').appendChild(div);
}

function updateNums() {
  document.querySelectorAll('.etapa-num').forEach((el, i) => {
    el.textContent = (i + 1) + 'º';
  });
}
