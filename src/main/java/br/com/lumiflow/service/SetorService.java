package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.SetorMapper;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.repository.EtapaSetorRepository;
import br.com.lumiflow.repository.MaquinaRepository;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class SetorService {
    private final SetorRepository setorRepository;
    private final EtapaSetorRepository etapaSetorRepository;
    private final SetorMapper setorMapper;
    private final MaquinaRepository maquinaRepository;

    public Setor buscarSetorPorId(Long id) {
        return setorRepository.findById(id).orElseThrow(() -> new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public List<SetorListagemDTO> listarSetores() {
        return setorRepository.findAllByOrderByNomeAsc().stream().map(s -> new SetorListagemDTO(s.getId(), s.getNome(), s.getPossuiEtapas(), maquinaRepository.countBySetorId(s.getId()))).toList();
    }

    @Transactional
    public void novoSetor(SetorDTO dto) {
        if (setorRepository.findByNome(dto.nome().toUpperCase(Locale.ROOT)).isPresent())
            throw new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE);
        Setor setor = setorRepository.save(setorMapper.toEntity(dto));
        salvarEtapasDoSetor(setor, dto);
    }

    @Transactional
    public void excluirSetor(Long id) {
        Setor setor = buscarSetorPorId(id);
        if (maquinaRepository.countBySetorId(id) > 0)
            throw new BusinessException(AppMessages.ERROR_SECTOR_HAS_MACHINES);
        try {
            etapaSetorRepository.deleteAllBySetorId(id);
            setorRepository.delete(setor);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(AppMessages.ERROR_SECTOR_IN_USE);
        }
    }

    @Transactional
    public void editarSetor(long id, SetorDTO dto) {
        Setor setor = buscarSetorPorId(id);
        setorRepository.findByNome(dto.nome().toUpperCase(Locale.ROOT)).filter(s -> !s.getId().equals(id)).ifPresent(s -> {
            throw new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE);
        });
        setor.setNome(dto.nome().trim().toUpperCase(Locale.ROOT));
        setor.setPossuiEtapas(dto.possuiEtapas());
        setorRepository.save(setor);
        etapaSetorRepository.deleteAllBySetorId(id);
        if (Boolean.TRUE.equals(dto.possuiEtapas())) salvarEtapasDoSetor(setor, dto);
    }

    private void salvarEtapasDoSetor(Setor setor, SetorDTO dto) {
        if (dto.etapas() == null) return;
        for (int i = 0; i < dto.etapas().size(); i++) {
            EtapaSetorDTO etapaDto = dto.etapas().get(i);
            EtapaSetor etapa = new EtapaSetor();
            etapa.setNome(etapaDto.nome());
            etapa.setSetor(setor);
            etapa.setOrdem(etapaDto.ordem() == null ? i + 1 : etapaDto.ordem());
            etapaSetorRepository.save(etapa);
        }
    }


}
