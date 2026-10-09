package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.HistoricoLancamentosDTO;
import br.com.lumiflow.dto.lancamento.LancamentoProducaoDTO;
import br.com.lumiflow.dto.lancamento.RefugoDTO;
import br.com.lumiflow.dto.lancamento.RetrabalhoDTO;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final OrdemProducaoRepository ordemProducaoRepository;
    private final LancamentoRepository lancamentoRepository;
    private final RefugoRepository refugoRepository;
    private final RetrabalhoRepository retrabalhoRepository;
    private final MaquinaRepository maquinaRepository;
    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final SetorService setorService;
    private final OperadorRepository operadorRepository;
    private final OperadorService operadorService;

    @Transactional(readOnly = true)
    public OrdemSetor buscarEtapaAtiva(Long ordemId, Long ordemSetorId) {
        if (ordemId == null || ordemSetorId == null) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        OrdemSetor ordemSetor = ordemSetorRepository.findById(ordemSetorId)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE));

        if (!ordemSetor.getOrdemProducao().getId().equals(ordemId)) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        if (ordemSetor.getStatus() != EstatusOrdemProducao.LIBERADA
                && ordemSetor.getStatus() != EstatusOrdemProducao.EM_ANDAMENTO) {
            throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
        }

        return ordemSetor;
    }

    @Transactional(readOnly = true)
    public List<Maquina> listarMaquinasDoSetor(Long setorId) {
        return maquinaRepository.findBySetorIdOrderByNomeAsc(setorId);
    }

    @Transactional(readOnly = true)
    public List<Operador> listarOperadoresDoSetor(Long setorId) {
        return operadorRepository.findBySetorPadraoIdOrSetorPadraoIsNullOrderByNomeAsc(setorId);
    }

    @Transactional
    public OrdemSetor lancarProducao(Long ordemId, Long ordemSetorId, LancamentoProducaoDTO dto) {
        try {
            // Validação imediata do DTO e da quantidade antes de consultar a base de dados
            if (dto == null || dto.quantidade() == null || dto.quantidade() <= 0) {
                throw new BusinessException("A quantidade lançada deve ser maior que zero.");
            }

            OrdemSetor ordemSetor = buscarEtapaAtiva(ordemId, ordemSetorId);

            if (dto.quantidade() > ordemSetor.getQtdPendente()) {
                throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
            }

            Maquina maquina = maquinaRepository.findById(dto.maquinaId())
                    .filter(m -> m.getSetor().getId().equals(ordemSetor.getSetor().getId()))
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));

            Operador operador = operadorRepository.findById(dto.operadorId())
                    .filter(o -> o.getSetorPadrao() == null
                            || o.getSetorPadrao().getId().equals(ordemSetor.getSetor().getId()))
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND));

            EtapaSetor etapaSetor = roteiroProdutoRepository
                    .findFirstByProdutoIdAndSequencia(ordemSetor.getOrdemProducao().getProduto().getId(),
                            ordemSetor.getSequencia())
                    .map(RoteiroProduto::getEtapaSetor)
                    .orElse(null);

            Lancamento lancamento = new Lancamento();
            lancamento.setQtdProduzida(dto.quantidade());
            lancamento.setObservacao(dto.observacao());
            lancamento.setOrdemSetor(ordemSetor);
            lancamento.setMaquina(maquina);
            lancamento.setOperador(operador);
            lancamento.setEtapaSetor(etapaSetor);
            lancamentoRepository.save(lancamento);

            if (ordemSetor.getInicio() == null) {
                ordemSetor.setInicio(LocalDateTime.now());
            }
            if (ordemSetor.getStatus() == EstatusOrdemProducao.LIBERADA) {
                ordemSetor.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);
            }
            ordemSetor.setQtdProduzida(ordemSetor.getQtdProduzida() + dto.quantidade());
            ordemSetor.setQtdPendente(ordemSetor.getQtdPendente() - dto.quantidade());

            if (ordemSetor.getQtdPendente() == 0) {
                ordemSetor.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetor.setFim(LocalDateTime.now());
            }

            ordemSetorRepository.save(ordemSetor);
            avancarParaProximaEtapa(ordemSetor, dto.quantidade());
            return ordemSetor;

        } catch (BusinessException e) {
            // Relança exceções de negócio tratadas para o Controller exibir ao utilizador
            throw e;
        } catch (ObjectOptimisticLockingFailureException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao lançar produção para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Erro de persistência ao registar o lançamento. Tente novamente.");
        } catch (Exception e) {
            log.error("Erro inesperado ao lançar produção para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Ocorreu um erro interno ao processar o lançamento da produção.");
        }
    }

    private void avancarParaProximaEtapa(OrdemSetor ordemSetorAtual, Integer quantidadeLancada) {
        avancarParaProximaEtapaAPartirDe(ordemSetorAtual.getOrdemProducao(), ordemSetorAtual.getSequencia(),
                ordemSetorAtual.getVariante(), quantidadeLancada);
        verificarConclusaoOrdem(ordemSetorAtual.getOrdemProducao());
    }

    private void verificarConclusaoOrdem(OrdemProducao ordem) {
        boolean existeEtapaAberta = ordemSetorRepository
                .existsByOrdemProducaoIdAndStatusNot(ordem.getId(), EstatusOrdemProducao.CONCLUIDA);

        if (!existeEtapaAberta) {
            ordem.setStatus(EstatusOrdemProducao.CONCLUIDA);
            ordemProducaoRepository.save(ordem);
        }
    }

    private void avancarParaProximaEtapaAPartirDe(OrdemProducao ordem, Integer sequenciaReferencia,
                                                  String variante, Integer quantidadeLancada) {
        RoteiroProduto proximoPasso = roteiroProdutoRepository
                .findProximoPasso(ordem.getProduto().getId(), sequenciaReferencia)
                .orElse(null);

        if (proximoPasso == null) {
            return;
        }

        OrdemSetor proximaEtapa = ordemSetorRepository
                .findByOrdemProducaoIdAndSequenciaAndVariante(ordem.getId(), proximoPasso.getSequencia(), variante)
                .orElse(null);

        if (proximaEtapa == null) {
            proximaEtapa = new OrdemSetor();
            proximaEtapa.setOrdemProducao(ordem);
            proximaEtapa.setSetor(proximoPasso.getSetor());
            proximaEtapa.setSequencia(proximoPasso.getSequencia());
            proximaEtapa.setVariante(variante);
            proximaEtapa.setQtdRecebida(0);
            proximaEtapa.setQtdProduzida(0);
            proximaEtapa.setQtdPendente(0);
            proximaEtapa.setStatus(EstatusOrdemProducao.DISPONIVEL); // <--- Nasce corretamente como DISPONIVEL
        }

        proximaEtapa.setQtdRecebida(proximaEtapa.getQtdRecebida() + quantidadeLancada);
        proximaEtapa.setQtdPendente(proximaEtapa.getQtdPendente() + quantidadeLancada);

        if (proximaEtapa.getQtdPendente() > 0) {
            // Apenas estados válidos do enum
            if (proximaEtapa.getStatus() == EstatusOrdemProducao.CONCLUIDA ||
                    proximaEtapa.getStatus() == EstatusOrdemProducao.DISPONIVEL) {
                if (proximaEtapa.getQtdProduzida() > 0) {
                    proximaEtapa.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);
                } else {
                    proximaEtapa.setStatus(EstatusOrdemProducao.DISPONIVEL); // <--- Garante que fica DISPONIVEL
                }
                proximaEtapa.setFim(null);
            }
        }

        ordemSetorRepository.save(proximaEtapa);
    }


    @Transactional
    public OrdemSetor registrarRefugo(Long ordemId, Long ordemSetorId, RefugoDTO dto) {
        try {
            if (dto == null || dto.quantidade() == null || dto.quantidade() <= 0) {
                throw new BusinessException("A quantidade de refugo deve ser maior que zero.");
            }

            OrdemSetor ordemSetorAtual = buscarEtapaAtiva(ordemId, ordemSetorId);
            Setor setorOrigem = setorService.buscarSetorPorId(dto.setorOrigemId());
            Operador operador = operadorService.buscarOperadorPorId(dto.operadorId());

            if (operador.getSetorPadrao() != null
                    && !operador.getSetorPadrao().getId().equals(ordemSetorAtual.getSetor().getId())) {
                throw new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND);
            }

            if (dto.quantidade() > ordemSetorAtual.getQtdPendente()) {
                throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
            }

            // Sequência REAL do setor de origem no roteiro do produto — desambiguada por
            // etapaSetorId quando o setor tem mais de um passo no roteiro (ex.: Dobra
            // com 1ª e 2ª Dobra). Calculada uma única vez e usada de forma consistente
            // na busca da linha acumuladora, na criação dela e na gravação do Refugo.
            // Também valida que o setor escolhido realmente faz parte do roteiro (item 8).
            Integer sequenciaOrigem = buscarPassoNoRoteiro(
                    ordemSetorAtual.getOrdemProducao().getProduto().getId(),
                    setorOrigem.getId(),
                    dto.etapaSetorId()
            ).getSequencia();
            if (sequenciaOrigem > ordemSetorAtual.getSequencia()) {
                throw new BusinessException(AppMessages.ERROR_REFUGO_ORIGEM_INVALIDA);
            }
            ordemSetorAtual.setQtdRecebida(ordemSetorAtual.getQtdRecebida() - dto.quantidade());
            ordemSetorAtual.setQtdPendente(ordemSetorAtual.getQtdPendente() - dto.quantidade());
            if (ordemSetorAtual.getQtdPendente() == 0) {
                ordemSetorAtual.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetorAtual.setFim(LocalDateTime.now());
            }
            ordemSetorRepository.save(ordemSetorAtual);

            // Retrabalho: cria uma NOVA linha no setor de origem, marcada como refugo.
            // A linha normal do setor não é alterada.
            // Descarte: a peça sai da produção, então não cria linha.

            OrdemSetor linhaRefugo = null;
            if (dto.destino() == DestinoRefugo.RETRABALHO) {
                linhaRefugo = ordemSetorRepository.save(criarOrdemSetorParaRefugo(
                        ordemSetorAtual.getOrdemProducao(), setorOrigem, sequenciaOrigem,
                        ordemSetorAtual.getVariante(), dto.quantidade()));
            }

            Refugo refugo = new Refugo();
            refugo.setVariante(ordemSetorAtual.getVariante());
            refugo.setQtdRefugo(dto.quantidade());
            refugo.setMotivo(dto.motivo());
            refugo.setDestino(dto.destino());
            refugo.setDataHora(LocalDateTime.now());
            refugo.setOrdemSetor(ordemSetorAtual);
            refugo.setSetorOrigem(setorOrigem);
            refugo.setSequenciaOrigem(sequenciaOrigem);
            refugo.setOperador(operador);
            refugo.setOrdemSetorRetrabalho(linhaRefugo);
            refugoRepository.save(refugo);

            verificarConclusaoOrdem(ordemSetorAtual.getOrdemProducao());

            return ordemSetorAtual;


        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao registar refugo para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Erro de persistência ao registar o refugo. Tente novamente.");
        } catch (Exception e) {
            log.error("Erro inesperado ao registar refugo para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Ocorreu um erro interno ao processar o registo de refugo.");
        }
    }

    private OrdemSetor criarOrdemSetorParaRefugo(OrdemProducao ordem, Setor setorOrigem,
                                                 Integer sequenciaOrigem, String variante, Integer quantidade) {
        OrdemSetor nova = new OrdemSetor();
        nova.setOrdemProducao(ordem);
        nova.setSetor(setorOrigem);
        nova.setSequencia(sequenciaOrigem);
        nova.setVariante(variante);
        nova.setQtdRecebida(quantidade);
        nova.setQtdProduzida(0);
        nova.setQtdPendente(quantidade);
        nova.setStatus(EstatusOrdemProducao.DISPONIVEL);
        nova.setOrigemRefugo(true);
        return nova;
    }


    private RoteiroProduto buscarPassoNoRoteiro(Long produtoId, Long setorId, Long etapaSetorId) {
        List<RoteiroProduto> passosDoSetor = roteiroProdutoRepository.buscarPorProdutoId(produtoId).stream()
                .filter(rp -> rp.getSetor().getId().equals(setorId))
                .toList();

        if (passosDoSetor.isEmpty()) {
            throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_NOTFOUND);
        }

        if (passosDoSetor.size() == 1) {
            return passosDoSetor.get(0);
        }

        if (etapaSetorId == null) {
            throw new BusinessException(AppMessages.ERROR_REFUGO_ETAPA_REQUIRED);
        }

        return passosDoSetor.stream()
                .filter(rp -> rp.getEtapaSetor() != null && rp.getEtapaSetor().getId().equals(etapaSetorId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ROUTE_STEP_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public List<Refugo> listarRefugosParaRetrabalho(Long ordemProducaoId) {
        return refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                ordemProducaoId, DestinoRefugo.RETRABALHO);
    }

    @Transactional
    public OrdemSetor registrarRetrabalho(Long ordemId, RetrabalhoDTO dto) {
        try {
            if (dto == null || dto.quantidade() == null || dto.quantidade() <= 0) {
                throw new BusinessException("A quantidade de retrabalho deve ser maior que zero.");
            }

            Refugo refugo = refugoRepository.findById(dto.refugoId())
                    .filter(r -> r.getOrdemSetor().getOrdemProducao().getId().equals(ordemId))
                    .filter(r -> r.getDestino() == DestinoRefugo.RETRABALHO)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_REFUGO_NOTFOUND));

            int jaRetrabalhado = retrabalhoRepository.somarRetrabalhadoPorRefugo(dto.refugoId());
            if (dto.quantidade() > refugo.getQtdRefugo() - jaRetrabalhado) {
                throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
            }

            Operador operador = operadorService.buscarOperadorPorId(dto.operadorId());

            Maquina maquina = null;
            if (dto.maquinaId() != null) {
                maquina = maquinaRepository.findById(dto.maquinaId())
                        .filter(m -> m.getSetor().getId().equals(refugo.getSetorOrigem().getId()))
                        .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));
            }

            OrdemSetor ordemSetorOrigem = refugo.getOrdemSetorRetrabalho();
            if (ordemSetorOrigem == null) {
                throw new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE);
            }

            if (dto.quantidade() > ordemSetorOrigem.getQtdPendente()) {
                throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
            }

            Retrabalho retrabalho = new Retrabalho();
            retrabalho.setQtdRefeita(dto.quantidade());
            retrabalho.setObservacao(dto.observacao());
            retrabalho.setDataHora(LocalDateTime.now());
            retrabalho.setRefugo(refugo);
            retrabalho.setMaquina(maquina);
            retrabalho.setOperador(operador);
            retrabalhoRepository.save(retrabalho);

            ordemSetorOrigem.setQtdProduzida(ordemSetorOrigem.getQtdProduzida() + dto.quantidade());
            ordemSetorOrigem.setQtdPendente(ordemSetorOrigem.getQtdPendente() - dto.quantidade());

            if (ordemSetorOrigem.getQtdPendente() == 0) {
                ordemSetorOrigem.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetorOrigem.setFim(LocalDateTime.now());
            } else {
                ordemSetorOrigem.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);
            }
            ordemSetorRepository.save(ordemSetorOrigem);

            avancarParaProximaEtapaAPartirDe(ordemSetorOrigem.getOrdemProducao(), refugo.getSequenciaOrigem(),
                    ordemSetorOrigem.getVariante(), dto.quantidade());
            verificarConclusaoOrdem(ordemSetorOrigem.getOrdemProducao());

            return ordemSetorOrigem;

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao registar retrabalho para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Erro de persistência ao registar o retrabalho. Tente novamente.");
        } catch (Exception e) {
            log.error("Erro inesperado ao registar retrabalho para a ordem {}: {}", ordemId, e.getMessage(), e);
            throw new BusinessException("Ocorreu um erro interno ao processar o registo de retrabalho.");
        }
    }

    @Transactional(readOnly = true)
    public String buscarNomeEtapa(OrdemSetor ordemSetor) {
        return roteiroProdutoRepository
                .findFirstByProdutoIdAndSequencia(ordemSetor.getOrdemProducao().getProduto().getId(),
                        ordemSetor.getSequencia())
                .map(RoteiroProduto::getEtapaSetor)
                .map(EtapaSetor::getNome)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<HistoricoLancamentosDTO> listarHistoricoLancamentos(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(Math.max(pagina, 0), Math.min(Math.max(tamanho, 1), 100));



        return lancamentoRepository.buscarHistorico(null, pageable).map(l -> new HistoricoLancamentosDTO(
                l.getOrdemSetor().getOrdemProducao().getId(),
                l.getOrdemSetor().getOrdemProducao().getNumero(),
                l.getOrdemSetor().getOrdemProducao().getProduto().getNome(),
                l.getOrdemSetor().getSetor().getNome(),
                l.getEtapaSetor() != null ? l.getEtapaSetor().getNome() : null,
                l.getOrdemSetor().getVariante(),
                l.getMaquina().getNome(),
                l.getQtdProduzida(),
                l.getObservacao(),
                l.getOperador().getNome(),
                l.getDataHora()
        ));
    }

    @Transactional(readOnly = true)
    public List<Maquina> listarMaquinasParaRetrabalho(Long ordemProducaoId) {
        List<Long> setoresOrigem = listarRefugosParaRetrabalho(ordemProducaoId).stream()
                .map(r -> r.getSetorOrigem().getId())
                .distinct()
                .toList();

        if (setoresOrigem.isEmpty()) {
            return List.of();
        }

        return maquinaRepository.findBySetorIdInOrderByNomeAsc(setoresOrigem);
    }


}