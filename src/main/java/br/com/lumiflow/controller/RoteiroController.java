package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.ProdutoService;
import br.com.lumiflow.service.RoteiroService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/roteiros")
public class RoteiroController {

    private final RoteiroService roteiroService;
    private final ProdutoService produtoService;
    private final SetorService setorService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("roteiros", roteiroService.listarRoteiros());
        model.addAttribute("produtos", produtoService.listarProdutosPorCodigo());
        model.addAttribute("setores", setorService.listarSetores());
        model.addAttribute("roteiroDTO", new RoteiroDTO(null, new ArrayList<>()));
        return "roteiro/Roteiros";
    }

    @GetMapping("/{produtoId}")
    @ResponseBody
    public RoteiroDTO buscar(@PathVariable Long produtoId) {
        return roteiroService.buscarRoteiroPorProdutoId(produtoId);
    }

    @PostMapping
    public String salvar(@Valid @ModelAttribute("roteiroDTO") RoteiroDTO roteiroDTO,
                         BindingResult result,
                         RedirectAttributes attributes) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/roteiros";
        }

        try {
            // Chama especificamente o método de criação (Responsabilidade Única)
            roteiroService.criar(roteiroDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_ROUTE_CREATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/editar")
    public String editar(@PathVariable Long produtoId,
                         @Valid @ModelAttribute("roteiroDTO") RoteiroDTO roteiroDTO,
                         BindingResult result,
                         RedirectAttributes attributes) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/roteiros";
        }

        try {
            // Chama especificamente o método de atualização isolado
            roteiroService.atualizar(produtoId, roteiroDTO);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_ROUTE_UPDATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/excluir")
    public String excluir(@PathVariable Long produtoId,
                          RedirectAttributes attributes) {

        try {
            roteiroService.excluirPorProdutoId(produtoId);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_ROUTE_DELETED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/roteiros";
    }
}
