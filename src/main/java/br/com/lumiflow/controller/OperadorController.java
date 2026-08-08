package br.com.lumiflow.controller;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/operadores")
public class OperadorController {

    private final OperadorService operadorService;
    private final SetorService setorService;

    @GetMapping
    public String listaOperadores(Model model) {

        model.addAttribute("operadores",operadorService.listarOperadores());
        model.addAttribute("listaSetores",setorService.listarSetores());
        model.addAttribute("operadorDTO",new OperadorDTO(null,null,null,null));

        return "operador/Operadores";
    }


}
