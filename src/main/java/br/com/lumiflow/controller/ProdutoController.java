package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final MessageSource messageSource;

    @GetMapping
    public String produtos(@RequestParam(required = false) String nome, Model model, Locale locale) {
        try {
            carregarPagina(model, nome);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de produtos: {}", e.getMessage(), e);
            model.addAttribute("error",
                    messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("produtos", List.of());
            if (!model.containsAttribute("produtoDTO")) {
                model.addAttribute("produtoDTO",
                        new ProdutoDTO(null, null, null, null));
            }
        }
        return "produto/Produtos";
    }

    @PostMapping
    public String novoProduto(@Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                              BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            produtoService.novoProduto(dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_CREATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar produto '{}': {}", dto.nome(), e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("produtoDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar produto '{}': {}", dto.nome(), e.getMessage(), e);
            attributes.addFlashAttribute("error",
                    messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("produtoDTO", dto);
        }

        return "redirect:/dashboard/produtos";
    }

    @PostMapping("/{id}/excluir")
    public String excluirProduto(@PathVariable Long id, RedirectAttributes attributes, Locale locale) {
        try {
            produtoService.deletarProduto(id);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir produto id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir produto id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        return "redirect:/dashboard/produtos";
    }

    @PostMapping("/{id}/editar")
    public String editarProduto(@PathVariable Long id, @Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                                BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, locale);
        }

        try {
            produtoService.editarProduto(id, dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_UPDATED, new Object[]{dto.nome()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar produto id {}: {}", id, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("produtoDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar produto id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource
                    .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("produtoDTO", dto);
        }

        return "redirect:/dashboard/produtos";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model, null);
        model.addAttribute("error", messageSource
                .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "produto/Produtos";
    }

    private void carregarPagina(Model model, String nome) {
        try {
            model.addAttribute("produtos", produtoService.buscarProdutos(nome));
        } catch (Exception e) {
            log.error("Erro ao carregar produtos na página: {}", e.getMessage(), e);
            model.addAttribute("produtos", List.of());
        }

        if (!model.containsAttribute("produtoDTO")) {
            model.addAttribute("produtoDTO",
                    new ProdutoDTO(null, null, null, null));
        }
    }
}