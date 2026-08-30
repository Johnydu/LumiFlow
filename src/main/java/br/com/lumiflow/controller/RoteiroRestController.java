package br.com.lumiflow.controller;

import br.com.lumiflow.service.RoteiroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roteiros")
@RequiredArgsConstructor
public class RoteiroRestController {

    private final RoteiroService roteiroService;

    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<String>> buscarFluxoPorProduto(@PathVariable Long produtoId) {

        return ResponseEntity.ok(roteiroService.buscarFluxoPorProdutoId(produtoId));
    }
}
