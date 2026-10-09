package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "setor")
@EqualsAndHashCode(of = "id", callSuper = false)
public class Setor extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @OneToMany(mappedBy = "setor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EtapaSetor> etapas = new ArrayList<>();

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (nome != null) nome = nome.trim().toUpperCase(Locale.ROOT);
    }

}
