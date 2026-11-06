package br.com.lumiflow.exception;

import br.com.lumiflow.config.AppMessages;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final String DESTINO_PADRAO = "redirect:/dashboard/ordens-setor";

    @Mock
    private MessageSource messageSource;

    private GlobalExceptionHandler handler() {
        return new GlobalExceptionHandler(messageSource);
    }

    private MockHttpServletRequest requisicao(String uri, String referer) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        if (referer != null) {
            request.addHeader("Referer", referer);
        }
        return request;
    }

    private void stubMensagemDeNegocio(BusinessException ex, String textoResolvido) {
        when(messageSource.getMessage(eq(ex.getMessageKey()), eq(ex.getArgs()), eq(ex.getMessageKey()), eq(PT_BR)))
                .thenReturn(textoResolvido);
    }

    @Test
    @DisplayName("BusinessException: resolve a mensagem pela chave e volta ao referer interno")
    void businessExceptionVoltaParaReferer() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_USER_NOTFOUND, 7);
        stubMensagemDeNegocio(ex, "Usuário não encontrado");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        Object view = handler().handleBusinessException(ex, redirect,
                requisicao("/usuarios/excluir", "http://localhost/usuarios"), PT_BR);

        assertThat(view).isEqualTo("redirect:http://localhost/usuarios");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("Usuário não encontrado");
    }

    @Test
    @DisplayName("BusinessException: sem referer vai para o destino padrão")
    void businessExceptionSemRefererVaiParaDestinoPadrao() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "Ordem não encontrada");

        Object view = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/ordens/1", null), PT_BR);

        assertThat(view).isEqualTo(DESTINO_PADRAO);
    }

    @Test
    @DisplayName("BusinessException: referer apontando para a própria requisição evita loop e usa destino padrão")
    void refererIgualRequisicaoAtualUsaDestinoPadrao() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "msg");

        Object view = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/ordens/liberar", "http://localhost/ordens/liberar?x=1"), PT_BR);

        assertThat(view).isEqualTo(DESTINO_PADRAO);
    }

    @Test
    @DisplayName("BusinessException: referer de outro host (open redirect) é ignorado")
    void refererExternoEhIgnorado() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "msg");

        Object view = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/ordens/1", "http://malicioso.example.com/phishing"), PT_BR);

        assertThat(view).isEqualTo(DESTINO_PADRAO);
    }

    @Test
    @DisplayName("BusinessException: referer inválido não quebra e usa destino padrão")
    void refererInvalidoUsaDestinoPadrao() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "msg");

        Object view = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/ordens/1", "http://[invalido"), PT_BR);

        assertThat(view).isEqualTo(DESTINO_PADRAO);
    }

    @Test
    @DisplayName("BusinessException: referer relativo (sem host) é considerado interno")
    void refererRelativoEhInterno() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "msg");

        Object view = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/ordens/1", "/produtos"), PT_BR);

        assertThat(view).isEqualTo("redirect:/produtos");
    }

    @Test
    @DisplayName("Conflito de concorrência: mensagem específica e redirecionamento seguro")
    void concorrenciaMostraMensagemEspecifica() {
        when(messageSource.getMessage(eq(AppMessages.ERROR_CONCURRENT_UPDATE), isNull(), eq(PT_BR)))
                .thenReturn("Registro alterado por outro usuário");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        Object view = handler().handleConcorrencia(redirect,
                requisicao("/ordens/1/liberar", "http://localhost/dashboard"), PT_BR);

        assertThat(view).isEqualTo("redirect:http://localhost/dashboard");
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("Registro alterado por outro usuário");
    }

    @Test
    @DisplayName("Exceção genérica: mostra mensagem de erro interno sem expor detalhes")
    void excecaoGenericaMostraErroInterno() {
        when(messageSource.getMessage(eq(AppMessages.ERROR_SYSTEM_INTERNAL), isNull(), eq(PT_BR)))
                .thenReturn("Erro interno");
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        Object view = handler().handleInesperado(new IllegalStateException("detalhe sensível"), redirect,
                requisicao("/qualquer", null), PT_BR);

        assertThat(view).isEqualTo(DESTINO_PADRAO);
        assertThat(redirect.getFlashAttributes().get("error")).isEqualTo("Erro interno");
        assertThat(redirect.getFlashAttributes().values()).noneMatch(v -> String.valueOf(v).contains("sensível"));
        verify(messageSource).getMessage(eq(AppMessages.ERROR_SYSTEM_INTERNAL), isNull(), eq(PT_BR));
    }

    @Test
    @DisplayName("AccessDeniedException é relançada para o Spring Security tratar")
    void accessDeniedEhRelancada() {
        AccessDeniedException ex = new AccessDeniedException("negado");

        assertThatThrownBy(() -> handler().handleAccessDenied(ex)).isSameAs(ex);
    }

    @Test
    @DisplayName("NoResourceFoundException mantém o 404 herdado do ResponseEntityExceptionHandler")
    void noResourceResponde404() throws Exception {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "favicon.ico");

        ResponseEntity<Object> resposta = handler().handleException(ex,
                new ServletWebRequest(requisicao("/favicon.ico", null)));

        assertThat(resposta).isNotNull();
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Requisição /api/**: BusinessException vira ProblemDetail com status derivado da chave, sem redirect")
    void requisicaoApiRetornaProblemDetail() {
        BusinessException ex = new BusinessException(AppMessages.ERROR_ORDER_NOTFOUND);
        stubMensagemDeNegocio(ex, "Ordem não encontrada");

        Object resposta = handler().handleBusinessException(ex, new RedirectAttributesModelMap(),
                requisicao("/api/ordens/1", "http://localhost/ordens"), PT_BR);

        assertThat(resposta).isInstanceOf(ResponseEntity.class);
        ResponseEntity<?> entity = (ResponseEntity<?>) resposta;
        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(entity.getBody()).isInstanceOfSatisfying(ProblemDetail.class,
                pd -> assertThat(pd.getDetail()).isEqualTo("Ordem não encontrada"));
    }
}
