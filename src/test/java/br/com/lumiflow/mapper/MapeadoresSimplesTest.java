package br.com.lumiflow.mapper;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.nivelacesso.NivelAcessoDTO;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.entity.ChapaVidro;
import br.com.lumiflow.entity.EtapaSetor;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.entity.enums.TipoVidro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapeadores pequenos, agrupados por afinidade. */
class MapeadoresSimplesTest {

    @Nested
    @DisplayName("EtapaSetorMapper")
    class EtapaSetor_ {
        private final EtapaSetorMapper mapper = Mappers.getMapper(EtapaSetorMapper.class);

        @Test
        @DisplayName("Mapeia id, nome e ordem preservando a ordem da lista")
        void mapeiaLista() {
            EtapaSetor primeira = new EtapaSetor();
            primeira.setId(1L);
            primeira.setNome("1ª DOBRA");
            primeira.setOrdem(1);
            EtapaSetor segunda = new EtapaSetor();
            segunda.setId(2L);
            segunda.setNome("2ª DOBRA");
            segunda.setOrdem(2);

            List<EtapaSetorDTO> dtos = mapper.toListDTO(List.of(primeira, segunda));

            assertThat(dtos).containsExactly(new EtapaSetorDTO(1L, "1ª DOBRA", 1), new EtapaSetorDTO(2L, "2ª DOBRA", 2));
        }

        @Test
        @DisplayName("Lista nula retorna nulo e vazia retorna vazia")
        void nulosEVazios() {
            assertThat(mapper.toListDTO(null)).isNull();
            assertThat(mapper.toListDTO(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("NivelAcessoMapper")
    class NivelAcesso_ {
        private final NivelAcessoMapper mapper = Mappers.getMapper(NivelAcessoMapper.class);

        @Test
        @DisplayName("Converte o enum de descrição para texto")
        void converteEnumParaTexto() {
            NivelAcesso admin = new NivelAcesso();
            admin.setId(1L);
            admin.setDescricao(Descricao.ADMIN);
            NivelAcesso operador = new NivelAcesso();
            operador.setId(5L);
            operador.setDescricao(Descricao.OPERADOR);

            List<NivelAcessoDTO> dtos = mapper.toListDTO(List.of(admin, operador));

            assertThat(dtos).containsExactly(new NivelAcessoDTO(1L, "ADMIN"), new NivelAcessoDTO(5L, "OPERADOR"));
        }

        @Test
        @DisplayName("Lista nula retorna nulo")
        void listaNula() {
            assertThat(mapper.toListDTO(null)).isNull();
        }
    }

    @Nested
    @DisplayName("ChapaVidroMapper")
    class ChapaVidro_ {
        private final ChapaVidroMapper mapper = Mappers.getMapper(ChapaVidroMapper.class);

        @Test
        @DisplayName("toEntity copia tipo, descrição e estoque mínimo e não define id")
        void toEntity() {
            ChapaVidro chapa = mapper.toEntity(new ChapaVidroDTO(TipoVidro.JATEADO, "Vidro 8mm", 12));

            assertThat(chapa.getId()).isNull();
            assertThat(chapa.getTipoVidro()).isEqualTo(TipoVidro.JATEADO);
            assertThat(chapa.getDescricao()).isEqualTo("Vidro 8mm");
            assertThat(chapa.getEstoqueMinimo()).isEqualTo(12);
        }

        @Test
        @DisplayName("Entrada nula retorna nulo")
        void nulo() {
            assertThat(mapper.toEntity(null)).isNull();
        }
    }
}
