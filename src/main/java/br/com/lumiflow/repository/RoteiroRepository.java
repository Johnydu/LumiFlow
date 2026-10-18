package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Roteiro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoteiroRepository extends JpaRepository<Roteiro, Long> {

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
