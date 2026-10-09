package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Operador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OperadorRepository extends JpaRepository<Operador, Long> {
    List<Operador> findAllByOrderByNomeAsc();


    boolean existsByNome(String nome);

    boolean existsByNomeAndIdNot(String nome, Long id);

    List<Operador>findBySetorPadraoId(Long setorId);

    List<Operador> findBySetorPadraoIdOrSetorPadraoIsNullOrderByNomeAsc(Long setorId);

    @Query("SELECT o FROM Operador o LEFT JOIN o.setorPadrao s WHERE " +
            "(:termo IS NULL OR :termo = '' OR LOWER(o.nome) LIKE LOWER(CONCAT('%', :termo, '%'))) " +
            "AND (:setorId IS NULL OR s.id = :setorId OR s.id IS NULL)")
    List<Operador> buscarPorNomeESetor(@Param("termo") String termo, @Param("setorId") Long setorId);
}
