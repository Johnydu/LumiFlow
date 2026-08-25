package br.com.lumiflow.repository;

import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SetorRepository extends JpaRepository<Setor,Long> {
    List<Setor> findAllByOrderByNomeAsc();

    Optional<Setor> findByNome(String nome);

    @Query("""
        SELECT new br.com.lumiflow.dto.dashboard.SetorResumoDTO(
            s.id,
            s.nome,
            COUNT(DISTINCT os.ordemProducao.id)
        )
        FROM Setor s
        LEFT JOIN OrdemSetor os ON os.setor = s
        GROUP BY s.id, s.nome
        ORDER BY COUNT(DISTINCT os.ordemProducao.id) DESC
    """)
    List<SetorResumoDTO> obterResumoOrdensPorSetor();
}
