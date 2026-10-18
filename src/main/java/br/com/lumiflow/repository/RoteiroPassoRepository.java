package br.com.lumiflow.repository;

import br.com.lumiflow.dto.roteiro.PassoDoProdutoDTO;
import br.com.lumiflow.entity.RoteiroPasso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoteiroPassoRepository extends JpaRepository<RoteiroPasso, Long> {

    @Query("""
           select p from RoteiroPasso p
           join Produto pr on pr.roteiro.id = p.roteiro.id
           where pr.id = :produtoId
           order by p.sequencia asc
           """)
    List<RoteiroPasso> buscarPorProdutoId(@Param("produtoId") Long produtoId);

    @Query("""
           select p from RoteiroPasso p
           join Produto pr on pr.roteiro.id = p.roteiro.id
           where pr.id = :produtoId and p.sequencia = :sequencia
           """)
    Optional<RoteiroPasso> findFirstByProdutoIdAndSequencia(@Param("produtoId") Long produtoId,
                                                            @Param("sequencia") Integer sequencia);

    @Query("""
           select p from RoteiroPasso p
           join Produto pr on pr.roteiro.id = p.roteiro.id
           where pr.id = :produtoId and p.sequencia > :sequenciaAtual
           order by p.sequencia asc
           limit 1
           """)
    Optional<RoteiroPasso> findProximoPasso(@Param("produtoId") Long produtoId,
                                            @Param("sequenciaAtual") Integer sequenciaAtual);

    @Modifying(flushAutomatically = true)
    @Query("delete from RoteiroPasso p where p.roteiro.id = :roteiroId")
    void deletarPorRoteiroId(@Param("roteiroId") Long roteiroId);

    @Query("""
           select new br.com.lumiflow.dto.roteiro.PassoDoProdutoDTO(pr.id, p.sequencia, e.nome)
           from RoteiroPasso p
           join Produto pr on pr.roteiro.id = p.roteiro.id
           left join p.etapaSetor e
           where pr.id in :produtoIds
           """)
    List<PassoDoProdutoDTO> buscarPassosPorProdutoIds(@Param("produtoIds") Collection<Long> produtoIds);

    List<RoteiroPasso> findByRoteiroIdOrderBySequenciaAsc(Long roteiroId);

    @Query("""
       select p from RoteiroPasso p
       join fetch p.setor
       left join fetch p.etapaSetor
       where p.roteiro.id in :roteiroIds
       order by p.roteiro.id, p.sequencia
       """)
    List<RoteiroPasso> buscarPassosDosRoteiros(@Param("roteiroIds") Collection<Long> roteiroIds);

    @Query("""
       select p from RoteiroPasso p
       join fetch p.setor
       left join fetch p.etapaSetor
       join Produto pr on pr.roteiro.id = p.roteiro.id
       where pr.id = :produtoId
       order by p.sequencia asc
       """)
    List<RoteiroPasso> buscarPassosComSetorEEtapa(@Param("produtoId") Long produtoId);

}