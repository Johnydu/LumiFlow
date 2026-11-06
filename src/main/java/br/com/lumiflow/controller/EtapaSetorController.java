package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.service.EtapaSetorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Etapas de um setor (usado pelo cadastro de setores). Erros (setor inexistente, falha inesperada)
 * são convertidos em ProblemDetail pelo GlobalExceptionHandler, de forma idêntica ao RoteiroRestController.
 */
@RestController
@RequestMapping("/dashboard/setores")
@RequiredArgsConstructor
public class EtapaSetorController {

    private final EtapaSetorService etapaSetorService;

    @GetMapping("/{setorId}/etapas")
    public List<EtapaSetorDTO> listarEtapasPorSetor(@PathVariable Long setorId) {
        return etapaSetorService.listarPorSetorId(setorId);
    }
}
