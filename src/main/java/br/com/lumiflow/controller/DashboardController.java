package br.com.lumiflow.controller;

import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.service.DashboardService;
import br.com.lumiflow.service.OrdemProducaoService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final OrdemProducaoService ordemProducaoService;

    @GetMapping
    private String carregarMetricas(Model model) {
        DashboardResumoDTO resumoDTO = dashboardService.obterResumoEstatisticas();
        Page<OrdemProducao> paginaOrdens = ordemProducaoService.listarOrdens(0, 10);
        List<SetorResumoDTO> resumoSetores = dashboardService.obterResumoSetores();

        model.addAttribute("totalOrdens", resumoDTO.totalOrdens() );
        model.addAttribute("ordensAndamento", resumoDTO.ordensAndamento() );
        model.addAttribute("ordensConcluidas", resumoDTO.ordensConcluidas() );
        model.addAttribute("ordensPendentes", resumoDTO.ordensPendentes() );

        model.addAttribute("listaOrdensEmAndamento",paginaOrdens);

        model.addAttribute("resumosSetores", resumoSetores);

        return "dashboard/Dashboard";
    }


}