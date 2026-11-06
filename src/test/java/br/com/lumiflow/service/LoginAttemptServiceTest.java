package br.com.lumiflow.service;

import br.com.lumiflow.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private LoginAttemptService service;

    @BeforeEach
    void configurarPropriedades() {
        ReflectionTestUtils.setField(service, "maxTentativas", 5);
        ReflectionTestUtils.setField(service, "escalaMinutos", new long[]{15L});
        ReflectionTestUtils.setField(service, "resetEscalonamentoHoras", 24L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("registrarFalha ignora login nulo, vazio ou em branco")
    void registrarFalhaIgnoraLoginInvalido(String login) {
        service.registrarFalha(login);

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("registrarFalha não tenta bloquear quando o login não existe")
    void registrarFalhaLoginInexistente() {
        when(usuarioRepository.incrementarTentativasFalhas("fantasma")).thenReturn(0);

        service.registrarFalha("fantasma");

        verify(usuarioRepository, never()).bloquearSeExcedeuLimite(any(), any(), anyInt());
    }

    @Test
    @DisplayName("registrarFalha incrementa e tenta bloquear com o limite e a janela configurados")
    void registrarFalhaTentaBloquear() {
        when(usuarioRepository.incrementarTentativasFalhas("ana")).thenReturn(1);
        when(usuarioRepository.bloquearSeExcedeuLimite(eq("ana"), any(LocalDateTime.class), eq(5))).thenReturn(1);
        LocalDateTime antes = LocalDateTime.now();

        service.registrarFalha("ana");

        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(usuarioRepository).bloquearSeExcedeuLimite(eq("ana"), ate.capture(), eq(5));
        assertThat(ate.getValue()).isCloseTo(antes.plusMinutes(15), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("registrarFalha sem exceder o limite apenas incrementa (bloqueio retorna zero linhas)")
    void registrarFalhaSemExcederLimite() {
        when(usuarioRepository.incrementarTentativasFalhas("ana")).thenReturn(1);
        when(usuarioRepository.bloquearSeExcedeuLimite(eq("ana"), any(LocalDateTime.class), eq(5))).thenReturn(0);

        service.registrarFalha("ana");

        verify(usuarioRepository).incrementarTentativasFalhas("ana");
        verify(usuarioRepository).bloquearSeExcedeuLimite(eq("ana"), any(LocalDateTime.class), eq(5));
    }

    @Test
    @DisplayName("registrarFalha respeita limites diferentes dos padrões")
    void registrarFalhaComLimitesCustomizados() {
        ReflectionTestUtils.setField(service, "maxTentativas", 3);
        ReflectionTestUtils.setField(service, "escalaMinutos", new long[]{30L});
        when(usuarioRepository.incrementarTentativasFalhas("ana")).thenReturn(1);
        when(usuarioRepository.bloquearSeExcedeuLimite(eq("ana"), any(LocalDateTime.class), eq(3))).thenReturn(0);
        LocalDateTime antes = LocalDateTime.now();

        service.registrarFalha("ana");

        ArgumentCaptor<LocalDateTime> ate = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(usuarioRepository).bloquearSeExcedeuLimite(eq("ana"), ate.capture(), eq(3));
        assertThat(ate.getValue()).isCloseTo(antes.plusMinutes(30), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    @DisplayName("registrarSucesso limpa o controle de bloqueio do login")
    void registrarSucessoLimpa() {
        service.registrarSucesso("ana");

        verify(usuarioRepository).limparControleDeBloqueio("ana");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    @DisplayName("registrarSucesso ignora login nulo, vazio ou em branco")
    void registrarSucessoIgnoraLoginInvalido(String login) {
        service.registrarSucesso(login);

        verifyNoInteractions(usuarioRepository);
    }
}
