package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Entity
@Table(name = "roteiro")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id", callSuper = false)
public class Roteiro extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (descricao != null) descricao = descricao.trim().toUpperCase(Locale.ROOT);
        if (codigo != null) codigo = codigo.trim().toUpperCase(Locale.ROOT);
    }
}
