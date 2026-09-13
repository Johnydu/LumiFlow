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
        return setorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public List<SetorListagemDTO> listarSetores() {
        List<Setor> setores = setorRepository.findAllByOrderByNomeAsc();
        return setores.stream().map(s -> new SetorListagemDTO(
                s.getId(),
                s.getNome(),
                maquinaRepository.countBySetorId(s.getId()),
                setorMapper.toEtapaDtoList(s.getEtapas())
        )).toList();
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
        setorRepository.findByNome(dto.nome().toUpperCase(Locale.ROOT)).filter(s -> !s.getId()
                .equals(id)).ifPresent(s -> {
            throw new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE);
        });
        setor.setNome(dto.nome().trim().toUpperCase(Locale.ROOT));

        // Remove o campo obsoleto possuiEtapas e salva as etapas dinamicamente enviadas
        setorRepository.save(setor);

        etapaSetorRepository.deleteAllBySetorId(id);
        salvarEtapasDoSetor(setor, dto);
    }

    private void salvarEtapasDoSetor(Setor setor, SetorDTO dto) {
        if (dto.etapas() == null || dto.etapas().isEmpty()) return;
        for (int i = 0; i < dto.etapas().size(); i++) {
            EtapaSetorDTO etapaDto = dto.etapas().get(i);
            if (etapaDto.nome() == null || etapaDto.nome().isBlank()) continue;

            EtapaSetor etapa = new EtapaSetor();
            etapa.setNome(etapaDto.nome().trim());
            etapa.setSetor(setor);
            etapa.setOrdem(etapaDto.ordem() == null ? i + 1 : etapaDto.ordem());
            etapaSetorRepository.save(etapa);
        }
    }

    @Transactional(readOnly = true)
    public List<SetorListagemDTO> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return listarSetores();
        }

        List<Setor> setores = setorRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome.trim());
        return setores.stream().map(s -> new SetorListagemDTO(
                s.getId(),
                s.getNome(),
                maquinaRepository.countBySetorId(s.getId()),
                setorMapper.toEtapaDtoList(s.getEtapas())
        )).toList();
    }
}
