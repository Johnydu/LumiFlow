package br.com.lumiflow.service;

import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.HistoricoMovimentacaoDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.dto.vidracaria.SaldoVidroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.ChapaVidroMapper;
import br.com.lumiflow.model.ChapaVidro;
import br.com.lumiflow.model.MovimentacaoVidro;
import br.com.lumiflow.model.Operador;
import br.com.lumiflow.model.Usuario;
import br.com.lumiflow.model.enums.TipoMovimentacao;
import br.com.lumiflow.repository.ChapaVidroRepository;
import br.com.lumiflow.repository.MovimentacaoVidroRepository;
import br.com.lumiflow.repository.OperadorRepository;
import br.com.lumiflow.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class VidracariaService {

    private final ChapaVidroRepository chapaVidroRepository;
    private final MovimentacaoVidroRepository movimentacaoVidroRepository;
    private final ChapaVidroMapper chapaVidroMapper;
    private final OperadorRepository operadorRepository;

    /**
     * Retorna a lista de chapas cadastradas para preencher o <select> da tela.
     */
    public List<ChapaVidro> listarChapas() {
        return chapaVidroRepository.findAll();
    }

    /**
     * Calcula o saldo atual de cada tipo de vidro e verifica se está abaixo do estoque mínimo.
     */
    @Transactional(readOnly = true)
    public List<SaldoVidroDTO> obterSaldosAtuais() {
        List<ChapaVidro> chapas = chapaVidroRepository.findAll();

        return chapas.stream().map(chapa -> {
            // 1. Busca a soma das entradas
            Integer entradas = movimentacaoVidroRepository
                    .somarQuantidadePorChapaETipo(chapa.getId(), TipoMovimentacao.ENTRADA);

            // 2. Busca a soma das saídas/consumos
            Integer consumos = movimentacaoVidroRepository
                    .somarQuantidadePorChapaETipo(chapa.getId(), TipoMovimentacao.SAIDA);

            // 3. Calcula o saldo final (Entradas - Consumos)
            int saldoAtual = entradas - consumos;

            // 4. Verifica se o estoque está abaixo ou igual ao limite mínimo cadastrado
            boolean abaixoDoMinimo = saldoAtual <= chapa.getEstoqueMinimo();

            return new SaldoVidroDTO(
                    chapa.getId(),
                    chapa.getTipoVidro(),
                    chapa.getDescricao(),
                    saldoAtual,
                    chapa.getEstoqueMinimo(),
                    abaixoDoMinimo
            );
        }).toList();
    }

    /**
     * Lista o histórico completo de movimentações ordenado do mais recente para o mais antigo.
     */
    @Transactional(readOnly = true)
    public List<HistoricoMovimentacaoDTO> listarHistorico() {
        return movimentacaoVidroRepository.findAllByOrderByDataHoraDesc()
                .stream()
                .map(m -> new HistoricoMovimentacaoDTO(
                        m.getId(),
                        m.getTipoMovimentacao(),
                        m.getChapaVidro().getTipoVidro(),
                        m.getChapaVidro().getDescricao(),
                        m.getQuantidade(),
                        m.getDataHora(),
                        m.getOperador() != null ? m.getOperador().getNome() : "Sistema",
                        m.getObservacao()
                )).toList();
    }

    /**
     * Grava um novo lançamento de Entrada ou Consumo no banco de dados.
     */
    @Transactional
    public void registrarMovimentacao(MovimentacaoVidroDTO dto) {
        // 1. Valida e busca a chapa
        ChapaVidro chapa = chapaVidroRepository.findById(dto.chapaVidroId())
                .orElseThrow(() -> new BusinessException("Chapa de vidro não encontrada."));

        // 2. Busca o operador selecionado no formulário pelo ID
        Operador operador = operadorRepository.findById(dto.operadorId())
                .orElseThrow(() -> new BusinessException("Operador selecionado não foi localizado."));

        // 3. Monta a entidade
        MovimentacaoVidro movimentacao = new MovimentacaoVidro();
        movimentacao.setChapaVidro(chapa);
         // 👈 Atribui o operador selecionado (preenche a coluna usuario_id)
        movimentacao.setQuantidade(dto.quantidade());
        movimentacao.setOperador(operador);
        movimentacao.setTipoMovimentacao(dto.tipoMovimentacao());
        movimentacao.setDataHora(dto.dataHora() != null ? dto.dataHora() : LocalDateTime.now());
        movimentacao.setObservacao(dto.observacao());

        movimentacaoVidroRepository.save(movimentacao);
    }

    @Transactional
    public void cadastrarChapa(ChapaVidroDTO dto) {
        // Valida se esse tipo já não foi cadastrado anteriormente
        if (chapaVidroRepository.existsByTipoVidro(dto.tipoVidro())) {
            throw new BusinessException("Já existe um cadastro para o tipo de vidro selecionado.");
        }

        chapaVidroRepository.save(chapaVidroMapper.toEntity(dto));
    }
}