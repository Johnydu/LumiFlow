package br.com.lumiflow.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/configuracoes")
public class ConfiguracaoController {

    /** Mesmo critério da regra de URL em SecurityConfig (ADMIN e SUPERVISOR entram por herança). */
    @GetMapping
    @PreAuthorize("hasAnyRole('GESTAO','PCP')")
    public String paginaConfiguracoes() {
        return "configuracao/Configuracoes";
    }
}
