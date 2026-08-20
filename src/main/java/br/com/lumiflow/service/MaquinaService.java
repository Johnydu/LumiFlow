package br.com.lumiflow.service;

import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.model.Maquina;
import br.com.lumiflow.mapper.MaquinaMapper;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.MaquinaRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MaquinaMapper maquinaMapper;
    private final SetorService setorService;


    @Transactional(readOnly = true)
    public List<MaquinaDTO> listarMaquinas(){
       return maquinaMapper.toDtoList(maquinaRepository.findAllByOrderByNomeAsc());

    }

    @Transactional
    public void novaMaquina(@Valid MaquinaDTO maquinaDTO) {
        if(maquinaRepository.findByNome(maquinaDTO.nome()).isPresent()){
            throw new BusinessException("Já existe uma máquina cadastrada com esse nome '" +
                            maquinaDTO.nome() + "'"
            );
        }
        Maquina  maquina = maquinaMapper.toEntity(maquinaDTO);
        maquina.setSetor(setorService.buscarSetorPorId(maquinaDTO.setorId()));

        maquinaRepository.save(maquina);
    }


    @Transactional
    public void deletarMaquina(long id) {
        Maquina maquina = maquinaRepository.findById(id).orElseThrow(
                ()-> new BusinessException("Maquina não encontrada"));

        maquinaRepository.delete(maquina);
    }

    @Transactional
    public void editarMaquina(Long id, MaquinaDTO maquinaDTO) {
        Maquina maquina = maquinaRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Máquina não encontrada"));

        maquinaRepository.findByNome(maquinaDTO.nome())
                .filter(outra -> !outra.getId().equals(id))
                .ifPresent(outra -> {
                    throw new BusinessException(
                            "Já existe uma máquina cadastrada com esse nome"
                    );
                });

        maquina.setNome(maquinaDTO.nome().trim().toUpperCase(Locale.ROOT));
        maquina.setSetor(setorService.buscarSetorPorId(maquinaDTO.setorId()));

        maquinaRepository.save(maquina);

    }
}
