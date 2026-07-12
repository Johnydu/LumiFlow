package br.com.lumiflow.service;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.dto.UsuarioListaDTO;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.repository.UsuarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;
    private final PasswordEncoder passwordEncoder;


    public Usuario novoUsuario(@NotNull @Valid UsuarioDTO usuarioDTO){
        if(usuarioRepository.findByLogin(usuarioDTO.login()).isPresent()){
            throw new BusinessException("Usuario já existente");
        }
        Usuario usuario = usuarioMapper.toEntity(usuarioDTO);
        usuario.setSenha(passwordEncoder.encode(usuarioDTO.senha()));
        return usuarioRepository.save(usuario);
    }

    public List<UsuarioListaDTO> listarTodos() {
         return  usuarioMapper.toListDTO(usuarioRepository.findAll());
    }

    public void excluirUsuario(Long id){

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado."));

        usuarioRepository.delete(usuario);
    }

    public void atualizarUsuario(Long id, @Valid UsuarioDTO usuarioDTO) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(()-> new BusinessException("Usuário não encontrado."));

        usuarioRepository.findByLogin(usuarioDTO.login())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new BusinessException("Já existe outro usuário com esse login");
                });

        usuario.setLogin(usuarioDTO.login());
        usuario.setNome(usuarioDTO.nome());
        usuario.setNivelAcesso(usuarioDTO.nivelAcesso());
        usuario.setSetor(usuarioDTO.setor());
        if (usuarioDTO.senha() != null && !usuarioDTO.senha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(usuarioDTO.senha()));
        }

        usuarioRepository.save(usuario);

    }
}
