package br.com.lumiflow.controller;

import br.com.lumiflow.dto.lancamento.HistoricoLancamentoFiltroDTO;
import br.com.lumiflow.security.UsuarioDetails;
import br.com.lumiflow.service.EtapaSetorService;
import br.com.lumiflow.service.HistoricoLancamentosService;
import br.com.lumiflow.service.OperadorService;
import br.com.lumiflow.service.SetorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/dashboard/historico")
@RequiredArgsConstructor
public class HistoricoLancamentosController {

    private static final int TAMANHO_PAGINA = 50;

    private final HistoricoLancamentosService historicoLancamentosService;
    private final SetorService setorService;
    private final OperadorService operadorService;
    private final EtapaSetorService etapaSetorService;

    @GetMapping
    public String listarHistorico(@ModelAttribute("filtro") HistoricoLancamentoFiltroDTO filtro,
                                  Authentication authentication,
                                  @AuthenticationPrincipal UsuarioDetails usuarioLogado,
                                  @RequestParam(value = "setorId", required = false) Long setorIdParam,
                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                  Model model) {

        boolean isSuperior = authentication.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN") ||
                        a.getAuthority().equals("ROLE_PCP") ||
                        a.getAuthority().equals("ROLE_GESTAO") ||
                        a.getAuthority().equals("ROLE_SUPERVISOR")
        );

        // Se for superior, usa o filtro da tela; se for operador, prioriza o setor que veio via parâmetro da URL (ou mantém se já estiver no filtro)
        Long setorId = isSuperior ? filtro.setorId() : (setorIdParam != null ? setorIdParam : filtro.setorId());
        Long operadorId = isSuperior ? filtro.operadorId() : null;

        HistoricoLancamentoFiltroDTO filtroAplicado =
                new HistoricoLancamentoFiltroDTO(setorId, filtro.etapaSetorId(), operadorId);

        model.addAttribute("pagina", historicoLancamentosService.listarComFiltros(
                filtroAplicado, PageRequest.of(Math.max(page, 0), TAMANHO_PAGINA)));
        model.addAttribute("setores", isSuperior ? setorService.listarSetores() : List.of());
        model.addAttribute("operadores", isSuperior ? operadorService.listarOperadores() : List.of());
        model.addAttribute("etapas", setorId == null
                ? List.of()
                : etapaSetorService.listarPorSetorId(setorId));
        model.addAttribute("filtro", filtroAplicado);
        model.addAttribute("isSuperior", isSuperior);

        return "historico/HistoricoLancamento";
    }
}