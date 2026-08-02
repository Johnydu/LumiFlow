package br.com.lumiflow.model;


import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamento")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id")
public class Lancamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qtd_produzida",nullable = false)
    private Integer qtdProduzida;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "data_hora", nullable = false,insertable = false, updatable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_setor_id", nullable = false)
    private OrdemSetor ordemSetor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etapa_setor_id")
    private EtapaSetor etapaSetor;

}
