package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemProducaoDTO;
import br.com.lumiflow.dto.produto.ProdutoBuscaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.security.UsuarioDetails;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.ProdutoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/ordens")
public class OrdemProducaoController {

    private final OrdemProducaoService ordemProducaoService;
    private final ProdutoService produtoService;
    private final MessageSource messageSource;

    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    @GetMapping("/novaOrdem")
    public String novaOrdemForm(Model model, Locale locale) {
        try {
            carregarPaginaNovaOrdem(model);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar o formulário de nova ordem: {}", e.getMessage(), e);
            model.addAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            if (!model.containsAttribute("ordemDTO")) {
                model.addAttribute("ordemDTO",
                        new OrdemProducaoDTO(null, null, null, null));
            }
        }
        return "ordens/NovaOrdem";
    }

    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    @PostMapping
    public String novaOrdem(@Valid @ModelAttribute("ordemDTO") OrdemProducaoDTO dto, BindingResult result,
                            Model model, RedirectAttributes attributes, Locale locale,
                            @AuthenticationPrincipal UsuarioDetails usuarioLogado) {

        if (usuarioLogado == null) {
            // Principal ausente ou de outro tipo: sem identificar o autor não há como criar a ordem.
            throw new AccessDeniedException("Usuário autenticado não identificado");
        }

        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            ordemProducaoService.criar(dto, usuarioLogado.getId());
            attributes.addFlashAttribute("success", messageSource
                    .getMessage(AppMessages.SUCCESS_ORDER_CREATED, null, locale));
            return "redirect:/dashboard";
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar ordem de produção: {}", e.getMessage());
            String errorMessage = MensagensWeb.de(messageSource, e, locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("ordemDTO", dto);
            return "redirect:/dashboard/ordens/novaOrdem";
        } catch (Exception e) {
            log.error("Erro inesperado ao criar ordem de produção: {}", e.getMessage(), e);
            attributes.addFlashAttribute("error", MensagensWeb.validacaoFalhou(messageSource, locale));
            attributes.addFlashAttribute("ordemDTO", dto);
            return "redirect:/dashboard/ordens/novaOrdem";
        }
    }

    private String erroValidacao(Model model, BindingResult result, Locale locale) {
        carregarPaginaNovaOrdem(model);
        String mensagem = MensagensWeb.deBinding(messageSource, result, locale);
        model.addAttribute("error", mensagem);
        return "ordens/NovaOrdem";
    }

    private void carregarPaginaNovaOrdem(Model model) {
        if (!model.containsAttribute("ordemDTO")) {
            model.addAttribute("ordemDTO",
                    new OrdemProducaoDTO(null, null, null, null));
        }
        OrdemProducaoDTO dto = (OrdemProducaoDTO) model.getAttribute("ordemDTO");
        ProdutoBuscaDTO produtoSelecionado = dto == null ? null
                : produtoService.buscarParaSelecaoPorId(dto.produtoId());
        model.addAttribute("produtoSelecionado", produtoSelecionado);
    }
}