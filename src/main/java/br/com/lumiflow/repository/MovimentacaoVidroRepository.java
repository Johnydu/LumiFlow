package br.com.lumiflow.repository;

import br.com.lumiflow.entity.MovimentacaoVidro;
import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.entity.enums.TipoVidro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoVidroRepository extends JpaRepository<MovimentacaoVidro, Long> {

    @Query(value = """
            SELECT m FROM MovimentacaoVidro m
            LEFT JOIN FETCH m.operador
            JOIN FETCH m.chapaVidro c
            WHERE (:tipoMovimentacao IS NULL OR m.tipoMovimentacao = :tipoMovimentacao)
              AND (:tipoVidro IS NULL OR c.tipoVidro = :tipoVidro)
            ORDER BY m.dataHora DESC, m.id DESC
            """,
            countQuery = """
            SELECT COUNT(m) FROM MovimentacaoVidro m
            JOIN m.chapaVidro c
            WHERE (:tipoMovimentacao IS NULL OR m.tipoMovimentacao = :tipoMovimentacao)
              AND (:tipoVidro IS NULL OR c.tipoVidro = :tipoVidro)
            """)
    Page<MovimentacaoVidro> filtrarHistorico(
            @Param("tipoMovimentacao") TipoMovimentacao tipoMovimentacao,
            @Param("tipoVidro") TipoVidro tipoVidro,
            Pageable pageable
    );

    @Query("SELECT COALESCE(SUM(m.quantidade), 0) FROM MovimentacaoVidro m" +
            " WHERE m.chapaVidro.id = :chapaId AND m.tipoMovimentacao = :tipo")
    int somarQuantidadePorChapaETipo(@Param("chapaId") Long chapaId, @Param("tipo") TipoMovimentacao tipo);

    /** Saldo (entradas - saídas) de todas as chapas em UMA consulta: [chapaId, saldo]. Chapa sem movimento não aparece. */
    @Query("""
            SELECT m.chapaVidro.id,
                   COALESCE(SUM(CASE WHEN m.tipoMovimentacao = br.com.lumiflow.entity.enums.TipoMovimentacao.ENTRADA
                                     THEN m.quantidade
                                     WHEN m.tipoMovimentacao = br.com.lumiflow.entity.enums.TipoMovimentacao.SAIDA
                                     THEN -m.quantidade
                                     ELSE 0 END), 0)
            FROM MovimentacaoVidro m
            GROUP BY m.chapaVidro.id
            """)
    List<Object[]> somarSaldosPorChapa();
}