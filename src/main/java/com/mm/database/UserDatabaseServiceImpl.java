package com.mm.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import io.vertx.ext.sql.ResultSet;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.ext.jdbc.JDBCClient;
import io.vertx.rxjava.ext.sql.SQLConnection;
import rx.Observable;
import rx.Single;

import java.util.HashMap;
import java.util.List;

public class UserDatabaseServiceImpl implements UserDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDatabaseServiceImpl.class);

    private final HashMap<SqlQuery, String> sqlQueryStringHashMap;
    private final JDBCClient jdbcClient;

    UserDatabaseServiceImpl(io.vertx.ext.jdbc.JDBCClient jdbcClient, HashMap<SqlQuery, String> sqlQueryStringHashMap,
                            Handler<AsyncResult<UserDatabaseService>> readyHandler){
        this.jdbcClient = new JDBCClient(jdbcClient);
        this.sqlQueryStringHashMap = sqlQueryStringHashMap;

        //数据库连接
    }

    // tag::rx-get-connection[]
    private Single<SQLConnection> getConnection() {
        return jdbcClient.rxGetConnection().flatMap(conn -> {
            Single<SQLConnection> connectionSingle = Single.just(conn); // <1>
            return connectionSingle.doOnUnsubscribe(conn::close); // <2>
        });
    }
    // end::rx-get-connection[]

    @Override
    public UserDatabaseService getUserByNameAndPass(String username, String password, Handler<AsyncResult<JsonObject>> resultHandler) {
        getConnection()
                .flatMap(conn -> conn.rxQueryWithParams(sqlQueryStringHashMap.get(SqlQuery.GET_USER),
                        new JsonArray().add(username).add(password)))
                .map(result -> {
                    if (result.getNumRows() > 0) {
                        JsonArray row = result.getResults().get(0);
                        return new JsonObject()
                                .put("found", true)
                                .put("username", row.getString(0))
                                .put("password", row.getString(1));
                    } else {
                        return new JsonObject().put("found", false);
                    }
                }).subscribe(RxHelper.toSubscriber(resultHandler));
        return this;
    }

    @Override
    public UserDatabaseService getAllUser(Handler<AsyncResult<List<JsonObject>>> resultHandler) {
        getConnection()
                .flatMap(conn -> conn.rxQuery(sqlQueryStringHashMap.get(SqlQuery.GET_ALL_USER)))
                .map(ResultSet::getRows)
                .subscribe(RxHelper.toSubscriber(resultHandler));
        return this;
    }

    @Override
    public UserDatabaseService saveUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        getConnection().flatMap(conn -> conn.rxUpdateWithParams(sqlQueryStringHashMap.get(SqlQuery.SAVE_USER),
                new JsonArray().add(username).add(password)))
                .map(res -> (Void) null)
                .subscribe(RxHelper.toSubscriber(resultHandler));
        return this;
    }

    @Override
    public UserDatabaseService deleteUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        getConnection().flatMap(conn -> {
            JsonArray data = new JsonArray().add(username).add(password);
            return conn.rxUpdateWithParams(sqlQueryStringHashMap.get(SqlQuery.DELETE_USER), data);
        })
        .map(res -> (Void) null)
        .subscribe(RxHelper.toSubscriber(resultHandler));
        return this;
    }
}