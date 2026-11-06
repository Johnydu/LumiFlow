package br.com.lumiflow.repository;

import br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO;
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

    /** Roteiro ATUAL do produto. Usado só para pré-visualizar/criar ordem; as ordens usam o próprio roteiro. */
    @Query("""
           select p from RoteiroPasso p
           join fetch p.setor
           left join fetch p.etapaSetor
           join Produto pr on pr.roteiro.id = p.roteiro.id
           where pr.id = :produtoId
           order by p.sequencia asc
           """)
    List<RoteiroPasso> buscarPorProdutoId(@Param("produtoId") Long produtoId);

    List<RoteiroPasso> findByRoteiroIdOrderBySequenciaAsc(Long roteiroId);

    /** Etapa do setor referenciada por algum roteiro (a FK é ON DELETE SET NULL: apagar a etapa apagaria o vínculo). */
    boolean existsByEtapaSetorId(Long etapaSetorId);

    Optional<RoteiroPasso> findByRoteiroIdAndSequencia(Long roteiroId, Integer sequencia);

    @Query("""
           select p from RoteiroPasso p
           where p.roteiro.id = :roteiroId and p.sequencia > :sequenciaAtual
           order by p.sequencia asc
           limit 1
           """)
    Optional<RoteiroPasso> buscarProximoPasso(@Param("roteiroId") Long roteiroId,
                                              @Param("sequenciaAtual") Integer sequenciaAtual);

    @Modifying(flushAutomatically = true)
    @Query("delete from RoteiroPasso p where p.roteiro.id = :roteiroId")
    void deletarPorRoteiroId(@Param("roteiroId") Long roteiroId);

    @Query("""
           select new br.com.lumiflow.dto.roteiro.PassoDoRoteiroDTO(p.roteiro.id, p.sequencia, e.nome)
           from RoteiroPasso p
           left join p.etapaSetor e
           where p.roteiro.id in :roteiroIds
           """)
    List<PassoDoRoteiroDTO> buscarPassosPorRoteiroIds(@Param("roteiroIds") Collection<Long> roteiroIds);

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
           where p.roteiro.id = :roteiroId
           order by p.sequencia asc
           """)
    List<RoteiroPasso> buscarPassosComSetorEEtapa(@Param("roteiroId") Long roteiroId);
}
