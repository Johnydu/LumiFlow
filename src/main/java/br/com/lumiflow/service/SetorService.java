package br.com.lumiflow.service;

import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.SetorMapper;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class SetorService {

    private final SetorRepository setorRepository;
    private final SetorMapper setorMapper;

    public Setor buscarSetorPorId(Long idSetor) {
        return setorRepository.findById(idSetor)
                .orElseThrow(()-> new BusinessException("Setor não encontrado"));
    }

    public List<SetorDTO> listarSetores() {
        return setorMapper.toListDto(setorRepository.findAll());
    }
}
