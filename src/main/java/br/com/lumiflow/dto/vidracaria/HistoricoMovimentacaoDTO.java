package br.com.lumiflow.dto.vidracaria;

import br.com.lumiflow.entity.enums.TipoMovimentacao;
import br.com.lumiflow.entity.enums.TipoVidro;
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