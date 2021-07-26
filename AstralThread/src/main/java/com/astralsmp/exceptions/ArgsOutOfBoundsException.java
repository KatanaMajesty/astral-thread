package com.astralsmp.exceptions;

/**
 * Метод использовался для проверки на правильность работы счётчика аргументов и кастомных исключений
 */
@Deprecated
public class ArgsOutOfBoundsException extends AstralException {
    public ArgsOutOfBoundsException(int argCount) {
        super("Неверное количество аргументов: " + argCount);
    }
}
