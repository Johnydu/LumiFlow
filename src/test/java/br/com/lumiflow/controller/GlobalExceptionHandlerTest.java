package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Locale;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercita o GlobalExceptionHandler por meio de um controller de teste que lança cada tipo de exceção.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.ControladorDeExcecoes.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(GlobalExceptionHandlerTest.ControladorDeExcecoes.class)
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private static final String BASE = "/dashboard/teste-excecoes";
    private static final String DESTINO_PADRAO = "/dashboard/ordens-setor";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;

    @Controller
    @RequestMapping(BASE)
    static class ControladorDeExcecoes {

        @GetMapping("/negocio")
        String negocio() {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
        }

        @GetMapping("/negocio-args")
        String negocioComArgumentos() {
            throw new BusinessException(AppMessages.SUCCESS_USER_CREATED, "joao");
        }

        @GetMapping("/negocio-chave-desconhecida")
        String chaveDesconhecida() {
            throw new BusinessException("chave.que.nao.existe");
        }

        @GetMapping("/concorrencia")
        String concorrencia() {
            throw new ObjectOptimisticLockingFailureException(Object.class, 1L);
        }

        @GetMapping("/inesperado")
        String inesperado() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/acesso-negado")
        String acessoNegado() {
            throw new AccessDeniedException("negado");
        }

        @GetMapping("/sem-recurso")
        String semRecurso() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "favicon.ico");
        }

        @GetMapping("/json")
        @ResponseBody
        String json() {
            throw new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND);
        }

        @GetMapping("/somente-get")
        @ResponseBody
        String somenteGet() {
            return "ok";
        }
    }

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    // ---------- BusinessException ----------

    @Test
    @WithMockUser
    @DisplayName("BusinessException sem Referer: flash de erro traduzido e redirect para o destino padrão")
    void businessExceptionSemReferer() throws Exception {
        mockMvc.perform(get(BASE + "/negocio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DESTINO_PADRAO))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_NOTFOUND)));
    }

    @Test
    @WithMockUser
    @DisplayName("BusinessException aplica os argumentos da exceção na mensagem")
    void businessExceptionComArgumentos() throws Exception {
        mockMvc.perform(get(BASE + "/negocio-args"))
                .andExpect(flash().attribute("error", "Usuário joao criado com sucesso!"));
    }

    @Test
    @WithMockUser
    @DisplayName("BusinessException com chave inexistente usa a própria chave como mensagem")
    void businessExceptionChaveDesconhecida() throws Exception {
        mockMvc.perform(get(BASE + "/negocio-chave-desconhecida"))
                .andExpect(flash().attribute("error", "chave.que.nao.existe"));
    }

    @Test
    @WithMockUser
    @DisplayName("mensagem respeita o idioma escolhido via ?lang=en")
    void mensagemRespeitaIdioma() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").param("lang", "en"))
                .andExpect(flash().attribute("error",
                        messageSource.getMessage(AppMessages.ERROR_PRODUCT_NOTFOUND, null, Locale.ENGLISH)));
    }

    // ---------- Referer ----------

    @Test
    @WithMockUser
    @DisplayName("Referer absoluto do mesmo host é usado como destino")
    void refererMesmoHost() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "http://localhost/dashboard/produtos"))
                .andExpect(redirectedUrl("http://localhost/dashboard/produtos"));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer do mesmo host com query string é preservado")
    void refererComQuery() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "http://localhost/dashboard/produtos?nome=abc"))
                .andExpect(redirectedUrl("http://localhost/dashboard/produtos?nome=abc"));
    }

    @Test
    @WithMockUser
    @DisplayName("comparação de host do Referer ignora maiúsculas/minúsculas")
    void refererHostCaseInsensitive() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "http://LOCALHOST/dashboard/produtos"))
                .andExpect(redirectedUrl("http://LOCALHOST/dashboard/produtos"));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer relativo (sem host) é aceito")
    void refererRelativo() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "/dashboard/setores"))
                .andExpect(redirectedUrl("/dashboard/setores"));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer de outro host é ignorado (evita open redirect)")
    void refererOutroHost() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "https://site-malicioso.example/phishing"))
                .andExpect(redirectedUrl(DESTINO_PADRAO));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer protocol-relative para outro host é ignorado")
    void refererProtocolRelative() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "//site-malicioso.example/x"))
                .andExpect(redirectedUrl(DESTINO_PADRAO));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer que aponta para a própria requisição é ignorado (evita loop)")
    void refererIgualRequisicaoAtual() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "http://localhost" + BASE + "/negocio"))
                .andExpect(redirectedUrl(DESTINO_PADRAO));
    }

    @Test
    @WithMockUser
    @DisplayName("Referer malformado é ignorado")
    void refererMalformado() throws Exception {
        mockMvc.perform(get(BASE + "/negocio").header("Referer", "::url invalida::"))
                .andExpect(redirectedUrl(DESTINO_PADRAO));
    }

    // ---------- Demais exceções ----------

    @Test
    @WithMockUser
    @DisplayName("ObjectOptimisticLockingFailureException usa a mensagem de edição concorrente")
    void concorrencia() throws Exception {
        mockMvc.perform(get(BASE + "/concorrencia").header("Referer", "http://localhost/dashboard/ordens-setor/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/dashboard/ordens-setor/1"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_CONCURRENT_UPDATE)));
    }

    @Test
    @WithMockUser
    @DisplayName("exceção inesperada vira mensagem de erro interno e redirect")
    void excecaoInesperada() throws Exception {
        mockMvc.perform(get(BASE + "/inesperado"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(DESTINO_PADRAO))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SYSTEM_INTERNAL)));
    }

    @Test
    @WithMockUser
    @DisplayName("AccessDeniedException é repassada e resulta em 403 (não é engolida pelo handler genérico)")
    void accessDeniedRepassada() throws Exception {
        mockMvc.perform(get(BASE + "/acesso-negado"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    @DisplayName("NoResourceFoundException é repassada e resulta em 404")
    void noResourceRepassada() throws Exception {
        mockMvc.perform(get(BASE + "/sem-recurso"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    @DisplayName("endpoint JSON com BusinessException recebe ProblemDetail com o status da chave, sem redirect")
    void endpointJsonRecebeProblemDetail() throws Exception {
        mockMvc.perform(get(BASE + "/json"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @WithMockUser
    @DisplayName("método HTTP não suportado mantém o 405 herdado do ResponseEntityExceptionHandler")
    void metodoNaoSuportado() throws Exception {
        mockMvc.perform(post(BASE + "/somente-get").with(csrf()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("anônimo nunca chega ao handler: é redirecionado para o login")
    void anonimoVaiParaLogin() throws Exception {
        mockMvc.perform(get(BASE + "/negocio"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
