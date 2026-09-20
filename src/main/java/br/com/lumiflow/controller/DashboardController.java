package br.com.lumiflow.controller;

import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.service.DashboardService;
import lombok.AllArgsConstructor;
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


    @GetMapping
    public String carregarMetricas(Model model) {
        DashboardResumoDTO resumoDTO = dashboardService.obterResumoEstatisticas();
        List<DashboardOrdemDTO> ordensAtivas = dashboardService.listarOrdensAtivas(10);
        List<SetorResumoDTO> resumoSetores = dashboardService.obterResumoSetores();

        model.addAttribute("totalOrdens", resumoDTO.totalOrdens());
        model.addAttribute("ordensAndamento", resumoDTO.ordensAndamento());
        model.addAttribute("ordensConcluidas", resumoDTO.ordensConcluidas());
        model.addAttribute("ordensPendentes", resumoDTO.ordensPendentes());
        model.addAttribute("ordensAtivas", ordensAtivas);
        model.addAttribute("resumoSetores", resumoSetores);

        return "dashboard/Dashboard";
    }


}