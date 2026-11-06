package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.ProdutoVinculoDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.RoteiroService;
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

import java.util.ArrayList;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = RoteiroController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(RoteiroController.class)
@DisplayName("RoteiroController")
class RoteiroControllerTest {

    private static final String URL = "/dashboard/roteiros";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    RoteiroService roteiroService;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    // ---------- GET listar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET lista roteiros e setores e entrega um RoteiroDTO vazio")
    void listaRoteiros() throws Exception {
        List<RoteiroListagemDTO> roteiros = List.of(new RoteiroListagemDTO(1L, "R1", "desc", 2, List.of("Corte"), 3L));
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(1L, "Corte", 1, List.of()));
        when(roteiroService.listarRoteiros("r1")).thenReturn(roteiros);
        when(setorService.listarSetores()).thenReturn(setores);

        mockMvc.perform(get(URL).param("busca", "r1"))
                .andExpect(status().isOk())
                .andExpect(view().name("roteiro/Roteiros"))
                .andExpect(model().attribute("roteiros", roteiros))
                .andExpect(model().attribute("setores", setores))
                .andExpect(model().attribute("roteiroDTO", new RoteiroDTO(null, null, null, new ArrayList<>())));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET AJAX devolve só o fragmento e não carrega setores")
    void listaAjax() throws Exception {
        mockMvc.perform(get(URL).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk())
                .andExpect(view().name("roteiro/Roteiros :: listaRoteiros"))
                .andExpect(model().attributeExists("roteiros"))
                .andExpect(model().attributeDoesNotExist("setores"));

        verifyNoInteractions(setorService);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET com erro inesperado no service é tratado pelo advice (redirect com flash genérico)")
    void listaComErro() throws Exception {
        when(roteiroService.listarRoteiros(any())).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SYSTEM_INTERNAL)));
    }

    // ---------- GET JSON ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET /{id} retorna o roteiro em JSON")
    void buscaPorId() throws Exception {
        when(roteiroService.buscarPorId(7L)).thenReturn(
                new RoteiroDTO(7L, "R1", "desc", List.of(new PassoRoteiroDTO(1L, null), new PassoRoteiroDTO(2L, 5L))));

        mockMvc.perform(get(URL + "/7"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.codigo").value("R1"))
                .andExpect(jsonPath("$.passos.length()").value(2))
                .andExpect(jsonPath("$.passos[1].etapaSetorId").value(5));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET /{id} com BusinessException vira 404")
    void buscaPorIdInexistente() throws Exception {
        when(roteiroService.buscarPorId(99L)).thenThrow(new BusinessException(AppMessages.ERROR_ROUTE_NOTFOUND));

        mockMvc.perform(get(URL + "/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET /{id}/produtos/busca retorna a página de produtos para vínculo")
    void buscaProdutosParaVinculo() throws Exception {
        when(roteiroService.buscarProdutosParaVinculo(7L, "vid", true, 2)).thenReturn(new PaginaDTO<>(
                List.of(new ProdutoVinculoDTO(1L, "Vidro", "P1", "R1", true)), 2, false, 21L));

        mockMvc.perform(get(URL + "/7/produtos/busca")
                        .param("busca", "vid").param("vinculados", "true").param("pagina", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].nome").value("Vidro"))
                .andExpect(jsonPath("$.itens[0].vinculadoAqui").value(true))
                .andExpect(jsonPath("$.pagina").value(2))
                .andExpect(jsonPath("$.temMais").value(false))
                .andExpect(jsonPath("$.total").value(21));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("GET /{id}/produtos/busca sem parâmetros usa busca vazia, vinculados=false e página 0")
    void buscaProdutosPadrao() throws Exception {
        when(roteiroService.buscarProdutosParaVinculo(7L, "", false, 0))
                .thenReturn(new PaginaDTO<>(List.of(), 0, false, 0L));

        mockMvc.perform(get(URL + "/7/produtos/busca")).andExpect(status().isOk());

        verify(roteiroService).buscarProdutosParaVinculo(7L, "", false, 0);
    }

    // ---------- POST salvar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST válido cria o roteiro com seus passos e avisa sucesso")
    void criaRoteiro() throws Exception {
        mockMvc.perform(post(URL).with(csrf())
                        .param("codigo", "R1").param("descricao", "desc")
                        .param("passos[0].setorId", "1").param("passos[0].etapaSetorId", "2")
                        .param("passos[1].setorId", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ROUTE_CREATED)));

        ArgumentCaptor<RoteiroDTO> dto = ArgumentCaptor.forClass(RoteiroDTO.class);
        verify(roteiroService).criar(dto.capture());
        assertThat(dto.getValue().codigo()).isEqualTo("R1");
        assertThat(dto.getValue().descricao()).isEqualTo("desc");
        assertThat(dto.getValue().passos())
                .containsExactly(new PassoRoteiroDTO(1L, 2L), new PassoRoteiroDTO(3L, null));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem passos devolve flash de validação e não chama o service")
    void criaSemPassos() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("codigo", "R1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(roteiroService, never()).criar(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com passo sem setor (validação aninhada) devolve flash de validação")
    void criaPassoSemSetor() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("codigo", "R1").param("passos[0].etapaSetorId", "2"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(roteiroService, never()).criar(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST sem código devolve flash de validação")
    void criaSemCodigo() throws Exception {
        mockMvc.perform(post(URL).with(csrf()).param("passos[0].setorId", "1"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(roteiroService, never()).criar(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException no service é tratado pelo advice (sem Referer vai para o destino padrão)")
    void criaComBusinessExceptionSemReferer() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE)).when(roteiroService).criar(any(RoteiroDTO.class));

        mockMvc.perform(post(URL).with(csrf()).param("codigo", "R1").param("passos[0].setorId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/ordens-setor"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ROUTE_CODE_DUPLICATE)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST com BusinessException volta para a página de origem quando há Referer interno")
    void criaComBusinessExceptionComReferer() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE)).when(roteiroService).criar(any(RoteiroDTO.class));

        mockMvc.perform(post(URL).with(csrf()).header("Referer", "http://localhost/dashboard/produtos")
                        .param("codigo", "R1").param("passos[0].setorId", "1"))
                .andExpect(redirectedUrl("http://localhost/dashboard/produtos"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ROUTE_CODE_DUPLICATE)));
    }

    // ---------- POST editar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar válido atualiza e avisa sucesso")
    void editaRoteiro() throws Exception {
        mockMvc.perform(post(URL + "/7/editar").with(csrf())
                        .param("codigo", "R1").param("passos[0].setorId", "1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ROUTE_UPDATED)));

        verify(roteiroService).atualizar(eq(7L), any(RoteiroDTO.class));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar sem passos devolve flash de validação")
    void editaInvalido() throws Exception {
        mockMvc.perform(post(URL + "/7/editar").with(csrf()).param("codigo", "R1"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(roteiroService, never()).atualizar(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/editar com roteiro em uso (BusinessException) é tratado pelo advice")
    void editaRoteiroEmUso() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ROUTE_IN_USE))
                .when(roteiroService).atualizar(eq(7L), any(RoteiroDTO.class));

        mockMvc.perform(post(URL + "/7/editar").with(csrf())
                        .param("codigo", "R1").param("passos[0].setorId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ROUTE_IN_USE)));
    }

    // ---------- POST duplicar ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/duplicar válido duplica com o novo código")
    void duplicaRoteiro() throws Exception {
        mockMvc.perform(post(URL + "/7/duplicar").with(csrf()).param("codigo", "R2"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ROUTE_DUPLICATED)));

        verify(roteiroService).duplicar(7L, "R2");
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/duplicar sem código devolve flash de validação")
    void duplicaSemCodigo() throws Exception {
        mockMvc.perform(post(URL + "/7/duplicar").with(csrf()).param("codigo", ""))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(roteiroService, never()).duplicar(any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/duplicar com código acima de 30 caracteres devolve flash de validação")
    void duplicaCodigoGrande() throws Exception {
        mockMvc.perform(post(URL + "/7/duplicar").with(csrf()).param("codigo", "x".repeat(31)))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/duplicar com código duplicado (BusinessException) é tratado pelo advice")
    void duplicaCodigoDuplicado() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ROUTE_CODE_DUPLICATE))
                .when(roteiroService).duplicar(7L, "R2");

        mockMvc.perform(post(URL + "/7/duplicar").with(csrf()).param("codigo", "R2"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ROUTE_CODE_DUPLICATE)));
    }

    // ---------- POST vincular produtos ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/produtos repassa as listas de produtos a adicionar e remover")
    void vinculaProdutos() throws Exception {
        mockMvc.perform(post(URL + "/7/produtos").with(csrf())
                        .param("adicionar", "10").param("adicionar", "20").param("remover", "30"))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ROUTE_PRODUCTS_LINKED)));

        verify(roteiroService).vincularProdutos(7L, List.of(10L, 20L), List.of(30L));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/produtos sem listas ainda chama o service e avisa sucesso")
    void vinculaSemListas() throws Exception {
        mockMvc.perform(post(URL + "/7/produtos").with(csrf()))
                .andExpect(redirectedUrl(URL));

        verify(roteiroService).vincularProdutos(eq(7L), any(), any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/produtos com produto inexistente (BusinessException) é tratado pelo advice")
    void vinculaProdutoInexistente() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_PRODUCT_NOTFOUND))
                .when(roteiroService).vincularProdutos(eq(7L), any(), any());

        mockMvc.perform(post(URL + "/7/produtos").with(csrf()).param("adicionar", "10"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_PRODUCT_NOTFOUND)));
    }

    // ---------- POST excluir ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir remove o roteiro e avisa sucesso")
    void excluiRoteiro() throws Exception {
        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ROUTE_DELETED)));

        verify(roteiroService).excluir(7L);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /{id}/excluir com produtos vinculados (BusinessException) é tratado pelo advice")
    void excluiComProdutosVinculados() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ROUTE_HAS_PRODUCTS)).when(roteiroService).excluir(7L);

        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ROUTE_HAS_PRODUCTS)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(URL).param("codigo", "R1")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/editar").param("codigo", "R1")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/duplicar").param("codigo", "R2")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/produtos")).andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/excluir")).andExpect(status().isForbidden());

        verifyNoInteractions(roteiroService);
    }

    @ParameterizedTest(name = "perfil {0} gerencia roteiros")
    @ValueSource(strings = {"PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP e superiores têm acesso")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isOk());
        mockMvc.perform(post(URL + "/7/excluir").with(perfil(role)).with(csrf()))
                .andExpect(redirectedUrl(URL));
    }

    @ParameterizedTest(name = "perfil {0} recebe 403")
    @ValueSource(strings = {"GESTAO", "OPERADOR"})
    @DisplayName("GESTAO e OPERADOR não acessam roteiros")
    void perfisProibidos(String role) throws Exception {
        mockMvc.perform(get(URL).with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/7").with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/7/produtos/busca").with(perfil(role))).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).with(perfil(role)).with(csrf()).param("codigo", "R1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL + "/7/excluir").with(perfil(role)).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(roteiroService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post(URL + "/7/excluir").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(roteiroService);
    }
}
