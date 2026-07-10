package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.dto.UsuarioListaDTO;
import br.com.lumiflow.entity.Usuario;
import org.mapstruct.Mapper;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    Usuario toEntity(UsuarioDTO usuarioDto);
    UsuarioDTO toDTO(Usuario usuario);
    Usuario toEntity(UsuarioListaDTO usuarioListaDTO);
    UsuarioListaDTO toUsuarioListaDTO(Usuario usuario);
    List<UsuarioListaDTO> toListDTO(List<Usuario> usuarios);
    List<Usuario> toEntity(List<UsuarioListaDTO> usuarioListaDTO);
}