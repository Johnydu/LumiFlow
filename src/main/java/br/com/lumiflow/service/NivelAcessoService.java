package br.com.lumiflow.service;

import br.com.lumiflow.dto.NivelAcessoDTO;
import br.com.lumiflow.mapper.NiveAcessoMapper;
import br.com.lumiflow.repository.NivelAcessoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class NivelAcessoService {
    private final NivelAcessoRepository nivelAcessoRepository;
    private final NiveAcessoMapper niveAcessoMapper;

    public List<NivelAcessoDTO> listarTodos() {
        return niveAcessoMapper.toListDTO(nivelAcessoRepository.findAll());
    }
}
