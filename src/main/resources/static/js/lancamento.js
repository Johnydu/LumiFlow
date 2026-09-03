// Função para alternar entre as abas do lançamento
function showTab(tabName) {
  // Esconde todos os cards de formulário
  document.getElementById('tab-producao').style.display = 'none';
  document.getElementById('tab-refugo').style.display = 'none';
  document.getElementById('tab-retrabalho').style.display = 'none';

  // Remove a classe 'active' de todos os botões de aba
  document.getElementById('tab-btn-prod').classList.remove('active');
  document.getElementById('tab-btn-ref').classList.remove('active');
  document.getElementById('tab-btn-ret').classList.remove('active');

  // Exibe o card selecionado e ativa o botão correspondente
  if (tabName === 'producao') {
    document.getElementById('tab-producao').style.display = 'block';
    document.getElementById('tab-btn-prod').classList.add('active');
  } else if (tabName === 'refugo') {
    document.getElementById('tab-refugo').style.display = 'block';
    document.getElementById('tab-btn-ref').classList.add('active');
  } else if (tabName === 'retrabalho') {
    document.getElementById('tab-retrabalho').style.display = 'block';
    document.getElementById('tab-btn-ret').classList.add('active');
  }
}