package br.com.lumiflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "roteiro_produto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class RoteiroProduto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id",nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id",nullable = false)
    private Setor setor;

}
