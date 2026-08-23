package br.com.lumiflow.repository;

import br.com.lumiflow.entity.MovimentacaoVidro;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoVidroRepository extends JpaRepository<MovimentacaoVidro, Long> {


    List<MovimentacaoVidro> findAllByOrderByDataHoraDesc();

    @Query("""
        SELECT COALESCE(SUM(m.quantidade), 0)
        FROM MovimentacaoVidro m
        WHERE m.chapaVidro.id = :chapaId
          AND m.tipoMovimentacao = :tipo
   """)
    Integer somarQuantidadePorChapaETipo(@Param("chapaId") Long chapaId,
                                         @Param("tipo") TipoMovimentacao tipo);
    }