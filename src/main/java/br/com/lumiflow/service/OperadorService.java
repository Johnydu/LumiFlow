package br.com.lumiflow.service;

import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.mapper.OperadorMapper;
import br.com.lumiflow.repository.OperadorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final OperadorMapper operadorMapper;


    public List<OperadorListagemDTO> listarOperadores() {

       return operadorMapper.toListDto(operadorRepository.findAllByOrderByNomeAsc());


    }
}
