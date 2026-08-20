package br.com.lumiflow.service;

import br.com.lumiflow.dto.roteiro.PassoRoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroDTO;
import br.com.lumiflow.dto.roteiro.RoteiroListagemDTO;
import br.com.lumiflow.model.Produto;
import br.com.lumiflow.model.RoteiroProduto;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.RoteiroProdutoRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class RoteiroService {

    private final RoteiroProdutoRepository roteiroProdutoRepository;
    private final ProdutoService produtoService;
    private final SetorService setorService;
    private final EtapaSetorService etapaSetorService;

    public RoteiroDTO buscarRoteiroPorProdutoId(Long produtoId) {
        List<RoteiroProduto> roteiros = roteiroProdutoRepository.buscarPorProdutoId(produtoId);

        List<PassoRoteiroDTO> passos = roteiros.stream()
                .map(r -> new PassoRoteiroDTO(
                        r.getSetor().getId(),
                        r.getEtapaSetor() != null ? r.getEtapaSetor().getId() : null
                ))
                .toList();

        return new RoteiroDTO(produtoId, passos);
    }

    @Transactional(readOnly = true)
    public List<RoteiroListagemDTO> listarRoteiros() {
        return roteiroProdutoRepository.listarOrdenados()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getProduto().getId(),
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                ))
                .values()
                .stream()
                .map(lista -> {
                    RoteiroProduto primeiro = lista.get(0);

                    List<String> fluxoDescricao = lista.stream().map(r -> {
                        if (r.getEtapaSetor() != null) {
                            return r.getSetor().getNome() + " (" + r.getEtapaSetor().getNome() + ")";
                        }
                        return r.getSetor().getNome();
                    }).toList();

                    return new RoteiroListagemDTO(
                            primeiro.getProduto().getId(),
                            primeiro.getProduto().getNome(),
                            primeiro.getProduto().getCodigo(),
                            lista.size(),
                            fluxoDescricao
                    );
                })
                .toList();
    }

    @Transactional
    public void criar(RoteiroDTO roteiroDTO) {
        validarRoteiro(roteiroDTO);

        if (!roteiroProdutoRepository.buscarPorProdutoId(roteiroDTO.produtoId()).isEmpty()) {
            throw new BusinessException("Já existe um roteiro cadastrado para este produto. Utilize a opção de atualizar.");
        }

        salvarPersistenciaRoteiro(roteiroDTO.produtoId(), roteiroDTO);
    }

    @Transactional
    public void atualizar(Long produtoIdOriginal, RoteiroDTO roteiroDTO) {
        validarRoteiro(roteiroDTO);

        roteiroProdutoRepository.deletarPorProdutoId(produtoIdOriginal);

        salvarPersistenciaRoteiro(roteiroDTO.produtoId(), roteiroDTO);
    }

    @Transactional
    public void excluirPorProdutoId(Long produtoId) {
        roteiroProdutoRepository.deletarPorProdutoId(produtoId);
    }

    // =========================================================================
    // MÉTODOS PRIVADOS AUXILIARES (Isolando Responsabilidades)
    // =========================================================================

    private void validarRoteiro(RoteiroDTO roteiroDTO) {
        if (roteiroDTO.produtoId() == null) {
            throw new BusinessException("Selecione um produto");
        }

        if (roteiroDTO.passos() == null || roteiroDTO.passos().isEmpty()) {
            throw new BusinessException("Adicione ao menos uma etapa ao roteiro");
        }
    }


    private void salvarPersistenciaRoteiro(Long produtoId, RoteiroDTO roteiroDTO) {
        var produto = produtoService.buscarProdutoPorId(produtoId);

        List<RoteiroProduto> roteiro = new ArrayList<>();
        int sequencia = 10;

        for (var passoDTO : roteiroDTO.passos()) {
            var setor = setorService.buscarSetorPorId(passoDTO.setorId());

            RoteiroProduto item = new RoteiroProduto();
            item.setProduto((Produto) produto);
            item.setSetor(setor);
            item.setSequencia(sequencia);

            if (passoDTO.etapaSetorId() != null) {
                var etapa = etapaSetorService.buscarPorId(passoDTO.etapaSetorId());
                if (etapa == null) {
                    throw new BusinessException("Etapa de setor não encontrada com ID: " + passoDTO.etapaSetorId());
                }
                item.setEtapaSetor(etapa);
            }

            roteiro.add(item);
            sequencia += 10;
        }

        roteiroProdutoRepository.saveAll(roteiro);
    }
}