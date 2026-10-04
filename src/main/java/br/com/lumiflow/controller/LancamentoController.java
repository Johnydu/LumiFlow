package br.com.lumiflow.controller;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.lancamento.LancamentoProducaoDTO;
import br.com.lumiflow.dto.lancamento.RefugoDTO;
import br.com.lumiflow.dto.lancamento.RetrabalhoDTO;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.service.LancamentoService;
import br.com.lumiflow.service.SetorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Slf4j
@Controller
@RequestMapping("/dashboard/ordens/{ordemId}/setor/{ordemSetorId}/lancar")
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService lancamentoService;
    private final SetorService setorService;
    private final MessageSource messageSource;

    @GetMapping
    public String form(@PathVariable Long ordemId, @PathVariable Long ordemSetorId, Model model) {
        OrdemSetor ordemSetor = lancamentoService.buscarEtapaAtiva(ordemId, ordemSetorId);

        model.addAttribute("ordem", ordemSetor.getOrdemProducao());
        model.addAttribute("ordemSetor", ordemSetor);
        model.addAttribute("etapaNome", lancamentoService.buscarNomeEtapa(ordemSetor));

        model.addAttribute("maquinas",
                lancamentoService.listarMaquinasDoSetor(ordemSetor.getSetor().getId()));
        model.addAttribute("operadores",
                lancamentoService.listarOperadoresDoSetor(ordemSetor.getSetor().getId()));
        model.addAttribute("setores", setorService.listarSetores());
        model.addAttribute("refugosParaRetrabalho",
                lancamentoService.listarRefugosParaRetrabalho(ordemId));
        model.addAttribute("todasMaquinasParaRetrabalho",
                lancamentoService.listarMaquinasParaRetrabalho(ordemId));

        if (!model.containsAttribute("producaoDTO"))
            model.addAttribute("producaoDTO",
                    new LancamentoProducaoDTO(null, null, null, null));
        if (!model.containsAttribute("refugoDTO"))
            model.addAttribute("refugoDTO",
                    new RefugoDTO(null, null, null, null, null,null));
        if (!model.containsAttribute("retrabalhoDTO"))
            model.addAttribute("retrabalhoDTO",
                    new RetrabalhoDTO(null, null, null, null,null));

        return "ordens/Lancamento";
    }

    @PostMapping("/producao")
    public String producao(@PathVariable Long ordemId, @PathVariable Long ordemSetorId,
                           @Valid @ModelAttribute("producaoDTO") LancamentoProducaoDTO dto,
                           BindingResult result, RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) {
            return recarregarComErro(ordemId, ordemSetorId, "producaoDTO", dto, result, attributes);
        }

        try {
            OrdemSetor ordemSetor = lancamentoService.lancarProducao(ordemId, ordemSetorId, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_LAUNCH_REGISTERED, null, locale));

            if (ordemSetor.getStatus() == EstatusOrdemProducao.CONCLUIDA) {
                return "redirect:/dashboard/ordens-setor/" + ordemSetor.getSetor().getId();
            }

            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";

        } catch (BusinessException e) {
            log.warn("Erro de negócio ao lançar produção na ordem {}: {}", ordemId, e.getMessage());
            attributes.addFlashAttribute("error", traduzir(e, locale));
            attributes.addFlashAttribute("producaoDTO", dto);
            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";
        }
    }

    @PostMapping("/refugo")
    public String refugo(@PathVariable Long ordemId, @PathVariable Long ordemSetorId,
                         @Valid @ModelAttribute("refugoDTO") RefugoDTO dto, BindingResult result,
                         RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) {
            return recarregarComErro(ordemId, ordemSetorId, "refugoDTO", dto, result, attributes);
        }

        try {
            OrdemSetor ordemSetor = lancamentoService.registrarRefugo(ordemId, ordemSetorId, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_REFUGO_REGISTERED, null, locale));

            if (ordemSetor.getStatus() == EstatusOrdemProducao.CONCLUIDA) {
                return "redirect:/dashboard/ordens-setor/" + ordemSetor.getSetor().getId();
            }

            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";
        } catch (BusinessException e) {
            log.warn("Erro de negócio ao registar refugo na ordem {}: {}", ordemId, e.getMessage());
            attributes.addFlashAttribute("error", traduzir(e, locale));
            attributes.addFlashAttribute("refugoDTO", dto);
            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";
        }
    }

    @PostMapping("/retrabalho")
    public String retrabalho(@PathVariable Long ordemId, @PathVariable Long ordemSetorId,
                             @Valid @ModelAttribute("retrabalhoDTO") RetrabalhoDTO dto, BindingResult result,
                             RedirectAttributes attributes, Locale locale) {

        if (result.hasErrors()) {
            return recarregarComErro(ordemId, ordemSetorId, "retrabalhoDTO", dto, result, attributes);
        }

        try {
            OrdemSetor ordemSetorOrigem = lancamentoService.registrarRetrabalho(ordemId, dto);
            attributes.addFlashAttribute("success",
                    messageSource.getMessage(AppMessages.SUCCESS_RETRABALHO_REGISTERED, null, locale));

            if (ordemSetorOrigem.getId().equals(ordemSetorId)
                    && ordemSetorOrigem.getStatus() == EstatusOrdemProducao.CONCLUIDA) {
                return "redirect:/dashboard/ordens-setor/" + ordemSetorOrigem.getSetor().getId();
            }

            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";

        } catch (BusinessException e) {
            log.warn("Erro de negócio ao registar retrabalho na ordem {}: {}", ordemId, e.getMessage());
            attributes.addFlashAttribute("error", traduzir(e, locale));
            attributes.addFlashAttribute("retrabalhoDTO", dto);
            return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";
        }
    }

    private String recarregarComErro(Long ordemId, Long ordemSetorId, String nomeDto, Object dto,
                                     BindingResult result, RedirectAttributes attributes) {
        attributes.addFlashAttribute("org.springframework.validation.BindingResult." + nomeDto, result);
        attributes.addFlashAttribute(nomeDto, dto);
        return "redirect:/dashboard/ordens/" + ordemId + "/setor/" + ordemSetorId + "/lancar";
    }

    private String traduzir(BusinessException e, Locale locale) {
        return messageSource.getMessage(e.getMessageKey(), e.getArgs(), e.getMessageKey(), locale);
    }

}