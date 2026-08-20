package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.model.RoteiroProduto;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service @AllArgsConstructor
public class RoteiroService {
    private final RoteiroProdutoRepository roteiroProdutoRepository; private final ProdutoService produtoService; private final SetorService setorService; private final EtapaSetorService etapaSetorService;
    public RoteiroDTO buscarRoteiroPorProdutoId(Long produtoId) { List<RoteiroProduto> roteiros = roteiroProdutoRepository.buscarPorProdutoId(produtoId); return new RoteiroDTO(produtoId, roteiros.stream().map(r -> new PassoRoteiroDTO(r.getSetor().getId(), r.getEtapaSetor() == null ? null : r.getEtapaSetor().getId())).toList()); }
    @Transactional(readOnly = true) public List<RoteiroListagemDTO> listarRoteiros() { return roteiroProdutoRepository.listarOrdenados().stream().collect(Collectors.groupingBy(r -> r.getProduto().getId(), java.util.LinkedHashMap::new, Collectors.toList())).values().stream().map(lista -> { RoteiroProduto primeiro = lista.getFirst(); List<String> fluxo = lista.stream().map(r -> r.getEtapaSetor() == null ? r.getSetor().getNome() : r.getSetor().getNome() + " (" + r.getEtapaSetor().getNome() + ")").toList(); return new RoteiroListagemDTO(primeiro.getProduto().getId(), primeiro.getProduto().getNome(), primeiro.getProduto().getCodigo(), lista.size(), fluxo); }).toList(); }
    @Transactional public void criar(RoteiroDTO dto) { validarRoteiro(dto); if (!roteiroProdutoRepository.buscarPorProdutoId(dto.produtoId()).isEmpty()) throw new BusinessException(AppMessages.ERROR_ROUTE_ALREADY_EXISTS); salvarPersistenciaRoteiro(dto.produtoId(), dto); }
    @Transactional public void atualizar(Long produtoIdOriginal, RoteiroDTO dto) { validarRoteiro(dto); roteiroProdutoRepository.deletarPorProdutoId(produtoIdOriginal); salvarPersistenciaRoteiro(dto.produtoId(), dto); }
    @Transactional public void excluirPorProdutoId(Long produtoId) { roteiroProdutoRepository.deletarPorProdutoId(produtoId); }
    private void validarRoteiro(RoteiroDTO dto) { if (dto.produtoId() == null) throw new BusinessException(AppMessages.ERROR_ROUTE_PRODUCT_REQUIRED); if (dto.passos() == null || dto.passos().isEmpty()) throw new BusinessException(AppMessages.ERROR_ROUTE_STEP_REQUIRED); }
    private void salvarPersistenciaRoteiro(Long produtoId, RoteiroDTO dto) { var produto = produtoService.buscarProdutoPorId(produtoId); List<RoteiroProduto> roteiro = new ArrayList<>(); int sequencia = 10; for (var passo : dto.passos()) { var item = new RoteiroProduto(); item.setProduto(produto); item.setSetor(setorService.buscarSetorPorId(passo.setorId())); item.setSequencia(sequencia); if (passo.etapaSetorId() != null) item.setEtapaSetor(java.util.Optional.ofNullable(etapaSetorService.buscarPorId(passo.etapaSetorId())).orElseThrow(() -> new BusinessException(AppMessages.ERROR_ROUTE_STEP_NOTFOUND, new Object[]{passo.etapaSetorId()}))); roteiro.add(item); sequencia += 10; } roteiroProdutoRepository.saveAll(roteiro); }
}
