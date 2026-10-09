package dev.bolso.identidade.adapter.in.web;

import dev.bolso.identidade.EspacoNaoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros em application/problem+json (RFC 9457). Vale para a API toda: as exceções padrão do MVC
 * (id que não é UUID vira 400) e o {@link EspacoNaoEncontrado}, que qualquer feature pode lançar
 * ao chamar {@code IdentidadeApi.papelNoEspaco}.
 */
@RestControllerAdvice
class TratadorDeErros extends ResponseEntityExceptionHandler {

    @ExceptionHandler(EspacoNaoEncontrado.class)
    ProblemDetail espacoNaoEncontrado(EspacoNaoEncontrado e) {
        var problema = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problema.setTitle("Espaço não encontrado");
        return problema;
    }
}
