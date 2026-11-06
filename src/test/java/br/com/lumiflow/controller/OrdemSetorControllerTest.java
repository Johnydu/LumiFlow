package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.DividirEtapaDTO;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemListagemDTO;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.OrdemSetorService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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

@WebMvcTest(controllers = OrdemSetorController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(OrdemSetorController.class)
@DisplayName("OrdemSetorController")
class OrdemSetorControllerTest {

    private static final String URL = "/dashboard/ordens-setor";
    private static final String LIBERAR = URL + "/5/ordens/20/liberar";
    private static final String PAUSAR = URL + "/5/ordens/20/pausar";
    private static final String DIVIDIR = URL + "/5/ordens/20/dividir";
    private static final String LISTA_DO_SETOR = "/dashboard/ordens-setor/5";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    OrdemSetorService ordemSetorService;
    @MockitoBean
    OrdemProducaoService ordemProducaoService;
    @MockitoBean
    SetorService setorService;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    // ---------- GET resumo dos setores ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET lista o resumo dos setores repassando a busca")
    void listaResumoSetores() throws Exception {
        List<ResumoSetorDTO> resumo = List.of(new ResumoSetorDTO(5L, "Corte", "Ana", 4L, 1L));
        when(ordemSetorService.obterResumoSetores("corte")).thenReturn(resumo);

        mockMvc.perform(get(URL).param("busca", "corte"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/ListaOrdemSetores"))
                .andExpect(model().attribute("resumoSetores", resumo));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET com falha no service mostra erro e lista vazia")
    void listaResumoComFalha() throws Exception {
        when(ordemSetorService.obterResumoSetores(any())).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/ListaOrdemSetores"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("resumoSetores", List.of()));
    }

    // ---------- GET ordens por setor ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /{setorId} lista as ordens do setor aplicando os filtros")
    void listaOrdensDoSetor() throws Exception {
        OrdemListagemDTO ordem = new OrdemListagemDTO(1L, 20L, "OP-1", "Vidro", "P1", "Corte reto", null, false,
                10, 5, 50.0, null, LocalDateTime.of(2026, 1, 1, 8, 0), EstatusOrdemProducao.LIBERADA);
        Setor setor = new Setor();
        setor.setId(5L);
        setor.setNome("Corte");
        when(ordemProducaoService.listarPorSetorIdEFiltros(eq(5L), any(OrdemFiltroDTO.class), eq(2)))
                .thenReturn(new PageImpl<>(List.of(ordem), PageRequest.of(2, 10), 21));
        when(setorService.buscarSetorPorId(5L)).thenReturn(setor);

        mockMvc.perform(get(URL + "/5").param("busca", "OP-1").param("status", "LIBERADA")
                        .param("setor", "x").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/ListaOrdem"))
                .andExpect(model().attribute("ordens", List.of(ordem)))
                .andExpect(model().attribute("setorAtual", "Corte"))
                .andExpect(model().attribute("setorIdAtual", 5L))
                .andExpect(model().attribute("buscaAtual", "OP-1"))
                .andExpect(model().attribute("statusAtual", "LIBERADA"))
                .andExpect(model().attribute("totalOrdens", 21L))
                .andExpect(model().attributeExists("pagina"));

        ArgumentCaptor<OrdemFiltroDTO> filtro = ArgumentCaptor.forClass(OrdemFiltroDTO.class);
        verify(ordemProducaoService).listarPorSetorIdEFiltros(eq(5L), filtro.capture(), eq(2));
        assertThat(filtro.getValue()).isEqualTo(new OrdemFiltroDTO("x", "OP-1", "LIBERADA"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /{setorId} com setor inexistente exibe 'Desconhecido'")
    void setorDesconhecido() throws Exception {
        when(ordemProducaoService.listarPorSetorIdEFiltros(eq(5L), any(OrdemFiltroDTO.class), anyInt()))
                .thenReturn(new PageImpl<>(List.of()));
        when(setorService.buscarSetorPorId(5L)).thenReturn(null);

        mockMvc.perform(get(URL + "/5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("setorAtual", "Desconhecido"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /{setorId} com BusinessException (setor inexistente) volta à lista de setores com o aviso")
    void listaOrdensSetorInexistente() throws Exception {
        when(ordemProducaoService.listarPorSetorIdEFiltros(eq(5L), any(OrdemFiltroDTO.class), anyInt()))
                .thenThrow(new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND));

        mockMvc.perform(get(URL + "/5"))
                .andExpect(redirectedUrl("/dashboard/ordens-setor"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_SECTOR_NOTFOUND)));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /{setorId} com falha inesperada mostra erro e lista vazia")
    void listaOrdensComFalha() throws Exception {
        when(ordemProducaoService.listarPorSetorIdEFiltros(eq(5L), any(OrdemFiltroDTO.class), anyInt()))
                .thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL + "/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/ListaOrdem"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("ordens", List.of()))
                .andExpect(model().attribute("totalOrdens", 0))
                .andExpect(model().attribute("setorAtual", "Desconhecido"));
    }

    // ---------- Liberar ----------

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar libera a etapa e volta para a lista do setor")
    void liberaEtapa() throws Exception {
        mockMvc.perform(post(LIBERAR).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ORDER_LIBERADA)));

        verify(ordemProducaoService).liberarOrdem(20L);
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com retorno=detalhes e ordemId volta para os detalhes da ordem")
    void liberaERetornaParaDetalhes() throws Exception {
        mockMvc.perform(post(LIBERAR).with(csrf()).param("retorno", "detalhes").param("ordemId", "10"))
                .andExpect(redirectedUrl("/dashboard/ordens/10/detalhes"));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com retorno=detalhes sem ordemId volta para a lista do setor")
    void liberaRetornoDetalhesSemOrdemId() throws Exception {
        mockMvc.perform(post(LIBERAR).with(csrf()).param("retorno", "detalhes"))
                .andExpect(redirectedUrl(LISTA_DO_SETOR));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar ignora retorno com URL externa (sem open redirect)")
    void liberaIgnoraRetornoExterno() throws Exception {
        mockMvc.perform(post(LIBERAR).with(csrf()).param("retorno", "http://site-malicioso.example").param("ordemId", "10"))
                .andExpect(redirectedUrl(LISTA_DO_SETOR));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com BusinessException devolve flash de erro e volta para a lista")
    void liberaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE)).when(ordemProducaoService).liberarOrdem(20L);

        mockMvc.perform(post(LIBERAR).with(csrf()))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ORDER_NOT_AVAILABLE)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com edição concorrente é repassado ao advice (mensagem de concorrência)")
    void liberaComConcorrencia() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Object.class, 20L))
                .when(ordemProducaoService).liberarOrdem(20L);

        mockMvc.perform(post(LIBERAR).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/ordens-setor"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_CONCURRENT_UPDATE)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com concorrência e Referer volta para a página de origem")
    void liberaComConcorrenciaEReferer() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Object.class, 20L))
                .when(ordemProducaoService).liberarOrdem(20L);

        mockMvc.perform(post(LIBERAR).with(csrf()).header("Referer", "http://localhost/dashboard/ordens/10/detalhes"))
                .andExpect(redirectedUrl("http://localhost/dashboard/ordens/10/detalhes"));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST liberar com erro inesperado devolve mensagem genérica")
    void liberaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(ordemProducaoService).liberarOrdem(20L);

        mockMvc.perform(post(LIBERAR).with(csrf()))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- Pausar ----------

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST pausar devolve a etapa para disponível e avisa sucesso")
    void pausaEtapa() throws Exception {
        mockMvc.perform(post(PAUSAR).with(csrf()))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ORDER_PAUSADA)));

        verify(ordemProducaoService).pausarEtapa(20L);
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST pausar com retorno=detalhes volta para os detalhes")
    void pausaERetornaParaDetalhes() throws Exception {
        mockMvc.perform(post(PAUSAR).with(csrf()).param("retorno", "detalhes").param("ordemId", "10"))
                .andExpect(redirectedUrl("/dashboard/ordens/10/detalhes"));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST pausar com BusinessException devolve flash de erro")
    void pausaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_ORDER_NOT_PAUSABLE)).when(ordemProducaoService).pausarEtapa(20L);

        mockMvc.perform(post(PAUSAR).with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ORDER_NOT_PAUSABLE)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST pausar com edição concorrente é repassado ao advice")
    void pausaComConcorrencia() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Object.class, 20L))
                .when(ordemProducaoService).pausarEtapa(20L);

        mockMvc.perform(post(PAUSAR).with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_CONCURRENT_UPDATE)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST pausar com erro inesperado devolve mensagem genérica")
    void pausaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(ordemProducaoService).pausarEtapa(20L);

        mockMvc.perform(post(PAUSAR).with(csrf()))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- Dividir ----------

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir válido repassa as divisões ao service e avisa sucesso")
    void dividePedido() throws Exception {
        mockMvc.perform(post(DIVIDIR).with(csrf())
                        .param("divisoes[0].variante", "A").param("divisoes[0].quantidade", "5")
                        .param("divisoes[1].variante", "B").param("divisoes[1].quantidade", "3"))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_ORDER_DIVIDED)));

        ArgumentCaptor<DividirEtapaDTO> dto = ArgumentCaptor.forClass(DividirEtapaDTO.class);
        verify(ordemProducaoService).dividirEtapa(eq(20L), dto.capture());
        assertThat(dto.getValue().divisoes()).hasSize(2);
        assertThat(dto.getValue().divisoes().get(0).variante()).isEqualTo("A");
        assertThat(dto.getValue().divisoes().get(1).quantidade()).isEqualTo(3);
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir sem divisões devolve flash de validação e não chama o service")
    void dividirSemDivisoes() throws Exception {
        mockMvc.perform(post(DIVIDIR).with(csrf()))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(ordemProducaoService, never()).dividirEtapa(any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir com item inválido (validação aninhada) devolve flash de validação")
    void dividirItemInvalido() throws Exception {
        mockMvc.perform(post(DIVIDIR).with(csrf())
                        .param("divisoes[0].variante", "").param("divisoes[0].quantidade", "0"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));

        verify(ordemProducaoService, never()).dividirEtapa(any(), any());
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir inválido respeita retorno=detalhes")
    void dividirInvalidoRetornaParaDetalhes() throws Exception {
        mockMvc.perform(post(DIVIDIR).with(csrf()).param("retorno", "detalhes").param("ordemId", "10"))
                .andExpect(redirectedUrl("/dashboard/ordens/10/detalhes"));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir com BusinessException devolve flash de erro")
    void dividirComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_DIVISION_SUM_MISMATCH))
                .when(ordemProducaoService).dividirEtapa(eq(20L), any(DividirEtapaDTO.class));

        mockMvc.perform(post(DIVIDIR).with(csrf())
                        .param("divisoes[0].variante", "A").param("divisoes[0].quantidade", "5"))
                .andExpect(redirectedUrl(LISTA_DO_SETOR))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_DIVISION_SUM_MISMATCH)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir com edição concorrente é repassado ao advice")
    void dividirComConcorrencia() throws Exception {
        doThrow(new ObjectOptimisticLockingFailureException(Object.class, 20L))
                .when(ordemProducaoService).dividirEtapa(eq(20L), any(DividirEtapaDTO.class));

        mockMvc.perform(post(DIVIDIR).with(csrf())
                        .param("divisoes[0].variante", "A").param("divisoes[0].quantidade", "5"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_CONCURRENT_UPDATE)));
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POST dividir com erro inesperado devolve mensagem genérica")
    void dividirComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha"))
                .when(ordemProducaoService).dividirEtapa(eq(20L), any(DividirEtapaDTO.class));

        mockMvc.perform(post(DIVIDIR).with(csrf())
                        .param("divisoes[0].variante", "A").param("divisoes[0].quantidade", "5"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    // ---------- CSRF e autorização ----------

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("POSTs sem CSRF são rejeitados com 403")
    void postsSemCsrf() throws Exception {
        mockMvc.perform(post(LIBERAR)).andExpect(status().isForbidden());
        mockMvc.perform(post(PAUSAR)).andExpect(status().isForbidden());
        mockMvc.perform(post(DIVIDIR)).andExpect(status().isForbidden());

        verifyNoInteractions(ordemProducaoService);
    }

    @ParameterizedTest(name = "perfil {0} libera, pausa e divide")
    @ValueSource(strings = {"GESTAO", "PCP", "SUPERVISOR", "ADMIN"})
    @DisplayName("perfis gerenciais podem liberar, pausar e dividir")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(post(LIBERAR).with(perfil(role)).with(csrf())).andExpect(redirectedUrl(LISTA_DO_SETOR));
        mockMvc.perform(post(PAUSAR).with(perfil(role)).with(csrf())).andExpect(redirectedUrl(LISTA_DO_SETOR));
        mockMvc.perform(post(DIVIDIR).with(perfil(role)).with(csrf())).andExpect(redirectedUrl(LISTA_DO_SETOR));
    }

    @Test
    @DisplayName("OPERADOR não pode liberar, pausar nem dividir (403) mas pode consultar as listas")
    void operadorProibidoDeAgir() throws Exception {
        mockMvc.perform(post(LIBERAR).with(perfil("OPERADOR")).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(PAUSAR).with(perfil("OPERADOR")).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(DIVIDIR).with(perfil("OPERADOR")).with(csrf())).andExpect(status().isForbidden());

        verifyNoInteractions(ordemProducaoService);

        mockMvc.perform(get(URL).with(perfil("OPERADOR"))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(post(LIBERAR).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(ordemProducaoService, ordemSetorService);
    }
}
