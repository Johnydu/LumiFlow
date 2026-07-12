package br.com.lumiflow.service;

import br.com.lumiflow.dto.SetorDTO;
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

    public List<SetorDTO> listarTodos() {
        return setorMapper.toListDto(setorRepository.findAll());
    }
}
