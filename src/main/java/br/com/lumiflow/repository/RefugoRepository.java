package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefugoRepository extends JpaRepository<Refugo, Long> {
    List<Refugo> findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
            Long ordemProducaoId, DestinoRefugo destino);

    @Query("SELECT COALESCE(SUM(r.qtdRefugo), 0) FROM Refugo r WHERE r.destino = :destino")
    Long somarQuantidadePorDestino(@Param("destino") DestinoRefugo destino);

    @Query("""
    SELECT r FROM Refugo r
    JOIN FETCH r.ordemSetor os
    JOIN FETCH os.setor
    JOIN FETCH r.setorOrigem
    JOIN FETCH r.operador
    WHERE os.ordemProducao.id = :ordemId
    ORDER BY r.dataHora DESC
""")
    List<Refugo> buscarPorOrdem(@Param("ordemId") Long ordemId);
}