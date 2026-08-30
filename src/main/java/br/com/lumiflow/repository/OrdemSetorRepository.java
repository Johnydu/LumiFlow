package br.com.lumiflow.repository;

import br.com.lumiflow.entity.OrdemSetor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrdemSetorRepository extends JpaRepository<OrdemSetor, Long> {

    @Query("""
        SELECT
            s.id,
            s.nome,
            COUNT(os.id),
            SUM(CASE WHEN os.status = 'FINALIZADO' THEN 1 ELSE 0 END)
        FROM Setor s
        LEFT JOIN OrdemSetor os ON os.setor.id = s.id
        WHERE (:busca IS NULL OR LOWER(s.nome) LIKE LOWER(CONCAT('%', CAST(:busca AS string), '%')))
        GROUP BY s.id, s.nome
        ORDER BY s.nome ASC
    """)
    List<Object[]> buscarResumoSetores(@Param("busca") String busca);

    @Query("""
        SELECT os FROM OrdemSetor os
        WHERE os.setor.id = :setorId
        AND os.qtdPendente > 0
        ORDER BY os.criadoEm ASC
        """)
    List<OrdemSetor> findDisponiveisPorSetor(@Param("setorId") Long setorId);

    // Busca a etapa específica de uma ordem em um setor (pra achar "a próxima")
    Optional<OrdemSetor> findByOrdemProducaoIdAndSequencia(Long ordemProducaoId, Integer sequencia);

    // Todas as etapas de uma ordem (pra verificar se ela terminou tudo)
    List<OrdemSetor> findByOrdemProducaoIdOrderBySequenciaAsc(Long ordemProducaoId);

    Optional<OrdemSetor> findByOrdemProducaoIdAndSetorId(Long ordemProducaoId, Long setorId);

    List<OrdemSetor> findBySetorIdAndOrdemProducaoIdIn(Long setorId, List<Long> ordemProducaoIds);
}

