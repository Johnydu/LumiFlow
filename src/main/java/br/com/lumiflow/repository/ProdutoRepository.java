package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByNome(String nome);

    List<Produto> findAllByOrderByNomeAsc();


     Boolean  existsByCodigo(String codigo);

    Boolean existsByNome(String nome);
}



