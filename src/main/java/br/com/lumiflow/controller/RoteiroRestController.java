package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.service.RoteiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consultas de roteiro para telas AJAX. Erros de negócio e falhas inesperadas viram ProblemDetail
 * (status correto, sem vazar detalhes) no GlobalExceptionHandler, igual ao EtapaSetorController.
 */
@RestController
@RequestMapping("/api/roteiros")
@RequiredArgsConstructor
public class RoteiroRestController {

    private final RoteiroService roteiroService;

    @GetMapping("/produto/{produtoId}")
    public List<String> buscarFluxoPorProduto(@PathVariable Long produtoId) {
        return roteiroService.buscarFluxoPorProdutoId(produtoId);
    }

    @GetMapping("/{roteiroId}/setor/{setorId}/etapas")
    public List<EtapaSetorDTO> buscarEtapasDoSetor(@PathVariable Long roteiroId,
                                                   @PathVariable Long setorId) {
        return roteiroService.buscarEtapasDoSetorNoRoteiro(roteiroId, setorId);
    }
}
