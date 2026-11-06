package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.NivelAcessoService;
import br.com.lumiflow.service.SetorService;
import br.com.lumiflow.service.UsuarioService;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.logadoComo;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

@WebMvcTest(controllers = UsuarioController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(UsuarioController.class)
@DisplayName("UsuarioController")
class UsuarioControllerTest {

    private static final String URL = "/dashboard/usuario";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    UsuarioService usuarioService;
    @MockitoBean
    NivelAcessoService nivelAcessoService;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave, Object... args) {
        return messageSource.getMessage(chave, args, PT_BR);
    }

    private MockHttpServletRequestBuilder postCriacao() {
        return post(URL).with(csrf())
                .param("nome", "Maria").param("login", "maria").param("senha", "Senha1234")
                .param("nivelAcessoId", "2").param("setorId", "1");
    }

    private MockHttpServletRequestBuilder postEdicao() {
        return post(URL + "/9/editar").with(csrf())
                .param("id", "9").param("nome", "Maria").param("login", "maria")
                .param("nivelAcessoId", "2");
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET lista usuários, níveis de acesso e setores e entrega DTO vazio")
    void listaUsuarios() throws Exception {
        List<UsuarioListaDTO> usuarios = List.of(new UsuarioListaDTO(1L, "Maria", "maria", 2L, 1L, "PCP", "Corte"));
        List<NivelAcessoDTO> niveis = List.of(new NivelAcessoDTO(2L, "PCP"));
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(1L, "Corte", 1, List.of()));
        when(usuarioService.buscar("mar")).thenReturn(usuarios);
        when(nivelAcessoService.listarTodos()).thenReturn(niveis);
        when(setorService.listarSetores()).thenReturn(setores);

        mockMvc.perform(get(URL).param("nome", "mar"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"))
                .andExpect(model().attribute("usuarios", usuarios))
                .andExpect(model().attribute("niveisAcesso", niveis))
                .andExpect(model().attribute("setores", setores))
                .andExpect(model().attribute("usuarioDTO", new UsuarioDTO(null, null, null, null, null, null)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET com falha no service mantém a view e informa o erro")
    void listaComFalha() throws Exception {
        when(usuarioService.buscar(any())).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- POST criar ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST válido cria o usuário e avisa sucesso com o login")
    void criaUsuario() throws Exception {
        mockMvc.perform(postCriacao())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_USER_CREATED, "maria")));

        verify(usuarioService).novoUsuario(new UsuarioDTO(null, "Maria", "maria", "Senha1234", 2L, 1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST com senha curta reexibe a view com a mensagem de tamanho da senha")
    void criaSenhaCurta() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .param("nome", "Maria").param("login", "maria").param("senha", "abc")
                        .param("nivelAcessoId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"))
                .andExpect(model().attribute("error", msg("validation.password.size")))
                .andExpect(model().attributeHasFieldErrors("usuarioDTO", "senha"));

        verify(usuarioService, never()).novoUsuario(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST sem login reexibe a view com erro de validação")
    void criaSemLogin() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .param("nome", "Maria").param("senha", "Senha1234").param("nivelAcessoId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuarioDTO", "login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST sem nível de acesso reexibe a view com erro de validação")
    void criaSemNivelAcesso() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .param("nome", "Maria").param("login", "maria").param("senha", "Senha1234"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuarioDTO", "nivelAcessoId"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST com BusinessException devolve flash de erro e o DTO sem a senha")
    void criaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS))
                .when(usuarioService).novoUsuario(any(UsuarioDTO.class));

        mockMvc.perform(postCriacao())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_USER_ALREADY_EXISTS)))
                .andExpect(flash().attribute("usuarioDTO", new UsuarioDTO(null, "Maria", "maria", null, 2L, 1L)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST com erro inesperado devolve mensagem genérica e o DTO sem a senha")
    void criaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(usuarioService).novoUsuario(any(UsuarioDTO.class));

        mockMvc.perform(postCriacao())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(flash().attribute("usuarioDTO", new UsuarioDTO(null, "Maria", "maria", null, 2L, 1L)));
    }

    // ---------- POST excluir ----------

    @Test
    @DisplayName("POST /{id}/excluir repassa o id do alvo e o id do usuário logado")
    void excluiUsuario() throws Exception {
        mockMvc.perform(post(URL + "/9/excluir").with(csrf()).with(logadoComo(1L, "Admin", Descricao.ADMIN)))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_USER_DELETED)));

        verify(usuarioService).excluirUsuario(9L, 1L);
    }

    @Test
    @DisplayName("POST /{id}/excluir com BusinessException (autoexclusão) devolve flash de erro")
    void excluiComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_USER_CANNOT_DELETE_SELF))
                .when(usuarioService).excluirUsuario(1L, 1L);

        mockMvc.perform(post(URL + "/1/excluir").with(csrf()).with(logadoComo(1L, "Admin", Descricao.ADMIN)))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_USER_CANNOT_DELETE_SELF)));
    }

    @Test
    @DisplayName("POST /{id}/excluir com erro inesperado devolve mensagem genérica")
    void excluiComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(usuarioService).excluirUsuario(anyLong(), anyLong());

        mockMvc.perform(post(URL + "/9/excluir").with(csrf()).with(logadoComo(1L, "Admin", Descricao.ADMIN)))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{id}/excluir com principal que não é UsuarioDetails é negado (403) sem excluir")
    void excluiComPrincipalSemId() throws Exception {
        mockMvc.perform(post(URL + "/9/excluir").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso com o login")
    void editaUsuario() throws Exception {
        mockMvc.perform(postEdicao())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_USER_UPDATED, "maria")));

        verify(usuarioService).editarUsuario(eq(9L), eq(new UsuarioEdicaoDTO(9L, "Maria", "maria", 2L, null, null)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{id}/editar sem login reexibe a view com erro de validação")
    void editaInvalido() throws Exception {
        mockMvc.perform(post(URL + "/9/editar").with(csrf())
                        .param("id", "9").param("nome", "Maria").param("nivelAcessoId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"))
                .andExpect(model().attributeHasFieldErrors("usuarioDTO", "login"));

        verify(usuarioService, never()).editarUsuario(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{id}/editar com BusinessException devolve flash de erro e o DTO sem a senha")
    void editaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_USER_LAST_ADMIN))
                .when(usuarioService).editarUsuario(eq(9L), any(UsuarioEdicaoDTO.class));

        mockMvc.perform(postEdicao().param("senha", "NovaSenha123"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_USER_LAST_ADMIN)))
                .andExpect(flash().attribute("usuarioDTO", new UsuarioDTO(9L, "Maria", "maria", null, 2L, null)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /{id}/editar com erro inesperado devolve mensagem genérica")
    void editaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(usuarioService).editarUsuario(eq(9L), any(UsuarioEdicaoDTO.class));

        mockMvc.perform(postEdicao())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(URL).param("nome", "Maria")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/9/excluir")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/9/editar").param("nome", "Maria")).andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("ADMIN acessa a listagem")
    void adminAcessa() throws Exception {
        mockMvc.perform(get(URL).with(perfil("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("usuario/ListaUsuario"));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"SUPERVISOR", "PCP", "GESTAO", "OPERADOR"})
    @DisplayName("somente ADMIN gerencia usuários (a hierarquia não vale de baixo para cima)")
    void naoAdminProibido(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(postCriacao().with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/9/excluir").with(csrf()).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(postEdicao().with(perfil(role))).andExpect(status().isForbidden());

        verifyNoInteractions(usuarioService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(usuarioService);
    }
}
