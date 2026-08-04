package br.com.lumiflow.service;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.model.Usuario;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.UsuarioRepository;
import br.com.lumiflow.validation.LoginValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;
    private final NivelAcessoService nivelAcessoService;
    private final SetorService setorService;

    public void validarLogin(@NotNull @Valid String login) {
        // Validar formato do login (previne SQL injection e XSS)
        if (!LoginValidator.isValidLogin(login)) {
            throw new BusinessException("Login inválido: use apenas letras, números, hífens e underscores (3-50 caracteres)");
        }
        
        if (usuarioRepository.findByLogin(login.trim()).isPresent()) {
            throw new BusinessException("Usuario já existente");
        }
    }

    public Usuario validarUsuarioPorId(@NotNull Long idUsuario) {

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(()-> new BusinessException("Usuário não encontrado"));

    }



    public void novoUsuario (UsuarioDTO usuarioDTO){

        validarLogin(usuarioDTO.login());
        
        // Validar força da senha
        if (!LoginValidator.isValidPassword(usuarioDTO.senha())) {
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

       usuarioRepository.save(usuario);
    }

    public List<UsuarioListaDTO> listarTodos () {
        return usuarioMapper.toListDTO(usuarioRepository.findAll());
    }

    public void excluirUsuario (Long id){

        Usuario usuario = validarUsuarioPorId(id);

        usuarioRepository.delete(usuario);
    }

    public void editarUsuario(Long id, UsuarioEdicaoDTO usuarioEdicaoDTO) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado"));

        // Validar novo login
        if (!LoginValidator.isValidLogin(usuarioEdicaoDTO.login())) {
            throw new BusinessException("Login inválido: use apenas letras, números, hífens e underscores (3-50 caracteres)");
        }

        usuarioRepository.findByLogin(usuarioEdicaoDTO.login())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new BusinessException("Já existe outro usuário com esse login");
                });


        usuario.setNome(usuarioEdicaoDTO.nome());
        usuario.setLogin(usuarioEdicaoDTO.login().trim());
        usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(usuarioEdicaoDTO.nivelAcessoId()));
        usuario.setSetor(usuarioEdicaoDTO.setorId() != null
                ? setorService.buscarSetorPorId(usuarioEdicaoDTO.setorId())
                : null
        );
        if (usuarioEdicaoDTO.senha() != null
                && !usuarioEdicaoDTO.senha().isBlank()) {

            // Validar força da senha
            if (!LoginValidator.isValidPassword(usuarioEdicaoDTO.senha())) {
                throw new BusinessException(LoginValidator.getPasswordRequirements());
            }
            usuario.setSenha(passwordEncoder.encode(usuarioEdicaoDTO.senha()));

        }
        usuarioRepository.save(usuario);
    }

        public UsuarioLogadoDTO obterUsuarioLogado() {

            Usuario usuario = (Usuario) SecurityContextHolder
                    .getContext()
                    .getAuthentication()
                    .getPrincipal();

            return new UsuarioLogadoDTO(usuario.getNome(),
                    usuario.getNivelAcesso().getDescricao().name());
        }

    }
