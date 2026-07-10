package br.com.lumiflow.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(
            ResourceNotFoundException ex,
            Model model) {

        model.addAttribute("erro", ex.getMessage());

        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(
            BusinessException ex,
            Model model) {

        model.addAttribute("erro", ex.getMessage());

        return "error/business";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneric(
            Exception ex,
            Model model) {

        model.addAttribute(
                "erro",
                "Ocorreu um erro inesperado."
        );

        return "error/500";
    }
}