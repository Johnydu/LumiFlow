package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.entity.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "nivelAcesso", ignore = true)
    @Mapping(target = "setor", ignore = true)
    Usuario toEntity(UsuarioDTO usuarioDto);

    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "nivelAcesso", ignore = true)
    @Mapping(target = "setor", ignore = true)
    Usuario toEntity(UsuarioEdicaoDTO usuarioEdicaoDTO);

    @Mapping(target = "nivelAcesso", source = "nivelAcesso.descricao")
    @Mapping(target = "setor", source = "setor.nome")
    @Mapping(target = "nivelAcessoId", source = "nivelAcesso.id")
    @Mapping(target = "setorId", expression = "java(usuario.getSetor() != null ? usuario.getSetor().getId() : null)")
    UsuarioListaDTO toUsuarioListaDTO(Usuario usuario);



    List<UsuarioListaDTO> toListDTO(List<Usuario> usuarios);
}
