package br.com.lumiflow.repository;

import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {

    // Busca pelo código/número da OP (Ex: "OP-2026-00001")
    Optional<OrdemProducao> findByNumero(String numero);

    // Lista OPs por status (ex: buscar todas PENDENTES ou EM_ANDAMENTO)
    List<OrdemProducao> findByStatus(EstatusOrdemProducao status);

    // Verifica se o número informado já existe no banco
    boolean existsByNumero(String numero);
}