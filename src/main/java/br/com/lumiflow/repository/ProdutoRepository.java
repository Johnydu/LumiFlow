package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    List<Produto> findByRoteiroIdOrderByNomeAsc(Long roteiroId);

    List<Produto> findByRoteiroIdInOrderByNomeAsc(Collection<Long> roteiroIds);

    long countByRoteiroId(Long roteiroId);

    @Query("select p from Produto p " +
            "left join fetch p.roteiro" +
            " order by p.nome asc")
    List<Produto> listarComRoteiro();
}



