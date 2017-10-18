package com.mm.http;

import com.mm.database.UserDatabaseService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.mm.database.UserDatabaseVerticle.*;

public class HttpServerVerticle extends AbstractVerticle {

    public static final int CONFIG_HTTP_PORT = 8080;
    public static final String CONFIG_SERVICE_ADDRESS = "user.query";

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

    private UserDatabaseService userDatabaseService;

    @Override
    public void start(Future<Void> startFuture) {
        String userDbQuery = CONFIG_SERVICE_ADDRESS;
        userDatabaseService = UserDatabaseService.createProxy(vertx, userDbQuery);

        //http server
        HttpServer server = vertx.createHttpServer(new HttpServerOptions());

        //路由
        Router router = Router.router(vertx);

        //RESTful API
        router.get("/accounts");
        router.post("/accounts");
        router.get("/accounts/:username");
        router.put("/accounts/:username");
        router.delete("/accounts/:username");

        //开启端口监听
        server
                .requestHandler(router::accept)
                .listen(8080, ar -> {
                    if (ar.succeeded()) {
                        LOGGER.info("HTTP server running on port " + 8080);
                        startFuture.complete();
                    } else {
                        LOGGER.error("Could not start a HTTP server", ar.cause());
                        startFuture.fail(ar.cause());
                    }
                });
    }

    private void getAllAccounts(RoutingContext context) {
        userDatabaseService.getAllUser(reply -> {
           JsonObject response = new JsonObject();
           if (reply.succeeded()) {
               List<JsonObject> users = reply.result()
                       .stream()
                       .map(obj -> new JsonObject()
                               .put("id", obj.getString("username"))
                               .put("name", obj.getString("password")))
                       .collect(Collectors.toList());
               response.put("success", true).put("users", users);
               context.response().setStatusCode(200);
               context.response().putHeader("Content-Type", "application/json");
               context.response().end(response.encode());
           } else {
               response.put("success", false).put("error", reply.cause().getMessage());
               context.response().setStatusCode(500);
               context.response().putHeader("Content-Type", "application/json");
               context.response().end(response.encode());
           }
        });
    }
}






