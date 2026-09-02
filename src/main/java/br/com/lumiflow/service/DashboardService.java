package br.com.lumiflow.service;


import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class DashboardService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final SetorRepository setorRepository;


    @Transactional(readOnly = true)
    public DashboardResumoDTO obterResumoEstatisticas(){

        Long total= ordemProducaoRepository.count();

        Long andamento = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO);
        Long concluidas = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.CONCLUIDA);
        Long pendentes = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.DISPONIVEL);


        return new DashboardResumoDTO(total, andamento, concluidas, pendentes);
    }


    @Transactional(readOnly = true)
    public List<SetorResumoDTO> obterResumoSetores() {
        return setorRepository.obterResumoOrdensPorSetor();
    }
}