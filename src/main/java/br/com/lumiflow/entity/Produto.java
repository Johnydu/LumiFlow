package br.com.lumiflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Locale;

@Entity
@Table(name = "produto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class Produto extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nome", nullable = false)
    private String nome;

    @NotNull
    @Column(name = "codigo",nullable = false,unique = true,length = 30)
    private String codigo;

    @Column(name = "descricao")
    private  String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roteiro_id")
    private Roteiro roteiro;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (nome != null) nome = nome.trim().toUpperCase(Locale.ROOT);
        if (codigo != null) codigo = codigo.trim().toUpperCase(Locale.ROOT);
        if (descricao != null) descricao = descricao.trim().toUpperCase(Locale.ROOT);
    }

}
