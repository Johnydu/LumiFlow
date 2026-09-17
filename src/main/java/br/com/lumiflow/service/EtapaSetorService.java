package br.com.lumiflow.service;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.mapper.EtapaSetorMapper;
import br.com.lumiflow.repository.EtapaSetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class EtapaSetorService {

    private final EtapaSetorRepository etapaSetorRepository;
    private final EtapaSetorMapper etapaSetorMapper;

    public EtapaSetor buscarPorId(Long id) {

        return etapaSetorRepository.findById(id).orElse(null);
    }

    public List<EtapaSetorDTO> listarPorSetorId(Long setorId) {

        return etapaSetorMapper.toListDTO(etapaSetorRepository.findBySetorIdOrderByOrdemAsc(setorId));
    }
}
