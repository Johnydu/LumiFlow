package br.com.lumiflow.service;

import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.exception.BusinessException;
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

    public NivelAcesso buscarNivelAcessoPorId(Long id) {
        return nivelAcessoRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Nível de acesso não encontrado"));
    }

    public List<NivelAcessoDTO> listarTodos() {
        return niveAcessoMapper.toListDTO(nivelAcessoRepository.findAll());
    }
}
