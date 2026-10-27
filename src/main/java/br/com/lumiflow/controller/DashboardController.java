package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.dashboard.DashboardOrdemDTO;
import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final MessageSource messageSource;

    @GetMapping
    public String carregarMetricas(@RequestParam(required = false) String busca,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(value = "page", defaultValue = "0") int page,
                                   Model model, Locale locale) {
        try {
            DashboardResumoDTO resumoDTO = dashboardService.obterResumoEstatisticas();
            Page<DashboardOrdemDTO> paginaOrdens = dashboardService.listarOrdens(busca, status, page);
            List<SetorResumoDTO> resumoSetores = dashboardService.obterResumoSetores();

            if (resumoDTO != null) {
                model.addAttribute("totalOrdens", resumoDTO.totalOrdens());
                model.addAttribute("ordensAndamento", resumoDTO.ordensAndamento());
                model.addAttribute("ordensConcluidas", resumoDTO.ordensConcluidas());
                model.addAttribute("ordensPendentes", resumoDTO.ordensPendentes());
            } else {
                model.addAttribute("totalOrdens", 0L);
                model.addAttribute("ordensAndamento", 0L);
                model.addAttribute("ordensConcluidas", 0L);
                model.addAttribute("ordensPendentes", 0L);
            }

            model.addAttribute("paginaOrdens", paginaOrdens);
            model.addAttribute("ordens", paginaOrdens.getContent());
            model.addAttribute("resumoSetores", resumoSetores != null ? resumoSetores : List.of());

        } catch (Exception e) {
            log.error("Erro inesperado ao carregar as métricas do dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("totalOrdens", 0L);
            model.addAttribute("ordensAndamento", 0L);
            model.addAttribute("ordensConcluidas", 0L);
            model.addAttribute("ordensPendentes", 0L);
            model.addAttribute("paginaOrdens", Page.empty());
            model.addAttribute("ordens", List.of());
            model.addAttribute("resumoSetores", List.of());
        }

        return "dashboard/Dashboard";
    }
}