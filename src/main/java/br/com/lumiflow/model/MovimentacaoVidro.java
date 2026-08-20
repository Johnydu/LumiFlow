package br.com.lumiflow.model;

import br.com.lumiflow.model.enums.TipoMovimentacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacao_vidro")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(of = "id", callSuper = true)
public class MovimentacaoVidro extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne( fetch = FetchType.LAZY)
    @JoinColumn(name = "chapa_vidro_id")
    private ChapaVidro chapaVidro;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_movimentacao",length = 20,nullable = false)
    private TipoMovimentacao tipoMovimentacao;

    @Column(name = "quantidade",nullable = false)
    private Integer quantidade;

    @Column(name = "observacao",length = 100)
    private String observacao;

    @DateTimeFormat
    @Column(name = "data_hora")
    private LocalDateTime dataHora;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operador_id", nullable = false)
    @NotNull(message = "O operador responsável é obrigatório")
    private Operador operador; //
}
