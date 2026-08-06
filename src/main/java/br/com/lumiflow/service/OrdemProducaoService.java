package br.com.lumiflow.service;

import br.com.lumiflow.dto.ordens.OrdenListaDTO;
import br.com.lumiflow.mapper.OrdemProducaoMapper;
import br.com.lumiflow.repository.OrdemProducaoRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@AllArgsConstructor
public class OrdemProducaoService {

    private final OrdemProducaoRepository ordemProducaoRepository;
    private final OrdemProducaoMapper ordemProducaoMapper;

    public List<OrdenListaDTO> listarOrdens() {

         return ordemProducaoMapper.toListDTO(ordemProducaoRepository.findAll());

    }
}
