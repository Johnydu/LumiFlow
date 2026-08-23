package br.com.lumiflow.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "setor")
@EqualsAndHashCode(of = "id",callSuper = true)
public class Setor extends Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "possui_etapas")
    private Boolean possuiEtapas;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "setor_operador",
            joinColumns = @JoinColumn(name = "setor_id"),
            inverseJoinColumns = @JoinColumn(name = "operador_id")
    )
    private Set<Operador> operadores = new HashSet<>();


    // --- MÉTODOS UTILITÁRIOS ---

    /**
     * Retorna os nomes dos operadores formatados em uma única String (ex: "Carlos, Ana, João").
     * Ideal para exibir na tabela da tela ListaOrdemSetores.
     */
    public String getNomesOperadoresFormatados() {
        if (operadores == null || operadores.isEmpty()) {
            return "Nenhum operador";
        }
        return operadores.stream()
                .map(Operador::getNome)
                .collect(Collectors.joining(", "));
    }

    public void adicionarOperador(Operador operador) {
        this.operadores.add(operador);
    }

    public void removerOperador(Operador operador) {
        this.operadores.remove(operador);
    }

}
