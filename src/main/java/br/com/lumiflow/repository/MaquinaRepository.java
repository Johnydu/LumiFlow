package br.com.lumiflow.repository;


import br.com.lumiflow.entity.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    Optional<Maquina> findByNome(String nome);
    List<Maquina> findAllByOrderByNomeAsc();
}
