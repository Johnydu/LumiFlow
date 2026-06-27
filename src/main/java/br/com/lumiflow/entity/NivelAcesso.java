package br.com.lumiflow.entity;

import br.com.lumiflow.entity.enums.Roles;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nivel_acesso")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class NivelAcesso extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
     private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "descricao", nullable = false,unique = true)
    private Roles role;

}
