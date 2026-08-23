package br.com.lumiflow.service;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.maquina.MaquinaDTO;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.mapper.MaquinaMapper;
import br.com.lumiflow.entity.Maquina;
import br.com.lumiflow.repository.MaquinaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service @AllArgsConstructor
public class MaquinaService {
    private final MaquinaRepository maquinaRepository; private final MaquinaMapper maquinaMapper; private final SetorService setorService;
    @Transactional(readOnly = true) public List<MaquinaDTO> listarMaquinas() { return maquinaMapper.toDtoList(maquinaRepository.findAllByOrderByNomeAsc()); }
    @Transactional public void novaMaquina(MaquinaDTO dto) { if (maquinaRepository.findByNome(dto.nome()).isPresent()) throw new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE); Maquina maquina = maquinaMapper.toEntity(dto); maquina.setSetor(setorService.buscarSetorPorId(dto.setorId())); maquinaRepository.save(maquina); }
    @Transactional public void deletarMaquina(long id) { maquinaRepository.delete(buscarPorId(id)); }
    @Transactional public void editarMaquina(Long id, MaquinaDTO dto) { Maquina maquina = buscarPorId(id); maquinaRepository.findByNome(dto.nome()).filter(outra -> !outra.getId().equals(id)).ifPresent(outra -> { throw new BusinessException(AppMessages.ERROR_MACHINE_NAME_DUPLICATE); }); maquina.setNome(dto.nome().trim().toUpperCase(Locale.ROOT)); maquina.setSetor(setorService.buscarSetorPorId(dto.setorId())); maquinaRepository.save(maquina); }
    private Maquina buscarPorId(long id) { return maquinaRepository.findById(id).orElseThrow(() -> new BusinessException(AppMessages.ERROR_MACHINE_NOTFOUND)); }
}
