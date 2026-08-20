package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;

import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.VidracariaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@AllArgsConstructor
@RequestMapping("/dashboard/vidracaria")
public class VidracariaController {

    private final VidracariaService vidracariaService;
    private final OperadorService operadorService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("saldos", vidracariaService.obterSaldosAtuais());
        model.addAttribute("chapas", vidracariaService.listarChapas());
        model.addAttribute("historico", vidracariaService.listarHistorico());

        // Passa a lista de operadores cadastrados para o HTML[cite: 1]
        model.addAttribute("operadores", operadorService.listarOperadores());

        return "vidracaria/Vidracaria";
    }

    @PostMapping("/movimentar")
    public String registrarMovimentacao(@Valid @ModelAttribute ("movimentacaoDTO") MovimentacaoVidroDTO dto,
                                        BindingResult result, RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/vidracaria";
        }
        try {
            vidracariaService.registrarMovimentacao(dto);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_GLASS_MOVEMENT_CREATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/dashboard/vidracaria";

    }

    @PostMapping("/cadastrar-chapa")
    public String cadastrarChapa(@Valid @ModelAttribute("chapaDTO") ChapaVidroDTO dto,
                                 BindingResult result,
                                 RedirectAttributes attributes) {
        if (result.hasErrors()) {
            attributes.addFlashAttribute("error", AppMessages.ERROR_VALIDATION_FAILED);
            return "redirect:/dashboard/vidracaria";
        }

        try {
            vidracariaService.cadastrarChapa(dto);
            attributes.addFlashAttribute("success", AppMessages.SUCCESS_GLASS_TYPE_CREATED);
        } catch (BusinessException e) {
            attributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/dashboard/vidracaria";
    }
}
