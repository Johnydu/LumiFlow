package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.LancamentoProducaoDTO;
import br.com.lumiflow.dto.lancamento.RefugoDTO;
import br.com.lumiflow.dto.lancamento.RetrabalhoDTO;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.LancamentoService;
import br.com.lumiflow.service.SetorService;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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

@WebMvcTest(controllers = LancamentoController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(LancamentoController.class)
@DisplayName("LancamentoController")
class LancamentoControllerTest {

    private static final String BASE = "/dashboard/ordens/10/setor/20/lancar";
    private static final String PAGINA_LANCAMENTO = BASE;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    LancamentoService lancamentoService;
    @MockitoBean
    SetorService setorService;

    private OrdemSetor ordemSetor;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    private OrdemSetor criarOrdemSetor(Long id, EstatusOrdemProducao status) {
        Setor setor = new Setor();
        setor.setId(5L);
        setor.setNome("Corte");
        OrdemProducao ordem = new OrdemProducao();
        ordem.setId(10L);
        ordem.setNumero("OP-10");
        OrdemSetor os = new OrdemSetor();
        os.setId(id);
        os.setSetor(setor);
        os.setOrdemProducao(ordem);
        os.setStatus(status);
        return os;
    }

    @BeforeEach
    void stubsPadrao() {
        ordemSetor = criarOrdemSetor(20L, EstatusOrdemProducao.EM_ANDAMENTO);
        when(lancamentoService.buscarEtapaAtiva(10L, 20L)).thenReturn(ordemSetor);
        when(lancamentoService.buscarNomeEtapa(ordemSetor)).thenReturn("Corte reto");
    }

    private MockHttpServletRequestBuilder postProducao() {
        return post(BASE + "/producao").with(csrf())
                .param("maquinaId", "1").param("operadorId", "2").param("quantidade", "5")
                .param("observacao", "ok");
    }

    private MockHttpServletRequestBuilder postRefugo() {
        return post(BASE + "/refugo").with(csrf())
                .param("quantidade", "2").param("setorOrigemId", "5").param("etapaSetorId", "7")
                .param("operadorId", "2").param("destino", "DESCARTE").param("motivo", "Quebra");
    }

    private MockHttpServletRequestBuilder postRetrabalho() {
        return post(BASE + "/retrabalho").with(csrf())
                .param("refugoId", "9").param("maquinaId", "1").param("operadorId", "2")
                .param("quantidade", "3").param("observacao", "obs");
    }

    // ---------- GET ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET carrega ordem, etapa, listas de apoio e DTOs vazios no model")
    void getCarregaModel() throws Exception {
        Maquina maquina = mock(Maquina.class);
        Operador operador = mock(Operador.class);
        Refugo refugo = mock(Refugo.class);
        Maquina maquinaRetrabalho = mock(Maquina.class);
        when(lancamentoService.listarMaquinasDoSetor(5L)).thenReturn(List.of(maquina));
        when(lancamentoService.listarOperadoresDoSetor(5L)).thenReturn(List.of(operador));
        when(lancamentoService.listarRefugosParaRetrabalho(10L)).thenReturn(List.of(refugo));
        when(lancamentoService.listarMaquinasParaRetrabalho(10L)).thenReturn(List.of(maquinaRetrabalho));

        mockMvc.perform(get(PAGINA_LANCAMENTO))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/Lancamento"))
                .andExpect(model().attribute("ordem", ordemSetor.getOrdemProducao()))
                .andExpect(model().attribute("ordemSetor", ordemSetor))
                .andExpect(model().attribute("etapaNome", "Corte reto"))
                .andExpect(model().attribute("maquinas", List.of(maquina)))
                .andExpect(model().attribute("operadores", List.of(operador)))
                .andExpect(model().attribute("refugosParaRetrabalho", List.of(refugo)))
                .andExpect(model().attribute("todasMaquinasParaRetrabalho", List.of(maquinaRetrabalho)))
                .andExpect(model().attributeExists("setores"))
                .andExpect(model().attribute("producaoDTO", new LancamentoProducaoDTO(null, null, null, null)))
                .andExpect(model().attribute("refugoDTO", new RefugoDTO(null, null, null, null, null, null)))
                .andExpect(model().attribute("retrabalhoDTO", new RetrabalhoDTO(null, null, null, null, null)));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET preserva o DTO recebido via flash attribute")
    void getPreservaDtoDoFlash() throws Exception {
        LancamentoProducaoDTO dto = new LancamentoProducaoDTO(1L, 2L, 3, "x");

        mockMvc.perform(get(PAGINA_LANCAMENTO).flashAttr("producaoDTO", dto))
                .andExpect(status().isOk())
                .andExpect(model().attribute("producaoDTO", dto));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET com etapa inexistente (BusinessException) é tratado pelo advice com redirect e flash de erro")
    void getEtapaInexistente() throws Exception {
        when(lancamentoService.buscarEtapaAtiva(10L, 99L))
                .thenThrow(new BusinessException(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND));

        mockMvc.perform(get("/dashboard/ordens/10/setor/99/lancar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard/ordens-setor"))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_ORDER_SECTOR_NOTFOUND)));
    }

    // ---------- Produção ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao válido registra, avisa sucesso e volta para a tela de lançamento")
    void producaoValida() throws Exception {
        when(lancamentoService.lancarProducao(eq(10L), eq(20L), any(LancamentoProducaoDTO.class)))
                .thenReturn(ordemSetor);

        mockMvc.perform(postProducao())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_LAUNCH_REGISTERED)));

        ArgumentCaptor<LancamentoProducaoDTO> dto = ArgumentCaptor.forClass(LancamentoProducaoDTO.class);
        verify(lancamentoService).lancarProducao(eq(10L), eq(20L), dto.capture());
        assertThat(dto.getValue()).isEqualTo(new LancamentoProducaoDTO(1L, 2L, 5, "ok"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao que conclui a etapa redireciona para a lista do setor")
    void producaoConcluiEtapa() throws Exception {
        when(lancamentoService.lancarProducao(eq(10L), eq(20L), any(LancamentoProducaoDTO.class)))
                .thenReturn(criarOrdemSetor(20L, EstatusOrdemProducao.CONCLUIDA));

        mockMvc.perform(postProducao())
                .andExpect(redirectedUrl("/dashboard/ordens-setor/5"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao com quantidade inválida reexibe a tela com erro e não chama o service")
    void producaoInvalida() throws Exception {
        mockMvc.perform(post(BASE + "/producao").with(csrf())
                        .param("maquinaId", "1").param("operadorId", "2").param("quantidade", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/Lancamento"))
                .andExpect(model().attribute("error", msg("validation.quantity.positive")))
                .andExpect(model().attributeHasFieldErrors("producaoDTO", "quantidade"))
                .andExpect(model().attributeExists("ordem", "maquinas", "operadores"));

        verify(lancamentoService, never()).lancarProducao(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao sem máquina informa que a máquina é obrigatória")
    void producaoSemMaquina() throws Exception {
        mockMvc.perform(post(BASE + "/producao").with(csrf())
                        .param("operadorId", "2").param("quantidade", "1"))
                .andExpect(model().attribute("error", msg("validation.machine.required")));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao com BusinessException devolve flash de erro e o DTO digitado")
    void producaoBusinessException() throws Exception {
        when(lancamentoService.lancarProducao(eq(10L), eq(20L), any(LancamentoProducaoDTO.class)))
                .thenThrow(new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING));

        mockMvc.perform(postProducao())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING)))
                .andExpect(flash().attribute("producaoDTO", new LancamentoProducaoDTO(1L, 2L, 5, "ok")));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /producao sem CSRF é rejeitado com 403")
    void producaoSemCsrf() throws Exception {
        mockMvc.perform(post(BASE + "/producao")
                        .param("maquinaId", "1").param("operadorId", "2").param("quantidade", "5"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lancamentoService);
    }

    // ---------- Refugo ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo válido registra e volta para a tela de lançamento")
    void refugoValido() throws Exception {
        when(lancamentoService.registrarRefugo(eq(10L), eq(20L), any(RefugoDTO.class))).thenReturn(ordemSetor);

        mockMvc.perform(postRefugo())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_REFUGO_REGISTERED)));

        ArgumentCaptor<RefugoDTO> dto = ArgumentCaptor.forClass(RefugoDTO.class);
        verify(lancamentoService).registrarRefugo(eq(10L), eq(20L), dto.capture());
        assertThat(dto.getValue()).isEqualTo(new RefugoDTO(2, 5L, 7L, 2L, DestinoRefugo.DESCARTE, "Quebra"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo que conclui a etapa redireciona para a lista do setor")
    void refugoConcluiEtapa() throws Exception {
        when(lancamentoService.registrarRefugo(eq(10L), eq(20L), any(RefugoDTO.class)))
                .thenReturn(criarOrdemSetor(20L, EstatusOrdemProducao.CONCLUIDA));

        mockMvc.perform(postRefugo()).andExpect(redirectedUrl("/dashboard/ordens-setor/5"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo sem motivo reexibe a tela com a mensagem de motivo obrigatório")
    void refugoSemMotivo() throws Exception {
        mockMvc.perform(post(BASE + "/refugo").with(csrf())
                        .param("quantidade", "2").param("setorOrigemId", "5").param("operadorId", "2")
                        .param("destino", "DESCARTE").param("motivo", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/Lancamento"))
                .andExpect(model().attribute("error", msg("validation.reason.required")));

        verify(lancamentoService, never()).registrarRefugo(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo com destino inválido reexibe a tela com erro")
    void refugoDestinoInvalido() throws Exception {
        mockMvc.perform(post(BASE + "/refugo").with(csrf())
                        .param("quantidade", "2").param("setorOrigemId", "5").param("operadorId", "2")
                        .param("destino", "XYZ").param("motivo", "Quebra"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/Lancamento"))
                .andExpect(model().attributeExists("error"));

        verify(lancamentoService, never()).registrarRefugo(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo com BusinessException devolve flash de erro e o DTO")
    void refugoBusinessException() throws Exception {
        when(lancamentoService.registrarRefugo(eq(10L), eq(20L), any(RefugoDTO.class)))
                .thenThrow(new BusinessException(AppMessages.ERROR_REFUGO_ORIGEM_INVALIDA));

        mockMvc.perform(postRefugo())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_REFUGO_ORIGEM_INVALIDA)))
                .andExpect(flash().attributeExists("refugoDTO"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /refugo sem CSRF é rejeitado com 403")
    void refugoSemCsrf() throws Exception {
        mockMvc.perform(post(BASE + "/refugo").param("quantidade", "2"))
                .andExpect(status().isForbidden());
    }

    // ---------- Retrabalho ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho válido registra e volta para a tela de lançamento")
    void retrabalhoValido() throws Exception {
        when(lancamentoService.registrarRetrabalho(eq(10L), any(RetrabalhoDTO.class))).thenReturn(ordemSetor);

        mockMvc.perform(postRetrabalho())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_RETRABALHO_REGISTERED)));

        ArgumentCaptor<RetrabalhoDTO> dto = ArgumentCaptor.forClass(RetrabalhoDTO.class);
        verify(lancamentoService).registrarRetrabalho(eq(10L), dto.capture());
        assertThat(dto.getValue()).isEqualTo(new RetrabalhoDTO(9L, 1L, 2L, 3, "obs"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho cuja origem é a etapa atual e está concluída vai para a lista do setor")
    void retrabalhoOrigemConcluida() throws Exception {
        when(lancamentoService.registrarRetrabalho(eq(10L), any(RetrabalhoDTO.class)))
                .thenReturn(criarOrdemSetor(20L, EstatusOrdemProducao.CONCLUIDA));

        mockMvc.perform(postRetrabalho()).andExpect(redirectedUrl("/dashboard/ordens-setor/5"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho cuja origem é outra etapa concluída permanece na tela de lançamento")
    void retrabalhoOrigemOutraEtapa() throws Exception {
        when(lancamentoService.registrarRetrabalho(eq(10L), any(RetrabalhoDTO.class)))
                .thenReturn(criarOrdemSetor(21L, EstatusOrdemProducao.CONCLUIDA));

        mockMvc.perform(postRetrabalho()).andExpect(redirectedUrl(PAGINA_LANCAMENTO));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho sem refugo vinculado reexibe a tela com erro")
    void retrabalhoSemRefugo() throws Exception {
        mockMvc.perform(post(BASE + "/retrabalho").with(csrf())
                        .param("operadorId", "2").param("quantidade", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("ordens/Lancamento"))
                .andExpect(model().attribute("error", msg("validation.refugo.required")));

        verify(lancamentoService, never()).registrarRetrabalho(any(), any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho com BusinessException devolve flash de erro e o DTO")
    void retrabalhoBusinessException() throws Exception {
        when(lancamentoService.registrarRetrabalho(eq(10L), any(RetrabalhoDTO.class)))
                .thenThrow(new BusinessException(AppMessages.ERROR_REFUGO_NOTFOUND));

        mockMvc.perform(postRetrabalho())
                .andExpect(redirectedUrl(PAGINA_LANCAMENTO))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_REFUGO_NOTFOUND)))
                .andExpect(flash().attribute("retrabalhoDTO", new RetrabalhoDTO(9L, 1L, 2L, 3, "obs")));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /retrabalho sem CSRF é rejeitado com 403")
    void retrabalhoSemCsrf() throws Exception {
        mockMvc.perform(post(BASE + "/retrabalho").param("refugoId", "9"))
                .andExpect(status().isForbidden());
    }

    // ---------- Autorização ----------

    @ParameterizedTest(name = "perfil {0} acessa o lançamento")
    @ValueSource(strings = {"OPERADOR", "PCP", "GESTAO", "SUPERVISOR", "ADMIN"})
    @DisplayName("todos os perfis da hierarquia (a partir de OPERADOR) acessam")
    void perfisPermitidos(String role) throws Exception {
        mockMvc.perform(get(PAGINA_LANCAMENTO).with(perfil(role)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("perfil fora da hierarquia (USER) recebe 403 em GET e POST")
    void perfilForaDaHierarquia() throws Exception {
        mockMvc.perform(get(PAGINA_LANCAMENTO)).andExpect(status().isForbidden());
        mockMvc.perform(postProducao()).andExpect(status().isForbidden());

        verifyNoInteractions(lancamentoService);
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get(PAGINA_LANCAMENTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(postProducao())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(lancamentoService);
    }
}
