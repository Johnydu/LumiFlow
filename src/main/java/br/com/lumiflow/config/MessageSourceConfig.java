package br.com.lumiflow.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;
import java.util.Locale;

/**
 * Configuração de internacionalização (i18n) e MessageSource.
 *
 * Define como as mensagens são carregadas e qual é o locale padrão.
 */
@Configuration
public class MessageSourceConfig {

    /**
     * Configura o MessageSource para carregar mensagens de messages.properties.
     * Suporta fallback automático entre variações de locale.
     */
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

        // Nome base dos arquivos (sem extensão)
        messageSource.setBasename("messages");

        // Encoding para suportar acentuação
        messageSource.setDefaultEncoding("UTF-8");

        // Cache: -1 em DEV, >0 em PROD
        messageSource.setCacheSeconds(-1);

        // Fallback para locale padrão se chave não encontrada
        messageSource.setFallbackToSystemLocale(true);

        return messageSource;
    }

    /**
     * Define o locale padrão da aplicação.
     * Pode ser alterado para SessionLocaleResolver em produção.
     */
    @Bean
    public LocaleResolver localeResolver() {
        FixedLocaleResolver resolver = new FixedLocaleResolver();
        resolver.setDefaultLocale(new Locale("pt", "BR"));
        return resolver;
    }
}