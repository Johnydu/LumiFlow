package br.com.lumiflow.repository;


import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {

    List<OrdemProducao> findByStatusNotOrderByDataCriacaoDesc(EstatusOrdemProducao status, Pageable pageable);

    @Query(value = "SELECT nextval('ordem_producao_numero_seq')", nativeQuery = true)
    Long proximoNumeroSequencial();


    @Query("SELECT o FROM OrdemProducao o JOIN FETCH o.produto WHERE o.id = :id")
    Optional<OrdemProducao> buscarComProduto(@Param("id") Long id);

    boolean existsByProdutoIdAndStatusNot(Long produtoId, EstatusOrdemProducao status);

    boolean existsByProdutoRoteiroIdAndStatusNot(Long roteiroId, EstatusOrdemProducao status);
}