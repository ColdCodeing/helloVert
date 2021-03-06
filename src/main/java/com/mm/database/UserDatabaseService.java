package com.mm.database;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

import java.util.HashMap;
import java.util.List;

@ProxyGen
public interface UserDatabaseService {
    static UserDatabaseService create(JDBCClient jdbcClient, HashMap<SqlQuery, String> sqlQueries,
                                      Handler<AsyncResult<UserDatabaseService>> readyHandler) {
        return new UserDatabaseServiceImpl(jdbcClient, sqlQueries, readyHandler);
    }

    static UserDatabaseService createProxy(Vertx vertx, String address) {
        return new UserDatabaseServiceVertxEBProxy(vertx, address) ;
    }

    @Fluent
    UserDatabaseService getUserByNameAndPass(String username, String password, Handler<AsyncResult<JsonObject>> resultHandler);

    @Fluent
    UserDatabaseService getAllUser(Handler<AsyncResult<List<JsonObject>>> resultHandler);

    @Fluent
    UserDatabaseService saveUser(String username, String password, Handler<AsyncResult<Void>> resultHandler);

    @Fluent
    UserDatabaseService deleteUser(String username, String password, Handler<AsyncResult<Void>> resultHandler);
}
