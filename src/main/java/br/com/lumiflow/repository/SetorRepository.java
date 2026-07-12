package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SetorRepository extends JpaRepository<Setor,Long> {
}
