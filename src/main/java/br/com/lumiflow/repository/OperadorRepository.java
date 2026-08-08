package br.com.lumiflow.repository;

import br.com.lumiflow.model.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {
    List<Operador> findAllByOrderByNomeAsc();
}
