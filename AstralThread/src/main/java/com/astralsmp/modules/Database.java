package com.astralsmp.modules;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Стандартный класс для инициализации соединения с базой данных
 * Использую HikariCP, пока очень радует ♥
 */
public class Database {

    public static final String dbName = "astraldatabase.db";
    private static final String URL = String.format("jdbc:sqlite:C:/SQLite3/%s", dbName);
    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource dataSource;

    /*
     * Статический блок инициализации. Добавляем параметры в конфиг Hikari, создаёт dataSource через параметры конфига
     */
    static {
        config.setJdbcUrl(URL);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        config.addDataSourceProperty("connectionTimeout", "15000");
        dataSource = new HikariDataSource(config);
    }

    /**
     * Метод для получения статического объекта соединения с бд
     *
     * @return возвращает соединения с базой данных
     * @throws SQLException выкидывается если при подключении к бд возникла непредвиденная ошибка
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
