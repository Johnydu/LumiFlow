package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Retrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetrabalhoRepository extends JpaRepository<Retrabalho, Long> {
}