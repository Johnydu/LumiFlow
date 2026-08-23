package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {
    List<Operador> findAllByOrderByNomeAsc();

    Operador findByNome(String nome);

    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);

    List<Operador>findBySetorPadraoId(Long setorId);

    Optional<Operador> findByNomeIgnoreCase(String nome);
}
