package br.com.lumiflow.service;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.MaquinaMapper;
import br.com.lumiflow.repository.MaquinaRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MaquinaMapper maquinaMapper;



    public List<MaquinaDTO> listarMaquinas(){
       return maquinaMapper.toDtoList(maquinaRepository.findAllByOrderByNomeAsc());

    }

    public void novaMaquina(@Valid MaquinaDTO maquinaDTO) {
        if(maquinaRepository.findByNome(maquinaDTO.nome()).isPresent()){
            throw new BusinessException("Já existe uma máquina cadastrada com o nome '" +
                            maquinaDTO.nome() + "'"
            );
        }
        maquinaRepository.save(maquinaMapper.toEntity(maquinaDTO));
    }


    public void deletarMaquina(long id) {
        Maquina maquina = maquinaRepository.findById(id).orElseThrow(
                ()-> new BusinessException("Maquina não encontrada"));

        maquinaRepository.delete(maquina);
    }
}
