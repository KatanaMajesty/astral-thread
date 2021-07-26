package com.astralsmp.exceptions;

public class AstralException extends Exception {

    public AstralException(String string) {
        super("ASTRAL SMP EXCEPTION: " + string);
    }

    public AstralException() {
        super("ASTRAL SMP EXCEPTION");
    }
}
