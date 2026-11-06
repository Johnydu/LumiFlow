package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.produto.ProdutoDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public String produtos(@RequestParam(required = false) String nome,
                           @RequestParam(value = "page", defaultValue = "0") int page,
                           Model model, Locale locale) {
        try {
            carregarPagina(model, nome, page);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de produtos: {}", e.getMessage(), e);
            model.addAttribute("error",
                    MensagensWeb.validacaoFalhou(messageSource, locale));
            model.addAttribute("produtos", List.of());
            model.addAttribute("pagina", Page.empty());
            model.addAttribute("nomeAtual", nome);
            if (!model.containsAttribute("produtoDTO")) {
                model.addAttribute("produtoDTO",
                        new ProdutoDTO(null, null, null, null));
            }
        }
        return "produto/Produtos";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String novoProduto(@Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                              BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            produtoService.novoProduto(dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_CREATED, new Object[]{dto.identificacao()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar produto '{}': {}", dto.nome(), e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("produtoDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar produto '{}': {}", dto.nome(), e.getMessage(), e);
            attributes.addFlashAttribute("error",
                    MensagensWeb.validacaoFalhou(messageSource, locale));
            attributes.addFlashAttribute("produtoDTO", dto);
        }

        return "redirect:/dashboard/produtos";
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String excluirProduto(@PathVariable Long id, RedirectAttributes attributes, Locale locale) {
        try {
            produtoService.deletarProduto(id);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir produto id {}: {}", id, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir produto id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
        }

        return "redirect:/dashboard/produtos";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String editarProduto(@PathVariable Long id, @Valid @ModelAttribute("produtoDTO") ProdutoDTO dto,
                                BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            produtoService.editarProduto(id, dto);
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_PRODUCT_UPDATED, new Object[]{dto.identificacao()}, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao editar produto id {}: {}", id, e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("produtoDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao editar produto id {}: {}", id, e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            attributes.addFlashAttribute("produtoDTO", dto);
        }

        return "redirect:/dashboard/produtos";
    }

    private String erroValidacao(Model model, BindingResult result, Locale locale) {
        carregarPagina(model, null, 0);
        String mensagem = MensagensWeb.deBinding(messageSource, result, locale);
        model.addAttribute("error", mensagem);
        return "produto/Produtos";
    }

    private void carregarPagina(Model model, String nome, int page) {
        // Falhas de carga propagam: o chamador mostra o erro ao usuário (nunca uma lista vazia silenciosa).
        Page<br.com.lumiflow.dto.produto.ProdutoListagemDTO> pagina = produtoService.buscarProdutos(nome, page);
        model.addAttribute("produtos", pagina.getContent());
        model.addAttribute("pagina", pagina);
        model.addAttribute("nomeAtual", nome);

        if (!model.containsAttribute("produtoDTO")) {
            model.addAttribute("produtoDTO",
                    new ProdutoDTO(null, null, null, null));
        }
    }
}