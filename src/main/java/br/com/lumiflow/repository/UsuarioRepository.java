package br.com.lumiflow.repository;

import br.com.lumiflow.entity.Usuario;
import br.com.lumiflow.entity.enums.Descricao;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    List<Usuario> findAllByOrderByNomeAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM Usuario u JOIN u.nivelAcesso n WHERE n.descricao = :descricao")
    List<Usuario> findAllByNivelAcessoForUpdate(@Param("descricao") Descricao descricao);

    /**
     * Incremento atômico feito no banco: duas falhas simultâneas não perdem contagem.
     * Retorna 0 quando o login não existe.
     */
    @Modifying
    @Query("UPDATE Usuario u SET u.tentativasFalhas = u.tentativasFalhas + 1 WHERE u.login = :login")
    int incrementarTentativasFalhas(@Param("login") String login);

    /**
     * Aplica o bloqueio somente se o limite foi atingido e zera o contador,
     * para que o usuário tenha uma nova janela quando o bloqueio expirar.
     */
    @Modifying
    @Query("""
            UPDATE Usuario u
               SET u.bloqueadoAte = :ate, u.tentativasFalhas = 0
             WHERE u.login = :login AND u.tentativasFalhas >= :max
            """)
    int bloquearSeExcedeuLimite(@Param("login") String login,
                                @Param("ate") LocalDateTime ate,
                                @Param("max") int max);

    @Query("SELECT u FROM Usuario u WHERE LOWER(u.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Usuario> findByNome(@Param("nome") String nome);

    /**
     * Só atualiza se houver algo a limpar, evitando disparar o trigger de
     * atualizado_em a cada login bem-sucedido.
     */
    @Modifying
    @Query("""
            UPDATE Usuario u
               SET u.tentativasFalhas = 0, u.bloqueadoAte = NULL
             WHERE u.login = :login
               AND (u.tentativasFalhas > 0 OR u.bloqueadoAte IS NOT NULL)
            """)
    int limparControleDeBloqueio(@Param("login") String login);
}