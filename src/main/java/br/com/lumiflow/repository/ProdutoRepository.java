package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNome(String nome);

    List<Produto> findAllByOrderByNomeAsc();

     Boolean  existsByCodigo(String codigo);

    Boolean existsByNome(String nome);

    List<Produto> findByNomeContainingIgnoreCaseOrCodigoContainingIgnoreCase(String nome, String codigo);

    long countByRoteiroId(Long roteiroId);

    @Query(value = """
        select p from Produto p left join fetch p.roteiro
        where lower(p.codigo) like lower(concat('%', :busca, '%'))
           or lower(p.nome) like lower(concat('%', :busca, '%'))
        order by p.codigo asc
        """,
            countQuery = """
        select count(p) from Produto p
        where lower(p.codigo) like lower(concat('%', :busca, '%'))
           or lower(p.nome) like lower(concat('%', :busca, '%'))
        """)
    Page<Produto> buscarParaVinculo(@Param("busca") String busca, Pageable pageable);

    @Query(value = """
        select p from Produto p left join fetch p.roteiro
        where p.roteiro.id = :roteiroId
          and (lower(p.codigo) like lower(concat('%', :busca, '%'))
            or lower(p.nome) like lower(concat('%', :busca, '%')))
        order by p.codigo asc
        """,
            countQuery = """
        select count(p) from Produto p
        where p.roteiro.id = :roteiroId
          and (lower(p.codigo) like lower(concat('%', :busca, '%'))
            or lower(p.nome) like lower(concat('%', :busca, '%')))
        """)
    Page<Produto> buscarVinculados(@Param("roteiroId") Long roteiroId,
                                   @Param("busca") String busca, Pageable pageable);

    @Query("select p.roteiro.id, count(p) from Produto p "
            + "where p.roteiro.id in :roteiroIds group by p.roteiro.id")
    List<Object[]> contarPorRoteiros(@Param("roteiroIds") Collection<Long> roteiroIds);

}


