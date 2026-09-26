package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.NivelAcessoMapper;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.repository.NivelAcessoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NivelAcessoService {
    private final NivelAcessoRepository nivelAcessoRepository;
    private final NivelAcessoMapper nivelAcessoMapper;

    @Transactional(readOnly = true)
    public NivelAcesso buscarNivelAcessoPorId(Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND);
            }
            return nivelAcessoRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar nível de acesso id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar nível de acesso id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<NivelAcessoDTO> listarTodos() {
        try {
            return nivelAcessoMapper.toListDTO(nivelAcessoRepository.findAll());
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar níveis de acesso: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar níveis de acesso: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}