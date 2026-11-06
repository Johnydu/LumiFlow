package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.HistoricoMovimentacaoDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.dto.vidracaria.SaldoVidroDTO;
import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.entity.enums.TipoVidro;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.VidracariaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.PT_BR;
import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

@WebMvcTest(controllers = VidracariaController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(VidracariaController.class)
@DisplayName("VidracariaController")
class VidracariaControllerTest {

    private static final String URL = "/dashboard/vidracaria";
    private static final String FRAGMENTO = "vidracaria/tabela-historico-movimentacao-vidro :: tabela";

    @Autowired
    MockMvc mockMvc;
    @Autowired
    MessageSource messageSource;
    @MockitoBean
    VidracariaService vidracariaService;
    @MockitoBean
    OperadorService operadorService;

    private HistoricoMovimentacaoDTO movimento;

    private String msg(String chave) {
        return messageSource.getMessage(chave, null, PT_BR);
    }

    @BeforeEach
    void stubsPadrao() {
        movimento = new HistoricoMovimentacaoDTO(1L, TipoMovimentacao.ENTRADA, TipoVidro.INCOLOR, "Float 8mm", 10,
                LocalDateTime.of(2026, 1, 15, 8, 0), "Maria", null);
        when(vidracariaService.listarHistorico(any(), any(), anyInt()))
                .thenReturn(new PageImpl<>(List.of(movimento), PageRequest.of(0, 10), 1));
    }

    private MockHttpServletRequestBuilder postMovimentacao() {
        return post(URL + "/movimentar").with(csrf())
                .param("chapaVidroId", "1").param("tipoMovimentacao", "ENTRADA").param("quantidade", "5")
                .param("operadorId", "2").param("observacao", "obs");
    }

    private MockHttpServletRequestBuilder postChapa() {
        return post(URL + "/cadastrar-chapa").with(csrf())
                .param("tipoVidro", "INCOLOR").param("descricao", "Float 8mm").param("estoqueMinimo", "10");
    }

    // ---------- GET página ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET carrega saldos, chapas, histórico paginado e operadores")
    void carregaPagina() throws Exception {
        List<SaldoVidroDTO> saldos = List.of(new SaldoVidroDTO(1L, TipoVidro.INCOLOR, "Float 8mm", 10, 5, false));
        ChapaVidro chapa = mock(ChapaVidro.class);
        List<OperadorListagemDTO> operadores = List.of(new OperadorListagemDTO(2L, "Ana", "Op", 1L, "Corte"));
        when(vidracariaService.obterSaldosAtuais()).thenReturn(saldos);
        when(vidracariaService.listarChapas()).thenReturn(List.of(chapa));
        when(operadorService.listarOperadores()).thenReturn(operadores);

        mockMvc.perform(get(URL).param("tipoMovimentacao", "ENTRADA").param("tipoVidroFiltro", "INCOLOR")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("vidracaria/Vidracaria"))
                .andExpect(model().attribute("saldos", saldos))
                .andExpect(model().attribute("chapas", List.of(chapa)))
                .andExpect(model().attribute("historico", List.of(movimento)))
                .andExpect(model().attributeExists("paginaHistorico"))
                .andExpect(model().attribute("operadores", operadores))
                .andExpect(model().attribute("tipoMovimentacaoAtual", "ENTRADA"))
                .andExpect(model().attribute("tipoVidroFiltroAtual", "INCOLOR"))
                .andExpect(model().attributeDoesNotExist("error"));

        verify(vidracariaService).listarHistorico("ENTRADA", "INCOLOR", 1);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET com falha nos services mostra erro e coleções vazias")
    void carregaPaginaComFalha() throws Exception {
        when(vidracariaService.obterSaldosAtuais()).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(view().name("vidracaria/Vidracaria"))
                .andExpect(model().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)))
                .andExpect(model().attribute("saldos", List.of()))
                .andExpect(model().attribute("historico", List.of()))
                .andExpect(model().attribute("operadores", List.of()));
    }

    // ---------- GET filtrar histórico (AJAX) ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /filtrar-historico devolve o fragmento da tabela com o histórico filtrado")
    void filtraHistorico() throws Exception {
        mockMvc.perform(get(URL + "/filtrar-historico").param("tipoMovimentacao", "SAIDA")
                        .param("tipoVidroFiltro", "JATEADO").param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name(FRAGMENTO))
                .andExpect(model().attribute("historico", List.of(movimento)));

        verify(vidracariaService).listarHistorico("SAIDA", "JATEADO", 2);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /filtrar-historico converte filtros em branco para nulo")
    void filtraHistoricoComFiltrosEmBranco() throws Exception {
        mockMvc.perform(get(URL + "/filtrar-historico").param("tipoMovimentacao", " ").param("tipoVidroFiltro", ""))
                .andExpect(status().isOk());

        verify(vidracariaService).listarHistorico(null, null, 0);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("GET /filtrar-historico com falha responde 500 com o fragmento vazio")
    void filtraHistoricoComFalha() throws Exception {
        when(vidracariaService.listarHistorico(any(), any(), anyInt())).thenThrow(new IllegalStateException("falha"));

        mockMvc.perform(get(URL + "/filtrar-historico"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name(FRAGMENTO))
                .andExpect(model().attribute("historico", List.of()));
    }

    // ---------- POST movimentar ----------

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar válido registra a movimentação e avisa sucesso")
    void registraMovimentacao() throws Exception {
        mockMvc.perform(postMovimentacao())
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_GLASS_MOVEMENT_CREATED)));

        verify(vidracariaService).registrarMovimentacao(
                new MovimentacaoVidroDTO(1L, TipoMovimentacao.ENTRADA, 5, 2L, null, "obs"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar com quantidade zero reexibe a página com erro")
    void movimentacaoQuantidadeZero() throws Exception {
        mockMvc.perform(post(URL + "/movimentar").with(csrf())
                        .param("chapaVidroId", "1").param("tipoMovimentacao", "SAIDA").param("quantidade", "0")
                        .param("operadorId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("vidracaria/Vidracaria"))
                .andExpect(model().attribute("error", msg("validation.quantity.positive")))
                .andExpect(model().attributeHasFieldErrors("movimentacaoDTO", "quantidade"));

        verify(vidracariaService, never()).registrarMovimentacao(any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar sem operador pede para selecionar um operador")
    void movimentacaoSemOperador() throws Exception {
        mockMvc.perform(post(URL + "/movimentar").with(csrf())
                        .param("chapaVidroId", "1").param("tipoMovimentacao", "SAIDA").param("quantidade", "1"))
                .andExpect(model().attribute("error", msg("validation.operator.select")));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar com tipo de movimentação inválido reexibe a página com erro")
    void movimentacaoTipoInvalido() throws Exception {
        mockMvc.perform(post(URL + "/movimentar").with(csrf())
                        .param("chapaVidroId", "1").param("tipoMovimentacao", "XYZ").param("quantidade", "1")
                        .param("operadorId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("vidracaria/Vidracaria"))
                .andExpect(model().attributeExists("error"));

        verify(vidracariaService, never()).registrarMovimentacao(any());
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar com BusinessException (estoque insuficiente) devolve flash de erro e o DTO")
    void movimentacaoComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_GLASS_INSUFFICIENT_STOCK))
                .when(vidracariaService).registrarMovimentacao(any(MovimentacaoVidroDTO.class));

        mockMvc.perform(postMovimentacao())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_GLASS_INSUFFICIENT_STOCK)))
                .andExpect(flash().attributeExists("movimentacaoDTO"));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar com erro inesperado devolve mensagem genérica")
    void movimentacaoComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha"))
                .when(vidracariaService).registrarMovimentacao(any(MovimentacaoVidroDTO.class));

        mockMvc.perform(postMovimentacao())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("POST /movimentar sem CSRF é rejeitado com 403")
    void movimentacaoSemCsrf() throws Exception {
        mockMvc.perform(post(URL + "/movimentar").param("quantidade", "1"))
                .andExpect(status().isForbidden());

        verify(vidracariaService, never()).registrarMovimentacao(any());
    }

    // ---------- POST cadastrar chapa ----------

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa válido cadastra e avisa sucesso")
    void cadastraChapa() throws Exception {
        mockMvc.perform(postChapa())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("success", msg(AppMessages.SUCCESS_GLASS_TYPE_CREATED)));

        verify(vidracariaService).cadastrarChapa(new ChapaVidroDTO(TipoVidro.INCOLOR, "Float 8mm", 10));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa com estoque mínimo negativo reexibe a página com erro")
    void chapaEstoqueNegativo() throws Exception {
        mockMvc.perform(post(URL + "/cadastrar-chapa").with(csrf())
                        .param("tipoVidro", "INCOLOR").param("descricao", "Float").param("estoqueMinimo", "-1"))
                .andExpect(status().isOk())
                .andExpect(view().name("vidracaria/Vidracaria"))
                .andExpect(model().attribute("error", msg("validation.stock.min.negative")));

        verify(vidracariaService, never()).cadastrarChapa(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa sem tipo de vidro pede para selecionar o tipo")
    void chapaSemTipo() throws Exception {
        mockMvc.perform(post(URL + "/cadastrar-chapa").with(csrf())
                        .param("descricao", "Float").param("estoqueMinimo", "1"))
                .andExpect(model().attribute("error", msg("validation.glass.type.required")));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa sem descrição reexibe a página com erro de validação")
    void chapaSemDescricao() throws Exception {
        mockMvc.perform(post(URL + "/cadastrar-chapa").with(csrf())
                        .param("tipoVidro", "INCOLOR").param("estoqueMinimo", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("chapaDTO", "descricao"));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa com tipo de vidro inexistente reexibe a página com erro")
    void chapaTipoInvalido() throws Exception {
        mockMvc.perform(post(URL + "/cadastrar-chapa").with(csrf())
                        .param("tipoVidro", "FOSCO").param("descricao", "Float").param("estoqueMinimo", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("error"));

        verify(vidracariaService, never()).cadastrarChapa(any());
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa com BusinessException (tipo duplicado) devolve flash de erro e o DTO")
    void chapaComBusinessException() throws Exception {
        doThrow(new BusinessException(AppMessages.ERROR_GLASS_TYPE_DUPLICATE))
                .when(vidracariaService).cadastrarChapa(any(ChapaVidroDTO.class));

        mockMvc.perform(postChapa())
                .andExpect(redirectedUrl(URL))
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_GLASS_TYPE_DUPLICATE)))
                .andExpect(flash().attribute("chapaDTO", new ChapaVidroDTO(TipoVidro.INCOLOR, "Float 8mm", 10)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa com erro inesperado devolve mensagem genérica")
    void chapaComErroInesperado() throws Exception {
        doThrow(new IllegalStateException("falha")).when(vidracariaService).cadastrarChapa(any(ChapaVidroDTO.class));

        mockMvc.perform(postChapa())
                .andExpect(flash().attribute("error", msg(AppMessages.ERROR_VALIDATION_FAILED)));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("POST /cadastrar-chapa sem CSRF é rejeitado com 403")
    void chapaSemCsrf() throws Exception {
        mockMvc.perform(post(URL + "/cadastrar-chapa").param("tipoVidro", "INCOLOR"))
                .andExpect(status().isForbidden());
    }

    // ---------- Autorização ----------

    @ParameterizedTest(name = "perfil {0} cadastra chapa")
    @ValueSource(strings = {"PCP", "GESTAO", "SUPERVISOR", "ADMIN"})
    @DisplayName("PCP, GESTAO, SUPERVISOR e ADMIN cadastram chapas")
    void perfisPodemCadastrarChapa(String role) throws Exception {
        mockMvc.perform(postChapa().with(perfil(role)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(URL));
    }

    @Test
    @DisplayName("OPERADOR não cadastra chapa (403 vindo da segurança de método) mas movimenta e consulta")
    void operadorNaoCadastraChapa() throws Exception {
        mockMvc.perform(postChapa().with(perfil("OPERADOR"))).andExpect(status().isForbidden());
        verify(vidracariaService, never()).cadastrarChapa(any());

        mockMvc.perform(get(URL).with(perfil("OPERADOR"))).andExpect(status().isOk());
        mockMvc.perform(postMovimentacao().with(perfil("OPERADOR")))
                .andExpect(redirectedUrl(URL));
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login em todas as rotas")
    void anonimo() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get(URL + "/filtrar-historico"))
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(postMovimentacao())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(postChapa())
                .andExpect(redirectedUrlPattern("**/login"));

        verify(vidracariaService, never()).registrarMovimentacao(any());
        verify(vidracariaService, never()).cadastrarChapa(any());
    }
}
