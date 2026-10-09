package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.MaquinaMapper;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.repository.MaquinaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaquinaService {

    private final MaquinaRepository maquinaRepository;
    private final MaquinaMapper maquinaMapper;
    private final SetorService setorService;

    @Transactional(readOnly = true)
    public List<MaquinaDTO> listarMaquinas() {
        try {
            return maquinaMapper.toDtoList(maquinaRepository.findAllByOrderByNomeAsc());
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar máquinas: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar máquinas: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void novaMaquina(MaquinaDTO dto) {
        try {
            if (dto == null || dto.nome() == null || dto.nome().isBlank()) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            String nomeTrimUppercase = normalizeName(dto.nome());
            if (maquinaRepository.findByNome(nomeTrimUppercase).isPresent()) {
                throw new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE);
            }

            Maquina maquina = maquinaMapper.toEntity(dto);
            maquina.setNome(nomeTrimUppercase);
            if (dto.setorId() != null) {
                maquina.setSetor(setorService.buscarSetorPorId(dto.setorId()));
            }

            maquinaRepository.save(maquina);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar nova máquina: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar nova máquina: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void deletarMaquina(Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND);
            }
            Maquina maquina = buscarPorId(id);
            maquinaRepository.delete(maquina);
            maquinaRepository.flush();
        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Violação de integridade ao excluir máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_RECORD_IN_USE);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void editarMaquina(Long id, MaquinaDTO dto) {
        try {
            if (id == null || dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            Maquina maquina = buscarPorId(id);

            if (dto.nome() != null && !dto.nome().isBlank()) {
                maquinaRepository.findByNome(normalizeName(dto.nome()))
                        .filter(outra -> !outra.getId().equals(id))
                        .ifPresent(outra -> {
                            throw new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE);
                        });
            }

            if (dto.setorId() != null) {
                maquina.setSetor(setorService.buscarSetorPorId(dto.setorId()));
            }

            maquinaRepository.save(maquina);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao editar máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public Maquina buscarPorId(Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND);
            }
            return maquinaRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar máquina id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND);
        }
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim().toUpperCase(Locale.ROOT);
    }
}