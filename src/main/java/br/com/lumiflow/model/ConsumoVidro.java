package br.com.lumiflow.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "consumo_vidro")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = true)
public class ConsumoVidro extends  Auditoria{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tipo_vidro",nullable = false)
    private String tipoVidro;

    @Column(name = "quantidade",nullable = false)
    private BigDecimal quantidade;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "data_hora",nullable = false,insertable = false, updatable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id",nullable = false)
    private Usuario usuario;
}
