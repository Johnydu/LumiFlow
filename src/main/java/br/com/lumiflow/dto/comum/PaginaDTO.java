package br.com.lumiflow.dto.comum;

import java.util.List;

public record PaginaDTO<T>(

        List<T> itens,
        int pagina,
        boolean temMais,
        long total
)
{
}