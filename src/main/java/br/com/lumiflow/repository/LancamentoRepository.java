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


    @Query(value = """
            SELECT l FROM Lancamento l
            JOIN FETCH l.ordemSetor os
            JOIN FETCH os.setor
            JOIN FETCH os.ordemProducao o
            JOIN FETCH o.produto
            JOIN FETCH l.operador
            JOIN FETCH l.maquina
            LEFT JOIN FETCH l.etapaSetor
            WHERE (:setorId IS NULL OR os.setor.id = :setorId)
            ORDER BY l.dataHora DESC
            """,
            countQuery = """
    SELECT COUNT(l) FROM Lancamento l
    WHERE (:setorId IS NULL OR l.ordemSetor.setor.id = :setorId)
    """)
    Page<Lancamento> buscarHistorico(@Param("setorId") Long setorId, Pageable pageable);

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
}



