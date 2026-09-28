package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {


    @Query("SELECT l FROM Lancamento l " +
            "LEFT JOIN FETCH l.etapaSetor es " +
            "LEFT JOIN FETCH es.setor s " +
            "LEFT JOIN FETCH l.operador o " +
            "ORDER BY l.dataHora DESC")
    List<Lancamento> findAllComRelacionamentosOrderByDataHoraDesc();
}



