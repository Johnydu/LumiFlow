package br.com.lumiflow.controller;
import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.vidracaria.ChapaVidroDTO;
import br.com.lumiflow.dto.vidracaria.MovimentacaoVidroDTO;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.VidracariaService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Locale;

@Controller @AllArgsConstructor @RequestMapping("/dashboard/vidracaria")
public class VidracariaController {
    private final VidracariaService vidracariaService; private final OperadorService operadorService; private final MessageSource messageSource;
    @GetMapping public String index(Model model) { carregarPagina(model); return "vidracaria/Vidracaria"; }
    @PostMapping("/movimentar") public String registrarMovimentacao(@Valid @ModelAttribute("movimentacaoDTO") MovimentacaoVidroDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); vidracariaService.registrarMovimentacao(dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_GLASS_MOVEMENT_CREATED, null, locale)); return "redirect:/dashboard/vidracaria"; }
    @PostMapping("/cadastrar-chapa") public String cadastrarChapa(@Valid @ModelAttribute("chapaDTO") ChapaVidroDTO dto, BindingResult result, Model model, RedirectAttributes attributes, Locale locale) { if (result.hasErrors()) return erroValidacao(model, locale); vidracariaService.cadastrarChapa(dto); attributes.addFlashAttribute("success", messageSource.getMessage(AppMessages.SUCCESS_GLASS_TYPE_CREATED, null, locale)); return "redirect:/dashboard/vidracaria"; }
    private String erroValidacao(Model model, Locale locale) { carregarPagina(model); model.addAttribute("error", messageSource.getMessage(AppMessages.ERROR_VALIDATION_FAILED, null, locale)); return "vidracaria/Vidracaria"; }
    private void carregarPagina(Model model) { model.addAttribute("saldos", vidracariaService.obterSaldosAtuais()); model.addAttribute("chapas", vidracariaService.listarChapas()); model.addAttribute("historico", vidracariaService.listarHistorico()); model.addAttribute("operadores", operadorService.listarOperadores()); }
}
