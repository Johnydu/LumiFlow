package br.com.lumiflow.service;

import br.com.lumiflow.dto.ordens.OrdemFiltroDTO;
import br.com.lumiflow.entity.OrdemProducao;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class OrdemProducaoService {

    private final OrdemProducaoRepository ordemProducaoRepository;

    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorFiltros(OrdemFiltroDTO filtro) {
        String busca = (filtro.busca() != null && !filtro.busca().isBlank()) ? filtro.busca().trim() : null;
        String status = (filtro.status() != null && !filtro.status().isBlank()) ? filtro.status().trim() : null;

        return ordemProducaoRepository.buscarComFiltros(busca, status);
    }

    @Transactional(readOnly = true)
    public List<OrdemProducao> listarPorSetorIdEFiltros(Long setorId, OrdemFiltroDTO filtro) {
        String busca = (filtro.busca() != null && !filtro.busca().isBlank()) ? filtro.busca().trim() : null;
        String status = (filtro.status() != null && !filtro.status().isBlank()) ? filtro.status().trim() : null;

        return ordemProducaoRepository.buscarPorSetorIdEFiltros(setorId, busca, status);
    }

    public Page<OrdemProducao> listarOrdens(int pagina, int tamanho) {
        Pageable pageable = PageRequest.of(pagina, tamanho);
        return ordemProducaoRepository.findAllOrdenadasPorEmAndamento(pageable);
    }
}