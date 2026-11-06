package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.MaquinaService;
import br.com.lumiflow.service.SetorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = MaquinaController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(MaquinaController.class)
@DisplayName("MaquinaController")
class MaquinaControllerTest {

    private static final String URL = "/dashboard/maquinas";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    MaquinaService maquinaService;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET lista máquinas e setores e entrega um DTO vazio")
    void listaMaquinas() throws Exception {
        List<MaquinaDTO> maquinas = List.of(new MaquinaDTO(1L, "Serra", "Corte", 3L));
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(3L, "Corte", 1, List.of()));
        when(maquinaService.listarMaquinas()).thenReturn(maquinas);
        when(setorService.listarSetores()).thenReturn(setores);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("maquina/CadastroMaquinas"))
                .andExpect(model().attribute("maquinas", maquinas))
                .andExpect(model().attribute("setores", setores))
                .andExpect(model().attribute("maquinaDTO", new MaquinaDTO(null, null, null, null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com falha nos services mostra o erro e não esconde a falha atrás de listas vazias")
    void listaComFalhaNosServices() throws Exception {
        when(maquinaService.listarMaquinas()).thenThrow(new IllegalStateException("falha"));
        when(setorService.listarSetores()).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("maquina/CadastroMaquinas"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("maquinas", List.of()))
                .andExpect(model().attribute("setores", List.of()));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET preserva o DTO recebido via flash attribute")
    void preservaDtoDoFlash() throws Exception {
        MaquinaDTO dto = new MaquinaDTO(null, "Serra", null, 3L);

        mockMvc.perform(get(URL).flashAttr("maquinaDTO", dto))
                .andExpect(model().attribute("maquinaDTO", dto));
    }

    // ---------- POST criar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST válido cria a máquina e avisa sucesso com o nome")
    void criaMaquina() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_MACHINE_CREATED, "Serra")));

        ArgumentCaptor<MaquinaDTO> dto = ArgumentCaptor.forClass(MaquinaDTO.class);
        verify(maquinaService).novaMaquina(dto.capture());
        assertThat(dto.getValue()).isEqualTo(new MaquinaDTO(null, "Serra", null, 3L));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem nome reexibe a página com mensagem de nome obrigatório")
    void criaSemNome() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "").param("setorId", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("maquina/CadastroMaquinas"))
                .andExpect(model().attribute("error", msg("validation.machine.name.required")))
                .andExpect(model().attributeHasFieldErrors("maquinaDTO", "nome"))
                .andExpect(model().attributeExists("maquinas", "setores"));

        verify(maquinaService, never()).novaMaquina(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem setor informa que o setor é obrigatório")
    void criaSemSetor() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Serra"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.sector.required")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com nome acima de 50 caracteres é rejeitado")
    void criaNomeGrande() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "x".repeat(51)).param("setorId", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.name.size50")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE))
                .when(maquinaService).novaMaquina(any(MaquinaDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_MACHINE_NAME_DUPLICATE)))
                .andExpect(flash().attribute("maquinaDTO", new MaquinaDTO(null, "Serra", null, 3L)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com erro inesperado devolve mensagem genérica")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(maquinaService).novaMaquina(any(MaquinaDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST excluir ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir remove a máquina e avisa sucesso")
    void excluiMaquina() throws Exception {
        mockMvc.perform(post(URL + "/5/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_MACHINE_DELETED)));

        verify(maquinaService).deletarMaquina(5L);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com BusinessException devolve flash de erro")
    void excluiComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND)).when(maquinaService).deletarMaquina(5L);

        mockMvc.perform(post(URL + "/5/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_MACHINE_NOTFOUND)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com erro inesperado devolve mensagem genérica")
    void excluiComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(maquinaService).deletarMaquina(5L);

        mockMvc.perform(post(URL + "/5/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir sem CSRF é rejeitado com 403")
    void excluiSemCsrf() throws Exception {
        mockMvc.perform(post(URL + "/5/excluir"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(maquinaService);
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso com o nome")
    void editaMaquina() throws Exception {
        mockMvc.perform(post(URL + "/5/editar").with(csrf()).param("nome", "Serra 2").param("setorId", "3"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_MACHINE_UPDATED, "Serra 2")));

        verify(maquinaService).editarMaquina(eq(5L), eq(new MaquinaDTO(5L, "Serra 2", null, 3L)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar inválido reexibe a página com erro")
    void editaInvalido() throws Exception {
        mockMvc.perform(post(URL + "/5/editar").with(csrf()).param("nome", " ").param("setorId", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("maquina/CadastroMaquinas"))
                .andExpect(model().attribute("error", msg("validation.machine.name.required")));

        verify(maquinaService, never()).editarMaquina(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com BusinessException devolve flash de erro e o DTO")
    void editaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE))
                .when(maquinaService).editarMaquina(eq(5L), any(MaquinaDTO.class));

        mockMvc.perform(post(URL + "/5/editar").with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_MACHINE_NAME_DUPLICATE)))
                .andExpect(flash().attributeExists("maquinaDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com erro inesperado devolve mensagem genérica")
    void editaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(maquinaService).editarMaquina(eq(5L), any(MaquinaDTO.class));

        mockMvc.perform(post(URL + "/5/editar").with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar sem CSRF é rejeitado com 403")
    void editaSemCsrf() throws Exception {
        mockMvc.perform(post(URL + "/5/editar").param("nome", "Serra").param("setorId", "3"))
                .andExpect(status().isForbidden());
    }

    // ---------- Autorização ----------

    @ParameterizedTest(name = "perfil {0} acessa e altera máquinas")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores têm acesso a GET e POST")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isOk());
        mockMvc.perform(post(URL + "/5/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não acessam o cadastro de máquinas")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(perfil(role)).with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/5/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/5/editar").with(perfil(role)).with(csrf())
                        .param("nome", "Serra").param("setorId", "3"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(maquinaService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Serra").param("setorId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(maquinaService);
    }
}
