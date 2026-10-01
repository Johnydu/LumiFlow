package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {


    @Query("""
        SELECT l FROM Lancamento l
        JOIN FETCH l.ordemSetor os
        JOIN FETCH os.setor
        JOIN FETCH l.operador
        LEFT JOIN FETCH l.etapaSetor
        ORDER BY l.dataHora DESC
        """)
    List<Lancamento> findAllComRelacionamentosOrderByDataHoraDesc();
}



