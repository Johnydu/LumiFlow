package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/dashboard/roteiros")
public class RoteiroController {

    private final RoteiroService roteiroService;
    private final ProdutoService produtoService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String listar(
            @RequestParam(value = "nomeProduto", required = false) String nomeProduto,
            Model model,
            HttpServletRequest request,
            Locale locale) {

        try {
            carregarPagina(model, nomeProduto);
        } catch (Exception e) {
            log.error("Erro inesperado ao carregar a página de roteiros: {}", e.getMessage(), e);
            model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            model.addAttribute("roteiros", List.of());
            model.addAttribute("produtos", List.of());
            model.addAttribute("setores", List.of());
            if (!model.containsAttribute("roteiroDTO")) {
                model.addAttribute("roteiroDTO", new RoteiroDTO(null, new ArrayList<>()));
            }
        }

        if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
            return "roteiro/Roteiros :: listaRoteiros";
        }

        return "roteiro/Roteiros";
    }

    @GetMapping("/{produtoId}")
    @ResponseBody
    public ResponseEntity<RoteiroDTO> buscar(@PathVariable Long produtoId) {
        try {
            if (produtoId == null) {
                log.warn("Tentativa de buscar roteiro com ID de produto nulo.");
                return ResponseEntity.badRequest().build();
            }
            RoteiroDTO dto = roteiroService.buscarRoteiroPorProdutoId(produtoId);
            return ResponseEntity.ok(dto);
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao buscar roteiro para o produto id {}: {}", produtoId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar roteiro para o produto id {}: {}", produtoId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String salvar(@Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto, BindingResult result,
                         Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            roteiroService.criar(dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_ROUTE_CREATED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao criar roteiro: {}", e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("roteiroDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao criar roteiro: {}", e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("roteiroDTO", dto);
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/editar")
    @PreAuthorize("hasAnyRole('PCP','SUPERVISOR')")
    public String editar(@PathVariable Long produtoId, @Valid @ModelAttribute("roteiroDTO") RoteiroDTO dto,
                         BindingResult result, Model model, RedirectAttributes attributes, Locale locale) {
        if (result.hasErrors()) {
            return erroValidacao(model, result, locale);
        }

        try {
            roteiroService.atualizar(produtoId, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_ROUTE_UPDATED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao atualizar roteiro para o produto id {}: {}", produtoId, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
            attributes.addFlashAttribute("roteiroDTO", dto);
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar roteiro para o produto id {}: {}", produtoId, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
            attributes.addFlashAttribute("roteiroDTO", dto);
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/excluir")
    public String excluir(@PathVariable Long produtoId, RedirectAttributes attributes, Locale locale) {
        try {
            roteiroService.excluirPorProdutoId(produtoId);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_ROUTE_DELETED, null, locale));
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao excluir roteiro do produto id {}: {}", produtoId, e.getMessage());
            String errorMessage = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
            attributes.addFlashAttribute("error", errorMessage);
        } catch (Exception e) {
            log.error("Erro inesperado ao excluir roteiro do produto id {}: {}", produtoId, e.getMessage(), e);
            attributes.addFlashAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        }

        return "redirect:/dashboard/roteiros";
    }

    private String erroValidacao(Model model, BindingResult result, Locale locale) {
        carregarPagina(model, null);
        String mensagem = result.getAllErrors().stream()
                .findFirst()
                .map(error -> {
                    String defaultMessage = error.getDefaultMessage();
                    return defaultMessage == null ? "" :
                            messageSource.getMessage(defaultMessage, null, defaultMessage, locale);
                })
                .orElseGet(() -> messageSource.getMessage(
                        AppMessages.ERROR_VALIDATION_FAILED, null, locale));
        model.addAttribute("error", mensagem);
        return "roteiro/Roteiros";
    }

    private void carregarPagina(Model model, String nomeProduto) {
        try {
            if (nomeProduto != null && !nomeProduto.isBlank()) {
                model.addAttribute("roteiros", roteiroService.buscarPorNomeOuCodigoProduto(nomeProduto.trim()));
            } else {
                model.addAttribute("roteiros", roteiroService.listarRoteiros());
            }

            model.addAttribute("produtos", produtoService.listarProdutos());
            model.addAttribute("setores", setorService.listarSetores());
        } catch (Exception e) {
            log.error("Erro ao carregar dados auxiliares da página de roteiros: {}", e.getMessage(), e);
            model.addAttribute("roteiros", List.of());
            model.addAttribute("produtos", List.of());
            model.addAttribute("setores", List.of());
        }

        if (!model.containsAttribute("roteiroDTO")) {
            model.addAttribute("roteiroDTO", new RoteiroDTO(null, new ArrayList<>()));
        }
    }

    @GetMapping("/{produtoId}/setor/{setorId}/etapas")
    @ResponseBody
    public List<EtapaSetorDTO> buscarEtapasDoSetorNoRoteiro(@PathVariable Long produtoId,
                                                            @PathVariable Long setorId) {
        return roteiroService.buscarEtapasDoSetorNoRoteiro(produtoId, setorId);
    }

}