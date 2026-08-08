package br.com.lumiflow.controller;

import br.com.lumiflow.service.OrdemProducaoService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/dashboard/ordens/setor/{setorId}")
@AllArgsConstructor
@Slf4j
public class OredensController {

    private final OrdemProducaoService ordemProducaoService;

    @GetMapping
    public String exibirOrdens(Model model){
        log.info("Requisição GET: Listar todas as ordens");

        model.addAttribute("ordens",ordemProducaoService.listarOrdens());


        return "ordens/ListaOrdem";
    }
}
