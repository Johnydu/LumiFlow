package br.com.lumiflow.controller;

import br.com.lumiflow.service.OrdemDetalheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Tela de detalhes da ordem. Apenas recebe a requisição e delega ao service.
 * Liberar, dividir e pausar etapas ficam no OrdemSetorController.
 */
@Controller
@RequestMapping("/dashboard/ordens")
@RequiredArgsConstructor
public class OrdemDetalheController {

    private final OrdemDetalheService ordemDetalheService;

    @GetMapping("/{ordemId}/detalhes")
    @PreAuthorize("hasAnyRole('GESTAO','PCP','ADMIN','SUPERVISOR')")
    public String detalhes(@PathVariable Long ordemId, Model model) {
        model.addAttribute("ordem", ordemDetalheService.buscarDetalhe(ordemId));
        return "ordens/DetalhesOrdem";
    }
}