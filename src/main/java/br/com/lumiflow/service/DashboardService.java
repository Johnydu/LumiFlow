package br.com.lumiflow.service;


import br.com.lumiflow.dto.dashboard.DashboardResumoDTO;
import br.com.lumiflow.dto.dashboard.SetorResumoDTO;
import br.com.lumiflow.entity.enums.EstatusOrdemProducao;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import br.com.lumiflow.repository.SetorRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class DashboardService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final SetorRepository setorRepository;


    public DashboardResumoDTO obterResumoEstatisticas(){

        Long total= ordemProducaoRepository.count();

        Long andamento = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.EM_ANDAMENTO);
        Long concluidas = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.CONCLUIDA);
        Long pendentes = ordemProducaoRepository.countByStatus(EstatusOrdemProducao.DISPONIVEL);


        return new DashboardResumoDTO(total, andamento, concluidas, pendentes);
    }


    public List<SetorResumoDTO> obterResumoSetores() {
        return setorRepository.obterResumoOrdensPorSetor();
    }
}