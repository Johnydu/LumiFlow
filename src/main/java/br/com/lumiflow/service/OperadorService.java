package br.com.lumiflow.service;

import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.OperadorMapper;
import br.com.lumiflow.model.Operador;
import br.com.lumiflow.repository.OperadorRepository;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@AllArgsConstructor
public class OperadorService {

    private final OperadorRepository operadorRepository;
    private final OperadorMapper operadorMapper;
    private final SetorService setorService;

    public void validarNomeOperador(String nome){

        if (operadorRepository.existsByNome(nome)){
            throw new BusinessException("Operador com o nome: já cadastrado "+nome);
        }
    }

    // Validação para Edição (Ignora o operador com o ID informado)

    public void validarNomeOperadorParaEdicao(String nome, Long id) {
        if (nome != null && operadorRepository.existsByNomeAndIdNot(nome.trim().toUpperCase(Locale.ROOT), id)) {
            throw new BusinessException("Já existe outro operador cadastrado com o nome: " + nome);
        }
    }

    public Operador buscarOperadorPorId(Long id){
        return operadorRepository.findById(id).orElseThrow(()->new BusinessException("Operador nao encontrado"));
    }

    public List<OperadorListagemDTO> listarOperadores() {

       return operadorMapper.toListDto(operadorRepository.findAllByOrderByNomeAsc());

    }

    @Transactional
    public void novoOperador(@Valid OperadorDTO operadorDTO) {

        validarNomeOperador(operadorDTO.nome());

        operadorRepository.save(operadorMapper.toEntity(operadorDTO));
    }

    public void excluirOperador(@Valid Long id) {
        buscarOperadorPorId(id);

        operadorRepository.deleteById(id);


    }

    @Transactional
    public void editarOperador(Long id, @Valid OperadorDTO operadorDTO) {
        Operador operador = buscarOperadorPorId(id);

        validarNomeOperadorParaEdicao(operadorDTO.nome(), id);

        if (operadorDTO.funcao() != null && !operadorDTO.funcao().isBlank()) {
            operador.setFuncao(operadorDTO.funcao().trim().toUpperCase(Locale.ROOT));
        } else {
            operador.setFuncao(null);
        }

        if (operadorDTO.setorPadraoId() != null) {
            operador.setSetorPadrao(setorService.buscarSetorPorId(operadorDTO.setorPadraoId()));
        } else {
            operador.setSetorPadrao(null); // Permite deixar sem setor (Volante)
        }

        operadorRepository.save(operador);

    }
}
