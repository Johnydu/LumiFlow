package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.*;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.repository.UsuarioRepository;
import br.com.lumiflow.validation.LoginValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.context.SecurityContextHolder;
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
            validarLogin(dto.login());
            validarSenha(dto.senha());

            Usuario usuario = usuarioMapper.toEntity(dto);
            usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId()));
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
    public List<UsuarioListaDTO> listarTodos() {
        try {
            return usuarioMapper.toListDTO(usuarioRepository.findAll());
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao listar utilizadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao listar utilizadores: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Transactional
    public void excluirUsuario(Long id) {
        try {
            Usuario usuario = validarUsuarioPorId(id);
            usuarioRepository.delete(usuario);
        } catch (BusinessException e) {
            throw e;
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

            if (dto.login() == null || !LoginValidator.isValidLogin(dto.login())) {
                throw new BusinessException(AppMessages.ERROR_USER_LOGIN_INVALID);
            }

            usuarioRepository.findByLogin(dto.login().trim())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        throw new BusinessException(AppMessages.ERROR_USER_ALREADY_EXISTS);
                    });

            usuario.setNome(dto.nome());
            usuario.setLogin(dto.login().trim());
            usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(dto.nivelAcessoId()));
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

    public UsuarioLogadoDTO obterUsuarioLogado() {
        try {
            Usuario usuario = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            return new UsuarioLogadoDTO(usuario.getNome(), usuario.getNivelAcesso().getDescricao().name());
        } catch (Exception e) {
            log.error("Erro ao obter o utilizador logado: {}", e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_USER_NOTFOUND);
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || !LoginValidator.isValidPassword(senha)) {
            throw new BusinessException(AppMessages.ERROR_USER_PASSWORD_WEAK);
        }
    }
}