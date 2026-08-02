package br.com.lumiflow.repository;

import br.com.lumiflow.model.EtapaSetor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EtapaSetorRepository extends JpaRepository<EtapaSetor, Long> {
    List<EtapaSetor> findBySetorId(Long setorId);

    void deleteAllBySetorId(long id);
}
