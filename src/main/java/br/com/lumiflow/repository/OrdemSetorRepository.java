package br.com.lumiflow.repository;

import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemSetorRepository extends JpaRepository<OrdemSetor, Long> {

    @Query("""
        SELECT
            s.id,
            s.nome,
            COUNT(os.id),
            SUM(CASE WHEN os.status = 'CONCLUIDA' THEN 1 ELSE 0 END)
        FROM Setor s
        LEFT JOIN OrdemSetor os ON os.setor.id = s.id
        WHERE (:busca IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
        GROUP BY s.id, s.nome
        ORDER BY s.nome ASC
    """)
    List<Object[]> buscarResumoSetores(@Param("busca") String busca);


    @Query("""
    SELECT DISTINCT os FROM OrdemSetor os
    JOIN os.ordemProducao o
    JOIN o.produto p
    WHERE os.setor.id = :setorId
      AND (:busca IS NULL OR :busca = '' OR 
           LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%')) OR 
           LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%')))
      AND (:status IS NULL OR :status = '' OR CAST(os.status AS string) = :status)
    ORDER BY os.criadoEm DESC, os.sequencia ASC
""")
    List<OrdemSetor> buscarPorSetorIdEFiltros(@Param("setorId") Long setorId,
                                              @Param("busca") String busca,
                                              @Param("status") String status);


    @Query("""
    SELECT os FROM OrdemSetor os
    WHERE os.ordemProducao.id = :ordemProducaoId
      AND os.sequencia = :sequencia
      AND os.origemRefugo = false
      AND ((:variante IS NULL AND os.variante IS NULL) OR os.variante = :variante)
""")
    Optional<OrdemSetor> findByOrdemProducaoIdAndSequenciaAndVariante(
            @Param("ordemProducaoId") Long ordemProducaoId,
            @Param("sequencia") Integer sequencia,
            @Param("variante") String variante);


    List<OrdemSetor> findByOrdemProducaoIdInOrderBySequenciaAsc(List<Long> ordemProducaoIds);

    Long countByStatus(EstatusOrdemProducao status);

    Long countByStatusNot(EstatusOrdemProducao status);

    Long countByStatusAndFimBetween(EstatusOrdemProducao status, LocalDateTime inicio, LocalDateTime fim);

    @Query("""
    SELECT new br.com.lumiflow.dto.dashboard.SetorResumoDTO(
        s.id,
        s.nome,
        COUNT(os.id)
    )
    FROM Setor s
    LEFT JOIN OrdemSetor os ON os.setor = s AND os.status = br.com.lumiflow.entity.enums.EstatusOrdemProducao.DISPONIVEL
    GROUP BY s.id, s.nome
    ORDER BY COUNT(os.id) DESC
""")
    List<SetorResumoDTO> contarEtapasDisponiveisPorSetor();


    boolean existsByOrdemProducaoIdAndStatusNot(Long ordemProducaoId, EstatusOrdemProducao status);

    @Query("""
    SELECT os FROM OrdemSetor os
    JOIN FETCH os.setor
    WHERE os.ordemProducao.id = :ordemId
    ORDER BY os.sequencia ASC, os.origemRefugo ASC, os.id ASC
""")
    List<OrdemSetor> buscarLinhasDaOrdem(@Param("ordemId") Long ordemId);
}
