package br.com.lumiflow.repository;

import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
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


    @Query(value = """
        SELECT os FROM OrdemSetor os
        JOIN FETCH os.ordemProducao o
        JOIN FETCH o.produto p
        WHERE os.setor.id = :setorId
          AND os.status IN :statuses
          AND (:busca = ''
               OR LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%')))
        ORDER BY os.criadoEm DESC, os.sequencia ASC, os.id ASC
        """,
            countQuery = """
        SELECT COUNT(os) FROM OrdemSetor os
        JOIN os.ordemProducao o
        JOIN o.produto p
        WHERE os.setor.id = :setorId
          AND os.status IN :statuses
          AND (:busca = ''
               OR LOWER(o.numero) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
               OR LOWER(p.codigo) LIKE LOWER(CONCAT('%', :busca, '%')))
        """)
    Page<OrdemSetor> buscarPorSetorIdEFiltros(@Param("setorId") Long setorId,
                                               @Param("busca") String busca,
                                               @Param("statuses") Collection<EstatusOrdemProducao> statuses,
                                               Pageable pageable);


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


    @Query("""
        SELECT os FROM OrdemSetor os
        JOIN FETCH os.setor
        WHERE os.ordemProducao.id IN :ordemProducaoIds
        ORDER BY os.sequencia ASC
        """)
    List<OrdemSetor> findByOrdemProducaoIdInOrderBySequenciaAsc(@Param("ordemProducaoIds") List<Long> ordemProducaoIds);

    Long countByStatus(EstatusOrdemProducao status);

    Long countByStatusNot(EstatusOrdemProducao status);

    /** Intervalo semiaberto [inicio, fimExclusivo): não perde nem duplica o instante de virada do dia. */
    Long countByStatusAndFimGreaterThanEqualAndFimLessThan(EstatusOrdemProducao status,
                                                           LocalDateTime inicio, LocalDateTime fimExclusivo);

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
