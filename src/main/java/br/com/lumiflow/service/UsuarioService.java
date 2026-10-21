package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.*;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.repository.UsuarioRepository;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.validation.LoginValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;

    private void validarLogin(String login) {
        if (login == null || login.isBlank() || !LoginValidator.isValidLogin(login)) {
            throw new BusinessException(AppMessages.ERROR_USER_LOGIN_INVALID);
        }
        if (usuarioRepository.findByLogin(login.trim()).isPresent()) {
            throw new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS);
        }
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 50) {
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private void validarSetorObrigatorio(NivelAcesso nivelAcesso, Long setorId) {
        if (nivelAcesso.getDescricao() == Descricao.OPERADOR && setorId == null) {
            throw new BusinessException(AppMessages.ERROR_USER_OPERATOR_SECTOR_REQUIRED);
        }
    }

    private void validarQueNaoEUltimoAdmin(Usuario usuario) {
        if (usuario.getNivelAcesso().getDescricao() == Descricao.ADMIN
                && usuarioRepository.findAllByNivelAcessoForUpdate(Descricao.ADMIN).size() <= 1) {
            throw new BusinessException(AppMessages.ERROR_USER_LAST_ADMIN);
        }
    }

    private Usuario validarUsuarioPorId(Long id) {
        if (id == null) {
            throw new BusinessException(AppMessages.ERROR_USER_NOTFOUND);
        }
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new BusinessException(AppMessages.ERROR_USER_NOTFOUND));
    }

    @Transactional
    public void novoUsuario(UsuarioDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            validarNome(dto.nome());
            validarLogin(dto.login());
            validarSenha(dto.senha());

            Usuario usuario = usuarioMapper.toEntity(dto);
            NivelAcesso nivelAcesso = nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId());
            validarSetorObrigatorio(nivelAcesso, dto.setorId());
            usuario.setNivelAcesso(nivelAcesso);
            usuario.setLogin(dto.login().trim());
            usuario.setSetor(dto.setorId() == null ? null : setorService.buscarSetorPorId(dto.setorId()));
            usuario.setSenha(passwordEncoder.encode(dto.senha()));

            usuarioRepository.save(usuario);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao criar novo utilizador: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar novo utilizador: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional(readOnly = true)
    public List<UsuarioListaDTO> buscar(String termo) {
        try {
            if (termo == null || termo.isBlank()) {
                return usuarioMapper.toListDTO(usuarioRepository.findAllByOrderByNomeAsc());
            }
            String busca = termo.trim();
            return usuarioMapper.toListDTO(usuarioRepository
                    .findByNomeContainingIgnoreCaseOrLoginContainingIgnoreCaseOrderByNomeAsc(busca, busca));
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar utilizadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar utilizadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void excluirUsuario(Long id, Long usuarioAtualId) {
        try {
            Usuario usuario = validarUsuarioPorId(id);
            if (usuario.getId().equals(usuarioAtualId)) {
                throw new BusinessException(AppMessages.ERROR_USER_CANNOT_DELETE_SELF);
            }
            validarQueNaoEUltimoAdmin(usuario);
            usuarioRepository.delete(usuario);
            usuarioRepository.flush();
        } catch (BusinessException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            log.error("Violação de integridade ao excluir o utilizador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_RECORD_IN_USE);
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao excluir o utilizador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir o utilizador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void editarUsuario(Long id, UsuarioEdicaoDTO dto) {
        try {
            if (dto == null) {
                throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
            }
            Usuario usuario = validarUsuarioPorId(id);
            validarNome(dto.nome());

            if (dto.login() == null || !LoginValidator.isValidLogin(dto.login())) {
                throw new BusinessException(AppMessages.ERROR_USER_LOGIN_INVALID);
            }

            usuarioRepository.findByLogin(dto.login().trim())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS);
                    });

            NivelAcesso nivelAcesso = nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId());
            validarSetorObrigatorio(nivelAcesso, dto.setorId());
            if (usuario.getNivelAcesso().getDescricao() == Descricao.ADMIN
                    && nivelAcesso.getDescricao() != Descricao.ADMIN) {
                validarQueNaoEUltimoAdmin(usuario);
            }

            usuario.setNome(dto.nome().trim());
            usuario.setLogin(dto.login().trim());
            usuario.setNivelAcesso(nivelAcesso);
            usuario.setSetor(dto.setorId() == null ? null : setorService.buscarSetorPorId(dto.setorId()));

            if (dto.senha() != null && !dto.senha().isBlank()) {
                validarSenha(dto.senha());
                usuario.setSenha(passwordEncoder.encode(dto.senha()));
            }

            usuarioRepository.save(usuario);

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao editar o utilizador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar o utilizador id {}: {}", id, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || !LoginValidator.isValidPassword(senha)) {
            throw new BusinessException(AppMessages.ERROR_USER_PASSWORD_WEAK);
        }
    }
}