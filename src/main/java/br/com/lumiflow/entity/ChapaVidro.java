package br.com.lumiflow.entity;

import br.com.lumiflow.entity.enums.TipoVidro;
import jakarta.persistence.*;
import lombok.*;

import java.util.Locale;

@Entity
@Table(name = "chapa_vidro")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id",callSuper = false)
public class ChapaVidro extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vidro", length = 20, nullable = false)
    private TipoVidro tipoVidro;


    @Column(name = "descricao", length = 100, nullable = false)
    private String descricao;

    @Column(name = "estoque_minimo", nullable = false)
    private Integer estoqueMinimo;

    @PrePersist
    @PreUpdate
    private void normalizarTextos() {
        if (descricao != null) descricao = descricao.trim().toUpperCase(Locale.ROOT);
    }
}
