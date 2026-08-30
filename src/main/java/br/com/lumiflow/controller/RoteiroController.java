package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.service.ProdutoService;
import br.com.lumiflow.service.RoteiroService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Locale;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/roteiros")
public class RoteiroController {
    private final RoteiroService roteiroService;
    private final ProdutoService produtoService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String listar(Model model) {
        carregarPagina(model);
        return "roteiro/Roteiros";
    }

    @GetMapping("/{produtoId}")
    @ResponseBody
    public RoteiroDTO buscar(@PathVariable Long produtoId) {
        return roteiroService.buscarRoteiroPorProdutoId(produtoId);
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) return erroValidacao(model, locale);
        roteiroService.criar(dto);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_ROUTE_CREATED, null, locale));
        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/editar")
    public String editar(@PathVariable Long produtoId, @Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) return erroValidacao(model, locale);
        roteiroService.atualizar(produtoId, dto);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_ROUTE_UPDATED, null, locale));
        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/excluir")
    public String excluir(@PathVariable Long produtoId, RedirectAttributes attributes, Locale locale) {
        roteiroService.excluirPorProdutoId(produtoId);
        attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_ROUTE_DELETED, null, locale));
        return "redirect:/dashboard/roteiros";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPagina(model);
        model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "roteiro/Roteiros";
    }

    private void carregarPagina(Model model) {
        model.addAttribute("roteiros", roteiroService.listarRoteiros());
        model.addAttribute("produtos", produtoService.listarProdutos());
        model.addAttribute("setores", setorService.listarSetores());
        if (!model.containsAttribute("roteiroDTO"))
            model.addAttribute("roteiroDTO", new RoteiroDTO(null, new ArrayList<>()));
    }
}
