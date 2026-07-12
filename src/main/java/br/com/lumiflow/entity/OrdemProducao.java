package br.com.lumiflow.entity;

import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_producao")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = true)
public class OrdemProducao extends Auditoria{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false)
    private String numero;

    @Column(name = "quantidade",nullable = false)
    private Integer quantidade;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private EstatusOrdemProducao status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "criado_por_id", nullable = false)
    private Usuario criadoPor;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;


}
