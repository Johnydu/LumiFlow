package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Retrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetrabalhoRepository extends JpaRepository<Retrabalho, Long> {

    @Query("SELECT COALESCE(SUM(r.qtdRefeita), 0) FROM Retrabalho r WHERE r.refugo.id = :refugoId")
    Integer somarRetrabalhadoPorRefugo(@Param("refugoId") Long refugoId);

    @Query("""
    SELECT rt FROM Retrabalho rt
    JOIN FETCH rt.refugo r
    JOIN FETCH r.ordemSetor os
    JOIN FETCH r.setorOrigem
    JOIN FETCH rt.operador
    LEFT JOIN FETCH rt.maquina
    WHERE os.ordemProducao.id = :ordemId
    ORDER BY rt.dataHora DESC
""")
    List<Retrabalho> buscarPorOrdem(@Param("ordemId") Long ordemId);
}