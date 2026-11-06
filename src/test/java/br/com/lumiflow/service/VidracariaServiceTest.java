package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.HistoricoMovimentacaoDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.dto.vidracaria.SaldoVidroDTO;
import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.MovimentacaoVidro;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.entity.enums.TipoVidro;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ChapaVidroMapper;
import br.com.lumiflow.repository.ChapaVidroRepository;
import br.com.lumiflow.repository.MovimentacaoVidroRepository;
import br.com.lumiflow.repository.OperadorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VidracariaServiceTest {

    @Mock
    private ChapaVidroRepository chapaVidroRepository;
    @Mock
    private MovimentacaoVidroRepository movimentacaoVidroRepository;
    @Mock
    private ChapaVidroMapper chapaVidroMapper;
    @Mock
    private OperadorRepository operadorRepository;
    @Spy
    private Clock clock = RelogioTeste.real();

    @InjectMocks
    private VidracariaService service;

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    private ChapaVidro chapa(Long id, TipoVidro tipo, String descricao, int minimo) {
        ChapaVidro c = new ChapaVidro();
        c.setId(id);
        c.setTipoVidro(tipo);
        c.setDescricao(descricao);
        c.setEstoqueMinimo(minimo);
        return c;
    }

    private Operador operador(Long id, String nome) {
        Operador o = new Operador();
        o.setId(id);
        o.setNome(nome);
        return o;
    }

    @Nested
    @DisplayName("consultas de estoque")
    class Consultas {

        @Test
        @DisplayName("listarChapas retorna todas as chapas")
        void listarChapas() {
            List<ChapaVidro> chapas = List.of(chapa(1L, TipoVidro.INCOLOR, "A", 1));
            when(chapaVidroRepository.findAll()).thenReturn(chapas);

            assertThat(service.listarChapas()).isSameAs(chapas);
        }

        @Test
        @DisplayName("listarChapas converte erro de banco em falha de validação")
        void listarChapasErro() {
            when(chapaVidroRepository.findAll()).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.listarChapas(), AppMessages.ERROR_VALIDATION_FAILED);
        }

        @Test
        @DisplayName("Saldo é entradas menos saídas e sinaliza estoque no mínimo ou abaixo dele")
        void saldosAtuais() {
            ChapaVidro folgada = chapa(1L, TipoVidro.INCOLOR, "INCOLOR 8MM", 5);
            ChapaVidro noLimite = chapa(2L, TipoVidro.JATEADO, "JATEADO 8MM", 5);
            ChapaVidro abaixo = chapa(3L, TipoVidro.REFLEXIVEL, "REFLEXIVO", 5);
            when(chapaVidroRepository.findAll()).thenReturn(List.of(folgada, noLimite, abaixo));
            when(movimentacaoVidroRepository.somarSaldosPorChapa()).thenReturn(List.of(
                    new Object[]{1L, 16L}, new Object[]{2L, 5L}, new Object[]{3L, 3L}));

            List<SaldoVidroDTO> saldos = service.obterSaldosAtuais();

            assertThat(saldos).containsExactly(
                    new SaldoVidroDTO(1L, TipoVidro.INCOLOR, "INCOLOR 8MM", 16, 5, false),
                    new SaldoVidroDTO(2L, TipoVidro.JATEADO, "JATEADO 8MM", 5, 5, true),
                    new SaldoVidroDTO(3L, TipoVidro.REFLEXIVEL, "REFLEXIVO", 3, 5, true));
        }

        @Test
        @DisplayName("Sem chapas cadastradas não há saldos")
        void semChapas() {
            when(chapaVidroRepository.findAll()).thenReturn(List.of());
            when(movimentacaoVidroRepository.somarSaldosPorChapa()).thenReturn(List.of());

            assertThat(service.obterSaldosAtuais()).isEmpty();
        }

        @Test
        @DisplayName("Erro de banco ao calcular saldos vira falha de validação")
        void saldosErro() {
            when(chapaVidroRepository.findAll()).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.obterSaldosAtuais(), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("listarHistorico")
    class Historico {

        private MovimentacaoVidro movimentacao(Operador operador) {
            MovimentacaoVidro m = new MovimentacaoVidro();
            m.setId(1L);
            m.setTipoMovimentacao(TipoMovimentacao.ENTRADA);
            m.setChapaVidro(chapa(1L, TipoVidro.JATEADO, "JATEADO 8MM", 1));
            m.setQuantidade(10);
            m.setDataHora(LocalDateTime.of(2025, 1, 2, 3, 4));
            m.setOperador(operador);
            m.setObservacao("NF 123");
            return m;
        }

        @Test
        @DisplayName("Converte filtros válidos, usa página de 25 e mapeia o operador")
        void filtrosValidos() {
            when(movimentacaoVidroRepository.filtrarHistorico(TipoMovimentacao.ENTRADA, TipoVidro.JATEADO, PageRequest.of(2, 25)))
                    .thenReturn(new PageImpl<>(List.of(movimentacao(operador(1L, "ANA"))), PageRequest.of(2, 25), 60));

            Page<HistoricoMovimentacaoDTO> pagina = service.listarHistorico("ENTRADA", "JATEADO", 2);

            assertThat(pagina.getContent()).containsExactly(new HistoricoMovimentacaoDTO(
                    1L, TipoMovimentacao.ENTRADA, TipoVidro.JATEADO, "JATEADO 8MM", 10,
                    LocalDateTime.of(2025, 1, 2, 3, 4), "ANA", "NF 123"));
            // PageImpl recalcula o total (offset 50 + 1 item) quando a página é parcial.
            assertThat(pagina.getTotalElements()).isEqualTo(51);
        }

        @Test
        @DisplayName("Filtros nulos ou em branco são ignorados e página negativa vira zero")
        void filtrosVaziosSaoIgnorados() {
            when(movimentacaoVidroRepository.filtrarHistorico(null, null, PageRequest.of(0, 25)))
                    .thenReturn(new PageImpl<>(List.of(movimentacao(null)), PageRequest.of(0, 25), 1));

            Page<HistoricoMovimentacaoDTO> p1 = service.listarHistorico(null, "  ", -3);
            Page<HistoricoMovimentacaoDTO> p2 = service.listarHistorico("", null, 0);

            assertThat(p1.getContent()).hasSize(1);
            assertThat(p2.getContent()).hasSize(1);
            verify(movimentacaoVidroRepository, org.mockito.Mockito.times(2))
                    .filtrarHistorico(null, null, PageRequest.of(0, 25));
        }

        @Test
        @DisplayName("Filtro com valor desconhecido é erro explícito, sem consultar o repositório")
        void filtroInvalidoEhRejeitado() {
            assertChave(() -> service.listarHistorico("inexistente", null, 0), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.listarHistorico(null, "entrada", 0), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(movimentacaoVidroRepository);
        }

        @Test
        @DisplayName("Movimentação sem operador aparece como Sistema")
        void semOperador() {
            when(movimentacaoVidroRepository.filtrarHistorico(null, null, PageRequest.of(0, 25)))
                    .thenReturn(new PageImpl<>(List.of(movimentacao(null))));

            assertThat(service.listarHistorico(null, null, 0).getContent().get(0).nomeUsuario()).isEqualTo("Sistema");
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erro() {
            when(movimentacaoVidroRepository.filtrarHistorico(null, null, PageRequest.of(0, 25)))
                    .thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.listarHistorico(null, null, 0), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("registrarMovimentacao")
    class Movimentacao {

        private MovimentacaoVidroDTO dto(TipoMovimentacao tipo, int quantidade, LocalDateTime dataHora) {
            return new MovimentacaoVidroDTO(1L, tipo, quantidade, 2L, dataHora, "obs");
        }

        @Test
        @DisplayName("DTO nulo é rejeitado")
        void dtoNulo() {
            assertChave(() -> service.registrarMovimentacao(null), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(chapaVidroRepository, movimentacaoVidroRepository, operadorRepository);
        }

        @Test
        @DisplayName("Chapa inexistente é rejeitada")
        void chapaInexistente() {
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.empty());

            assertChave(() -> service.registrarMovimentacao(dto(TipoMovimentacao.ENTRADA, 1, null)),
                    AppMessages.ERROR_GLASS_SHEET_NOTFOUND);

            verify(movimentacaoVidroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Operador inexistente é rejeitado")
        void operadorInexistente() {
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(chapa(1L, TipoVidro.INCOLOR, "A", 1)));
            when(operadorRepository.findById(2L)).thenReturn(Optional.empty());

            assertChave(() -> service.registrarMovimentacao(dto(TipoMovimentacao.ENTRADA, 1, null)),
                    AppMessages.ERROR_GLASS_OPERATOR_NOTFOUND);

            verify(movimentacaoVidroRepository, never()).save(any());
        }

        @Test
        @DisplayName("ENTRADA não valida estoque, usa a data informada e grava todos os campos")
        void entradaGravaTudo() {
            ChapaVidro chapa = chapa(1L, TipoVidro.INCOLOR, "A", 1);
            Operador operador = operador(2L, "ANA");
            LocalDateTime data = LocalDateTime.of(2025, 5, 5, 8, 0);
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(chapa));
            when(operadorRepository.findById(2L)).thenReturn(Optional.of(operador));

            service.registrarMovimentacao(dto(TipoMovimentacao.ENTRADA, 30, data));

            verify(movimentacaoVidroRepository, never()).somarQuantidadePorChapaETipo(any(), any());
            ArgumentCaptor<MovimentacaoVidro> captor = ArgumentCaptor.forClass(MovimentacaoVidro.class);
            verify(movimentacaoVidroRepository).save(captor.capture());
            MovimentacaoVidro salva = captor.getValue();
            assertThat(salva.getChapaVidro()).isSameAs(chapa);
            assertThat(salva.getOperador()).isSameAs(operador);
            assertThat(salva.getTipoMovimentacao()).isEqualTo(TipoMovimentacao.ENTRADA);
            assertThat(salva.getQuantidade()).isEqualTo(30);
            assertThat(salva.getDataHora()).isEqualTo(data);
            assertThat(salva.getObservacao()).isEqualTo("obs");
        }

        @Test
        @DisplayName("Sem data informada usa o momento atual")
        void semDataUsaAgora() {
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(chapa(1L, TipoVidro.INCOLOR, "A", 1)));
            when(operadorRepository.findById(2L)).thenReturn(Optional.of(operador(2L, "ANA")));
            LocalDateTime antes = LocalDateTime.now();

            service.registrarMovimentacao(dto(TipoMovimentacao.ENTRADA, 1, null));

            ArgumentCaptor<MovimentacaoVidro> captor = ArgumentCaptor.forClass(MovimentacaoVidro.class);
            verify(movimentacaoVidroRepository).save(captor.capture());
            assertThat(captor.getValue().getDataHora())
                    .isCloseTo(antes, within(5, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("SAÍDA maior que o saldo é rejeitada por estoque insuficiente")
        void saidaAcimaDoSaldo() {
            ChapaVidro chapa = chapa(1L, TipoVidro.INCOLOR, "A", 1);
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(chapa));
            when(operadorRepository.findById(2L)).thenReturn(Optional.of(operador(2L, "ANA")));
            when(movimentacaoVidroRepository.somarQuantidadePorChapaETipo(1L, TipoMovimentacao.ENTRADA)).thenReturn(10);
            when(movimentacaoVidroRepository.somarQuantidadePorChapaETipo(1L, TipoMovimentacao.SAIDA)).thenReturn(4);

            assertChave(() -> service.registrarMovimentacao(dto(TipoMovimentacao.SAIDA, 7, null)),
                    AppMessages.ERROR_GLASS_INSUFFICIENT_STOCK);

            verify(movimentacaoVidroRepository, never()).save(any());
        }

        @Test
        @DisplayName("SAÍDA exatamente igual ao saldo é permitida")
        void saidaIgualAoSaldo() {
            ChapaVidro chapa = chapa(1L, TipoVidro.INCOLOR, "A", 1);
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(chapa));
            when(operadorRepository.findById(2L)).thenReturn(Optional.of(operador(2L, "ANA")));
            when(movimentacaoVidroRepository.somarQuantidadePorChapaETipo(1L, TipoMovimentacao.ENTRADA)).thenReturn(10);
            when(movimentacaoVidroRepository.somarQuantidadePorChapaETipo(1L, TipoMovimentacao.SAIDA)).thenReturn(4);

            service.registrarMovimentacao(dto(TipoMovimentacao.SAIDA, 6, null));

            verify(movimentacaoVidroRepository).save(any(MovimentacaoVidro.class));
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erroDeBanco() {
            when(chapaVidroRepository.findByIdForUpdate(1L)).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.registrarMovimentacao(dto(TipoMovimentacao.ENTRADA, 1, null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("cadastrarChapa")
    class Cadastrar {

        @Test
        @DisplayName("DTO nulo é rejeitado")
        void dtoNulo() {
            assertChave(() -> service.cadastrarChapa(null), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(chapaVidroRepository, chapaVidroMapper);
        }

        @Test
        @DisplayName("Tipo de vidro já cadastrado é rejeitado")
        void tipoDuplicado() {
            when(chapaVidroRepository.existsByTipoVidro(TipoVidro.JATEADO)).thenReturn(true);

            assertChave(() -> service.cadastrarChapa(new ChapaVidroDTO(TipoVidro.JATEADO, "x", 1)),
                    AppMessages.ERROR_GLASS_TYPE_DUPLICATE);

            verify(chapaVidroRepository, never()).save(any());
        }

        @Test
        @DisplayName("Salva a entidade mapeada quando o tipo é inédito")
        void salva() {
            ChapaVidroDTO dto = new ChapaVidroDTO(TipoVidro.REFLEXIVEL, "REFLEXIVO 6MM", 3);
            ChapaVidro entidade = chapa(null, TipoVidro.REFLEXIVEL, "REFLEXIVO 6MM", 3);
            when(chapaVidroRepository.existsByTipoVidro(TipoVidro.REFLEXIVEL)).thenReturn(false);
            when(chapaVidroMapper.toEntity(dto)).thenReturn(entidade);

            service.cadastrarChapa(dto);

            verify(chapaVidroRepository).save(entidade);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erroDeBanco() {
            when(chapaVidroRepository.existsByTipoVidro(TipoVidro.REFLEXIVEL)).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.cadastrarChapa(new ChapaVidroDTO(TipoVidro.REFLEXIVEL, "x", 1)),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
