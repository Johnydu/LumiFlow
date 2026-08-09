package br.com.lumiflow.controller;

import br.com.lumiflow.service.OrdemSetorService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping ("dashboard/ordens")
public class OrdemSetorController {

    private final OrdemSetorService ordemSetorService;



}
