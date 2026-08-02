package br.com.lumiflow.controller;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.service.EtapaSetorService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/dashboard/setores")
@AllArgsConstructor
public class EtapaSetorController {

    private final EtapaSetorService etapaSetorService;

    @GetMapping("/{setorId}/etapas")
    @ResponseBody
    public List<EtapaSetorDTO> listarEtapasPorSetor(@PathVariable Long setorId) {
        return etapaSetorService.listarPorSetorId(setorId).stream()
                .map(e -> new EtapaSetorDTO(e.getId(), e.getNome(),e.getOrdem()))
                .toList();
    }
}