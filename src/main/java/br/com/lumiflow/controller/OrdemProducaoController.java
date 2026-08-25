package br.com.lumiflow.controller;

import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.SetorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/ordens")
public class OrdemProducaoController {

    private final OrdemProducaoService ordemProducaoService;
    private final SetorService setorService;

    // Rota por setor específico
    @GetMapping("/setor/{setorId}")
    public String listarOrdensPorSetorId(@PathVariable("setorId") Long setorId,
                                         @ModelAttribute OrdemFiltroDTO filtro,
                                         Model model) {

        List<OrdemProducao> ordens = ordemProducaoService.listarPorSetorIdEFiltros(setorId, filtro);
        Setor setor = setorService.buscarSetorPorId(setorId);

        model.addAttribute("ordens", ordens);
        model.addAttribute("setorAtual", setor != null ? setor.getNome() : "Desconhecido");
        model.addAttribute("setorIdAtual", setorId);
        model.addAttribute("buscaAtual", filtro.busca());
        model.addAttribute("statusAtual", filtro.status());
        model.addAttribute("totalOrdens", ordens.size());

        return "ordens/ListaOrdem";
    }

    // Rota geral (todas as ordens)
    @GetMapping
    public String listarTodasOrdens(@ModelAttribute OrdemFiltroDTO filtro, Model model) {
        List<OrdemProducao> ordens = ordemProducaoService.listarPorFiltros(filtro);

        model.addAttribute("ordens", ordens);
        model.addAttribute("setorAtual", "Todas as Ordens");
        model.addAttribute("setorIdAtual", null);
        model.addAttribute("buscaAtual", filtro.busca());
        model.addAttribute("statusAtual", filtro.status());
        model.addAttribute("totalOrdens", ordens.size());

        return "ordens/ListaOrdem";
    }
}