/* ============================================================
   LumiFlow - Tela de Login
   Arquivo de scripts (login.js)
   ============================================================ */

// Função global para alternar a visibilidade da senha usando o FontAwesome
function togglePasswordVisibility() {
  const passwordInput = document.getElementById('password');
  const toggleIcon = document.getElementById('toggleIcon');

  if (!passwordInput || !toggleIcon) return;

  if (passwordInput.type === 'password') {
    passwordInput.type = 'text';
    toggleIcon.classList.remove('fa-eye');
    toggleIcon.classList.add('fa-eye-slash');
  } else {
    passwordInput.type = 'password';
    toggleIcon.classList.remove('fa-eye-slash');
    toggleIcon.classList.add('fa-eye');
  }
}

(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    // Captura correta baseada nos IDs reais do HTML
    var userInput = document.getElementById("username");
    var passInput = document.getElementById("password");

    // Captura o container principal do card de login
    var cardContainer = document.querySelector(".login-card-container");

    /* ---- Efeito "acender": destaca o card quando os campos estão preenchidos ---- */
    function verificarCampos() {
      if (!cardContainer || !userInput || !passInput) return;

      var preenchido =
          userInput.value.trim().length > 0 &&
          passInput.value.trim().length > 0;

      cardContainer.classList.toggle("ready", preenchido);
    }

    if (userInput && passInput) {
      userInput.addEventListener("input", verificarCampos);
      passInput.addEventListener("input", verificarCampos);
      verificarCampos(); // Estado inicial ao carregar a página
    }
  });
})();