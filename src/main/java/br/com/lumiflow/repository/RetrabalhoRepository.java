package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Retrabalho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RetrabalhoRepository extends JpaRepository<Retrabalho, Long> {

    @Query("SELECT COALESCE(SUM(r.qtdRefeita), 0) FROM Retrabalho r WHERE r.refugo.id = :refugoId")
    Integer somarRetrabalhadoPorRefugo(@Param("refugoId") Long refugoId);
}