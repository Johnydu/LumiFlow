package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.operador.OperadorDTO;
import br.com.lumiflow.dto.operador.OperadorListagemDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.OperadorMapper;
import br.com.lumiflow.entity.Operador;
import br.com.lumiflow.repository.OperadorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service @AllArgsConstructor
public class OperadorService {
    private final OperadorRepository operadorRepository; private final OperadorMapper operadorMapper; private final SetorService setorService;
    public void validarNomeOperador(String nome) { if (operadorRepository.existsByNome(nome)) throw new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE); }
    public void validarNomeOperadorParaEdicao(String nome, Long id) { if (nome != null && operadorRepository.existsByNomeAndIdNot(nome.trim().toUpperCase(Locale.ROOT), id)) throw new BusinessException(AppMessages.ERROR_OPERATOR_NAME_DUPLICATE); }
    public Operador buscarOperadorPorId(Long id) { return operadorRepository.findById(id).orElseThrow(() -> new BusinessException(AppMessages.ERROR_OPERATOR_NOTFOUND)); }
    @Transactional(readOnly = true) public List<OperadorListagemDTO> listarOperadores() { return operadorMapper.toListDto(operadorRepository.findAllByOrderByNomeAsc()); }
    @Transactional public void novoOperador(OperadorDTO dto) { validarNomeOperador(dto.nome()); Operador operador = operadorMapper.toEntity(dto); operador.setSetorPadrao(setorService.buscarSetorPorId(dto.setorPadraoId())); operadorRepository.save(operador); }
    @Transactional public void excluirOperador(Long id) { operadorRepository.delete(buscarOperadorPorId(id)); }
    @Transactional public void editarOperador(Long id, OperadorDTO dto) { Operador operador = buscarOperadorPorId(id); validarNomeOperadorParaEdicao(dto.nome(), id); operador.setNome(dto.nome().toUpperCase(Locale.ROOT)); operador.setFuncao(dto.funcao() == null || dto.funcao().isBlank() ? null : dto.funcao().trim().toUpperCase(Locale.ROOT)); operador.setSetorPadrao(dto.setorPadraoId() == null ? null : setorService.buscarSetorPorId(dto.setorPadraoId())); operadorRepository.save(operador); }
}
