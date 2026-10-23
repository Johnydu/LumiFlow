package br.com.lumiflow.controller;

import br.com.lumiflow.dto.comum.PaginaDTO;
import br.com.lumiflow.dto.produto.ProdutoBuscaDTO;
import br.com.lumiflow.service.ProdutoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
public class ProdutoRestController {

    private final ProdutoService produtoService;

    @GetMapping("/busca")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public PaginaDTO<ProdutoBuscaDTO> buscar(@RequestParam(defaultValue = "") String busca,
                                             @RequestParam(defaultValue = "0") int pagina) {
        return produtoService.buscarParaSelecao(busca, pagina);
    }
}
