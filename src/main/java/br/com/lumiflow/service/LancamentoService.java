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
        return operadorRepository.findBySetorPadraoId(setorId);
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
                    .filter(o -> o.getSetorPadrao().getId().equals(ordemSetor.getSetor().getId()))
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

            avancarParaProximaEtapa(ordemSetor, dto.quantidade());

            if (ordemSetor.getQtdPendente() == 0) {
                ordemSetor.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetor.setFim(LocalDateTime.now());
            }

            ordemSetorRepository.save(ordemSetor);
            return ordemSetor;

        } catch (BusinessException e) {
            // Relança exceções de negócio tratadas para o Controller exibir ao utilizador
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

        if (roteiroProdutoRepository.findProximoPasso(ordemSetorAtual.getOrdemProducao().getProduto().getId(),
                ordemSetorAtual.getSequencia()).isEmpty()
                && ordemSetorAtual.getQtdPendente() - quantidadeLancada <= 0) {
            OrdemProducao ordem = ordemSetorAtual.getOrdemProducao();
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
    public void registrarRefugo(Long ordemId, Long ordemSetorId, RefugoDTO dto) {
        try {
            if (dto == null || dto.quantidade() == null || dto.quantidade() <= 0) {
                throw new BusinessException("A quantidade de refugo deve ser maior que zero.");
            }

            OrdemSetor ordemSetorAtual = buscarEtapaAtiva(ordemId, ordemSetorId);
            Setor setorOrigem = setorService.buscarSetorPorId(dto.setorOrigemId());
            Operador operador = operadorService.buscarOperadorPorId(dto.operadorId());

            if (dto.quantidade() > ordemSetorAtual.getQtdPendente()) {
                throw new BusinessException(AppMessages.ERROR_LAUNCH_EXCEEDS_PENDING);
            }

            ordemSetorAtual.setQtdRecebida(ordemSetorAtual.getQtdRecebida() - dto.quantidade());
            ordemSetorAtual.setQtdPendente(ordemSetorAtual.getQtdPendente() - dto.quantidade());
            if (ordemSetorAtual.getQtdPendente() == 0 && ordemSetorAtual.getQtdProduzida() > 0) {
                ordemSetorAtual.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetorAtual.setFim(LocalDateTime.now());
            }
            ordemSetorRepository.save(ordemSetorAtual);

            // CORREÇÃO: Busca considerando também a sequência da etapa atual
            OrdemSetor ordemSetorOrigem = ordemSetorRepository
                    .findByOrdemProducaoIdAndSetorIdAndSequenciaAndStatusIn(
                            ordemSetorAtual.getOrdemProducao().getId(),
                            setorOrigem.getId(),
                            ordemSetorAtual.getSequencia(),
                            List.of(EstatusOrdemProducao.DISPONIVEL,
                                    EstatusOrdemProducao.EM_ANDAMENTO,
                                    EstatusOrdemProducao.CONCLUIDA))
                    .orElseGet(() -> criarOrdemSetorParaRefugo(
                            ordemSetorAtual.getOrdemProducao(),
                            setorOrigem,
                            ordemSetorAtual.getSequencia()));

            ordemSetorOrigem.setOrigemRefugo(true);
            ordemSetorOrigem.setQtdRecebida(ordemSetorOrigem.getQtdRecebida() + dto.quantidade());
            ordemSetorOrigem.setQtdPendente(ordemSetorOrigem.getQtdPendente() + dto.quantidade());
            ordemSetorRepository.save(ordemSetorOrigem);

            Refugo refugo = new Refugo();
            refugo.setQtdRefugo(dto.quantidade());
            refugo.setMotivo(dto.motivo());
            refugo.setDestino(dto.destino());
            refugo.setDataHora(LocalDateTime.now());
            refugo.setOrdemSetor(ordemSetorAtual);
            refugo.setSetorOrigem(setorOrigem);
            refugo.setSequenciaOrigem(ordemSetorAtual.getSequencia());
            refugo.setOperador(operador);
            refugoRepository.save(refugo);

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

    private OrdemSetor criarOrdemSetorParaRefugo(OrdemProducao ordem, Setor setorOrigem, Integer sequencia) {
        OrdemSetor novo = new OrdemSetor();
        novo.setOrdemProducao(ordem);
        novo.setSetor(setorOrigem);
        novo.setSequencia(sequencia); // <-- Usa a sequência real da etapa
        novo.setQtdRecebida(0);
        novo.setQtdProduzida(0);
        novo.setQtdPendente(0);
        novo.setStatus(EstatusOrdemProducao.DISPONIVEL);
        novo.setOrigemRefugo(true);
        return novo;
    }

    @Transactional(readOnly = true)
    public List<Refugo> listarRefugosParaRetrabalho(Long ordemProducaoId) {
        return refugoRepository.findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
                ordemProducaoId, DestinoRefugo.RETRABALHO);
    }

    @Transactional
    public void registrarRetrabalho(Long ordemId, RetrabalhoDTO dto) {
        try {
            if (dto == null || dto.quantidade() == null || dto.quantidade() <= 0) {
                throw new BusinessException("A quantidade de retrabalho deve ser maior que zero.");
            }

            Refugo refugo = refugoRepository.findById(dto.refugoId())
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_REFUGO_NOTFOUND));
            Operador operador = operadorService.buscarOperadorPorId(dto.operadorId());

            if (!refugo.getOrdemSetor().getOrdemProducao().getId().equals(ordemId)) {
                throw new BusinessException(AppMessages.ERROR_REFUGO_NOTFOUND);
            }

            Maquina maquina = null;
            if (dto.maquinaId() != null) {
                maquina = maquinaRepository.findById(dto.maquinaId())
                        .filter(m -> m.getSetor().getId().equals(refugo.getSetorOrigem().getId()))
                        .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));
            }

            Retrabalho retrabalho = new Retrabalho();
            retrabalho.setQtdRefeita(dto.quantidade());
            retrabalho.setObservacao(dto.observacao());
            retrabalho.setDataHora(LocalDateTime.now());
            retrabalho.setRefugo(refugo);
            retrabalho.setMaquina(maquina);
            retrabalho.setOperador(operador);
            retrabalhoRepository.save(retrabalho);

            OrdemSetor ordemSetorOrigem = ordemSetorRepository
                    .findByOrdemProducaoIdAndSetorIdAndSequenciaAndStatusIn(
                            ordemId,
                            refugo.getSetorOrigem().getId(),
                            refugo.getSequenciaOrigem(),
                            List.of(EstatusOrdemProducao.DISPONIVEL,
                                    EstatusOrdemProducao.EM_ANDAMENTO,
                                    EstatusOrdemProducao.CONCLUIDA))
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ORDER_NOT_AVAILABLE));

            ordemSetorOrigem.setQtdProduzida(ordemSetorOrigem.getQtdProduzida() + dto.quantidade());
            ordemSetorOrigem.setQtdPendente(ordemSetorOrigem.getQtdPendente() - dto.quantidade());
            ordemSetorOrigem.setStatus(EstatusOrdemProducao.EM_ANDAMENTO);

            avancarParaProximaEtapaAPartirDe(ordemSetorOrigem.getOrdemProducao(), refugo.getSequenciaOrigem(),
                    ordemSetorOrigem.getVariante(), dto.quantidade());

            if (ordemSetorOrigem.getQtdPendente() == 0) {
                ordemSetorOrigem.setStatus(EstatusOrdemProducao.CONCLUIDA);
                ordemSetorOrigem.setFim(LocalDateTime.now());
            }

            ordemSetorRepository.save(ordemSetorOrigem);

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
    public List<HistoricoLancamentosDTO> listarHistoricoLancamentos() {
        // AGORA SEGURO CONTRA N+1: Uma única query traz tudo estruturado
        List<Lancamento> lancamentos = lancamentoRepository.findAllComRelacionamentosOrderByDataHoraDesc();

        return lancamentos.stream().map(lancamento -> {
            String nomeSetor = "Setor";
            if (lancamento.getEtapaSetor() != null && lancamento.getEtapaSetor().getSetor() != null) {
                nomeSetor = lancamento.getEtapaSetor().getSetor().getNome();
            }

            return new HistoricoLancamentosDTO(
                    nomeSetor,
                    lancamento.getEtapaSetor() != null ? lancamento.getEtapaSetor().getNome() : null,
                    lancamento.getQtdProduzida(),
                    lancamento.getObservacao(),
                    lancamento.getOperador() != null ? lancamento.getOperador().getNome() : "Desconhecido",
                    lancamento.getDataHora()
            );
        }).toList();
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