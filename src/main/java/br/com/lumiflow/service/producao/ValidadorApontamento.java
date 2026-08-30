package br.com.lumiflow.service.producao;

import br.com.lumiflow.config.AppMessages;
import br.com.lumiflow.dto.apontamento.ApontamentoRequestDTO;
import br.com.lumiflow.entity.OrdemSetor;
import br.com.lumiflow.exception.BusinessException;
import org.springframework.stereotype.Component;

/**
 * @Component: peça auxiliar de validação, usada DENTRO de um caso de uso.
 * Não injeta nada aqui ainda, mas se precisasse, @RequiredArgsConstructor
 * geraria o construtor automaticamente a partir dos campos `final`.
 */
@Component
public class ValidadorApontamento {

    public void validar(OrdemSetor ordemSetor, ApontamentoRequestDTO dto) {
        int qtdRefugo = dto.qtdRefugo() != null ? dto.qtdRefugo() : 0;
        int totalLancado = dto.qtdProduzida() + qtdRefugo;

        if (totalLancado == 0) {
            throw new BusinessException(AppMessages.ERROR_APONTAMENTO_QUANTIDADE_ZERO);
        }

        if (totalLancado > ordemSetor.getQtdPendente()) {
            throw new BusinessException(
                    AppMessages.ERROR_APONTAMENTO_QUANTIDADE_EXCEDE,
                    totalLancado,
                    ordemSetor.getQtdPendente());
        }

        if (qtdRefugo > 0 && (dto.motivoRefugo() == null || dto.motivoRefugo().isBlank())) {
            throw new BusinessException(AppMessages.ERROR_APONTAMENTO_REFUGO_MOTIVO_OBRIGATORIO);
        }
    }
}