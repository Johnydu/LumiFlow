package br.com.lumiflow.entity;

import br.com.lumiflow.entity.enums.Descricao;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nivel_acesso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id",callSuper = true)
public class NivelAcesso extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "descricao", nullable = false,unique = true)
    private Descricao descricao;

}
