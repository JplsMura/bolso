package dev.bolso.lancamentos.adapter.in.web;

import dev.bolso.lancamentos.domain.ConflitoDeVersao;
import dev.bolso.lancamentos.domain.LancamentoNaoEncontrado;
import dev.bolso.lancamentos.domain.RegraDeNegocio;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de lançamentos e tags em problem+json. Os erros comuns do MVC e do espaço ficam no tratador da identidade. */
@RestControllerAdvice
class TratadorDeErrosLancamentos {

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
        problema.setTitle("O lançamento foi alterado por outra operação");
        problema.setDetail("Recarregue o lançamento e tente de novo.");
        return problema;
    }

    @ExceptionHandler(RegraDeNegocio.class)
    ProblemDetail regraDeNegocio(RegraDeNegocio e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problema.setTitle("Regra de negócio violada");
        return problema;
    }
}
