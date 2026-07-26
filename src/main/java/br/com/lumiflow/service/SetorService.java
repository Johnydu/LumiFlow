package br.com.lumiflow.service;

import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.mapper.SetorMapper;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.MaquinaRepository;
import br.com.lumiflow.repository.SetorRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class SetorService {

    private final SetorRepository setorRepository;
    private final SetorMapper setorMapper;
    private final MaquinaRepository maquinaRepository;

    public Setor buscarSetorPorId(Long idSetor) {
        return setorRepository.findById(idSetor)
                .orElseThrow(() -> new BusinessException("Setor não encontrado"));
    }

    public List<SetorListagemDTO> listarSetores() {

        return setorRepository.findAllByOrderByNomeAsc().stream()
                .map(setor -> new SetorListagemDTO(
                        setor.getId(),
                        setor.getNome(),
                        setor.getPossuiEtapas(),
                        maquinaRepository.countBySetorId(setor.getId())
                )).toList();

    }

    public void novoSetor(SetorDTO setorDTO) {

        if (setorRepository.findByNome(setorDTO.nome().toUpperCase()).isPresent()){
            throw new BusinessException("Já existe setor com esse nome");
        }
        setorRepository.save(setorMapper.toEntity(setorDTO));
    }


    public void exluirSetor(Long id) {

        Setor setor = buscarSetorPorId(id);
        setorRepository.delete(setor);
    }

    public void editarSetor(long id,SetorDTO setorDTO) {

        Setor setor = buscarSetorPorId(id);

        setor.setNome(setorDTO.nome().toUpperCase());
        setor.setPossuiEtapas(setorDTO.possuiEtapas());

        setorRepository.save(setor);
    }
}
