package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.SetorMapper;
import br.com.lumiflow.repository.EtapaSetorRepository;
import br.com.lumiflow.repository.LancamentoRepository;
import br.com.lumiflow.repository.MaquinaRepository;
import br.com.lumiflow.repository.RoteiroPassoRepository;
import br.com.lumiflow.repository.SetorRepository;
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

import java.util.ArrayList;
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
class SetorServiceTest {

    @Mock
    private SetorRepository setorRepository;
    @Mock
    private EtapaSetorRepository etapaSetorRepository;
    @Mock
    private SetorMapper setorMapper;
    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private RoteiroPassoRepository roteiroPassoRepository;
    @Mock
    private LancamentoRepository lancamentoRepository;

    @InjectMocks
    private SetorService service;

    private Setor setor(Long id, String nome) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome(nome);
        return s;
    }

    private EtapaSetor etapa(Long id, String nome, int ordem, Setor setor) {
        EtapaSetor e = new EtapaSetor();
        e.setId(id);
        e.setNome(nome);
        e.setOrdem(ordem);
        e.setSetor(setor);
        return e;
    }

    @Nested
    @DisplayName("buscarSetorPorId")
    class BuscarPorId {

        @Test
        @DisplayName("Retorna o setor encontrado")
        void encontrado() {
            Setor s = setor(1L, "CORTE");
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));

            assertThat(service.buscarSetorPorId(1L)).isSameAs(s);
        }

        @Test
        @DisplayName("Id nulo ou inexistente resulta em setor não encontrado")
        void naoEncontrado() {
            when(setorRepository.findById(2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarSetorPorId(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);
            assertThatThrownBy(() -> service.buscarSetorPorId(2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);
        }
    }

    @Nested
    @DisplayName("listagem e busca")
    class Listagem {

        @Test
        @DisplayName("listarSetores combina setores com a contagem de máquinas em uma única consulta")
        void listaComContagemDeMaquinas() {
            Setor corte = setor(1L, "CORTE");
            Setor dobra = setor(2L, "DOBRA");
            corte.getEtapas().add(etapa(10L, "SERRA", 1, corte)); // listas distintas para stubs distintos
            List<EtapaSetorDTO> etapasCorte = List.of(new EtapaSetorDTO(10L, "SERRA", 1));
            when(setorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(corte, dobra));
            when(maquinaRepository.contarMaquinasPorSetor()).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
            when(setorMapper.toEtapaDtoList(corte.getEtapas())).thenReturn(etapasCorte);
            when(setorMapper.toEtapaDtoList(dobra.getEtapas())).thenReturn(List.of());

            List<SetorListagemDTO> lista = service.listarSetores();

            assertThat(lista).containsExactly(
                    new SetorListagemDTO(1L, "CORTE", 3, etapasCorte),
                    new SetorListagemDTO(2L, "DOBRA", 0, List.of()));
            verify(maquinaRepository).contarMaquinasPorSetor();
        }

        @Test
        @DisplayName("listarSetores converte erro de banco em falha de validação")
        void listaErroDeBanco() {
            when(setorRepository.findAllByOrderByNomeAsc()).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.listarSetores())
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("buscarPorNome sem termo retorna a listagem completa")
        void buscaSemTermo() {
            when(setorRepository.findAllByOrderByNomeAsc()).thenReturn(List.of());
            when(maquinaRepository.contarMaquinasPorSetor()).thenReturn(List.of());

            assertThat(service.buscarPorNome("  ")).isEmpty();
            assertThat(service.buscarPorNome(null)).isEmpty();

            verify(setorRepository, never()).findByNomeContainingIgnoreCaseOrderByNomeAsc(any());
        }

        @Test
        @DisplayName("buscarPorNome normaliza o termo antes de consultar")
        void buscaComTermo() {
            Setor s = setor(1L, "CORTE");
            when(setorRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("COR")).thenReturn(List.of(s));
            when(maquinaRepository.contarMaquinasPorSetor()).thenReturn(List.of());
            when(setorMapper.toEtapaDtoList(s.getEtapas())).thenReturn(List.of());

            List<SetorListagemDTO> lista = service.buscarPorNome("  cor ");

            assertThat(lista).extracting(SetorListagemDTO::nome).containsExactly("CORTE");
        }

        @Test
        @DisplayName("buscarPorNome converte erro inesperado em falha de validação")
        void buscaErroInesperado() {
            when(setorRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc("COR")).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.buscarPorNome("cor"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("novoSetor")
    class Novo {

        @Test
        @DisplayName("DTO nulo ou nome em branco são rejeitados sem acessar o banco")
        void entradasInvalidas() {
            assertThatThrownBy(() -> service.novoSetor(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.novoSetor(new SetorDTO(null, "  ", List.of())))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(setorRepository, etapaSetorRepository, setorMapper);
        }

        @Test
        @DisplayName("Nome duplicado é rejeitado e nada é salvo")
        void nomeDuplicado() {
            when(setorRepository.findByNome("CORTE")).thenReturn(Optional.of(setor(1L, "CORTE")));

            assertThatThrownBy(() -> service.novoSetor(new SetorDTO(null, " corte ", List.of())))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NAME_DUPLICATE);

            verify(setorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Salva o setor e cria as etapas normalizadas, ignorando nomes em branco e numerando por posição")
        void salvaSetorEEtapas() {
            SetorDTO dto = new SetorDTO(null, "Dobra", List.of(
                    new EtapaSetorDTO(null, " 1ª dobra ", null),
                    new EtapaSetorDTO(null, "   ", null),
                    new EtapaSetorDTO(null, "2ª dobra", 9)));
            Setor salvo = setor(2L, "Dobra");
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());
            when(setorMapper.toEntity(dto)).thenReturn(new Setor());
            when(setorRepository.save(any(Setor.class))).thenReturn(salvo);

            service.novoSetor(dto);

            ArgumentCaptor<EtapaSetor> captor = ArgumentCaptor.forClass(EtapaSetor.class);
            verify(etapaSetorRepository, Mockito.times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(EtapaSetor::getNome).containsExactly("1ª DOBRA", "2ª DOBRA");
            assertThat(captor.getAllValues()).extracting(EtapaSetor::getOrdem).containsExactly(1, 9);
            assertThat(captor.getAllValues()).allMatch(e -> e.getSetor() == salvo);
        }

        @Test
        @DisplayName("Sem etapas salva apenas o setor")
        void semEtapas() {
            SetorDTO dto = new SetorDTO(null, "Solda", null);
            when(setorRepository.findByNome("SOLDA")).thenReturn(Optional.empty());
            when(setorMapper.toEntity(dto)).thenReturn(new Setor());
            when(setorRepository.save(any(Setor.class))).thenReturn(setor(3L, "Solda"));

            service.novoSetor(dto);

            verifyNoInteractions(etapaSetorRepository);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erroDeBanco() {
            SetorDTO dto = new SetorDTO(null, "Solda", null);
            when(setorRepository.findByNome("SOLDA")).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.novoSetor(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("excluirSetor")
    class Excluir {

        @Test
        @DisplayName("Setor inexistente ou id nulo resulta em não encontrado")
        void naoEncontrado() {
            when(setorRepository.findById(9L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.excluirSetor(null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);
            assertThatThrownBy(() -> service.excluirSetor(9L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);
        }

        @Test
        @DisplayName("Setor com máquinas não pode ser excluído e nada é removido")
        void comMaquinas() {
            when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L, "CORTE")));
            when(maquinaRepository.countBySetorId(1L)).thenReturn(2);

            assertThatThrownBy(() -> service.excluirSetor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_HAS_MACHINES);

            verifyNoInteractions(etapaSetorRepository);
            verify(setorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Remove as etapas, depois o setor, e faz flush")
        void excluiEmOrdem() {
            Setor s = setor(1L, "CORTE");
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(maquinaRepository.countBySetorId(1L)).thenReturn(0);

            service.excluirSetor(1L);

            InOrder ordem = Mockito.inOrder(etapaSetorRepository, setorRepository);
            ordem.verify(etapaSetorRepository).deleteAllBySetorId(1L);
            ordem.verify(setorRepository).delete(s);
            ordem.verify(setorRepository).flush();
        }

        @Test
        @DisplayName("Violação de integridade vira setor em uso")
        void emUso() {
            Setor s = setor(1L, "CORTE");
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(maquinaRepository.countBySetorId(1L)).thenReturn(0);
            Mockito.doThrow(new DataIntegrityViolationException("fk")).when(setorRepository).flush();

            assertThatThrownBy(() -> service.excluirSetor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_IN_USE);
        }

        @Test
        @DisplayName("Erro genérico de banco também é tratado como setor em uso")
        void erroDeBanco() {
            when(setorRepository.findById(1L)).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.excluirSetor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_IN_USE);
        }

        @Test
        @DisplayName("Erro inesperado vira falha de validação")
        void erroInesperado() {
            when(setorRepository.findById(1L)).thenThrow(new IllegalStateException("x"));

            assertThatThrownBy(() -> service.excluirSetor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("editarSetor")
    class Editar {

        @Test
        @DisplayName("DTO nulo ou nome em branco são rejeitados")
        void entradasInvalidas() {
            assertThatThrownBy(() -> service.editarSetor(1L, null))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
            assertThatThrownBy(() -> service.editarSetor(1L, new SetorDTO(1L, " ", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(setorRepository);
        }

        @Test
        @DisplayName("Setor inexistente é rejeitado")
        void inexistente() {
            when(setorRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.editarSetor(1L, new SetorDTO(1L, "Corte", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NOTFOUND);
        }

        @Test
        @DisplayName("Nome já usado por outro setor é rejeitado")
        void nomeDeOutroSetor() {
            when(setorRepository.findById(1L)).thenReturn(Optional.of(setor(1L, "CORTE")));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.of(setor(2L, "DOBRA")));

            assertThatThrownBy(() -> service.editarSetor(1L, new SetorDTO(1L, "dobra", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_SECTOR_NAME_DUPLICATE);

            verify(setorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Mantém etapas existentes, cria novas, remove as ausentes e renumera a ordem")
        void sincronizaEtapas() {
            Setor s = setor(1L, "DOBRA");
            EtapaSetor mantida = etapa(10L, "1ª", 1, s);
            EtapaSetor removida = etapa(11L, "2ª", 2, s);
            s.getEtapas().addAll(new ArrayList<>(List.of(mantida, removida)));
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.of(s));

            SetorDTO dto = new SetorDTO(1L, "  Dobra ", List.of(
                    new EtapaSetorDTO(null, "Nova", 99),
                    new EtapaSetorDTO(10L, "Primeira editada", 5),
                    new EtapaSetorDTO(null, "  ", 3)));

            service.editarSetor(1L, dto);

            assertThat(s.getNome()).isEqualTo("Dobra");
            assertThat(s.getEtapas()).hasSize(2).doesNotContain(removida);
            assertThat(mantida.getNome()).isEqualTo("Primeira editada");
            assertThat(mantida.getOrdem()).isEqualTo(2);
            EtapaSetor nova = s.getEtapas().stream().filter(e -> e.getId() == null).findFirst().orElseThrow();
            assertThat(nova.getNome()).isEqualTo("Nova");
            assertThat(nova.getOrdem()).isEqualTo(1);
            assertThat(nova.getSetor()).isSameAs(s);
            verify(setorRepository).save(s);
        }

        @Test
        @DisplayName("Etapa com id desconhecido é tratada como nova")
        void idDesconhecidoViraNova() {
            Setor s = setor(1L, "DOBRA");
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());

            service.editarSetor(1L, new SetorDTO(1L, "Dobra", List.of(new EtapaSetorDTO(777L, "Solta", 1))));

            assertThat(s.getEtapas()).hasSize(1);
            assertThat(s.getEtapas().get(0).getId()).isNull();
            assertThat(s.getEtapas().get(0).getNome()).isEqualTo("Solta");
        }

        @Test
        @DisplayName("Enviar apenas etapas em branco quando já existem etapas é rejeitado")
        void somenteEtapasEmBranco() {
            Setor s = setor(1L, "DOBRA");
            s.getEtapas().add(etapa(10L, "1ª", 1, s));
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.editarSetor(1L,
                    new SetorDTO(1L, "Dobra", List.of(new EtapaSetorDTO(null, "  ", 1)))))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);

            verify(setorRepository, never()).save(any());
        }


        @Test
        @DisplayName("Etapa ausente que está em uso em roteiro não pode ser removida")
        void etapaEmUsoEmRoteiro() {
            Setor s = setor(1L, "DOBRA");
            EtapaSetor emUso = etapa(11L, "2ª", 2, s);
            s.getEtapas().add(emUso);
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());
            when(roteiroPassoRepository.existsByEtapaSetorId(11L)).thenReturn(true);

            assertThatThrownBy(() -> service.editarSetor(1L,
                    new SetorDTO(1L, "Dobra", List.of(new EtapaSetorDTO(null, "Outra", 1)))))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_STAGE_IN_USE);

            verify(setorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Etapa ausente que está em uso em lançamento não pode ser removida")
        void etapaEmUsoEmLancamento() {
            Setor s = setor(1L, "DOBRA");
            s.getEtapas().add(etapa(11L, "2ª", 2, s));
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());
            when(roteiroPassoRepository.existsByEtapaSetorId(11L)).thenReturn(false);
            when(lancamentoRepository.existsByEtapaSetorId(11L)).thenReturn(true);

            assertThatThrownBy(() -> service.editarSetor(1L, new SetorDTO(1L, "Dobra", List.of())))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_STAGE_IN_USE);

            verify(setorRepository, never()).save(any());
        }

        @Test
        @DisplayName("Etapa em uso que continua na lista não dispara a verificação de uso")
        void etapaEmUsoMantidaNaoEhVerificada() {
            Setor s = setor(1L, "DOBRA");
            s.getEtapas().add(etapa(10L, "1ª", 1, s));
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());

            service.editarSetor(1L, new SetorDTO(1L, "Dobra", List.of(new EtapaSetorDTO(10L, "1ª", 1))));

            verifyNoInteractions(roteiroPassoRepository, lancamentoRepository);
            verify(setorRepository).save(s);
        }

        @Test
        @DisplayName("Lista de etapas nula não apaga as etapas existentes")
        void etapasNulasPreservamExistentes() {
            Setor s = setor(1L, "DOBRA");
            EtapaSetor existente = etapa(10L, "1ª", 1, s);
            s.getEtapas().add(existente);
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());

            service.editarSetor(1L, new SetorDTO(1L, "Dobra", null));

            assertThat(s.getEtapas()).containsExactly(existente);
            verifyNoInteractions(roteiroPassoRepository, lancamentoRepository);
            verify(setorRepository).save(s);
        }

        @Test
        @DisplayName("Lista de etapas vazia remove as etapas que não estão em uso")
        void listaVaziaRemoveEtapasLivres() {
            Setor s = setor(1L, "DOBRA");
            s.getEtapas().add(etapa(10L, "1ª", 1, s));
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("DOBRA")).thenReturn(Optional.empty());

            service.editarSetor(1L, new SetorDTO(1L, "Dobra", List.of()));

            assertThat(s.getEtapas()).isEmpty();
            verify(setorRepository).save(s);
        }

        @Test
        @DisplayName("Erro de banco ao salvar vira falha de validação")
        void erroDeBanco() {
            Setor s = setor(1L, "CORTE");
            when(setorRepository.findById(1L)).thenReturn(Optional.of(s));
            when(setorRepository.findByNome("CORTE")).thenReturn(Optional.empty());
            when(setorRepository.save(s)).thenThrow(new DataRetrievalFailureException("x"));

            assertThatThrownBy(() -> service.editarSetor(1L, new SetorDTO(1L, "Corte", null)))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("messageKey", AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
