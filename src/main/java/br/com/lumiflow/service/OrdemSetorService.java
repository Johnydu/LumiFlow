package br.com.lumiflow.service;

import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.Operador; //[cite: 1]
import br.com.lumiflow.repository.OperadorRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class OrdemSetorService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final OperadorRepository operadorRepository;

    @Transactional(readOnly = true)
    public List<ResumoSetorDTO> obterResumoSetores(String busca, LocalDate data) {
        LocalDate dataFiltro = (data != null) ? data : LocalDate.now();
        List<Object[]> resultados = ordemSetorRepository.buscarResumoSetores(busca);
        List<ResumoSetorDTO> lista = new ArrayList<>();

        for (Object[] row : resultados) {
            Long setorId = (Long) row[0];
            String setorNome = (String) row[1];
            Long qtdOrdens = (Long) row[2];
            Long ordensConcluidas = row[3] != null ? ((Number) row[3]).longValue() : 0L;

            // Busca os operadores cadastrados com o setorPadrao igual a este setor e une os nomes
            String operadoresDoDia = operadorRepository.findBySetorPadraoId(setorId)
                    .stream()
                    .map(Operador::getNome)
                    .collect(Collectors.joining(", "));

            lista.add(new ResumoSetorDTO(
                    setorId,
                    setorNome,
                    dataFiltro,
                    operadoresDoDia,
                    qtdOrdens,
                    ordensConcluidas
            ));
        }

        return lista;
    }
}