package br.com.lumiflow.entity;
import br.com.lumiflow.entity.enums.DestinoRefugo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "refugo")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = true)
public class Refugo extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "qtd_refugo",nullable = false)
    private Integer qtdRefugo;

    @Column(name = "motivo",nullable = false)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "destino",nullable = false)
    private DestinoRefugo destino;

    @Column(name = "data_hora",nullable = false)
    private LocalDateTime dataHora;

    @ManyToOne(fetch =  FetchType.LAZY)
    @JoinColumn(name = "ordem_setor_id",nullable = false)
    private OrdemSetor ordemSetor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_origem_id",nullable = false)
    private Setor setorOrigem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id",nullable = false)
    private Operador operador;

    @Column(name = "sequencia_origem", nullable = false)
    private Integer sequenciaOrigem;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (motivo != null) motivo = motivo.trim().toUpperCase(Locale.ROOT);
    }
}
