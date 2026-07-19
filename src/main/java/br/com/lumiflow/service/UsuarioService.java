package br.com.lumiflow.service;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.dto.usuario.UsuarioLogadoDTO;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.repository.UsuarioRepository;
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
        if (usuarioRepository.findByLogin(login).isPresent()) {
            throw new BusinessException("Usuario já existente");
        }
    }

    public Usuario validarUsuarioPorId(@NotNull Long idUsuario) {

        return usuarioRepository.findById(idUsuario)
                .orElseThrow(()-> new BusinessException("Usuário não encontrado"));

    }



    public void novoUsuario (UsuarioDTO usuarioDTO){

        validarLogin(usuarioDTO.login());

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

    public void atualizarUsuario (Long id, UsuarioEdicaoDTO usuarioEdicaoDTO) {

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado"));

        usuarioRepository.findByLogin(usuarioEdicaoDTO.login())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new BusinessException("Já existe outro usuário com esse login");
                });


        usuario.setNome(usuarioEdicaoDTO.nome());
        usuario.setLogin(usuarioEdicaoDTO.login());
        usuario.setNivelAcesso(nivelAcessoService.buscarNivelAcessoPorId(usuarioEdicaoDTO.nivelAcessoId()));
        usuario.setSetor(usuarioEdicaoDTO.setorId() != null
                ? setorService.buscarSetorPorId(usuarioEdicaoDTO.setorId())
                : null
        );
        if (usuarioEdicaoDTO.senha() != null
                && !usuarioEdicaoDTO.senha().isBlank()) {

            if (usuarioEdicaoDTO.senha().length() < 6) {
                throw new BusinessException(
                        "A senha deve ter no mínimo 6 caracteres"
                );
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
