package br.com.lumiflow.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "retrabalho")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = true)
public class Retrabalho extends Auditoria{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qtd_refeita",nullable = false)
    private Integer qtdRefeita;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "data_hora",nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "refugo_id",nullable = false)
    private Refugo refugo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id",nullable = false)
    private Maquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",nullable = false)
    private Usuario usuario;
}
