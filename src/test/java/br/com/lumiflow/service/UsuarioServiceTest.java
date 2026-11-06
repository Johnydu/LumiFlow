package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.usuario.UsuarioDTO;
import br.com.lumiflow.dto.usuario.UsuarioEdicaoDTO;
import br.com.lumiflow.dto.usuario.UsuarioListaDTO;
import br.com.lumiflow.entity.NivelAcesso;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.UsuarioMapper;
import br.com.lumiflow.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String SENHA_VALIDA = "Senha123";

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioMapper usuarioMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private NivelAcessoService nivelAcessoService;
    @Mock
    private SetorService setorService;
    @Mock
    private SessaoUsuarioService sessaoUsuarioService;

    @InjectMocks
    private UsuarioService service;

    private static void assertChave(Runnable acao, String chave) {
        assertThatThrownBy(acao::run)
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("messageKey", chave);
    }

    private NivelAcesso nivel(Long id, Descricao descricao) {
        return new NivelAcesso(id, descricao);
    }

    private Setor setor(Long id) {
        Setor s = new Setor();
        s.setId(id);
        s.setNome("SETOR " + id);
        return s;
    }

    private Usuario usuario(Long id, String login, NivelAcesso nivel, Setor setor) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setNome("ANA");
        u.setLogin(login);
        u.setSenha("hash-antigo");
        u.setNivelAcesso(nivel);
        u.setSetor(setor);
        return u;
    }

    @Nested
    @DisplayName("novoUsuario")
    class Novo {

        private UsuarioDTO dto(String nome, String login, String senha, Long nivelId, Long setorId) {
            return new UsuarioDTO(null, nome, login, senha, nivelId, setorId);
        }

        @Test
        @DisplayName("DTO nulo é rejeitado")
        void dtoNulo() {
            assertChave(() -> service.novoUsuario(null), AppMessages.ERROR_VALIDATION_FAILED);

            verifyNoInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("Nome nulo, em branco ou com mais de 50 caracteres é rejeitado")
        void nomeInvalido() {
            assertChave(() -> service.novoUsuario(dto(null, "ana", SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.novoUsuario(dto("  ", "ana", SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.novoUsuario(dto("x".repeat(51), "ana", SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_VALIDATION_FAILED);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nome com exatamente 50 caracteres é aceito")
        void nomeNoLimite() {
            UsuarioDTO dto = dto("x".repeat(50), "ana", SENHA_VALIDA, 1L, null);
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.empty());
            when(usuarioMapper.toEntity(dto)).thenReturn(new Usuario());
            when(nivelAcessoService.buscarNivelAcessoPorId(1L)).thenReturn(nivel(1L, Descricao.ADMIN));
            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("hash");

            service.novoUsuario(dto);

            verify(usuarioRepository).save(any(Usuario.class));
        }

        @Test
        @DisplayName("Login em formato inválido é rejeitado antes de consultar o banco")
        void loginInvalido() {
            assertChave(() -> service.novoUsuario(dto("Ana", "a b", SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_USER_LOGIN_INVALID);
            assertChave(() -> service.novoUsuario(dto("Ana", null, SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_USER_LOGIN_INVALID);

            verifyNoInteractions(usuarioRepository);
        }

        @Test
        @DisplayName("Login já cadastrado é rejeitado")
        void loginDuplicado() {
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(new Usuario()));

            assertChave(() -> service.novoUsuario(dto("Ana", "ana", SENHA_VALIDA, 1L, null)),
                    AppMessages.ERROR_USER_ALREADY_EXISTS);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Senha fraca é rejeitada e nunca é codificada")
        void senhaFraca() {
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.empty());

            assertChave(() -> service.novoUsuario(dto("Ana", "ana", "fraca", 1L, null)),
                    AppMessages.ERROR_USER_PASSWORD_WEAK);

            verifyNoInteractions(passwordEncoder);
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Perfil OPERADOR exige setor")
        void operadorExigeSetor() {
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.empty());
            UsuarioDTO dto = dto("Ana", "ana", SENHA_VALIDA, 5L, null);
            when(usuarioMapper.toEntity(dto)).thenReturn(new Usuario());
            when(nivelAcessoService.buscarNivelAcessoPorId(5L)).thenReturn(nivel(5L, Descricao.OPERADOR));

            assertChave(() -> service.novoUsuario(dto), AppMessages.ERROR_USER_OPERATOR_SECTOR_REQUIRED);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Nível de acesso inexistente propaga o erro e não salva")
        void nivelInexistente() {
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.empty());
            UsuarioDTO dto = dto("Ana", "ana", SENHA_VALIDA, 99L, null);
            when(usuarioMapper.toEntity(dto)).thenReturn(new Usuario());
            when(nivelAcessoService.buscarNivelAcessoPorId(99L))
                    .thenThrow(new BusinessException(AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND));

            assertChave(() -> service.novoUsuario(dto), AppMessages.ERROR_ACCESS_LEVEL_NOTFOUND);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Salva com senha codificada, nível e setor resolvidos")
        void salvaComSenhaCodificada() {
            UsuarioDTO dto = dto("Ana", "ana.lima", SENHA_VALIDA, 5L, 3L);
            NivelAcesso nivel = nivel(5L, Descricao.OPERADOR);
            Setor setor = setor(3L);
            when(usuarioRepository.findByLogin("ana.lima")).thenReturn(Optional.empty());
            when(usuarioMapper.toEntity(dto)).thenReturn(new Usuario());
            when(nivelAcessoService.buscarNivelAcessoPorId(5L)).thenReturn(nivel);
            when(setorService.buscarSetorPorId(3L)).thenReturn(setor);
            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("$2a$hash");

            service.novoUsuario(dto);

            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            Usuario salvo = captor.getValue();
            assertThat(salvo.getSenha()).isEqualTo("$2a$hash").isNotEqualTo(SENHA_VALIDA);
            assertThat(salvo.getLogin()).isEqualTo("ana.lima");
            assertThat(salvo.getNivelAcesso()).isSameAs(nivel);
            assertThat(salvo.getSetor()).isSameAs(setor);
        }

        @Test
        @DisplayName("Perfil não-operador pode ser criado sem setor")
        void semSetor() {
            UsuarioDTO dto = dto("Ana", "ana", SENHA_VALIDA, 2L, null);
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.empty());
            when(usuarioMapper.toEntity(dto)).thenReturn(new Usuario());
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(nivel(2L, Descricao.PCP));
            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("hash");

            service.novoUsuario(dto);

            verifyNoInteractions(setorService);
            ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(captor.capture());
            assertThat(captor.getValue().getSetor()).isNull();
        }

        @Test
        @DisplayName("Erros de banco e inesperados viram falha de validação")
        void erros() {
            UsuarioDTO dto = dto("Ana", "ana", SENHA_VALIDA, 2L, null);
            when(usuarioRepository.findByLogin("ana"))
                    .thenThrow(new DataRetrievalFailureException("x"))
                    .thenThrow(new IllegalStateException("x"));

            assertChave(() -> service.novoUsuario(dto), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.novoUsuario(dto), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("buscar")
    class Buscar {

        @Test
        @DisplayName("Sem termo lista todos ordenados por nome")
        void semTermo() {
            List<Usuario> todos = List.of(new Usuario());
            List<UsuarioListaDTO> dtos = List.of();
            when(usuarioRepository.findAllByOrderByNomeAsc()).thenReturn(todos);
            when(usuarioMapper.toListDTO(todos)).thenReturn(dtos);

            assertThat(service.buscar("  ")).isSameAs(dtos);
            assertThat(service.buscar(null)).isSameAs(dtos);

            verify(usuarioRepository, never())
                    .findByNomeContainingIgnoreCaseOrLoginContainingIgnoreCaseOrderByNomeAsc(any(), any());
        }

        @Test
        @DisplayName("Com termo busca por nome ou login usando o termo sem espaços nas pontas")
        void comTermo() {
            List<Usuario> achados = List.of(new Usuario());
            List<UsuarioListaDTO> dtos = List.of();
            when(usuarioRepository.findByNomeContainingIgnoreCaseOrLoginContainingIgnoreCaseOrderByNomeAsc("ana", "ana"))
                    .thenReturn(achados);
            when(usuarioMapper.toListDTO(achados)).thenReturn(dtos);

            assertThat(service.buscar("  ana ")).isSameAs(dtos);
        }

        @Test
        @DisplayName("Erro de banco vira falha de validação")
        void erro() {
            when(usuarioRepository.findAllByOrderByNomeAsc()).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.buscar(null), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("excluirUsuario")
    class Excluir {

        @Test
        @DisplayName("Id nulo ou inexistente resulta em usuário não encontrado")
        void naoEncontrado() {
            when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

            assertChave(() -> service.excluirUsuario(null, 1L), AppMessages.ERROR_USER_NOTFOUND);
            assertChave(() -> service.excluirUsuario(9L, 1L), AppMessages.ERROR_USER_NOTFOUND);
        }

        @Test
        @DisplayName("Usuário não pode excluir a si mesmo")
        void naoExcluiASiMesmo() {
            when(usuarioRepository.findById(1L))
                    .thenReturn(Optional.of(usuario(1L, "ana", nivel(1L, Descricao.ADMIN), null)));

            assertChave(() -> service.excluirUsuario(1L, 1L), AppMessages.ERROR_USER_CANNOT_DELETE_SELF);

            verify(usuarioRepository, never()).delete(any());
        }

        @Test
        @DisplayName("O último administrador não pode ser excluído")
        void ultimoAdmin() {
            Usuario admin = usuario(2L, "admin", nivel(1L, Descricao.ADMIN), null);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(usuarioRepository.findAllByNivelAcessoForUpdate(Descricao.ADMIN)).thenReturn(List.of(admin));

            assertChave(() -> service.excluirUsuario(2L, 1L), AppMessages.ERROR_USER_LAST_ADMIN);

            verify(usuarioRepository, never()).delete(any());
            verifyNoInteractions(sessaoUsuarioService);
        }

        @Test
        @DisplayName("Com outro administrador a exclusão é permitida: exclui, faz flush e encerra as sessões")
        void adminComOutroAdmin() {
            Usuario admin = usuario(2L, "admin", nivel(1L, Descricao.ADMIN), null);
            Usuario outro = usuario(3L, "outro", nivel(1L, Descricao.ADMIN), null);
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
            when(usuarioRepository.findAllByNivelAcessoForUpdate(Descricao.ADMIN)).thenReturn(List.of(admin, outro));

            service.excluirUsuario(2L, 1L);

            InOrder ordem = Mockito.inOrder(usuarioRepository, sessaoUsuarioService);
            ordem.verify(usuarioRepository).delete(admin);
            ordem.verify(usuarioRepository).flush();
            ordem.verify(sessaoUsuarioService).encerrarSessoes("admin");
        }

        @Test
        @DisplayName("Usuário comum é excluído sem consultar administradores")
        void usuarioComum() {
            Usuario comum = usuario(2L, "joao", nivel(5L, Descricao.OPERADOR), setor(1L));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comum));

            service.excluirUsuario(2L, 1L);

            verify(usuarioRepository, never()).findAllByNivelAcessoForUpdate(any());
            verify(usuarioRepository).delete(comum);
            verify(sessaoUsuarioService).encerrarSessoes("joao");
        }

        @Test
        @DisplayName("Violação de integridade vira registro em uso e não encerra sessões")
        void emUso() {
            Usuario comum = usuario(2L, "joao", nivel(5L, Descricao.OPERADOR), setor(1L));
            when(usuarioRepository.findById(2L)).thenReturn(Optional.of(comum));
            Mockito.doThrow(new DataIntegrityViolationException("fk")).when(usuarioRepository).flush();

            assertChave(() -> service.excluirUsuario(2L, 1L), AppMessages.ERROR_RECORD_IN_USE);

            verifyNoInteractions(sessaoUsuarioService);
        }

        @Test
        @DisplayName("Outros erros de banco viram falha de validação")
        void erroDeBanco() {
            when(usuarioRepository.findById(2L)).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.excluirUsuario(2L, 1L), AppMessages.ERROR_VALIDATION_FAILED);
        }
    }

    @Nested
    @DisplayName("editarUsuario")
    class Editar {

        private final NivelAcesso supervisor = nivel(2L, Descricao.SUPERVISOR);

        private Usuario existente() {
            return usuario(1L, "ana", supervisor, null);
        }

        private UsuarioEdicaoDTO dto(String nome, String login, Long nivelId, String senha, Long setorId) {
            return new UsuarioEdicaoDTO(1L, nome, login, nivelId, senha, setorId);
        }

        @Test
        @DisplayName("DTO nulo, usuário inexistente e id nulo são rejeitados")
        void entradasInvalidas() {
            when(usuarioRepository.findById(9L)).thenReturn(Optional.empty());

            assertChave(() -> service.editarUsuario(1L, null), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.editarUsuario(null, dto("Ana", "ana", 2L, null, null)), AppMessages.ERROR_USER_NOTFOUND);
            assertChave(() -> service.editarUsuario(9L, dto("Ana", "ana", 2L, null, null)), AppMessages.ERROR_USER_NOTFOUND);
        }

        @Test
        @DisplayName("Nome inválido e login inválido são rejeitados")
        void nomeELoginInvalidos() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente()));

            assertChave(() -> service.editarUsuario(1L, dto(" ", "ana", 2L, null, null)), AppMessages.ERROR_VALIDATION_FAILED);
            assertChave(() -> service.editarUsuario(1L, dto("Ana", "a b", 2L, null, null)), AppMessages.ERROR_USER_LOGIN_INVALID);
            assertChave(() -> service.editarUsuario(1L, dto("Ana", null, 2L, null, null)), AppMessages.ERROR_USER_LOGIN_INVALID);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Login pertencente a outro usuário é rejeitado")
        void loginDeOutroUsuario() {
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente()));
            when(usuarioRepository.findByLogin("bruno")).thenReturn(Optional.of(usuario(7L, "bruno", supervisor, null)));

            assertChave(() -> service.editarUsuario(1L, dto("Ana", "bruno", 2L, null, null)),
                    AppMessages.ERROR_USER_ALREADY_EXISTS);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Perfil OPERADOR exige setor também na edição")
        void operadorExigeSetor() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(5L)).thenReturn(nivel(5L, Descricao.OPERADOR));

            assertChave(() -> service.editarUsuario(1L, dto("Ana", "ana", 5L, null, null)),
                    AppMessages.ERROR_USER_OPERATOR_SECTOR_REQUIRED);

            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("Sem alterações de login, perfil, setor ou senha as sessões são preservadas")
        void semMudancaDePermissaoMantemSessoes() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);

            service.editarUsuario(1L, dto("  Ana Maria ", "ana", 2L, null, null));

            assertThat(u.getNome()).isEqualTo("Ana Maria");
            assertThat(u.getSenha()).isEqualTo("hash-antigo");
            verify(usuarioRepository).save(u);
            verifyNoInteractions(sessaoUsuarioService, passwordEncoder);
        }

        @Test
        @DisplayName("Mudança de login encerra as sessões do login ANTERIOR")
        void mudancaDeLoginEncerraSessoesDoLoginAnterior() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana.nova")).thenReturn(Optional.empty());
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);

            service.editarUsuario(1L, dto("Ana", "ana.nova", 2L, null, null));

            assertThat(u.getLogin()).isEqualTo("ana.nova");
            verify(sessaoUsuarioService).encerrarSessoes("ana");
        }

        @Test
        @DisplayName("Mudança de perfil encerra as sessões")
        void mudancaDePerfilEncerraSessoes() {
            Usuario u = existente();
            NivelAcesso pcp = nivel(3L, Descricao.PCP);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(3L)).thenReturn(pcp);

            service.editarUsuario(1L, dto("Ana", "ana", 3L, null, null));

            assertThat(u.getNivelAcesso()).isSameAs(pcp);
            verify(sessaoUsuarioService).encerrarSessoes("ana");
        }

        @Test
        @DisplayName("Mudança de setor encerra as sessões")
        void mudancaDeSetorEncerraSessoes() {
            Usuario u = existente();
            Setor novo = setor(4L);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);
            when(setorService.buscarSetorPorId(4L)).thenReturn(novo);

            service.editarUsuario(1L, dto("Ana", "ana", 2L, null, 4L));

            assertThat(u.getSetor()).isSameAs(novo);
            verify(sessaoUsuarioService).encerrarSessoes("ana");
        }

        @Test
        @DisplayName("Nova senha válida é codificada e encerra as sessões")
        void novaSenhaEhCodificada() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);
            when(passwordEncoder.encode(SENHA_VALIDA)).thenReturn("hash-novo");

            service.editarUsuario(1L, dto("Ana", "ana", 2L, SENHA_VALIDA, null));

            assertThat(u.getSenha()).isEqualTo("hash-novo");
            verify(sessaoUsuarioService).encerrarSessoes("ana");
        }

        @Test
        @DisplayName("Nova senha fraca é rejeitada e nada é salvo")
        void novaSenhaFraca() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);

            assertChave(() -> service.editarUsuario(1L, dto("Ana", "ana", 2L, "fraca", null)),
                    AppMessages.ERROR_USER_PASSWORD_WEAK);

            verify(usuarioRepository, never()).save(any());
            verifyNoInteractions(passwordEncoder, sessaoUsuarioService);
        }

        @Test
        @DisplayName("Senha em branco na edição mantém a senha atual")
        void senhaEmBrancoMantemAtual() {
            Usuario u = existente();
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);

            service.editarUsuario(1L, dto("Ana", "ana", 2L, "   ", null));

            assertThat(u.getSenha()).isEqualTo("hash-antigo");
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("Rebaixar o último administrador é rejeitado")
        void rebaixarUltimoAdmin() {
            NivelAcesso admin = nivel(1L, Descricao.ADMIN);
            Usuario u = usuario(1L, "ana", admin, null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);
            when(usuarioRepository.findAllByNivelAcessoForUpdate(Descricao.ADMIN)).thenReturn(List.of(u));

            assertChave(() -> service.editarUsuario(1L, dto("Ana", "ana", 2L, null, null)),
                    AppMessages.ERROR_USER_LAST_ADMIN);

            verify(usuarioRepository, never()).save(any());
            assertThat(u.getNivelAcesso()).isSameAs(admin);
        }

        @Test
        @DisplayName("Rebaixar um administrador é permitido quando há outro administrador")
        void rebaixarAdminComOutroAdmin() {
            NivelAcesso admin = nivel(1L, Descricao.ADMIN);
            Usuario u = usuario(1L, "ana", admin, null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(2L)).thenReturn(supervisor);
            when(usuarioRepository.findAllByNivelAcessoForUpdate(Descricao.ADMIN))
                    .thenReturn(List.of(u, usuario(2L, "outro", admin, null)));

            service.editarUsuario(1L, dto("Ana", "ana", 2L, null, null));

            assertThat(u.getNivelAcesso()).isSameAs(supervisor);
            verify(usuarioRepository).save(u);
            verify(sessaoUsuarioService).encerrarSessoes("ana");
        }

        @Test
        @DisplayName("Manter-se administrador não consulta a contagem de administradores")
        void adminContinuaAdmin() {
            NivelAcesso admin = nivel(1L, Descricao.ADMIN);
            Usuario u = usuario(1L, "ana", admin, null);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(u));
            when(usuarioRepository.findByLogin("ana")).thenReturn(Optional.of(u));
            when(nivelAcessoService.buscarNivelAcessoPorId(1L)).thenReturn(admin);

            service.editarUsuario(1L, dto("Ana", "ana", 1L, null, null));

            verify(usuarioRepository, never()).findAllByNivelAcessoForUpdate(any());
            verify(usuarioRepository).save(u);
        }

        @Test
        @DisplayName("Erros de banco viram falha de validação")
        void erroDeBanco() {
            when(usuarioRepository.findById(1L)).thenThrow(new DataRetrievalFailureException("x"));

            assertChave(() -> service.editarUsuario(1L, dto("Ana", "ana", 2L, null, null)),
                    AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}
