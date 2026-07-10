function showTab(tab) {
  ['producao', 'refugo', 'retrabalho'].forEach(t => {
    document.getElementById('tab-' + t).style.display = t === tab ? 'block' : 'none';
  });
  document.getElementById('tab-btn-prod').className = 'tab' + (tab === 'producao' ? ' active' : '');
  document.getElementById('tab-btn-ref').className = 'tab refugo' + (tab === 'refugo' ? ' active' : '');
  document.getElementById('tab-btn-ret').className = 'tab retrab' + (tab === 'retrabalho' ? ' active' : '');
}
