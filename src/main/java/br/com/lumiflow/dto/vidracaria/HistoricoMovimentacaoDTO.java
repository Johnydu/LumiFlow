package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.model.enums.TipoMovimentacao;
import br.com.lumiflow.model.enums.TipoVidro;
import java.time.LocalDateTime;

public record HistoricoMovimentacaoDTO(

        Long id,

        TipoMovimentacao tipoMovimentacao,

        TipoVidro tipoVidro,

        String descricaoVidro,

        Integer quantidade,

        LocalDateTime dataHora,

        String nomeUsuario,

        String observacao
) {}