package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@WebMvcTest(controllers = OperadorController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(OperadorController.class)
@DisplayName("OperadorController")
class OperadorControllerTest {

    private static final String URL = "/dashboard/operadores";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    OperadorService operadorService;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET sem filtros lista todos os operadores e os setores")
    void listaTodos() throws Exception {
        List<OperadorListagemDTO> operadores = List.of(new OperadorListagemDTO(1L, "Ana", "Op", 2L, "Corte"));
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(2L, "Corte", 1, List.of()));
        when(operadorService.listarOperadores()).thenReturn(operadores);
        when(setorService.listarSetores()).thenReturn(setores);

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/Operadores"))
                .andExpect(model().attribute("operadores", operadores))
                .andExpect(model().attribute("listaSetores", setores))
                .andExpect(model().attribute("operadorDTO", new OperadorDTO(null, null, null, null)));

        verify(operadorService, never()).buscarOperadores(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com nome e setor usa a busca filtrada")
    void listaFiltrada() throws Exception {
        List<OperadorListagemDTO> operadores = List.of(new OperadorListagemDTO(1L, "Ana", "Op", 2L, "Corte"));
        when(operadorService.buscarOperadores("ana", 2L)).thenReturn(operadores);

        mockMvc.perform(get(URL).param("nome", "ana").param("setorId", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("operadores", operadores));

        verify(operadorService, never()).listarOperadores();
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com nome em branco e sem setor volta a listar todos")
    void nomeEmBrancoListaTodos() throws Exception {
        mockMvc.perform(get(URL).param("nome", "  ")).andExpect(status().isOk());

        verify(operadorService).listarOperadores();
        verify(operadorService, never()).buscarOperadores(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET AJAX devolve somente o fragmento da lista")
    void getAjaxDevolveFragmento() throws Exception {
        mockMvc.perform(get(URL).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/Operadores :: listaOperadores"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com falha nos services mostra o erro e não esconde a falha atrás de listas vazias")
    void getComFalha() throws Exception {
        when(operadorService.listarOperadores()).thenThrow(new IllegalStateException("falha"));
        when(setorService.listarSetores()).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/Operadores"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("operadores", List.of()))
                .andExpect(model().attribute("listaSetores", List.of()));
    }

    // ---------- POST criar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST válido cria o operador e avisa sucesso com o nome")
    void criaOperador() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Ana").param("funcao", "Op").param("setorPadraoId", "2"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_OPERATOR_CREATED, "Ana")));

        verify(operadorService).novoOperador(new OperadorDTO(null, "Ana", "Op", 2L));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem nome reexibe a página com erro de validação")
    void criaSemNome() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/Operadores"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attributeHasFieldErrors("operadorDTO", "nome"));

        verify(operadorService, never()).novoOperador(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com função acima de 30 caracteres é rejeitado")
    void criaFuncaoGrande() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("nome", "Ana").param("funcao", "x".repeat(31)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", msg("validation.role.size30")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE))
                .when(operadorService).novoOperador(any(OperadorDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Ana"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE)))
                .andExpect(flash().attribute("operadorDTO", new OperadorDTO(null, "Ana", null, null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com erro inesperado devolve mensagem genérica")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(operadorService).novoOperador(any(OperadorDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("nome", "Ana"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST excluir ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir remove o operador e avisa sucesso")
    void excluiOperador() throws Exception {
        mockMvc.perform(post(URL + "/4/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_OPERATOR_DELETED)));

        verify(operadorService).excluirOperador(4L);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com BusinessException devolve flash de erro")
    void excluiComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND)).when(operadorService).excluirOperador(4L);

        mockMvc.perform(post(URL + "/4/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_OPERATOR_NOTFOUND)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com erro inesperado devolve mensagem genérica")
    void excluiComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(operadorService).excluirOperador(4L);

        mockMvc.perform(post(URL + "/4/excluir").with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso")
    void editaOperador() throws Exception {
        mockMvc.perform(post(URL + "/4/editar").with(csrf()).param("nome", "Ana Maria"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_OPERATOR_UPDATED, "Ana Maria")));

        verify(operadorService).editarOperador(eq(4L), eq(new OperadorDTO(4L, "Ana Maria", null, null)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar inválido reexibe a página com erro")
    void editaInvalido() throws Exception {
        mockMvc.perform(post(URL + "/4/editar").with(csrf()).param("nome", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("operador/Operadores"))
                .andExpect(model().attributeExists("error"));

        verify(operadorService, never()).editarOperador(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com BusinessException devolve flash de erro e o DTO")
    void editaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE))
                .when(operadorService).editarOperador(eq(4L), any(OperadorDTO.class));

        mockMvc.perform(post(URL + "/4/editar").with(csrf()).param("nome", "Ana"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE)))
                .andExpect(flash().attributeExists("operadorDTO"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com erro inesperado devolve mensagem genérica")
    void editaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(operadorService).editarOperador(eq(4L), any(OperadorDTO.class));

        mockMvc.perform(post(URL + "/4/editar").with(csrf()).param("nome", "Ana"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(URL).param("nome", "Ana")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/4/excluir")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/4/editar").param("nome", "Ana")).andExpect(status().isForbidden());

        verifyNoInteractions(operadorService);
    }

    @ParameterizedTest(name = "perfil {0} acessa operadores")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores têm acesso")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isOk());
        mockMvc.perform(post(URL + "/4/excluir").with(perfil(role)).with(csrf()))
                .andExpect(redirectedUrl(URL));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não acessam operadores")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(perfil(role)).with(csrf()).param("nome", "Ana"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/4/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(operadorService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
