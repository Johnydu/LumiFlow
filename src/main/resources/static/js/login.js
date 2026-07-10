document.addEventListener('DOMContentLoaded', () => {
  const userInput = document.getElementById('user');
  const passInput = document.getElementById('pass');

  function lightOn() {
    document.getElementById('bulb-fill')
      .setAttribute('fill', '#FFF5C0');
    document.getElementById('bulb-border')
      .setAttribute('stroke', '#FFD700');
    document.getElementById('inner-glow')
      .setAttribute('fill', 'rgba(255,220,50,0.6)');
    document.getElementById('filament')
      .setAttribute('opacity', '1');
    document.getElementById('glow')
      .classList.add('on');
    document.getElementById('screen')
      .classList.add('on');
    document.getElementById('card')
      .classList.add('on');
  }

  function lightOff() {
    document.getElementById('bulb-fill')
      .setAttribute('fill', '#1a1a2e');
    document.getElementById('bulb-border')
      .setAttribute('stroke', '#444');
    document.getElementById('inner-glow')
      .setAttribute('fill', 'rgba(255,220,50,0)');
    document.getElementById('filament')
      .setAttribute('opacity', '0');
    document.getElementById('glow')
      .classList.remove('on');
    document.getElementById('screen')
      .classList.remove('on');
    document.getElementById('card')
      .classList.remove('on');
  }

  function verificarCampos() {
    if (userInput.value.trim().length > 0 && passInput.value.trim().length > 0) {
      lightOn();
    } else {
      lightOff();
    }
  }

  userInput.addEventListener('input', verificarCampos);
  passInput.addEventListener('input', verificarCampos);
});
