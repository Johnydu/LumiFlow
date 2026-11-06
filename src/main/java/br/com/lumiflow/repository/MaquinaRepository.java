package br.com.lumiflow.repository;


import br.com.lumiflow.entity.Maquina;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MaquinaRepository extends JpaRepository<Maquina, Long> {

    Optional<Maquina> findByNome(String nome);

    @EntityGraph(attributePaths = "setor")
    List<Maquina> findAllByOrderByNomeAsc();

    Integer countBySetorId(Long setorId);

    /** Contagem de máquinas de todos os setores em uma única consulta: [setorId, quantidade]. */
    @Query("select m.setor.id, count(m) from Maquina m group by m.setor.id")
    List<Object[]> contarMaquinasPorSetor();

    List<Maquina> findBySetorIdOrderByNomeAsc(Long setorId);

    List<Maquina> findBySetorIdInOrderByNomeAsc(List<Long> setorIds);
}
