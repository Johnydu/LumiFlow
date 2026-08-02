package br.com.lumiflow.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produto")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = true)
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

    @OneToMany(mappedBy = "produto", cascade = CascadeType.ALL , orphanRemoval = true)
    @OrderBy("sequencia ASC ")
    private List<RoteiroProduto> roteiro = new ArrayList<>();


}
