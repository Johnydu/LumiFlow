package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Refugo;
import br.com.lumiflow.entity.enums.DestinoRefugo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RefugoRepository extends JpaRepository<Refugo, Long> {
    List<Refugo> findByOrdemSetor_OrdemProducao_IdAndDestinoOrderByDataHoraDesc(
            Long ordemProducaoId, DestinoRefugo destino);
}