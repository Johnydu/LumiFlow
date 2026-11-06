package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.OperadorMapper;
import br.com.lumiflow.repository.OperadorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperadorServiceTest {

    @Mock
    private OperadorRepository operadorRepository;
    @Mock
    private OperadorMapper operadorMapper;
    @Mock
    private SetorService setorService;

    @InjectMocks
    private OperadorService service;

    private Operador operador(Long id, String nome) {
        Operador o = new Operador();
        o.setId(id);
        o.setNome(nome);
        return o;
    }

    @Nested
    @DisplayName("validações de nome")
    class ValidacaoDeNome {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("validarNomeOperador rejeita nome nulo, vazio ou em branco")
        void nomeInvalido(String nome) {
            assertThatThrownBy(() -> service.validarNomeOperador(nome))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(operadorRepository);
        }

        @Test
        @DisplayName("validarNomeOperador consulta duplicidade pelo nome normalizado")
        void nomeDuplicado() {
            when(operadorRepository.existsByNome("JOÃO DA SILVA")).thenReturn(true);

            assertThatThrownBy(() -> service.validarNomeOperador("  joão da silva "))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("validarNomeOperador aceita nome inédito")
        void nomeInedito() {
            when(operadorRepository.existsByNome("ANA")).thenReturn(false);

            service.validarNomeOperador("ana");

            verify(operadorRepository).existsByNome("ANA");
        }

        @Test
        @DisplayName("validarNomeOperadorParaEdicao ignora o próprio registro na checagem")
        void edicaoUsaExistsAndIdNot() {
            when(operadorRepository.existsByNomeAndIdNot("ANA", 5L)).thenReturn(true);

            assertThatThrownBy(() -> service.validarNomeOperadorParaEdicao(" ana ", 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);
        }

        @Test
        @DisplayName("validarNomeOperadorParaEdicao não valida quando nome ou id estão ausentes")
        void edicaoSemDados() {
            service.validarNomeOperadorParaEdicao(null, 5L);
            service.validarNomeOperadorParaEdicao("  ", 5L);
            service.validarNomeOperadorParaEdicao("ana", null);

            verifyNoInteractions(operadorRepository);
        }
    }

    @Nested
    @DisplayName("buscarOperadorPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Retorna o operador encontrado")
        void encontrado() {
            Operador o = operador(1L, "ANA");
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(o));

            assertThat(service.buscarOperadorPorId(1L)).isSameAs(o);
        }

        @Test
        @DisplayName("Id nulo ou inexistente resulta em operador não encontrado")
        void naoEncontrado() {
            when(operadorRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarOperadorPorId(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NOTFOUND);
            assertThatThrownBy(() -> service.buscarOperadorPorId(2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NOTFOUND);
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            when(operadorRepository.findById(3L)).thenThrow(new DataRetrievalFailureException("x"));
            when(operadorRepository.findById(4L)).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.buscarOperadorPorId(3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.buscarOperadorPorId(4L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("listarOperadores")
    class Listar {

        @Test
        @DisplayName("Lista ordenada por nome e mapeada")
        void lista() {
            List<Operador> operadores = List.of(operador(1L, "ANA"));
            List<OperadorListagemDTO> dtos = List.of(new OperadorListagemDTO(1L, "ANA", null, null, null));
            when(operadorRepository.findAllByOrderByNomeAsc()).thenReturn(operadores);
            when(operadorMapper.toListDto(operadores)).thenReturn(dtos);

            assertThat(service.listarOperadores()).isSameAs(dtos);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erro() {
            when(operadorRepository.findAllByOrderByNomeAsc()).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.listarOperadores())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("novoOperador")
    class Novo {

        @Test
        @DisplayName("DTO nulo ou nome em branco são rejeitados sem salvar")
        void entradasInvalidas() {
            assertThatThrownBy(() -> service.novoOperador(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.novoOperador(new OperadorDTO(null, " ", "x", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verify(operadorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nome duplicado é rejeitado")
        void duplicado() {
            when(operadorRepository.existsByNome("ANA")).thenReturn(true);

            assertThatThrownBy(() -> service.novoOperador(new OperadorDTO(null, "ana", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);

            verify(operadorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Salva com nome normalizado e setor padrão resolvido")
        void salvaComSetor() {
            OperadorDTO dto = new OperadorDTO(null, " ana maria ", "cortadora", 3L);
            Setor setor = new Setor();
            setor.setId(3L);
            when(operadorRepository.existsByNome("ANA MARIA")).thenReturn(false);
            when(operadorMapper.toEntity(dto)).thenReturn(new Operador());
            when(setorService.buscarSetorPorId(3L)).thenReturn(setor);

            service.novoOperador(dto);

            ArgumentCaptor<Operador> captor = ArgumentCaptor.forClass(Operador.class);
            verify(operadorRepository).save(captor.capture());
            assertThat(captor.getValue().getNome()).isEqualTo("ANA MARIA");
            assertThat(captor.getValue().getSetorPadrao()).isSameAs(setor);
        }

        @Test
        @DisplayName("Sem setor padrão não consulta o SetorService")
        void semSetor() {
            OperadorDTO dto = new OperadorDTO(null, "ana", null, null);
            when(operadorMapper.toEntity(dto)).thenReturn(new Operador());

            service.novoOperador(dto);

            verifyNoInteractions(setorService);
            verify(operadorRepository).save(any(Operador.class));
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            OperadorDTO dto = new OperadorDTO(null, "ana", null, null);
            when(operadorMapper.toEntity(dto)).thenReturn(new Operador());
            when(operadorRepository.save(any(Operador.class)))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.novoOperador(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.novoOperador(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("excluirOperador")
    class Excluir {

        @Test
        @DisplayName("Id nulo resulta em operador não encontrado")
        void idNulo() {
            assertThatThrownBy(() -> service.excluirOperador(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NOTFOUND);

            verifyNoInteractions(operadorRepository);
        }

        @Test
        @DisplayName("Exclui e faz flush (nessa ordem)")
        void exclui() {
            Operador o = operador(1L, "ANA");
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(o));

            service.excluirOperador(1L);

            InOrder ordem = Mockito.inOrder(operadorRepository);
            ordem.verify(operadorRepository).delete(o);
            ordem.verify(operadorRepository).flush();
        }

        @Test
        @DisplayName("Operador com lançamentos (violação de integridade) vira registro em uso")
        void emUso() {
            Operador o = operador(1L, "ANA");
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(o));
            Mockito.doThrow(new DataIntegrityViolationException("fk")).when(operadorRepository).flush();

            assertThatThrownBy(() -> service.excluirOperador(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_RECORD_IN_USE);
        }

        @Test
        @DisplayName("Operador inexistente propaga não encontrado")
        void inexistente() {
            when(operadorRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluirOperador(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NOTFOUND);

            verify(operadorRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("editarOperador")
    class Editar {

        @Test
        @DisplayName("Id ou DTO nulos são rejeitados")
        void nulos() {
            assertThatThrownBy(() -> service.editarOperador(null, new OperadorDTO(null, "a", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.editarOperador(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Atualiza nome (normalizado), função (trim) e setor padrão")
        void atualizaCampos() {
            Operador existente = operador(1L, "ANTIGO");
            Setor setor = new Setor();
            setor.setId(4L);
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(operadorRepository.existsByNomeAndIdNot("NOVO NOME", 1L)).thenReturn(false);
            when(setorService.buscarSetorPorId(4L)).thenReturn(setor);

            service.editarOperador(1L, new OperadorDTO(1L, " novo nome ", "  Dobrador ", 4L));

            assertThat(existente.getNome()).isEqualTo("NOVO NOME");
            assertThat(existente.getFuncao()).isEqualTo("DOBRADOR");
            assertThat(existente.getSetorPadrao()).isSameAs(setor);
            verify(operadorRepository).save(existente);
        }

        @Test
        @DisplayName("Função em branco vira nulo, setor ausente remove o setor padrão e nome em branco mantém o atual")
        void camposVazios() {
            Operador existente = operador(1L, "ANA");
            existente.setFuncao("ANTIGA");
            existente.setSetorPadrao(new Setor());
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.editarOperador(1L, new OperadorDTO(1L, "  ", "   ", null));

            assertThat(existente.getNome()).isEqualTo("ANA");
            assertThat(existente.getFuncao()).isNull();
            assertThat(existente.getSetorPadrao()).isNull();
            verify(operadorRepository).save(existente);
        }

        @Test
        @DisplayName("Nome usado por outro operador é rejeitado sem salvar")
        void nomeDuplicado() {
            when(operadorRepository.findById(1L)).thenReturn(Optional.of(operador(1L, "ANA")));
            when(operadorRepository.existsByNomeAndIdNot("BRUNO", 1L)).thenReturn(true);

            assertThatThrownBy(() -> service.editarOperador(1L, new OperadorDTO(1L, "bruno", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);

            verify(operadorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Operador inexistente é rejeitado")
        void inexistente() {
            when(operadorRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.editarOperador(1L, new OperadorDTO(1L, "ana", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_OPERATOR_NOTFOUND);
        }
    }

    @Nested
    @DisplayName("buscarOperadores")
    class Buscar {

        @Test
        @DisplayName("Sem nome e sem setor retorna a listagem completa")
        void semFiltros() {
            List<Operador> todos = List.of(operador(1L, "ANA"));
            List<OperadorListagemDTO> dtos = List.of(new OperadorListagemDTO(1L, "ANA", null, null, null));
            when(operadorRepository.findAllByOrderByNomeAsc()).thenReturn(todos);
            when(operadorMapper.toListDto(todos)).thenReturn(dtos);

            assertThat(service.buscarOperadores("   ", null)).isSameAs(dtos);

            verify(operadorRepository, never()).buscarPorNomeESetor(any(), any());
        }

        @Test
        @DisplayName("Com filtros aplica trim no termo e delega à consulta por nome e setor")
        void comFiltros() {
            List<Operador> encontrados = List.of(operador(2L, "ANA"));
            List<OperadorListagemDTO> dtos = List.of(new OperadorListagemDTO(2L, "ANA", null, null, null));
            when(operadorRepository.buscarPorNomeESetor("an", 7L)).thenReturn(encontrados);
            when(operadorMapper.toListDto(encontrados)).thenReturn(dtos);

            assertThat(service.buscarOperadores("  an ", 7L)).isSameAs(dtos);
        }

        @Test
        @DisplayName("Somente setor informado envia termo nulo")
        void somenteSetor() {
            when(operadorRepository.buscarPorNomeESetor(null, 7L)).thenReturn(List.of());
            when(operadorMapper.toListDto(List.of())).thenReturn(List.of());

            assertThat(service.buscarOperadores(null, 7L)).isEmpty();

            verify(operadorRepository).buscarPorNomeESetor(null, 7L);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erro() {
            when(operadorRepository.buscarPorNomeESetor("an", null)).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.buscarOperadores("an", null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
