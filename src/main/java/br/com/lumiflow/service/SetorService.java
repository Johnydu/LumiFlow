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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SetorService {
    private final SetorRepository setorRepository;
    private final EtapaSetorRepository etapaSetorRepository;
    private final SetorMapper setorMapper;
    private final MaquinaRepository maquinaRepository;

    public Setor buscarSetorPorId(Long id) {
        if (id == null) {
            throw new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND);
        }
        return setorRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_SECTOR_NOTFOUND));
    }

    @Transactional(readOnly = true)
    public List<SetorListagemDTO> listarSetores() {
        try {
            List<Setor> setores = setorRepository.findAllByOrderByNomeAsc();
            return montarListagem(setores);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar setores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar setores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void novoSetor(SetorDTO dto) {
        try {
            if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }

            if (setorRepository.findByNome(normalizeName(dto.nome())).isPresent()) {
                throw new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE);
            }

            Setor setor = setorRepository.save(setorMapper.toEntity(dto));
            salvarEtapasDoSetor(setor, dto);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar novo setor: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar novo setor: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void excluirSetor(Long id) {
        try {
            Setor setor = buscarSetorPorId(id);
            if (maquinaRepository.countBySetorId(id) > 0) {
                throw new BusinessException(AppMessages.ERROR_SECTOR_HAS_MACHINES);
            }

            etapaSetorRepository.deleteAllBySetorId(id);
            setorRepository.delete(setor);
            setorRepository.flush();

        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException ex) {
            log.error("Violação de integridade ao excluir setor id {}: {}", id, ex.getMessage(), ex);
            throw new BusinessException(AppMessages.ERROR_SECTOR_IN_USE);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_SECTOR_IN_USE);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void editarSetor(Long id, SetorDTO dto) {
        try {
            if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }

            Setor setor = buscarSetorPorId(id);

            setorRepository.findByNome(normalizeName(dto.nome()))
                    .filter(s -> !s.getId().equals(id))
                    .ifPresent(s -> {
                        throw new BusinessException(AppMessages.ERROR_SECTOR_NAME_DUPLICATE);
                    });

            setor.setNome(dto.nome().trim());

            Map<Long, EtapaSetor> etapasExistentesMap = new HashMap<>();
            for (EtapaSetor etapa : setor.getEtapas()) {
                etapasExistentesMap.put(etapa.getId(), etapa);
            }

            Set<Long> etapasRecebidas = new HashSet<>();
            int ordem = 1;

            if (dto.etapas() != null) {
                for (EtapaSetorDTO etapaDto : dto.etapas()) {
                    if (etapaDto == null || etapaDto.nome() == null || etapaDto.nome().isBlank()) {
                        continue;
                    }

                    EtapaSetor etapa;
                    if (etapaDto.id() != null && etapasExistentesMap.containsKey(etapaDto.id())) {
                        etapa = etapasExistentesMap.get(etapaDto.id());
                        etapasRecebidas.add(etapaDto.id());
                    } else {
                        etapa = new EtapaSetor();
                        etapa.setSetor(setor);
                        setor.getEtapas().add(etapa);
                    }

                    etapa.setNome(etapaDto.nome().trim());
                    etapa.setOrdem(ordem++);
                }
            }

            setor.getEtapas().removeIf(etapa ->
                    etapa.getId() != null && !etapasRecebidas.contains(etapa.getId()));

            if (setor.getEtapas().isEmpty() && dto.etapas() != null && !dto.etapas().isEmpty()) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }

            setorRepository.save(setor);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao editar setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private void salvarEtapasDoSetor(Setor setor, SetorDTO dto) {
        if (dto.etapas() == null || dto.etapas().isEmpty()) return;
        for (int i = 0; i < dto.etapas().size(); i++) {
            EtapaSetorDTO etapaDto = dto.etapas().get(i);
            if (etapaDto.nome() == null || etapaDto.nome().isBlank()) continue;

            EtapaSetor etapa = new EtapaSetor();
            etapa.setNome(normalizeName(etapaDto.nome()));
            etapa.setSetor(setor);
            etapa.setOrdem(etapaDto.ordem() == null ? i + 1 : etapaDto.ordem());
            etapaSetorRepository.save(etapa);
        }
    }

    @Transactional(readOnly = true)
    public List<SetorListagemDTO> buscarPorNome(String nome) {
        try {
            if (nome == null || nome.isBlank()) {
                return listarSetores();
            }

            List<Setor> setores = setorRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(normalizeName(nome));
            return montarListagem(setores);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar setores por nome: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar setores por nome: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    /** Monta a listagem com UMA consulta de contagem de máquinas (em vez de uma por setor). */
    private List<SetorListagemDTO> montarListagem(List<Setor> setores) {
        Map<Long, Long> maquinasPorSetor = new HashMap<>();
        for (Object[] linha : maquinaRepository.contarMaquinasPorSetor()) {
            maquinasPorSetor.put(((Number) linha[0]).longValue(), ((Number) linha[1]).longValue());
        }
        return setores.stream().map(s -> new SetorListagemDTO(
                s.getId(),
                s.getNome(),
                maquinasPorSetor.getOrDefault(s.getId(), 0L).intValue(),
                setorMapper.toEtapaDtoList(s.getEtapas())
        )).toList();
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
    }
}
