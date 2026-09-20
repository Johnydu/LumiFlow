package br.com.lumiflow.controller;

import br.com.lumiflow.service.LancamentoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/lancamentos")
@AllArgsConstructor
public class HistoricoLancamentosController {
    
    private final LancamentoService lancamentoService;

    @GetMapping
    public String lancamentos(Model model) {

        carregarPagina(model);

        return "historico/HistoricoLancamento";
    }



    private void carregarPagina(Model model) {
        model.addAttribute("historicos",lancamentoService.listarHistoricoLancamentos());

    }
}
