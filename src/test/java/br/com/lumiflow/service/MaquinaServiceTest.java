package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.MaquinaMapper;
import br.com.lumiflow.repository.MaquinaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MaquinaServiceTest {

    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private MaquinaMapper maquinaMapper;
    @Mock
    private SetorService setorService;

    @InjectMocks
    private MaquinaService service;

    private Maquina maquina(Long id, String nome) {
        Maquina m = new Maquina();
        m.setId(id);
        m.setNome(nome);
        return m;
    }

    private Setor setor(Long id) {
        Setor s = new Setor();
        s.setId(id);
        return s;
    }

    @Nested
    @DisplayName("listarMaquinas")
    class Listar {

        @Test
        @DisplayName("Lista ordenada por nome e mapeada para DTO")
        void listaMapeada() {
            List<Maquina> maquinas = List.of(maquina(1L, "A"));
            List<MaquinaDTO> dtos = List.of(new MaquinaDTO(1L, "A", "S", 1L));
            when(maquinaRepository.findAllByOrderByNomeAsc()).thenReturn(maquinas);
            when(maquinaMapper.toDtoList(maquinas)).thenReturn(dtos);

            assertThat(service.listarMaquinas()).isSameAs(dtos);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erroDeBanco() {
            when(maquinaRepository.findAllByOrderByNomeAsc()).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.listarMaquinas())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Erro inesperado vira falha de validação")
        void erroInesperado() {
            when(maquinaRepository.findAllByOrderByNomeAsc()).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.listarMaquinas())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("novaMaquina")
    class Nova {

        @Test
        @DisplayName("DTO nulo, nome nulo ou em branco são rejeitados sem acessar o banco")
        void entradasInvalidas() {
            for (MaquinaDTO dto : new MaquinaDTO[]{null, new MaquinaDTO(null, null, null, 1L),
                    new MaquinaDTO(null, "   ", null, 1L)}) {
                assertThatThrownBy(() -> service.novaMaquina(dto))
                        .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            }

            verifyNoInteractions(maquinaRepository, maquinaMapper, setorService);
        }

        @Test
        @DisplayName("Nome duplicado (após normalizar) é rejeitado e nada é salvo")
        void nomeDuplicado() {
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.of(maquina(1L, "SERRA")));

            assertThatThrownBy(() -> service.novaMaquina(new MaquinaDTO(null, "  serra ", null, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NAME_DUPLICATE);

            verify(maquinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Salva a máquina com nome normalizado e setor resolvido")
        void salvaComSetor() {
            MaquinaDTO dto = new MaquinaDTO(null, "  serra fita ", null, 4L);
            Setor setor = setor(4L);
            when(maquinaRepository.findByNome("SERRA FITA")).thenReturn(Optional.empty());
            when(maquinaMapper.toEntity(dto)).thenReturn(new Maquina());
            when(setorService.buscarSetorPorId(4L)).thenReturn(setor);

            service.novaMaquina(dto);

            ArgumentCaptor<Maquina> captor = ArgumentCaptor.forClass(Maquina.class);
            verify(maquinaRepository).save(captor.capture());
            assertThat(captor.getValue().getNome()).isEqualTo("SERRA FITA");
            assertThat(captor.getValue().getSetor()).isSameAs(setor);
        }

        @Test
        @DisplayName("Sem setorId não consulta o setor")
        void semSetor() {
            MaquinaDTO dto = new MaquinaDTO(null, "serra", null, null);
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.empty());
            when(maquinaMapper.toEntity(dto)).thenReturn(new Maquina());

            service.novaMaquina(dto);

            verifyNoInteractions(setorService);
            verify(maquinaRepository).save(any(Maquina.class));
        }

        @Test
        @DisplayName("Setor inexistente propaga BusinessException sem salvar")
        void setorInexistente() {
            MaquinaDTO dto = new MaquinaDTO(null, "serra", null, 99L);
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.empty());
            when(maquinaMapper.toEntity(dto)).thenReturn(new Maquina());
            when(setorService.buscarSetorPorId(99L)).thenThrow(new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND));

            assertThatThrownBy(() -> service.novaMaquina(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);

            verify(maquinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Erro de banco ao salvar vira falha de validação")
        void erroDeBanco() {
            MaquinaDTO dto = new MaquinaDTO(null, "serra", null, null);
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.empty());
            when(maquinaMapper.toEntity(dto)).thenReturn(new Maquina());
            when(maquinaRepository.save(any(Maquina.class))).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.novaMaquina(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Erro inesperado vira falha de validação")
        void erroInesperado() {
            when(maquinaRepository.findByNome("SERRA")).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.novaMaquina(new MaquinaDTO(null, "serra", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("deletarMaquina")
    class Deletar {

        @Test
        @DisplayName("Id nulo resulta em máquina não encontrada")
        void idNulo() {
            assertThatThrownBy(() -> service.deletarMaquina(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);

            verifyNoInteractions(maquinaRepository);
        }

        @Test
        @DisplayName("Máquina inexistente resulta em não encontrada")
        void inexistente() {
            when(maquinaRepository.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletarMaquina(5L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);

            verify(maquinaRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Exclui e faz flush (nessa ordem) para detectar vínculos")
        void excluiComFlush() {
            Maquina m = maquina(5L, "SERRA");
            when(maquinaRepository.findById(5L)).thenReturn(Optional.of(m));

            service.deletarMaquina(5L);

            InOrder ordem = Mockito.inOrder(maquinaRepository);
            ordem.verify(maquinaRepository).delete(m);
            ordem.verify(maquinaRepository).flush();
        }

        @Test
        @DisplayName("Violação de integridade vira registro em uso")
        void emUso() {
            Maquina m = maquina(5L, "SERRA");
            when(maquinaRepository.findById(5L)).thenReturn(Optional.of(m));
            Mockito.doThrow(new DataIntegrityViolationException("fk")).when(maquinaRepository).flush();

            assertThatThrownBy(() -> service.deletarMaquina(5L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_RECORD_IN_USE);
        }

        @Test
        @DisplayName("Outros erros de banco viram falha de validação")
        void erroDeBanco() {
            Maquina m = maquina(5L, "SERRA");
            when(maquinaRepository.findById(5L)).thenReturn(Optional.of(m));
            Mockito.doThrow(new DataRetrievalFailureException("x")).when(maquinaRepository).delete(m);

            assertThatThrownBy(() -> service.deletarMaquina(5L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("editarMaquina")
    class Editar {

        @Test
        @DisplayName("Id ou DTO nulos são rejeitados")
        void entradasNulas() {
            assertThatThrownBy(() -> service.editarMaquina(null, new MaquinaDTO(null, "A", null, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.editarMaquina(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(maquinaRepository);
        }

        @Test
        @DisplayName("Máquina inexistente é rejeitada")
        void inexistente() {
            when(maquinaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.editarMaquina(1L, new MaquinaDTO(null, "A", null, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);
        }

        @Test
        @DisplayName("Nome já usado por OUTRA máquina é rejeitado")
        void nomeDeOutraMaquina() {
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina(1L, "ANTIGA")));
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.of(maquina(2L, "SERRA")));

            assertThatThrownBy(() -> service.editarMaquina(1L, new MaquinaDTO(null, "serra", null, 1L)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NAME_DUPLICATE);

            verify(maquinaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Manter o nome da própria máquina não é duplicidade; atualiza o setor e salva")
        void mantemPropriaMaquinaEAtualizaSetor() {
            Maquina existente = maquina(1L, "SERRA");
            Setor novoSetor = setor(8L);
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.of(existente));
            when(setorService.buscarSetorPorId(8L)).thenReturn(novoSetor);

            service.editarMaquina(1L, new MaquinaDTO(1L, "serra", null, 8L));

            assertThat(existente.getSetor()).isSameAs(novoSetor);
            verify(maquinaRepository).save(existente);
        }

        @Test
        @DisplayName("Sem setorId o setor atual é preservado")
        void semSetorPreservaAtual() {
            Maquina existente = maquina(1L, "SERRA");
            Setor atual = setor(3L);
            existente.setSetor(atual);
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.empty());

            service.editarMaquina(1L, new MaquinaDTO(1L, "serra", null, null));

            assertThat(existente.getSetor()).isSameAs(atual);
            verifyNoInteractions(setorService);
            verify(maquinaRepository).save(existente);
        }

        @Test
        @DisplayName("Grava o novo nome normalizado (trim + maiúsculas)")
        void gravaNovoNomeNormalizado() {
            Maquina existente = maquina(1L, "ANTIGA");
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(maquinaRepository.findByNome("SERRA NOVA")).thenReturn(Optional.empty());

            service.editarMaquina(1L, new MaquinaDTO(1L, "  serra nova ", null, null));

            assertThat(existente.getNome()).isEqualTo("SERRA NOVA");
            verify(maquinaRepository).save(existente);
        }

        @Test
        @DisplayName("Nome em branco mantém o nome atual")
        void nomeEmBrancoMantemAtual() {
            Maquina existente = maquina(1L, "SERRA");
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(existente));

            service.editarMaquina(1L, new MaquinaDTO(1L, "   ", null, null));

            assertThat(existente.getNome()).isEqualTo("SERRA");
            verify(maquinaRepository, never()).findByNome(any());
        }

        @Test
        @DisplayName("Erro de banco ao salvar vira falha de validação")
        void erroDeBanco() {
            Maquina existente = maquina(1L, "SERRA");
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(maquinaRepository.findByNome("SERRA")).thenReturn(Optional.empty());
            when(maquinaRepository.save(existente)).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.editarMaquina(1L, new MaquinaDTO(1L, "serra", null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("buscarPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Retorna a máquina encontrada")
        void encontrada() {
            Maquina m = maquina(1L, "SERRA");
            when(maquinaRepository.findById(1L)).thenReturn(Optional.of(m));

            assertThat(service.buscarPorId(1L)).isSameAs(m);
        }

        @Test
        @DisplayName("Id nulo ou inexistente resultam em máquina não encontrada")
        void naoEncontrada() {
            when(maquinaRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);
            assertThatThrownBy(() -> service.buscarPorId(2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);
        }

        @Test
        @DisplayName("Falhas de banco ou inesperadas também resultam em máquina não encontrada")
        void falhasViramNaoEncontrada() {
            when(maquinaRepository.findById(3L)).thenThrow(new DataRetrievalFailureException("x"));
            when(maquinaRepository.findById(4L)).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.buscarPorId(3L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);
            assertThatThrownBy(() -> service.buscarPorId(4L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_MACHINE_NOTFOUND);
        }
    }
}
