package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.NivelAcessoMapper;
import br.com.lumiflow.repository.NivelAcessoRepository;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NivelAcessoServiceTest {

    @Mock
    private NivelAcessoRepository nivelAcessoRepository;
    @Mock
    private NivelAcessoMapper nivelAcessoMapper;

    @InjectMocks
    private NivelAcessoService service;

    @Test
    @DisplayName("buscarNivelAcessoPorId retorna o nível encontrado")
    void buscaEncontrada() {
        NivelAcesso nivel = new NivelAcesso(1L, Descricao.ADMIN);
        when(nivelAcessoRepository.findById(1L)).thenReturn(Optional.of(nivel));

        assertThat(service.buscarNivelAcessoPorId(1L)).isSameAs(nivel);
    }

    @Test
    @DisplayName("buscarNivelAcessoPorId com id nulo lança nível não encontrado sem ir ao banco")
    void buscaIdNulo() {
        assertThatThrownBy(() -> service.buscarNivelAcessoPorId(null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND);

        verifyNoInteractions(nivelAcessoRepository);
    }

    @Test
    @DisplayName("buscarNivelAcessoPorId lança nível não encontrado quando o id não existe")
    void buscaInexistente() {
        when(nivelAcessoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarNivelAcessoPorId(9L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND);
    }

    @Test
    @DisplayName("buscarNivelAcessoPorId converte erro de banco em falha de validação")
    void buscaErroDeBanco() {
        when(nivelAcessoRepository.findById(1L)).thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.buscarNivelAcessoPorId(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("buscarNivelAcessoPorId converte erro inesperado em falha de validação")
    void buscaErroInesperado() {
        when(nivelAcessoRepository.findById(1L)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.buscarNivelAcessoPorId(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("listarTodos mapeia todos os níveis de acesso")
    void listarTodos() {
        List<NivelAcesso> niveis = List.of(new NivelAcesso(1L, Descricao.ADMIN));
        List<NivelAcessoDTO> dtos = List.of(new NivelAcessoDTO(1L, "ADMIN"));
        when(nivelAcessoRepository.findAll()).thenReturn(niveis);
        when(nivelAcessoMapper.toListDTO(niveis)).thenReturn(dtos);

        assertThat(service.listarTodos()).isSameAs(dtos);
    }

    @Test
    @DisplayName("listarTodos converte erro de banco em falha de validação")
    void listarTodosErroDeBanco() {
        when(nivelAcessoRepository.findAll()).thenThrow(new DataRetrievalFailureException("falha"));

        assertThatThrownBy(() -> service.listarTodos())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }

    @Test
    @DisplayName("listarTodos converte erro inesperado em falha de validação")
    void listarTodosErroInesperado() {
        when(nivelAcessoRepository.findAll()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> service.listarTodos())
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
    }
}
