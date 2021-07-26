package com.astralsmp.exceptions;

/**
 * Исключение, которое создано для проверки правильности инициализации таблицы в базе данных
 * Думаю, это поможет легко понимать в чём проблема при инициализации определённых систем
 */
public class InitTableException extends AstralException {
    public InitTableException(String systemName) {
        super(systemName);
    }
}
