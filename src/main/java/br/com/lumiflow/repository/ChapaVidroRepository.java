package br.com.lumiflow.repository;

import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.enums.TipoVidro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChapaVidroRepository extends JpaRepository<ChapaVidro, Long> {

    Optional<ChapaVidro> findByTipoVidro(TipoVidro tipoVidro);

    boolean existsByTipoVidro(TipoVidro tipoVidro);
}