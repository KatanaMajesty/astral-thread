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

    private static final String DB_TYPE = Config.getConfig().getString("Database.database_type");
    private static final String DB_HOST = Config.getConfig().getString("Database.database_host");
    private static final String DB_PORT = Config.getConfig().getString("Database.database_port");
    private static final String DB_NAME = Config.getConfig().getString("Database.database_name");
    private static final String DB_USER = Config.getConfig().getString("Database.database_user");
    private static final String DB_PASS = Config.getConfig().getString("Database.database_pass");
    private static final String URL = String.format("jdbc:%s://%s:%s/%s", DB_TYPE, DB_HOST, DB_PORT, DB_NAME);
    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource dataSource;

    /*
     * Статический блок инициализации. Добавляем параметры в конфиг Hikari, создаёт dataSource через параметры конфига
     */
    static {
        config.setJdbcUrl(URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASS);
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
