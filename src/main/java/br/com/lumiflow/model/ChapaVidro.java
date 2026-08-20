package br.com.lumiflow.model;

import br.com.lumiflow.model.enums.TipoVidro;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chapa_vidro")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id",callSuper = true)
public class ChapaVidro extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_vidro",length = 10,nullable = false)
    private TipoVidro tipoVidro;


    @Column(name = "descricao",length = 10)
    private String descricao;

    @Column(name = "estoque_minimo",nullable = false,length = 10)
    private Integer estoqueMinimo;
}
