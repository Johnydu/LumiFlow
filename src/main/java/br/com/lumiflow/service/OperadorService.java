package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.OperadorMapper;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.repository.OperadorRepository;
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
public class OperadorService {
    private final OperadorRepository operadorRepository;
    private final OperadorMapper operadorMapper;
    private final SetorService setorService;

    public void validarNomeOperador(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
        if (operadorRepository.existsByNome(nome.trim().toUpperCase(Locale.ROOT))) {
            throw new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);
        }
    }

    public void validarNomeOperadorParaEdicao(String nome, Long id) {
        if (nome != null && !nome.isBlank() && id != null) {
            if (operadorRepository.existsByNomeAndIdNot(nome.trim().toUpperCase(Locale.ROOT), id)) {
                throw new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE);
            }
        }
    }

    public Operador buscarOperadorPorId(Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND);
            }
            return operadorRepository.findById(id)
                    .orElseThrow(() -> new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND));
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<OperadorListagemDTO> listarOperadores() {
        try {
            return operadorMapper.toListDto(operadorRepository.findAllByOrderByNomeAsc());
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar operadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar operadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void novoOperador(OperadorDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            validarNomeOperador(dto.nome());

            Operador operador = operadorMapper.toEntity(dto);
            if (dto.nome() != null) {
                operador.setNome(dto.nome().trim().toUpperCase(Locale.ROOT));
            }
            operador.setFuncao(normalizarFuncao(dto.funcao()));
            if (dto.setorPadraoId() != null) {
                operador.setSetorPadrao(setorService.buscarSetorPorId(dto.setorPadraoId()));
            }

            operadorRepository.save(operador);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar novo operador: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar novo operador: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void excluirOperador(Long id) {
        try {
            if (id == null) {
                throw new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND);
            }
            Operador operador = buscarOperadorPorId(id);
            operadorRepository.delete(operador);
            operadorRepository.flush();
        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Violação de integridade ao excluir operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_RECORD_IN_USE);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void editarOperador(Long id, OperadorDTO dto) {
        try {
            if (id == null || dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            Operador operador = buscarOperadorPorId(id);

            validarNomeOperadorParaEdicao(dto.nome(), id);

            if (dto.nome() != null && !dto.nome().isBlank()) {
                operador.setNome(dto.nome().trim().toUpperCase(Locale.ROOT));
            }
            operador.setFuncao(normalizarFuncao(dto.funcao()));
            operador.setSetorPadrao(dto.setorPadraoId() == null ? null : setorService.buscarSetorPorId(dto.setorPadraoId()));

            operadorRepository.save(operador);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao editar operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar operador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    /** Mesma regra no cadastro e na edição: sem espaços nas pontas, em maiúsculas, vazio vira null. */
    private String normalizarFuncao(String funcao) {
        return funcao == null || funcao.isBlank() ? null : funcao.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<OperadorListagemDTO> buscarOperadores(String nome, Long setorId) {
        try {
            // Trata strings vazias vindas do form
            String termoBusca = (nome == null || nome.isBlank()) ? null : nome.trim();

            // Se ambos forem nulos ou vazios, retorna todos
            if (termoBusca == null && setorId == null) {
                return listarOperadores();
            }

            List<Operador> operadores = operadorRepository.buscarPorNomeESetor(termoBusca, setorId);
            return operadorMapper.toListDto(operadores);
        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar operadores por nome '{}' e setor id {}: {}", nome, setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar operadores por nome '{}' e setor id {}: {}", nome, setorId, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}