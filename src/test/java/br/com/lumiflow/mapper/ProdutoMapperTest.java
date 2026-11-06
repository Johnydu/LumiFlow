package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.produto.ProdutoListagemDTO;
import br.com.lumiflow.entity.Produto;
import br.com.lumiflow.entity.Roteiro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProdutoMapperTest {

    private final ProdutoMapper mapper = Mappers.getMapper(ProdutoMapper.class);

    private Produto produto(Roteiro roteiro) {
        Produto p = new Produto();
        p.setId(1L);
        p.setNome("PORTA");
        p.setCodigo("P-01");
        p.setDescricao("Porta de vidro");
        p.setRoteiro(roteiro);
        return p;
    }

    @Test
    @DisplayName("toDTO copia id, nome, código e descrição")
    void toDto() {
        ProdutoDTO dto = mapper.toDTO(produto(null));

        assertThat(dto).isEqualTo(new ProdutoDTO(1L, "PORTA", "P-01", "Porta de vidro"));
    }

    @Test
    @DisplayName("toEntity copia os campos do DTO e não preenche o roteiro")
    void toEntityIgnoraRoteiro() {
        Produto entity = mapper.toEntity(new ProdutoDTO(null, "Porta", "p-01", "desc"));

        assertThat(entity.getId()).isNull();
        assertThat(entity.getNome()).isEqualTo("Porta");
        assertThat(entity.getCodigo()).isEqualTo("p-01");
        assertThat(entity.getDescricao()).isEqualTo("desc");
        assertThat(entity.getRoteiro()).isNull();
    }

    @Test
    @DisplayName("toListagemDTO expõe código e descrição do roteiro")
    void listagemComRoteiro() {
        Roteiro roteiro = new Roteiro();
        roteiro.setCodigo("R-10");
        roteiro.setDescricao("CORTE E DOBRA");

        ProdutoListagemDTO dto = mapper.toListagemDTO(produto(roteiro));

        assertThat(dto.roteiroCodigo()).isEqualTo("R-10");
        assertThat(dto.roteiroDescricao()).isEqualTo("CORTE E DOBRA");
        assertThat(dto.codigo()).isEqualTo("P-01");
    }

    @Test
    @DisplayName("toListagemDTO aceita produto sem roteiro")
    void listagemSemRoteiro() {
        ProdutoListagemDTO dto = mapper.toListagemDTO(produto(null));

        assertThat(dto.roteiroCodigo()).isNull();
        assertThat(dto.roteiroDescricao()).isNull();
    }

    @Test
    @DisplayName("Listas e entradas nulas")
    void listasENulos() {
        List<ProdutoListagemDTO> listagem = mapper.toListagemDTO(List.of(produto(null), produto(null)));

        assertThat(listagem).hasSize(2);
        assertThat(mapper.toListDTO(List.of(produto(null)))).hasSize(1);
        assertThat(mapper.toDTO(null)).isNull();
        assertThat(mapper.toEntity(null)).isNull();
        assertThat(mapper.toListDTO(null)).isNull();
    }
}
