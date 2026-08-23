package br.com.lumiflow.service;

import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.repository.EtapaSetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EtapaSetorService {

    private final EtapaSetorRepository etapaSetorRepository;

    public EtapaSetor buscarPorId(Long id) {

        return etapaSetorRepository.findById(id).orElse(null);
    }

    public List<EtapaSetor> listarPorSetorId(Long setorId) {

        return etapaSetorRepository.findBySetorId(setorId);
    }
}
