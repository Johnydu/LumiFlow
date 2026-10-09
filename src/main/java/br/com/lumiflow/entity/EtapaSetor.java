package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "etapa_setor")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class EtapaSetor extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nome",nullable = false)
    private String nome;

    @Column(name = "ordem",nullable = false)
    private Integer ordem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (nome != null) nome = nome.trim().toUpperCase(Locale.ROOT);
    }

}
