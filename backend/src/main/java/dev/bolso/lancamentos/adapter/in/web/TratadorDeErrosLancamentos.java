package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.FaturaFechada;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.ParcelamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import java.net.URI;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de lançamentos, parcelamentos e tags em problem+json. Os erros comuns do MVC e do espaço ficam no tratador da identidade. */
@RestControllerAdvice
class TratadorDeErrosLancamentos {

    static final URI FATURA_FECHADA = URI.create("urn:bolso:problema:fatura-fechada");

    @ExceptionHandler(LancamentoNaoEncontrado.class)
    ProblemDetail naoEncontrado(LancamentoNaoEncontrado e) {
        var problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Lançamento não encontrado");
        return problema;
    }

    /** Duas gravações no mesmo instante: o banco acusa na hora do commit, e o efeito para o usuário é o mesmo. */
    @ExceptionHandler({ConflitoDeVersao.class, OptimisticLockingFailureException.class})
    ProblemDetail conflito(RuntimeException e) {
        var problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Alterado por outra operação");
        problema.setDetail("Recarregue e tente de novo.");
        return problema;
    }

    @ExceptionHandler(ParcelamentoNaoEncontrado.class)
    ProblemDetail parcelamentoNaoEncontrado(ParcelamentoNaoEncontrado e) {
        var problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Parcelamento não encontrado");
        return problema;
    }

    /** 422 com tipo próprio: a tela oferece o atalho para Cartões, onde se reabre a fatura. */
    @ExceptionHandler(FaturaFechada.class)
    ProblemDetail faturaFechada(FaturaFechada e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problema.setType(FATURA_FECHADA);
        problema.setTitle("Fatura fechada");
        return problema;
    }

    @ExceptionHandler(RegraDeNegocio.class)
    ProblemDetail regraDeNegocio(RegraDeNegocio e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problema.setTitle("Regra de negócio violada");
        return problema;
    }
}
