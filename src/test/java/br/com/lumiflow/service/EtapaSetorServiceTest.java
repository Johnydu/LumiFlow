package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.EtapaSetorMapper;
import br.com.lumiflow.repository.EtapaSetorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtapaSetorServiceTest {

    @Mock
    private EtapaSetorRepository etapaSetorRepository;
    @Mock
    private EtapaSetorMapper etapaSetorMapper;

    @InjectMocks
    private EtapaSetorService service;

    @Test
    @DisplayName("buscarPorId retorna nulo para id nulo sem consultar o banco")
    void buscarPorIdNulo() {
        assertThat(service.buscarPorId(null)).isNull();

        verifyNoInteractions(etapaSetorRepository);
    }

    @Test
    @DisplayName("buscarPorId retorna a etapa encontrada")
    void buscarPorIdEncontrada() {
        EtapaSetor etapa = new EtapaSetor();
        etapa.setId(3L);
        when(etapaSetorRepository.findById(3L)).thenReturn(Optional.of(etapa));

        assertThat(service.buscarPorId(3L)).isSameAs(etapa);
    }

    @Test
    @DisplayName("buscarPorId retorna nulo quando a etapa não existe")
    void buscarPorIdInexistente() {
        when(etapaSetorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.buscarPorId(99L)).isNull();
    }

    @Test
    @DisplayName("buscarPorId converte erro de banco em BusinessException de validação")
    void buscarPorIdErroDeBanco() {
        when(etapaSetorRepository.findById(3L)).thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.buscarPorId(3L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("buscarPorId converte erro inesperado em BusinessException de validação")
    void buscarPorIdErroInesperado() {
        when(etapaSetorRepository.findById(3L)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.buscarPorId(3L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("listarPorSetorId retorna lista vazia para setor nulo sem consultar o banco")
    void listarSetorNulo() {
        assertThat(service.listarPorSetorId(null)).isEmpty();

        verifyNoInteractions(etapaSetorRepository, etapaSetorMapper);
    }

    @Test
    @DisplayName("listarPorSetorId delega ao repositório ordenado e mapeia para DTO")
    void listarMapeiaResultado() {
        List<EtapaSetor> etapas = List.of(new EtapaSetor());
        List<EtapaSetorDTO> dtos = List.of(new EtapaSetorDTO(1L, "CORTE", 1));
        when(etapaSetorRepository.findBySetorIdOrderByOrdemAsc(5L)).thenReturn(etapas);
        when(etapaSetorMapper.toListDTO(etapas)).thenReturn(dtos);

        assertThat(service.listarPorSetorId(5L)).isSameAs(dtos);

        verify(etapaSetorRepository).findBySetorIdOrderByOrdemAsc(5L);
    }

    @Test
    @DisplayName("listarPorSetorId converte erro de banco em BusinessException de validação")
    void listarErroDeBanco() {
        when(etapaSetorRepository.findBySetorIdOrderByOrdemAsc(5L)).thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.listarPorSetorId(5L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("listarPorSetorId converte erro inesperado em BusinessException de validação")
    void listarErroInesperado() {
        when(etapaSetorRepository.findBySetorIdOrderByOrdemAsc(5L)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.listarPorSetorId(5L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }
}
