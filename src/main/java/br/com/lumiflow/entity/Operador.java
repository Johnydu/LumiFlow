package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "operador")
@EqualsAndHashCode(of = "id", callSuper = false)
public class Operador extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "funcao", length = 30)
    private String funcao;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_padrao_id")
    private Setor setorPadrao;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (nome != null) nome = nome.trim().toUpperCase(Locale.ROOT);
        if (funcao != null) funcao = funcao.trim().toUpperCase(Locale.ROOT);
    }

}
