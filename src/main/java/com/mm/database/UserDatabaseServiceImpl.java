package com.mm.database;

import com.sun.xml.internal.bind.v2.runtime.unmarshaller.XsiNilLoader;
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

public class UserDatabaseServiceImpl implements UserDatabaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserDatabaseServiceImpl.class);

    private final HashMap<SqlQuery, String> sqlQueryStringHashMap;
    private final JDBCClient jdbcClient;

    UserDatabaseServiceImpl(io.vertx.ext.jdbc.JDBCClient jdbcClient, HashMap<SqlQuery, String> sqlQueryStringHashMap,
                            Handler<AsyncResult<UserDatabaseService>> readyHandler){
        this.jdbcClient = new JDBCClient(jdbcClient);
        this.sqlQueryStringHashMap = sqlQueryStringHashMap;

        //数据库连接
        getConnection().flatMap(conn -> conn.rxExecute(sqlQueryStringHashMap.get(SqlQuery.CREATE_TABLE)))
                .map(v -> this)
                .subscribe(RxHelper.toSubscriber(readyHandler));
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
    public UserDatabaseService getAllUser(Handler<AsyncResult<JsonArray>> resultHandler) {
        return this;
    }

    @Override
    public UserDatabaseService saveUser(String username, String password, Handler<AsyncResult<Void>> resultHandler) {
        return null;
    }

    @Override
    public UserDatabaseService deleteUser(String username, Handler<AsyncResult<Void>> resultHandler) {
        return null;
    }
}