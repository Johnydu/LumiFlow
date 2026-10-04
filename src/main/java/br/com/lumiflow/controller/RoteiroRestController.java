package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.RoteiroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/roteiros")
@RequiredArgsConstructor
public class RoteiroRestController {

    private final RoteiroService roteiroService;

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<String>> buscarFluxoPorProduto(@PathVariable Long produtoId) {
        try {
            if (produtoId == null) {
                log.warn("Tentativa de buscar fluxo com ID de produto nulo.");
                return ResponseEntity.badRequest().build();
            }

            List<String> fluxo = roteiroService.buscarFluxoPorProdutoId(produtoId);
            return ResponseEntity.ok(fluxo);

        } catch (BusinessException e) {
            log.warn("Erro de negócio ao buscar fluxo para o produto id {}: {}", produtoId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (DataAccessException e) {
            log.error("Erro de base de dados ao buscar fluxo para o produto id {}: {}", produtoId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar fluxo para o produto id {}: {}", produtoId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/produto/{produtoId}/setor/{setorId}/etapas")
    public ResponseEntity<List<EtapaSetorDTO>> buscarEtapasDoSetor(@PathVariable Long produtoId,
                                                                   @PathVariable Long setorId) {
        return ResponseEntity.ok(roteiroService.buscarEtapasDoSetorNoRoteiro(produtoId, setorId));
    }
}