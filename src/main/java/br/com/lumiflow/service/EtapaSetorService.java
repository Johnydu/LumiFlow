package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.EtapaSetorMapper;
import br.com.lumiflow.repository.EtapaSetorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtapaSetorService {

    private final EtapaSetorRepository etapaSetorRepository;
    private final EtapaSetorMapper etapaSetorMapper;

    @Transactional(readOnly = true)
    public EtapaSetor buscarPorId(Long id) {
        try {
            if (id == null) {
                return null;
            }
            return etapaSetorRepository.findById(id).orElse(null);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar etapa de setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar etapa de setor id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<EtapaSetorDTO> listarPorSetorId(Long setorId) {
        try {
            if (setorId == null) {
                return List.of();
            }
            return etapaSetorMapper.toListDTO(etapaSetorRepository.findBySetorIdOrderByOrdemAsc(setorId));
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar etapas por setor id {}: {}", setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar etapas por setor id {}: {}", setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}