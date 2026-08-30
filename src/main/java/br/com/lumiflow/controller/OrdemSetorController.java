package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.dto.ordens.OrdemListagemDTO;
import br.com.lumiflow.dto.ordens.ResumoSetorDTO;
import br.com.lumiflow.entity.Setor;
import br.com.lumiflow.security.CustomAuthenticationSuccessHandler;
import br.com.lumiflow.service.OrdemProducaoService;
import br.com.lumiflow.service.OrdemSetorService;
import br.com.lumiflow.service.SetorService;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/dashboard/ordens-setor")
@AllArgsConstructor
public class OrdemSetorController {

    private final OrdemSetorService ordemSetorService;
    private final OrdemProducaoService ordemProducaoService;
    private final SetorService setorService;
    private final MessageSource messageSource;


    @GetMapping
    public String listarOrdensSetor(
            @RequestParam(value = "busca", required = false) String busca,
            @RequestParam(value = "data", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Model model) {

        List<ResumoSetorDTO> resumoSetores = ordemSetorService.obterResumoSetores(busca, data);
        model.addAttribute("resumoSetores", resumoSetores);

        return "ordens/ListaOrdemSetores";
    }

    @GetMapping("/{setorId}")
    public String listarOrdensPorSetorId(@PathVariable("setorId") Long setorId,
                                         @ModelAttribute OrdemFiltroDTO filtro,
                                         Model model) {

        List<OrdemListagemDTO> ordens = ordemProducaoService.listarPorSetorIdEFiltros(setorId, filtro);
        Setor setor = setorService.buscarSetorPorId(setorId);

        model.addAttribute("ordens", ordens);
        model.addAttribute("setorAtual", setor != null ? setor.getNome() : "Desconhecido");
        model.addAttribute("setorIdAtual", setorId);
        model.addAttribute("buscaAtual", filtro.busca());
        model.addAttribute("statusAtual", filtro.status());
        model.addAttribute("totalOrdens", ordens.size());

        return "ordens/ListaOrdem";
    }

    @PreAuthorize("hasAnyRole('PCP_SUPERVISOR', 'GESTAO')")
    @PostMapping("/{setorId}/ordens/{ordemId}/liberar")
    public String liberarOrdem(@PathVariable Long setorId, @PathVariable Long ordemId,
                               RedirectAttributes attributes, Locale locale) {

        ordemProducaoService.liberarOrdem(ordemId, setorId);
        attributes.addFlashAttribute("success", messageSource
                .getMessage(AppMessages.SUCCESS_ORDER_LIBERADA, null, locale));

        return "redirect:/dashboard/ordens-setor/" + setorId;
    }
}

