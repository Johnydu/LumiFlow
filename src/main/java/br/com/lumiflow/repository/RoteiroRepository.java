package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Roteiro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoteiroRepository extends JpaRepository<Roteiro, Long> {

    /**
     * Trava a linha do roteiro até o fim da transação. Serializa a edição estrutural do roteiro com a
     * criação de ordens que o usam (senão uma ordem poderia nascer com os passos antigos e ficar órfã).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Roteiro r where r.id = :id")
    Optional<Roteiro> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCodigoIgnoreCase(String codigo);

    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);

    @Query("""
           select distinct r from Roteiro r
           left join Produto p on p.roteiro.id = r.id
           where (:busca is null or :busca = ''
                  or lower(r.codigo) like lower(concat('%', :busca, '%'))
                  or lower(coalesce(r.descricao, '')) like lower(concat('%', :busca, '%'))
                  or lower(p.nome) like lower(concat('%', :busca, '%'))
                  or lower(p.codigo) like lower(concat('%', :busca, '%')))
           order by r.codigo asc
           """)
    List<Roteiro> buscar(@Param("busca") String busca);
}
