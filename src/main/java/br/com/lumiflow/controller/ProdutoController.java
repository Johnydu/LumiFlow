package br.com.lumiflow.controller;

import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    public String produtos(
            @RequestParam(required = false) String nome,
            Model model) {

        if (nome != null && !nome.isBlank()) {
            model.addAttribute("produtos",
                    produtoService.buscarPorNome(nome));
        } else {
            model.addAttribute("produtos",
                    produtoService.listarProdutosPorCodigo());
        }

        model.addAttribute("produtoDTO",
                new ProdutoDTO(null, null, null, null));

        return "produto/Produtos";
    }

    @PostMapping
    public String novoProduto(@Valid @ModelAttribute("produtoDTO") ProdutoDTO produtoDTO, BindingResult result,
                            RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha todos os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return  "redirect:/dashboard/produto";
        }
        try {
            produtoService.novoProduto(produtoDTO);
            attributes.addFlashAttribute("message","Produto cadastrado com sucesso");
            attributes.addFlashAttribute("messageType", "success");

        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard/produtos";

    }

    @PostMapping("{id}/excluir")
    public String excluirProduto(@PathVariable("id") Long id, RedirectAttributes attributes) {

        try {
            produtoService.deletarProduto(id);
            attributes.addFlashAttribute("message","Produto excluido com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {

            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return  "redirect:/dashboard/produtos";
    }

    @PostMapping("{id}/editar")
    public String editarProduto(@PathVariable("id") Long id, @Valid @ModelAttribute("produtoDTO") ProdutoDTO produtoDTO,
                                BindingResult result, RedirectAttributes attributes
                                ) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("message","Preencha todos os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return  "redirect:/dashboard/produtos";
        }

        try {
            produtoService.editarProduto(id,produtoDTO);
            attributes.addFlashAttribute("message","Produto editado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            e.printStackTrace();

            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return  "redirect:/dashboard/produtos";
    }

}
