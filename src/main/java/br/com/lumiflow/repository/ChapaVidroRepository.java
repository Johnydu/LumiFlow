package br.com.lumiflow.repository;

import br.com.lumiflow.model.ChapaVidro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChapaVidroRepository extends JpaRepository<ChapaVidro, Long> {
}
