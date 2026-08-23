package br.com.lumiflow.repository;

import br.com.lumiflow.entity.RoteiroProduto;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RoteiroProdutoRepository extends JpaRepository<RoteiroProduto, Long> {

    @Query("""
           select r
           from RoteiroProduto r
           order by r.produto.nome asc, r.sequencia asc
           """)
    List<RoteiroProduto> listarOrdenados();

    @Query("""
           select r
           from RoteiroProduto r
           where r.produto.id = :produtoId
           order by r.sequencia asc
           """)
    List<RoteiroProduto> buscarPorProdutoId(@Param("produtoId") Long produtoId);

    @Transactional
    @Modifying
    @Query("""
           delete
           from RoteiroProduto r
           where r.produto.id = :produtoId
           """)
    void deletarPorProdutoId(@Param("produtoId") Long produtoId);
}