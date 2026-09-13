package br.com.lumiflow.repository;

import br.com.lumiflow.entity.MovimentacaoVidro;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.entity.enums.TipoVidro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoVidroRepository extends JpaRepository<MovimentacaoVidro, Long> {

    List<MovimentacaoVidro> findAllByOrderByDataHoraDesc();

    @Query("SELECT m FROM MovimentacaoVidro m WHERE " +
            "(:tipoMovimentacao IS NULL OR m.tipoMovimentacao = :tipoMovimentacao) AND " +
            "(:tipoVidro IS NULL OR m.chapaVidro.tipoVidro = :tipoVidro) " +
            "ORDER BY m.dataHora DESC")
    List<MovimentacaoVidro> filtrarHistorico(
            @Param("tipoMovimentacao") TipoMovimentacao tipoMovimentacao,
            @Param("tipoVidro") TipoVidro tipoVidro
    );

    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoVidro m" +
            " WHERE m.chapaVidro.id = :chapaId AND m.tipoMovimentacao = :tipo")
    int somarQuantidadePorChapaETipo(@Param("chapaId") Long chapaId, @Param("tipo") TipoMovimentacao tipo);
}