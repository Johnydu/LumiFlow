package br.com.lumiflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "operador")
@EqualsAndHashCode(of = "id", callSuper = true)
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

}