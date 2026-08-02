package br.com.lumiflow.controller;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/vidracaria")
public class VidracariaController {

    @GetMapping
    public String vidracaria(){

        return "vidracaria/vidracaria";
    }

    
}
