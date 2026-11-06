package br.com.lumiflow.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AppMessages")
class AppMessagesTest {

    private static List<String> chaves() throws IllegalAccessException {
        List<String> chaves = new ArrayList<>();
        for (Field f : AppMessages.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == String.class) {
                chaves.add((String) f.get(null));
            }
        }
        return chaves;
    }

    @Test
    @DisplayName("todas as chaves sao nao vazias e unicas")
    void chavesNaoVaziasEUnicas() throws Exception {
        List<String> chaves = chaves();
        Set<String> unicas = new HashSet<>(chaves);

        assertThat(chaves).isNotEmpty().allSatisfy(c -> assertThat(c).isNotBlank().doesNotContain(" "));
        assertThat(unicas).hasSameSizeAs(chaves);
    }

    @Test
    @DisplayName("todas as chaves existem em pt-BR (messages.properties)")
    void chavesExistemEmPortugues() throws Exception {
        MessageSource ms = new MessageSourceConfig().messageSource();

        for (String chave : chaves()) {
            assertThat(ms.getMessage(chave, new Object[]{"x", "x", "x"}, Locale.forLanguageTag("pt-BR")))
                    .as("mensagem pt-BR da chave %s", chave).isNotBlank();
        }
    }

    @Test
    @DisplayName("todas as chaves existem em ingles (messages_en.properties)")
    void chavesExistemEmIngles() throws Exception {
        MessageSource ms = new MessageSourceConfig().messageSource();

        for (String chave : chaves()) {
            assertThat(ms.getMessage(chave, new Object[]{"x", "x", "x"}, Locale.ENGLISH))
                    .as("mensagem en da chave %s", chave).isNotBlank();
        }
    }

    @Test
    @DisplayName("classe utilitaria tem construtor privado")
    void construtorPrivado() throws Exception {
        Constructor<AppMessages> ctor = AppMessages.class.getDeclaredConstructor();
        assertThat(Modifier.isPrivate(ctor.getModifiers())).isTrue();
        ctor.setAccessible(true);

        // Nao deve lancar: apenas garante que a classe e instanciavel por reflexao sem efeitos colaterais.
        assertThat(ctor.newInstance()).isNotNull();
    }

    @Test
    @DisplayName("chave inexistente deve falhar com NoSuchMessageException")
    void chaveInexistente() {
        MessageSource ms = new MessageSourceConfig().messageSource();

        assertThatThrownBy(() -> ms.getMessage("chave.que.nao.existe", null, Locale.ENGLISH))
                .isInstanceOf(org.springframework.context.NoSuchMessageException.class);
    }
}
