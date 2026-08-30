package br.com.lumiflow.repository;

import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface OrdemProducaoRepository extends JpaRepository<OrdemProducao, Long> {

    // Busca geral (todas as ordens com filtro de texto e status)
    @Query("""
        SELECT DISTINCT o FROM OrdemProducao o
        WHERE (:busca IS NULL OR :busca = '' OR 
               LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%')) OR 
               LOWER(o.produto.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
          AND (:status IS NULL OR :status = '' OR CAST(o.status AS string) = :status)
        ORDER BY o.dataCriacao DESC
    """)
    List<OrdemProducao> buscarComFiltros(
            @Param("busca") String busca,
            @Param("status") String status
    );

    // Busca filtrada por um setor específico via ID
    @Query("""
    SELECT DISTINCT o FROM OrdemProducao o
    LEFT JOIN o.ordensSetor os
    WHERE os.setor.id = :setorId
      AND (:busca IS NULL OR :busca = '' OR 
           LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%')) OR 
           LOWER(o.produto.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
      AND (:status IS NULL OR :status = '' OR CAST(os.status AS string) = :status)
    ORDER BY o.dataCriacao DESC
""")
    List<OrdemProducao> buscarPorSetorIdEFiltros(
            @Param("setorId") Long setorId,
            @Param("busca") String busca,
            @Param("status") String status
    );

    Long countByStatus(EstatusOrdemProducao status);

    @Query("""
        SELECT o FROM OrdemProducao o 
        ORDER BY CASE WHEN o.status = br.com.lumiflow.entity.enums.EstatusOrdemProducao.EM_ANDAMENTO THEN 1 ELSE 2 END, 
                 o.id DESC
    """)
    Page<OrdemProducao> findAllOrdenadasPorEmAndamento(Pageable pageable);


    @Query(value = "SELECT nextval('ordem_producao_numero_seq')", nativeQuery = true)
    Long proximoNumeroSequencial();
}