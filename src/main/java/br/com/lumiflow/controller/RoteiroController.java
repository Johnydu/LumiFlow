package br.com.lumiflow.controller;

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
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/roteiros";
        }

        try {
            // Chama especificamente o método de criação (Responsabilidade Única)
            roteiroService.criar(roteiroDTO);
            attributes.addFlashAttribute("message", "Roteiro cadastrado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/editar")
    public String editar(@PathVariable Long produtoId,
                         @Valid @ModelAttribute("roteiroDTO") RoteiroDTO roteiroDTO,
                         BindingResult result,
                         RedirectAttributes attributes) {

        if (result.hasErrors()) {
            attributes.addFlashAttribute("message", "Preencha os campos corretamente");
            attributes.addFlashAttribute("messageType", "error");
            return "redirect:/dashboard/roteiros";
        }

        try {
            // Chama especificamente o método de atualização isolado
            roteiroService.atualizar(produtoId, roteiroDTO);
            attributes.addFlashAttribute("message", "Roteiro atualizado com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/dashboard/roteiros";
    }

    @PostMapping("/{produtoId}/excluir")
    public String excluir(@PathVariable Long produtoId,
                          RedirectAttributes attributes) {

        try {
            roteiroService.excluirPorProdutoId(produtoId);
            attributes.addFlashAttribute("message", "Roteiro removido com sucesso");
            attributes.addFlashAttribute("messageType", "success");
        } catch (BusinessException e) {
            attributes.addFlashAttribute("message", e.getMessage());
            attributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/dashboard/roteiros";
    }
}