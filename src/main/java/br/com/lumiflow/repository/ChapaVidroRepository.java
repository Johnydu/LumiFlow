package br.com.lumiflow.repository;

import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.enums.TipoVidro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapaVidroRepository extends JpaRepository<ChapaVidro, Long> {

    Optional<ChapaVidro> findByTipoVidro(TipoVidro tipoVidro);

    boolean existsByTipoVidro(TipoVidro tipoVidro);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ChapaVidro c WHERE c.id = :id")
    Optional<ChapaVidro> findByIdForUpdate(@Param("id") Long id);
}