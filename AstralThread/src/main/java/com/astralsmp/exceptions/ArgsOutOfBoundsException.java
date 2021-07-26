package com.astralsmp.exceptions;

public class ArgsOutOfBoundsException extends AstralException {
    public ArgsOutOfBoundsException(int argCount) {
        super("Неверное количество аргументов: " + argCount);
    }
}
