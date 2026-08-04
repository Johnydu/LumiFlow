package br.com.lumiflow.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/ordens")
@AllArgsConstructor
public class OredensController {

    @GetMapping
    public String ordens(){
        return "ordens/ListaOrdem";
    }
}
