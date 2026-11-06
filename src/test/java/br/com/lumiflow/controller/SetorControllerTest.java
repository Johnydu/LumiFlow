package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
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

@WebMvcTest(controllers = SetorController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(SetorController.class)
@DisplayName("SetorController")
class SetorControllerTest {

    private static final String URL = "/dashboard/setores";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET sem filtro lista todos os setores")
    void listaTodos() throws Exception {
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(1L, "Corte", 2, List.of()));
        when(setorService.listarSetores()).thenReturn(setores);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("setor/Setores"))
                .andExpect(model().attribute("setores", setores))
                .andExpect(model().attribute("setorDTO", new SetorDTO(null, null, null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com nome busca por nome sem espaços nas pontas")
    void buscaPorNome() throws Exception {
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(1L, "Corte", 2, List.of()));
        when(setorService.buscarPorNome("corte")).thenReturn(setores);

        mockMvc.perform(get(URL).param("nome", "  corte "))
                .andExpect(model().attribute("setores", setores));

        verify(setorService, never()).listarSetores();
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET AJAX devolve somente o fragmento da lista")
    void getAjax() throws Exception {
        mockMvc.perform(get(URL).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(view().name("setor/Setores :: listaSetores"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com falha no service mostra o erro (aviso ao usuário) e lista vazia")
    void getComFalha() throws Exception {
        when(setorService.listarSetores()).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("setor/Setores"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("setores", List.of()));
    }

    // ---------- POST criar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST válido cria o setor com suas etapas e avisa sucesso")
    void criaSetorComEtapas() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Corte")
                        .param("etapas[0].nome", "Corte reto").param("etapas[0].ordem", "1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_SECTOR_CREATED, "Corte")));

        ArgumentCaptor<SetorDTO> dto = ArgumentCaptor.forClass(SetorDTO.class);
        verify(setorService).novoSetor(dto.capture());
        assertThat(dto.getValue().nome()).isEqualTo("Corte");
        assertThat(dto.getValue().etapas()).containsExactly(new EtapaSetorDTO(null, "Corte reto", 1));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem nome reexibe a página com erro de validação")
    void criaSemNome() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("setor/Setores"))
                .andExpect(model().attributeExists("error", "setores"))
                .andExpect(model().attributeHasFieldErrors("setorDTO", "nome"));

        verify(setorService, never()).novoSetor(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com nome acima de 50 caracteres é rejeitado")
    void criaNomeGrande() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "x".repeat(51)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.name.size50")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE))
                .when(setorService).novoSetor(any(SetorDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Corte"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SECTOR_NAME_DUPLICATE)))
                .andExpect(flash().attributeExists("setorDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com erro inesperado devolve mensagem genérica")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(setorService).novoSetor(any(SetorDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Corte"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST excluir ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir remove o setor e avisa sucesso")
    void excluiSetor() throws Exception {
        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_SECTOR_DELETED)));

        verify(setorService).excluirSetor(7L);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com BusinessException (setor com máquinas) devolve flash de erro")
    void excluiComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_SECTOR_HAS_MACHINES)).when(setorService).excluirSetor(7L);

        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SECTOR_HAS_MACHINES)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com erro inesperado devolve mensagem genérica")
    void excluiComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(setorService).excluirSetor(7L);

        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso")
    void editaSetor() throws Exception {
        mockMvc.perform(post(URL + "/7/editar").with(csrf()).param("nome", "Lapidação"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_SECTOR_UPDATED, "Lapidação")));

        verify(setorService).editarSetor(eq(7L), any(SetorDTO.class));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar inválido reexibe a página com erro")
    void editaInvalido() throws Exception {
        mockMvc.perform(post(URL + "/7/editar").with(csrf()).param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("setor/Setores"))
                .andExpect(model().attributeExists("error"));

        verify(setorService, never()).editarSetor(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com BusinessException devolve flash de erro e o DTO")
    void editaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE))
                .when(setorService).editarSetor(eq(7L), any(SetorDTO.class));

        mockMvc.perform(post(URL + "/7/editar").with(csrf()).param("nome", "Corte"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SECTOR_NAME_DUPLICATE)))
                .andExpect(flash().attributeExists("setorDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com erro inesperado devolve mensagem genérica")
    void editaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(setorService).editarSetor(eq(7L), any(SetorDTO.class));

        mockMvc.perform(post(URL + "/7/editar").with(csrf()).param("nome", "Corte"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(URL).param("nome", "Corte")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/excluir")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/editar").param("nome", "Corte")).andExpect(status().isForbidden());

        verifyNoInteractions(setorService);
    }

    @ParameterizedTest(name = "perfil {0} acessa setores")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores têm acesso")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isOk());
        mockMvc.perform(post(URL + "/7/excluir").with(perfil(role)).with(csrf()))
                .andExpect(redirectedUrl(URL));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não acessam setores")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(perfil(role)).with(csrf()).param("nome", "Corte"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(setorService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
