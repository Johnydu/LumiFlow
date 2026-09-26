package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.*;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.OperadorRepository;
import br.com.lumiflow.repository.OrdemSetorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrdemSetorService {

    private final OrdemSetorRepository ordemSetorRepository;
    private final OperadorRepository operadorRepository;

    @Transactional(readOnly = true)
    public List<ResumoSetorDTO> obterResumoSetores(String busca, LocalDate data) {
        try {
            LocalDate dataFiltro = (data != null) ? data : LocalDate.now();
            String termoBusca = (busca != null && !busca.isBlank()) ? busca.trim() : null;

            List<Object[]> resultados = ordemSetorRepository.buscarResumoSetores(termoBusca);
            List<ResumoSetorDTO> lista = new ArrayList<>();

            if (resultados != null) {
                for (Object[] row : resultados) {
                    Long setorId = row[0] != null ? ((Number) row[0]).longValue() : null;
                    String setorNome = (String) row[1];
                    Long qtdOrdens = row[2] != null ? ((Number) row[2]).longValue() : 0L;
                    Long ordensConcluidas = row[3] != null ? ((Number) row[3]).longValue() : 0L;

                    if (setorId == null) {
                        continue;
                    }

                    String operadoresDoDia = "";
                    try {
                        operadoresDoDia = operadorRepository.findBySetorPadraoId(setorId)
                                .stream()
                                .map(Operador::getNome)
                                .collect(Collectors.joining(", "));
                    } catch (Exception ex) {
                        log.warn("Não foi possível carregar os operadores do setor id {}: {}", setorId, ex.getMessage());
                    }

                    lista.add(new ResumoSetorDTO(
                            setorId,
                            setorNome,
                            dataFiltro,
                            operadoresDoDia,
                            qtdOrdens,
                            ordensConcluidas
                    ));
                }
            }

            return lista;

        } catch (BusinessException e) {
            throw e;
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao obter resumo setores (busca: '{}'): {}", busca, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        } catch (Exception e) {
            log.error("Erro inesperado ao obter resumo setores (busca: '{}'): {}", busca, e.getMessage(), e);
            throw new BusinessException(AppMessages.ERROR_VALIDATION_FAILED);
        }
    }
}