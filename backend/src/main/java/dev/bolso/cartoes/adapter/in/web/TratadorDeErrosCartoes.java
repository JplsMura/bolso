package dev.bolso.cartoes.adapter.in.web;

import dev.bolso.cartoes.domain.CartaoNaoEncontrado;
import dev.bolso.cartoes.domain.FaturaNaoEncontrada;
import dev.bolso.cartoes.domain.RegraDoCartao;
import dev.bolso.cartoes.domain.VersaoDesatualizada;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Erros de cartões e faturas em problem+json, só para os controllers deste módulo (os erros comuns do MVC e do
 * espaço ficam no tratador da identidade).
 */
@RestControllerAdvice(basePackageClasses = TratadorDeErrosCartoes.class)
class TratadorDeErrosCartoes {

    @ExceptionHandler(CartaoNaoEncontrado.class)
    ProblemDetail cartaoNaoEncontrado(CartaoNaoEncontrado e) {
        var problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Cartão não encontrado");
        return problema;
    }

    @ExceptionHandler(FaturaNaoEncontrada.class)
    ProblemDetail faturaNaoEncontrada(FaturaNaoEncontrada e) {
        var problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Fatura não encontrada");
        return problema;
    }

    @ExceptionHandler({VersaoDesatualizada.class, OptimisticLockingFailureException.class})
    ProblemDetail conflito(RuntimeException e) {
        var problema = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problema.setTitle("Alterado por outra operação");
        problema.setDetail("Recarregue e tente de novo.");
        return problema;
    }

    @ExceptionHandler(RegraDoCartao.class)
    ProblemDetail regra(RegraDoCartao e) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(422), e.getMessage());
        problema.setTitle("Regra de negócio violada");
        return problema;
    }
}
