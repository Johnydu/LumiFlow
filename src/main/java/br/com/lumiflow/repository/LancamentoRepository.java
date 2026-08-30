package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    // Histórico de lançamentos de uma etapa (auditoria/tela de detalhe)
    List<Lancamento> findByOrdemSetorIdOrderByDataHoraDesc(Long ordemSetorId);

}
