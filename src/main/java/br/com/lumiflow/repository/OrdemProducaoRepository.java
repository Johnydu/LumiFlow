package br.com.lumiflow.repository;


import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {


    @Query(value = "SELECT nextval('ordem_producao_numero_seq')", nativeQuery = true)
    Long proximoNumeroSequencial();


    @Query("SELECT o FROM OrdemProducao o JOIN FETCH o.produto WHERE o.id = :id")
    Optional<OrdemProducao> buscarComProduto(@Param("id") Long id);

    /** Qualquer ordem (aberta ou concluída) que usa o roteiro: a estrutura dele fica congelada. */
    boolean existsByRoteiroId(Long roteiroId);

    @Query(value = """
        SELECT o FROM OrdemProducao o
        JOIN FETCH o.produto p
        WHERE o.status IN :statuses
          AND (:busca = ''
               OR LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.nome)   LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%')))
        ORDER BY CASE WHEN o.status = br.com.lumiflow.entity.enums.EstatusOrdemProducao.CONCLUIDA
                      THEN 1 ELSE 0 END,
                 o.dataCriacao DESC, o.id DESC
        """,
            countQuery = """
        SELECT COUNT(o) FROM OrdemProducao o
        JOIN o.produto p
        WHERE o.status IN :statuses
          AND (:busca = ''
               OR LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.nome)   LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%')))
        """)
    Page<OrdemProducao> buscarParaDashboard(@Param("busca") String busca,
                                            @Param("statuses") Collection<EstatusOrdemProducao> statuses,
                                            Pageable pageable);

}
