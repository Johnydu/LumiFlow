package br.com.lumiflow.service;

import br.com.lumiflow.dto.etapaSetor.EtapaSetorDTO;
import br.com.lumiflow.dto.setor.SetorDTO;
import br.com.lumiflow.dto.setor.SetorListagemDTO;
import br.com.lumiflow.model.Setor;
import br.com.lumiflow.model.EtapaSetor; // Certifique-se de importar o seu model de etapa
import br.com.lumiflow.mapper.SetorMapper;
import br.com.lumiflow.exception.BusinessException;
import br.com.lumiflow.repository.EtapaSetorRepository; // <--- Importante
import br.com.lumiflow.repository.MaquinaRepository;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class SetorService {

    private final SetorRepository setorRepository;
    private final EtapaSetorRepository etapaSetorRepository; // <--- Injetado aqui
    private final SetorMapper setorMapper;
    private final MaquinaRepository maquinaRepository;

    public Setor buscarSetorPorId(Long idSetor) {
        return setorRepository.findById(idSetor)
                .orElseThrow(() -> new BusinessException("Setor não encontrado"));
    }

    public List<SetorListagemDTO> listarSetores() {
        return setorRepository.findAllByOrderByNomeAsc().stream()
                .map(setor -> new SetorListagemDTO(
                        setor.getId(),
                        setor.getNome(),
                        setor.getPossuiEtapas(),
                        maquinaRepository.countBySetorId(setor.getId())
                )).toList();
    }

    @Transactional
    public void novoSetor(SetorDTO setorDTO) {
        if (setorRepository.findByNome(setorDTO.nome().toUpperCase()).isPresent()) {
            throw new BusinessException("Já existe setor com esse nome");
        }

        Setor setor = setorMapper.toEntity(setorDTO);
        setor.setNome(setor.getNome().toUpperCase());

        Setor setorSalvo = setorRepository.save(setor);

        // Se o DTO envia etapas e o setor possui etapas, salva elas
        salvarEtapasDoSetor(setorSalvo, setorDTO);
    }

    @Transactional
    public void excluirSetor(Long id) { // Nome corrigido de exluirSetor para excluirSetor
        Setor setor = buscarSetorPorId(id);

        // Opcional: Validação prévia se houver máquinas vinculadas
        long maquinasCount = maquinaRepository.countBySetorId(id);
        if (maquinasCount > 0) {
            throw new BusinessException("Não é possível excluir este setor pois existem máquinas vinculadas a ele.");
        }

        try {
            // Remove as etapas vinculadas primeiro para evitar violação de chave estrangeira
            etapaSetorRepository.deleteAllBySetorId(id);
            setorRepository.delete(setor);
        } catch (Exception e) {
            throw new BusinessException("Não é possível excluir este setor pois ele está em uso em roteiros ou processos.");
        }
    }

    @Transactional
    public void editarSetor(long id, SetorDTO setorDTO) {
        Setor setor = buscarSetorPorId(id);

        // Validação de nome duplicado (se mudou o nome e já existe outro com esse nome)
        setorRepository.findByNome(setorDTO.nome().toUpperCase()).ifPresent(s -> {
            if (!s.getId().equals(id)) {
                throw new BusinessException("Já existe outro setor com esse nome");
            }
        });

        setor.setNome(setorDTO.nome().toUpperCase());
        setor.setPossuiEtapas(setorDTO.possuiEtapas());

        setorRepository.save(setor);

        // Gerencia as etapas: se desmarcou que possui etapas, limpa do banco. Se possui, atualiza/insere as novas.
        if (!Boolean.TRUE.equals(setorDTO.possuiEtapas())) {
            etapaSetorRepository.deleteAllBySetorId(id);
        } else {
            // Remove as antigas e insere as novas enviadas pelo formulário
            etapaSetorRepository.deleteAllBySetorId(id);
            salvarEtapasDoSetor(setor, setorDTO);
        }
    }

    // Método auxiliar para processar as etapas vindas do DTO
    private void salvarEtapasDoSetor(Setor setor, SetorDTO setorDTO) {
        if (setorDTO.etapas() != null && !setorDTO.etapas().isEmpty()) {
            for (int i = 0; i < setorDTO.etapas().size(); i++) {
                EtapaSetorDTO etapaDto = setorDTO.etapas().get(i);

                EtapaSetor etapa = new EtapaSetor();
                etapa.setNome(etapaDto.nome());
                etapa.setSetor(setor);

                // BLINDAGEM: Se a ordem veio do DTO, usa ela. Se veio nula, usa a posição sequencial (i + 1)
                int ordemFinal = (etapaDto.ordem() != null) ? etapaDto.ordem() : (i + 1);
                etapa.setOrdem(ordemFinal);

                etapaSetorRepository.save(etapa);
            }
        }
    }
}