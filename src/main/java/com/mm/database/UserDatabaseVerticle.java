package com.mm.database;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.serviceproxy.ProxyHelper;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

//挂载Service
public class UserDatabaseVerticle extends AbstractVerticle {


    public static final String CONFIG_USER_SQL_QUERIES_RESOURCE_FILE = "userdb.sqlqueries.resource.file";
    public static final String CONFIG_JDBC_URL = "jdbc:mysql://localhost:3306/hellovert?" +
            "useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
    public static final String CONFIG_JDBC_DRIVER_CLASS = "com.mysql.jdbc.Driver";
    public static final String CONFIG_DATABASE_USER_NAME = "root";
    public static final String CONFIG_DATABASE_PASSWORD = "111111";
    public static final String CONFIG_SERVICE_ADDRESS = "user.query";

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        HashMap<SqlQuery, String> sqlQueries = loadSqlQueries();

        JDBCClient jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", CONFIG_JDBC_URL)
                .put("driver_class", CONFIG_JDBC_DRIVER_CLASS)
                .put("user", CONFIG_DATABASE_USER_NAME)
                .put("password", CONFIG_DATABASE_PASSWORD)
                .put("max_pool_size", 30));

        UserDatabaseService.create(jdbcClient, sqlQueries, ready -> {
           if (ready.succeeded()) {
               ProxyHelper.registerService(UserDatabaseService.class, vertx, ready.result(), CONFIG_SERVICE_ADDRESS);
               startFuture.complete();
           } else {
               startFuture.fail(ready.cause());
           }
        });
    }

    private HashMap<SqlQuery, String> loadSqlQueries() throws IOException {

        String queriesFile = config().getString(CONFIG_USER_SQL_QUERIES_RESOURCE_FILE);
        InputStream queriesInputStream;
        if (queriesFile != null) {
            queriesInputStream = new FileInputStream(queriesFile);
        } else {
            queriesInputStream = getClass().getResourceAsStream("/user-db-queries.properties");
        }

        Properties queriesProps = new Properties();
        queriesProps.load(queriesInputStream);
        queriesInputStream.close();

        HashMap<SqlQuery, String> sqlQueries = new HashMap<>();
        sqlQueries.put(SqlQuery.GET_USER, queriesProps.getProperty("get-user"));
        sqlQueries.put(SqlQuery.GET_ALL_USER, queriesProps.getProperty("get-all-user"));
        sqlQueries.put(SqlQuery.SAVE_USER, queriesProps.getProperty("save-user"));
        sqlQueries.put(SqlQuery.DELETE_USER, queriesProps.getProperty("delete-user"));
        return sqlQueries;
    }
}

