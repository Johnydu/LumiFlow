package br.com.lumiflow.controller;


import br.com.lumiflow.service.ChapaVidroService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/vidracaria")
public class VidracariaController {

    private final ChapaVidroService vidracariaSerivice;

}
