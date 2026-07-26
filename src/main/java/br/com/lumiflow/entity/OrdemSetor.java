package br.com.lumiflow.entity;
import br.com.lumiflow.entity.enums.EstatusOrdemSetor;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ordem_setor")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = true)
public class OrdemSetor extends Auditoria{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sequencia",nullable = false)
    private Integer sequencia;

    @Column(name = "qtd_recebida",nullable = false)
    private Integer qtdRecebida;

    @Column(name = "qtd_produzida",nullable = false)
    private Integer qtdProduzida;

    @Column(name = "qtd_pendente",nullable = false)
    private Integer qtdPendente = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false)
    private EstatusOrdemSetor status;

    @Column(name = "inicio")
    private LocalDateTime inicio;


    @Column(name = "fim")
    private LocalDateTime fim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_producao_id", nullable = false)
    private OrdemProducao ordemProducao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

}
