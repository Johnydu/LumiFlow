package br.com.lumiflow.repository;

import br.com.lumiflow.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SetorRepository extends JpaRepository<Setor,Long> {
    List<Setor> findAllByOrderByNomeAsc();
    Optional<Setor> findByNome(String nome);
}
