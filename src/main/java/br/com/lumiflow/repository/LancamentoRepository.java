package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Lancamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    /** Etapa do setor usada em algum lançamento (a FK é ON DELETE SET NULL: apagar a etapa perderia o histórico). */
    boolean existsByEtapaSetorId(Long etapaSetorId);


    @Query("""
    SELECT l FROM Lancamento l
    JOIN FETCH l.ordemSetor os
    JOIN FETCH os.setor
    JOIN FETCH l.operador
    JOIN FETCH l.maquina
    LEFT JOIN FETCH l.etapaSetor
    WHERE os.ordemProducao.id = :ordemId
    ORDER BY l.dataHora DESC
""")
    List<Lancamento> buscarPorOrdem(@Param("ordemId") Long ordemId);

    @Query(value = """
        SELECT l FROM Lancamento l
        JOIN FETCH l.ordemSetor os
        JOIN FETCH os.ordemProducao op
        JOIN FETCH op.produto p
        JOIN FETCH os.setor s
        LEFT JOIN FETCH l.operador oped
        LEFT JOIN FETCH l.maquina m
        LEFT JOIN FETCH l.etapaSetor etapa
        WHERE (:setorId IS NULL OR s.id = :setorId)
          AND (:etapaSetorId IS NULL OR etapa.id = :etapaSetorId)
          AND (:operadorId IS NULL OR oped.id = :operadorId)
        ORDER BY l.dataHora DESC
        """,
        countQuery = """
        SELECT COUNT(l) FROM Lancamento l
        WHERE (:setorId IS NULL OR l.ordemSetor.setor.id = :setorId)
          AND (:etapaSetorId IS NULL OR l.etapaSetor.id = :etapaSetorId)
          AND (:operadorId IS NULL OR l.operador.id = :operadorId)
        """)
    Page<Lancamento> buscarComFiltros(
            @Param("setorId") Long setorId,
            @Param("etapaSetorId") Long etapaSetorId,
            @Param("operadorId") Long operadorId,
            Pageable pageable
    );
}

