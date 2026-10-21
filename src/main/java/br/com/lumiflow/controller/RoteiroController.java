package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.roteiro.DuplicarRoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.VincularProdutosDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.RoteiroService;
import br.com.lumiflow.service.SetorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/roteiros")
public class RoteiroController {

    private static final String PODE_GERENCIAR = "hasAnyRole('ADMIN', 'SUPERVISOR','PCP')";
    private static final String REDIRECT_LISTA = "redirect:/dashboard/roteiros";

    private final RoteiroService roteiroService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String listar(@RequestParam(value = "busca", required = false) String busca,
                         Model model, HttpServletRequest request) {
        log.debug("Acessando listagem de roteiros. Termo de busca: [{}]", busca);
        model.addAttribute("roteiros", roteiroService.listarRoteiros(busca));

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "roteiro/Roteiros :: listaRoteiros";
        }

        model.addAttribute("setores", setorService.listarSetores());
        model.addAttribute("produtos", roteiroService.listarProdutosParaVinculo());
        if (!model.containsAttribute("roteiroDTO")) {
            model.addAttribute("roteiroDTO", new RoteiroDTO(null, null, null, new ArrayList<>()));
        }
        return "roteiro/Roteiros";
    }

    @GetMapping("/{id}")
    @ResponseBody
    public ResponseEntity<RoteiroDTO> buscar(@PathVariable Long id) {
        log.debug("Buscando detalhes do roteiro ID: [{}]", id);
        try {
            return ResponseEntity.ok(roteiroService.buscarPorId(id));
        } catch (BusinessException e) {
            log.warn("Roteiro ID: [{}] não encontrado: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @PreAuthorize(PODE_GERENCIAR)
    public String salvar(@Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto, BindingResult result,
                         RedirectAttributes attributes, Locale locale, Principal principal) {
        String usuario = principal != null ? principal.getName() : "Desconhecido";
        if (result.hasErrors()) {
            log.warn("Erro de validação ao criar roteiro pelo usuário [{}]. Erros: {}", usuario, result.getAllErrors());
            return erroValidacao(attributes, locale);
        }

        log.info("Usuário [{}] criando novo roteiro com código [{}]", usuario, dto.codigo());
        roteiroService.criar(dto);
        return sucesso(attributes, AppMessages.SUCCESS_ROUTE_CREATED, locale);
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize(PODE_GERENCIAR)
    public String editar(@PathVariable Long id, @Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto,
                         BindingResult result, RedirectAttributes attributes, Locale locale, Principal principal) {
        String usuario = principal != null ? principal.getName() : "Desconhecido";
        if (result.hasErrors()) {
            log.warn("Erro de validação ao atualizar roteiro ID: [{}] pelo usuário [{}]", id, usuario);
            return erroValidacao(attributes, locale);
        }

        log.info("Usuário [{}] atualizando roteiro ID: [{}]", usuario, id);
        roteiroService.atualizar(id, dto);
        return sucesso(attributes, AppMessages.SUCCESS_ROUTE_UPDATED, locale);
    }

    @PostMapping("/{id}/duplicar")
    @PreAuthorize(PODE_GERENCIAR)
    public String duplicar(@PathVariable Long id, @Valid @ModelAttribute("duplicarDTO") DuplicarRoteiroDTO dto,
                           BindingResult result, RedirectAttributes attributes, Locale locale, Principal principal) {
        String usuario = principal != null ? principal.getName() : "Desconhecido";
        if (result.hasErrors()) {
            log.warn("Erro de validação ao duplicar roteiro ID: [{}] pelo usuário [{}]", id, usuario);
            return erroValidacao(attributes, locale);
        }

        log.info("Usuário [{}] duplicando roteiro ID: [{}] para novo código [{}]", usuario, id, dto.codigo());
        roteiroService.duplicar(id, dto.codigo());
        return sucesso(attributes, AppMessages.SUCCESS_ROUTE_DUPLICATED, locale);
    }

    @PostMapping("/{id}/produtos")
    @PreAuthorize(PODE_GERENCIAR)
    public String vincularProdutos(@PathVariable Long id, @ModelAttribute("vincularDTO") VincularProdutosDTO dto,
                                   RedirectAttributes attributes, Locale locale, Principal principal) {
        String usuario = principal != null ? principal.getName() : "Desconhecido";
        log.info("Usuário [{}] atualizando vínculo de produtos para o roteiro ID: [{}]", usuario, id);

        roteiroService.vincularProdutos(id, dto.produtoIds());
        return sucesso(attributes, AppMessages.SUCCESS_ROUTE_PRODUCTS_LINKED, locale);
    }

    @PostMapping("/{id}/excluir")
    @PreAuthorize(PODE_GERENCIAR)
    public String excluir(@PathVariable Long id, RedirectAttributes attributes, Locale locale, Principal principal) {
        String usuario = principal != null ? principal.getName() : "Desconhecido";
        log.info("Usuário [{}] solicitando exclusão do roteiro ID: [{}]", usuario, id);

        roteiroService.excluir(id);
        return sucesso(attributes, AppMessages.SUCCESS_ROUTE_DELETED, locale);
    }

    private String sucesso(RedirectAttributes attributes, String chave, Locale locale) {
        attributes.addFlashAttribute("success", messageSource.getMessage(chave, null, locale));
        return REDIRECT_LISTA;
    }

    private String erroValidacao(RedirectAttributes attributes, Locale locale) {
        attributes.addFlashAttribute("error",
                messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        return REDIRECT_LISTA;
    }
}