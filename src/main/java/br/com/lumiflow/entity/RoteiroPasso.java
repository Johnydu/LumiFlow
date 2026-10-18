package br.com.lumiflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "roteiro_passo")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class RoteiroPasso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "sequencia", nullable = false)
    private Integer sequencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roteiro_id", nullable = false)
    private Roteiro roteiro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id",nullable = false)
    private Setor setor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_setor_id")
    private EtapaSetor etapaSetor;

}
