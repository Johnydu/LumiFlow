package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "setor")
@EqualsAndHashCode(of = "id", callSuper = true)
public class Setor extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @OneToMany(mappedBy = "setor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EtapaSetor> etapas = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "setor_operador",
            joinColumns = @JoinColumn(name = "setor_id"),
            inverseJoinColumns = @JoinColumn(name = "operador_id")
    )
    private Set<Operador> operadores = new HashSet<>();

}