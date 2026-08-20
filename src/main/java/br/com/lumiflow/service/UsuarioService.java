package br.com.lumiflow.service;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.model.Usuario;
import br.com.lumiflow.repository.UsuarioRepository;
import br.com.lumiflow.validation.LoginValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@AllArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;

    /**
     * Valida se o login é válido e único
     */
    public void validarLogin(@NotNull @Valid String login) {
        log.debug("Validando formato e unicidade do login: {}", login);
        
        if (!LoginValidator.isValidLogin(login)) {
            log.warn("Login inválido (formato): {}", login);
            throw new BusinessException("Login inválido: use apenas letras, números, hífens e underscores (3-50 caracteres)");
        }
        
        if (usuarioRepository.findByLogin(login.trim()).isPresent()) {
            log.warn("Tentativa de criar usuário com login duplicado: {}", login);
            throw new BusinessException("Usuario já existente");
        }
        
        log.debug("Login validado com sucesso: {}", login);
    }

    /**
     * Busca usuário por ID com validação
     */
    public Usuario validarUsuarioPorId(@NotNull Long idUsuario) {
        log.debug("Buscando usuário por ID: {}", idUsuario);
        
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> {
                    log.warn("Usuário não encontrado. ID: {}", idUsuario);
                    return new BusinessException("Usuário não encontrado");
                });
    }

    /**
     * Cria novo usuário com validações de negócio
     */
    @Transactional
    public void novoUsuario(UsuarioDTO usuarioDTO) {
        log.info("Iniciando criação de novo usuário: {}", usuarioDTO.login());
        
        try {
            validarLogin(usuarioDTO.login());
            
            if (!LoginValidator.isValidPassword(usuarioDTO.senha())) {
                log.warn("Senha fraca para usuário: {}", usuarioDTO.login());
                throw new BusinessException(LoginValidator.getPasswordRequirements());
            }

            Usuario usuario = usuarioMapper.toEntity(usuarioDTO);
            usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(usuarioDTO.nivelAcessoId()));
            usuario.setSetor(
                    usuarioDTO.setorId() != null
                            ? setorService.buscarSetorPorId(usuarioDTO.setorId())
                            : null
            );
            usuario.setSenha(passwordEncoder.encode(usuarioDTO.senha()));

            Usuario usuarioCriado = usuarioRepository.save(usuario);
            
            log.info("Usuário criado com sucesso. ID: {}, Login: {}, Nível: {}", 
                     usuarioCriado.getId(), usuarioCriado.getLogin(), 
                     usuarioCriado.getNivelAcesso().getDescricao());
            
        } catch (BusinessException ex) {
            log.warn("Erro ao criar usuário: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Erro inesperado ao criar usuário: {}. Login: {}", 
                      ex.getMessage(), usuarioDTO.login(), ex);
            throw new BusinessException("Erro ao criar usuário");
        }
    }

    /**
     * Lista todos os usuários
     */
    @Transactional(readOnly = true)
    public List<UsuarioListaDTO> listarTodos() {
        log.debug("Listando todos os usuários");
        
        try {
            List<UsuarioListaDTO> usuarios = usuarioMapper.toListDTO(usuarioRepository.findAll());
            log.info("Total de usuários carregados: {}", usuarios.size());
            return usuarios;
            
        } catch (Exception ex) {
            log.error("Erro ao listar usuários: {}", ex.getMessage(), ex);
            throw new BusinessException("Erro ao listar usuários");
        }
    }

    /**
     * Exclui usuário por ID
     */
    @Transactional
    public void excluirUsuario(Long id) {
        log.info("Excluindo usuário ID: {}", id);
        
        try {
            Usuario usuario = validarUsuarioPorId(id);
            
            usuarioRepository.delete(usuario);
            
            log.info("Usuário excluído com sucesso. ID: {}, Login: {}", 
                     id, usuario.getLogin());
            
        } catch (Exception ex) {
            log.error("Erro ao excluir usuário ID: {}. Mensagem: {}", 
                      id, ex.getMessage(), ex);
            throw new BusinessException("Erro ao excluir usuário");
        }
    }

    /**
     * Edita usuário existente
     */
    @Transactional
    public void editarUsuario(Long id, UsuarioEdicaoDTO usuarioEdicaoDTO) {
        log.info("Editando usuário ID: {}", id);
        
        try {
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Usuário não encontrado para edição. ID: {}", id);
                        return new BusinessException("Usuário não encontrado");
                    });

            if (!LoginValidator.isValidLogin(usuarioEdicaoDTO.login())) {
                log.warn("Login inválido ao editar usuário ID: {}. Login: {}", 
                         id, usuarioEdicaoDTO.login());
                throw new BusinessException("Login inválido: use apenas letras, números, hífens e underscores (3-50 caracteres)");
            }

            usuarioRepository.findByLogin(usuarioEdicaoDTO.login())
                    .filter(outro -> !outro.getId().equals(id))
                    .ifPresent(outro -> {
                        log.warn("Tentativa de alterar para login duplicado. ID: {}, Login: {}", 
                                 id, usuarioEdicaoDTO.login());
                        throw new BusinessException("Já existe outro usuário com esse login");
                    });

            usuario.setNome(usuarioEdicaoDTO.nome().toUpperCase(Locale.ROOT));
            usuario.setLogin(usuarioEdicaoDTO.login().trim());
            usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(usuarioEdicaoDTO.nivelAcessoId()));
            usuario.setSetor(usuarioEdicaoDTO.setorId() != null
                    ? setorService.buscarSetorPorId(usuarioEdicaoDTO.setorId())
                    : null
            );
            
            if (usuarioEdicaoDTO.senha() != null && !usuarioEdicaoDTO.senha().isBlank()) {
                if (!LoginValidator.isValidPassword(usuarioEdicaoDTO.senha())) {
                    log.warn("Senha fraca ao editar usuário ID: {}", id);
                    throw new BusinessException(LoginValidator.getPasswordRequirements());
                }
                usuario.setSenha(passwordEncoder.encode(usuarioEdicaoDTO.senha()));
                log.debug("Senha atualizada para usuário ID: {}", id);
            }
            
            usuarioRepository.save(usuario);
            
            log.info("Usuário editado com sucesso. ID: {}, Login: {}", id, usuario.getLogin());
            
        } catch (BusinessException ex) {
            log.warn("Erro ao editar usuário ID: {}. Mensagem: {}", id, ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Erro inesperado ao editar usuário ID: {}. Mensagem: {}", 
                      id, ex.getMessage(), ex);
            throw new BusinessException("Erro ao editar usuário");
        }
    }

    /**
     * Obtém dados do usuário autenticado
     */
    public UsuarioLogadoDTO obterUsuarioLogado() {
        log.debug("Obtendo dados do usuário autenticado");
        
        try {
            Usuario usuario = (Usuario) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();
            
            log.debug("Usuário logado: {}, Nível: {}", 
                     usuario.getId(), usuario.getNivelAcesso().getDescricao());

            return new UsuarioLogadoDTO(usuario.getNome(),
                    usuario.getNivelAcesso().getDescricao().name());
                    
        } catch (Exception ex) {
            log.error("Erro ao obter usuário logado: {}", ex.getMessage(), ex);
            throw new BusinessException("Erro ao obter dados do usuário");
        }
    }
}
