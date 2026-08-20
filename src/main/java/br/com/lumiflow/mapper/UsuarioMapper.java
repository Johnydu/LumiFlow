package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.model.Usuario;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;


@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(target = "senha", ignore = true)
    @Mapping(target = "nivelAcesso", ignore = true)
    @Mapping(target = "setor", ignore = true)
    @Mapping(target = "nome", source = "nome", qualifiedByName = "normalizarNome")
    Usuario toEntity(UsuarioDTO usuarioDto);

    @Named("normalizarNome")
    default String normalizarNome(String nome) {
        return nome == null
                ? null
                : nome.trim().toUpperCase(java.util.Locale.ROOT);
    }

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
