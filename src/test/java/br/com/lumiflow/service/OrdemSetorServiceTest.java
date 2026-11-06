package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OperadorRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrdemSetorServiceTest {

    @Mock
    private OrdemSetorRepository ordemSetorRepository;
    @Mock
    private OperadorRepository operadorRepository;

    @InjectMocks
    private OrdemSetorService service;

    private Operador operador(String nome, Long setorId) {
        Operador o = new Operador();
        o.setNome(nome);
        if (setorId != null) {
            Setor s = new Setor();
            s.setId(setorId);
            o.setSetorPadrao(s);
        }
        return o;
    }

    private List<Object[]> linhas(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @Test
    @DisplayName("Monta o resumo com operadores do setor e contagens de ordens")
    void montaResumoComOperadores() {
        when(ordemSetorRepository.buscarResumoSetores(null))
                .thenReturn(linhas(new Object[]{1L, "CORTE", 4L, 1L}, new Object[]{2L, "DOBRA", 0L, 0L}));
        when(operadorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(
                operador("ANA", 1L), operador("BRUNO", 1L), operador("CARLA", null), operador("DIEGO", 3L)));

        List<ResumoSetorDTO> resumo = service.obterResumoSetores(null);

        assertThat(resumo).containsExactly(
                new ResumoSetorDTO(1L, "CORTE", "ANA, BRUNO", 4L, 1L),
                new ResumoSetorDTO(2L, "DOBRA", "", 0L, 0L));
        assertThat(resumo.get(0).percentualConcluido()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Aplica trim na busca e envia nulo quando a busca é nula ou em branco")
    void normalizaTermoDeBusca() {
        when(ordemSetorRepository.buscarResumoSetores("corte")).thenReturn(List.of());
        when(ordemSetorRepository.buscarResumoSetores(null)).thenReturn(List.of());

        service.obterResumoSetores("  corte ");
        service.obterResumoSetores("   ");
        service.obterResumoSetores(null);

        verify(ordemSetorRepository).buscarResumoSetores("corte");
        verify(ordemSetorRepository, org.mockito.Mockito.times(2)).buscarResumoSetores(null);
    }

    @Test
    @DisplayName("Converte contagens nulas em zero e ignora linhas sem id de setor")
    void tratamentoDeNulosNasLinhas() {
        when(ordemSetorRepository.buscarResumoSetores(null)).thenReturn(linhas(
                new Object[]{null, "FANTASMA", 1L, 1L},
                new Object[]{5, "SOLDA", null, null}));

        List<ResumoSetorDTO> resumo = service.obterResumoSetores(null);

        assertThat(resumo).containsExactly(new ResumoSetorDTO(5L, "SOLDA", "", 0L, 0L));
    }

    @Test
    @DisplayName("Resultado nulo do repositório gera lista vazia")
    void resultadoNulo() {
        when(ordemSetorRepository.buscarResumoSetores(null)).thenReturn(null);

        assertThat(service.obterResumoSetores(null)).isEmpty();
    }

    @Test
    @DisplayName("Erro de banco vira BusinessException de validação")
    void erroDeBanco() {
        when(ordemSetorRepository.buscarResumoSetores(null)).thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.obterResumoSetores(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("Erro inesperado (ex.: tipo inesperado na linha) vira BusinessException de validação")
    void erroInesperado() {
        when(ordemSetorRepository.buscarResumoSetores(null))
                .thenReturn(linhas(new Object[]{1L, 123, 1L, 1L}));

        assertThatThrownBy(() -> service.obterResumoSetores(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }
}
