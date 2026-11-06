package br.com.lumiflow.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As anotações de validação dos DTOs referenciam chaves "validation.*" resolvidas pelo MessageSource.
 * Uma chave inexistente faria o usuário ver o texto cru; este teste detecta isso.
 */
class DtoValidationMessagesTest {

    @Test
    @DisplayName("Toda chave validation.* usada nos DTOs deve existir em pt-BR e em inglês")
    void chavesDeValidacaoDevemExistirNosArquivosDeMensagens() throws Exception {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);

        List<String> problemas = new ArrayList<>();
        int chavesVerificadas = 0;

        Resource[] recursos = new PathMatchingResourcePatternResolver()
                .getResources("classpath*:br/com/lumiflow/dto/**/*.class");
        for (Resource recurso : recursos) {
            String caminho = recurso.getURL().toString();
            String nomeClasse = caminho.substring(caminho.indexOf("br/com/lumiflow/dto"))
                    .replace('/', '.').replaceAll("\\.class$", "");
            Class<?> tipo = Class.forName(nomeClasse);
            for (Field campo : tipo.getDeclaredFields()) {
                for (Annotation anotacao : campo.getAnnotations()) {
                    String mensagem = mensagemDa(anotacao);
                    if (mensagem != null && mensagem.startsWith("validation.")) {
                        chavesVerificadas++;
                        for (Locale locale : List.of(Locale.forLanguageTag("pt-BR"), Locale.ENGLISH)) {
                            try {
                                source.getMessage(mensagem, null, locale);
                            } catch (NoSuchMessageException e) {
                                problemas.add(tipo.getSimpleName() + "." + campo.getName() + " -> " + mensagem + " (" + locale + ")");
                            }
                        }
                    }
                }
            }
        }

        assertThat(chavesVerificadas).as("deve encontrar anotações de validação nos DTOs").isPositive();
        assertThat(problemas).isEmpty();
    }

    private static String mensagemDa(Annotation anotacao) {
        try {
            Method metodo = anotacao.annotationType().getMethod("message");
            Object valor = metodo.invoke(anotacao);
            return valor instanceof String texto ? texto : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
