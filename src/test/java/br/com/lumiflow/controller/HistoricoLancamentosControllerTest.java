package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentoFiltroDTO;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentosDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.service.EtapaSetorService;
import br.com.lumiflow.service.HistoricoLancamentosService;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.lumiflow.controller.ControllerTestSupport.perfil;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = HistoricoLancamentosController.class)
@ContextConfiguration(classes = ControllerSliceConfig.class)
@Import(HistoricoLancamentosController.class)
@DisplayName("HistoricoLancamentosController")
class HistoricoLancamentosControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    HistoricoLancamentosService historicoLancamentosService;
    @MockitoBean
    SetorService setorService;
    @MockitoBean
    OperadorService operadorService;
    @MockitoBean
    EtapaSetorService etapaSetorService;

    @BeforeEach
    void stubsPadrao() {
        Page<HistoricoLancamentosDTO> vazia = new PageImpl<>(List.of());
        when(historicoLancamentosService.listarComFiltros(any(HistoricoLancamentoFiltroDTO.class), any(Pageable.class)))
                .thenReturn(vazia);
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("perfil superior usa todos os filtros da tela e carrega setores, operadores e etapas")
    void superiorUsaTodosOsFiltros() throws Exception {
        List<SetorListagemDTO> setores = List.of(new SetorListagemDTO(3L, "Corte", 2, List.of()));
        List<OperadorListagemDTO> operadores = List.of(new OperadorListagemDTO(5L, "Ana", "Op", 3L, "Corte"));
        List<EtapaSetorDTO> etapas = List.of(new EtapaSetorDTO(4L, "Corte reto", 1));
        when(setorService.listarSetores()).thenReturn(setores);
        when(operadorService.listarOperadores()).thenReturn(operadores);
        when(etapaSetorService.listarPorSetorId(3L)).thenReturn(etapas);

        mockMvc.perform(get("/dashboard/historico")
                        .param("setorId", "3").param("etapaSetorId", "4").param("operadorId", "5")
                        .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("historico/HistoricoLancamento"))
                .andExpect(model().attribute("isSuperior", true))
                .andExpect(model().attribute("setores", setores))
                .andExpect(model().attribute("operadores", operadores))
                .andExpect(model().attribute("etapas", etapas))
                .andExpect(model().attribute("filtro", new HistoricoLancamentoFiltroDTO(3L, 4L, 5L)))
                .andExpect(model().attributeExists("pagina"));

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(historicoLancamentosService).listarComFiltros(
                org.mockito.ArgumentMatchers.eq(new HistoricoLancamentoFiltroDTO(3L, 4L, 5L)), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    @WithMockUser(roles = "GESTAO")
    @DisplayName("sem setor selecionado a lista de etapas fica vazia e o service de etapas não é chamado")
    void semSetorSemEtapas() throws Exception {
        mockMvc.perform(get("/dashboard/historico"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("etapas", List.of()))
                .andExpect(model().attribute("filtro", new HistoricoLancamentoFiltroDTO(null, null, null)));

        verifyNoInteractions(etapaSetorService);
    }

    @Test
    @WithMockUser(roles = "OPERADOR")
    @DisplayName("operador: ignora operadorId, não carrega setores/operadores e mantém o setor informado")
    void operadorRestringeFiltros() throws Exception {
        List<EtapaSetorDTO> etapas = List.of(new EtapaSetorDTO(4L, "Corte reto", 1));
        when(etapaSetorService.listarPorSetorId(3L)).thenReturn(etapas);

        mockMvc.perform(get("/dashboard/historico").param("setorId", "3").param("operadorId", "5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isSuperior", false))
                .andExpect(model().attribute("setores", List.of()))
                .andExpect(model().attribute("operadores", List.of()))
                .andExpect(model().attribute("etapas", etapas))
                .andExpect(model().attribute("filtro", new HistoricoLancamentoFiltroDTO(3L, null, null)));

        verifyNoInteractions(setorService, operadorService);
        verify(historicoLancamentosService).listarComFiltros(
                org.mockito.ArgumentMatchers.eq(new HistoricoLancamentoFiltroDTO(3L, null, null)), any(Pageable.class));
    }

    @Test
    @WithMockUser(roles = "PCP")
    @DisplayName("página negativa é normalizada para 0")
    void paginaNegativa() throws Exception {
        mockMvc.perform(get("/dashboard/historico").param("page", "-5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(historicoLancamentosService).listarComFiltros(any(HistoricoLancamentoFiltroDTO.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @ParameterizedTest(name = "perfil {0} é tratado como superior")
    @ValueSource(strings = {"ADMIN", "SUPERVISOR", "PCP", "GESTAO"})
    @DisplayName("perfis gerenciais são superiores")
    void perfisSuperiores(String role) throws Exception {
        mockMvc.perform(get("/dashboard/historico").with(perfil(role)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("isSuperior", true));
    }

    @Test
    @DisplayName("anônimo é redirecionado para o login")
    void anonimo() throws Exception {
        mockMvc.perform(get("/dashboard/historico"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        verifyNoInteractions(historicoLancamentosService);
    }
}
