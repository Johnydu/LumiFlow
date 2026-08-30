package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.security.UsuarioDetails;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.ProdutoService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/ordens")
public class OrdemProducaoController {

    private final OrdemProducaoService ordemProducaoService;
    private final ProdutoService produtoService;
    private final MessageSource messageSource;



    @GetMapping("/novaOrdem")
    public String novaOrdemForm(Model model) {
        carregarPaginaNovaOrdem(model);
        return "ordens/NovaOrdem";
    }

    @PostMapping
    public String novaOrdem(@Valid @ModelAttribute("ordemDTO") OrdemProducaoDTO dto, BindingResult result,
                            Model model, RedirectAttributes attributes, Locale locale,
                            @AuthenticationPrincipal UsuarioDetails usuarioLogado) {

        if (result.hasErrors()) return erroValidacao(model, locale);

        ordemProducaoService.criar(dto, usuarioLogado.getUsuario());
        attributes.addFlashAttribute("success", messageSource
                .getMessage(AppMessages.SUCCESS_ORDER_CREATED, null, locale));
        return "redirect:/dashboard";
    }

    private String erroValidacao(Model model, Locale locale) {
        carregarPaginaNovaOrdem(model);
        model.addAttribute("error", messageSource
                .getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return "ordens/NovaOrdem";
    }

    private void carregarPaginaNovaOrdem(Model model) {
        model.addAttribute("produtos", produtoService.listarProdutos());

        if (!model.containsAttribute("ordemDTO"))
            model.addAttribute("ordemDTO", new OrdemProducaoDTO(null,
                    null, null, null));
    }
}