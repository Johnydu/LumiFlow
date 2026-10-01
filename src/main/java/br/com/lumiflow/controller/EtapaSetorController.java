package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.EtapaSetorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/dashboard/setores")
@RequiredArgsConstructor
public class EtapaSetorController {

    private final EtapaSetorService etapaSetorService;


    @GetMapping("/{setorId}/etapas")

    public ResponseEntity<List<EtapaSetorDTO>> listarEtapasPorSetor(@PathVariable Long setorId) {
        try {
            if (setorId == null) {
                log.warn("Tentativa de listar etapas com ID de setor nulo.");
                return ResponseEntity.badRequest().build();
            }

            List<EtapaSetorDTO> etapas = etapaSetorService.listarPorSetorId(setorId).stream()
                    .map(e -> new EtapaSetorDTO(e.id(), e.nome(), e.ordem()))
                    .toList();

            return ResponseEntity.ok(etapas);
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao listar etapas para o setor id {}: {}", setorId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro inesperado ao listar etapas para o setor id {}: {}", setorId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}