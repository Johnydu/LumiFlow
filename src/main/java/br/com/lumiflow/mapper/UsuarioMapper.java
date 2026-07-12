package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.UsuarioDTO;
import br.com.lumiflow.dto.UsuarioListaDTO;
import br.com.lumiflow.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UsuarioMapper {


    @Mapping(target = "senha", ignore = true)
    Usuario toEntity(UsuarioDTO usuarioDto);

    @Mapping(target = "senha", ignore = true)
    UsuarioDTO toDTO(Usuario usuario);

    Usuario toEntity(UsuarioListaDTO usuarioListaDTO);

    UsuarioListaDTO toUsuarioListaDTO(Usuario usuario);

    List<UsuarioListaDTO> toListDTO(List<Usuario> usuarios);

    List<Usuario> toEntity(List<UsuarioListaDTO> usuarioListaDTO);
}