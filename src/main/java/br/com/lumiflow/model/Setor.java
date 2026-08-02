package br.com.lumiflow.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "setor")
@EqualsAndHashCode(of = "id",callSuper = true)
public class Setor extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "possui_etapas")
    private Boolean possuiEtapas;



}
