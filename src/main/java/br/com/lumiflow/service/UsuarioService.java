package br.com.lumiflow.service;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.dto.UsuarioListaDTO;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.repository.UsuarioRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioMapper usuarioMapper;


    public Usuario novoUsuario( UsuarioDTO usuarioDTO){
        if(usuarioRepository.findByLogin(usuarioDTO.login()).isPresent()){
            throw new BusinessException("Usuario já existente");
        }
        Usuario usuario = usuarioMapper.toEntity(usuarioDTO);
        return usuarioRepository.save(usuario);
    }

    public List<UsuarioListaDTO> listarTodos() {
         return  usuarioMapper.toListDTO(usuarioRepository.findAllByOrderByNomeAsc());
    }

    public void excluirUsuario(Long id){

        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() ->
                        new BusinessException("Usuário não encontrado."));

        usuarioRepository.delete(usuario);
    }
}
