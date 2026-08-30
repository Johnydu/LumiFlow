package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/produtos")
public class ProdutoController {
    private final ProdutoService produtoService;
    private final MessageSource messageSource;

    @GetMapping
    public String produtos(@RequestParam(required = false) String nome, Model model) {
        carregarPagina(model, nome);
        return "produto/Produtos";
    }

    @PostMapping
    public String novoProduto(@Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                              BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            carregarPagina(model, null);
            model.addAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            return "produto/Produtos";
        }
        produtoService.novoProduto(dto);
        attributes.addFlashAttribute("success", messageSource
                .getMessage(AppMessages.SUCCESS_PRODUCT_CREATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/produtos";
    }

    @PostMapping("{id}/excluir")
    public String excluirProduto(@PathVariable Long id, RedirectAttributes attributes, Locale locale) {
        produtoService.deletarProduto(id);
        attributes.addFlashAttribute("success", messageSource
                .getMessage(AppMessages.SUCCESS_PRODUCT_DELETED, null, locale));
        return "redirect:/dashboard/produtos";
    }

    @PostMapping("{id}/editar")
    public String editarProduto(@PathVariable Long id, @Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                                BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            carregarPagina(model, null);
            model.addAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            return "produto/Produtos";
        }
        produtoService.editarProduto(id, dto);
        attributes.addFlashAttribute("success", messageSource
                .getMessage(AppMessages.SUCCESS_PRODUCT_UPDATED, new Object[]{dto.nome()}, locale));
        return "redirect:/dashboard/produtos";
    }

    private void carregarPagina(Model model, String nome) {
        model.addAttribute("produtos", nome != null && !nome.isBlank() ? produtoService
                .buscarPorNome(nome) : produtoService.listarProdutos());
        if (!model.containsAttribute("produtoDTO"))
            model.addAttribute("produtoDTO", new ProdutoDTO(null,
                    null, null, null));
    }
}
