package br.com.lumiflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("MessageSourceConfig")
class MessageSourceConfigTest {

    private final MessageSourceConfig config = new MessageSourceConfig();

    /** Expoe os interceptors registrados (getInterceptors e protegido). */
    private static class RegistroExposto extends InterceptorRegistry {
        List<Object> interceptors() {
            return getInterceptors();
        }
    }

    @Test
    @DisplayName("MessageSource deve resolver mensagens em portugues e ingles")
    void resolveMensagens() {
        MessageSource ms = config.messageSource();

        assertThat(ms.getMessage("error.user.notfound", null, Locale.forLanguageTag("pt-BR")))
                .isEqualTo("Usuário não encontrado.");
        assertThat(ms.getMessage("error.user.notfound", null, Locale.ENGLISH))
                .isEqualTo("User not found.");
    }

    @Test
    @DisplayName("deve interpolar argumentos na mensagem")
    void interpolaArgumentos() {
        assertThat(config.messageSource().getMessage("success.user.created", new Object[]{"JOAO"}, Locale.ENGLISH))
                .isEqualTo("User JOAO created successfully!");
    }

    @Test
    @DisplayName("locale nao suportado deve cair no bundle padrao (pt), sem usar locale do sistema")
    void localeNaoSuportado() {
        MessageSource ms = config.messageSource();

        assertThat(ms.getMessage("error.user.notfound", null, Locale.JAPANESE))
                .isEqualTo(ms.getMessage("error.user.notfound", null, Locale.forLanguageTag("pt-BR")));
    }

    @Test
    @DisplayName("resolver de locale deve ter pt-BR como padrao")
    void localePadrao() {
        LocaleResolver resolver = config.localeResolver();

        assertThat(resolver.resolveLocale(new MockHttpServletRequest()))
                .isEqualTo(Locale.forLanguageTag("pt-BR"));
    }

    @Test
    @DisplayName("interceptor deve usar o parametro 'lang'")
    void parametroLang() {
        assertThat(config.localeChangeInterceptor().getParamName()).isEqualTo("lang");
    }

    @Test
    @DisplayName("addInterceptors deve registrar o LocaleChangeInterceptor")
    void registraInterceptor() {
        RegistroExposto registro = new RegistroExposto();

        config.addInterceptors(registro);

        assertThat(registro.interceptors()).hasSize(1);
    }

    private MockHttpServletRequest requestComLang(LocaleResolver resolver, String lang) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, resolver);
        req.setParameter("lang", lang);
        return req;
    }

    @Test
    @DisplayName("?lang=en deve trocar o locale da sessao")
    void trocaLocale() throws Exception {
        LocaleResolver resolver = config.localeResolver();
        HandlerInterceptor interceptor = config.localeChangeInterceptor();
        MockHttpServletRequest req = requestComLang(resolver, "en");

        interceptor.preHandle(req, new MockHttpServletResponse(), new Object());

        assertThat(resolver.resolveLocale(req).getLanguage()).isEqualTo("en");
    }

    @Test
    @DisplayName("sem parametro lang o locale padrao e mantido")
    void semParametro() throws Exception {
        LocaleResolver resolver = config.localeResolver();
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, resolver);

        config.localeChangeInterceptor().preHandle(req, new MockHttpServletResponse(), new Object());

        assertThat(resolver.resolveLocale(req)).isEqualTo(Locale.forLanguageTag("pt-BR"));
    }

    @Test
    @DisplayName("abuso: ?lang com caracteres invalidos e ignorado (sem HTTP 500 em rota publica)")
    void langInvalidoEhIgnorado() throws Exception {
        LocaleChangeInterceptor interceptor = config.localeChangeInterceptor();
        LocaleResolver resolver = config.localeResolver();
        MockHttpServletRequest req = requestComLang(resolver, "!!<script>");

        assertThat(interceptor.preHandle(req, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(resolver.resolveLocale(req)).isEqualTo(Locale.forLanguageTag("pt-BR"));
    }
}
