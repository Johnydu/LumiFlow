package br.com.lumiflow.controller;

import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.service.OrdemSetorService;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/dashboard/ordens-setor")
@AllArgsConstructor
public class OrdemSetorController {

    private final OrdemSetorService ordemSetorService;

    @GetMapping
    public String listarOrdensSetor(
            @RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model) {

        List<ResumoSetorDTO> resumoSetores = ordemSetorService.obterResumoSetores(busca, data);
        model.addAttribute("resumoSetores", resumoSetores);

        return "ordens/ListaOrdemSetores";
    }


}