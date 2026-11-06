package br.com.lumiflow.dto;

import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DtoLogicTest {

    @Nested
    @DisplayName("OrdemFiltroDTO")
    class OrdemFiltro {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        @DisplayName("Não tem filtro de setor quando nulo, vazio ou em branco")
        void semFiltroDeSetor(String setor) {
            assertThat(new OrdemFiltroDTO(setor, "busca", "status").temFiltroSetor()).isFalse();
        }

        @Test
        @DisplayName("Tem filtro de setor quando há texto")
        void comFiltroDeSetor() {
            assertThat(new OrdemFiltroDTO("Corte", null, null).temFiltroSetor()).isTrue();
        }
    }

    @Nested
    @DisplayName("ResumoSetorDTO.percentualConcluido")
    class ResumoSetor {

        @Test
        @DisplayName("Calcula a porcentagem de ordens concluídas")
        void calculaPercentual() {
            ResumoSetorDTO dto = new ResumoSetorDTO(1L, "Corte", "", 4L, 1L);

            assertThat(dto.percentualConcluido()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Retorna 100 quando todas as ordens estão concluídas")
        void cemPorCento() {
            assertThat(new ResumoSetorDTO(1L, "Corte", "", 3L, 3L).percentualConcluido())
                    .isCloseTo(100.0, within(0.0001));
        }

        @Test
        @DisplayName("Retorna zero quando não há ordens (evita divisão por zero)")
        void semOrdens() {
            assertThat(new ResumoSetorDTO(1L, "Corte", "", 0L, 0L).percentualConcluido()).isZero();
        }

        @Test
        @DisplayName("Retorna zero quando a quantidade de ordens é nula")
        void quantidadeNula() {
            assertThat(new ResumoSetorDTO(1L, "Corte", "", null, null).percentualConcluido()).isZero();
        }
    }

    @Nested
    @DisplayName("ProdutoDTO.identificacao")
    class Produto {

        @Test
        @DisplayName("Usa o nome quando informado")
        void usaNome() {
            assertThat(new ProdutoDTO(1L, "Porta", "P-01", null).identificacao()).isEqualTo("Porta");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"  "})
        @DisplayName("Usa o código quando o nome é nulo, vazio ou em branco")
        void usaCodigoQuandoSemNome(String nome) {
            assertThat(new ProdutoDTO(1L, nome, "P-01", null).identificacao()).isEqualTo("P-01");
        }
    }

    @Nested
    @DisplayName("Usuário: semSenha")
    class Usuario {

        @Test
        @DisplayName("UsuarioDTO.semSenha remove somente a senha")
        void usuarioDtoSemSenha() {
            UsuarioDTO original = new UsuarioDTO(5L, "Ana", "ana.lima", "Segredo123", 2L, 9L);

            UsuarioDTO limpo = original.semSenha();

            assertThat(limpo.senha()).isNull();
            assertThat(limpo).usingRecursiveComparison().ignoringFields("senha").isEqualTo(original);
            assertThat(original.senha()).isEqualTo("Segredo123");
        }

        @Test
        @DisplayName("UsuarioEdicaoDTO.semSenha converte para UsuarioDTO sem senha preservando os demais campos")
        void edicaoSemSenha() {
            UsuarioEdicaoDTO edicao = new UsuarioEdicaoDTO(5L, "Ana", "ana.lima", 2L, "NovaSenha1", 9L);

            UsuarioDTO dto = edicao.semSenha();

            assertThat(dto.id()).isEqualTo(5L);
            assertThat(dto.nome()).isEqualTo("Ana");
            assertThat(dto.login()).isEqualTo("ana.lima");
            assertThat(dto.nivelAcessoId()).isEqualTo(2L);
            assertThat(dto.setorId()).isEqualTo(9L);
            assertThat(dto.senha()).isNull();
        }
    }
}
