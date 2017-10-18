package com.mm;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
//import org.apache.commons.lang3.StringUtils;


public class RestVerticle extends AbstractVerticle {

    private static final String QUERY = "SELECT * FROM t_user WHERE username=? AND password=?";
    private JDBCClient jdbc;

//    public static void main(String[] args) {
//        //Vertx 基类
//        Vertx vertx = Vertx.vertx();
//        //发布服务
//        vertx.deployVerticle(new RestVerticle());
//    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        //数据库
        jdbc = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:mysql://localhost:3306/hellovert?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC")
                .put("driver_class", "com.mysql.jdbc.Driver")
                .put("user", "root")
                .put("password", "111111"), "My-App-Collection");

        //路由
        //主页
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Hello from my first Vert application</h1>");
        });

        router.route().handler(BodyHandler.create());
        router.post("/account/:param1/:param2").handler(this::handleLoginIn);
        router.delete("/account/:param1").handler(this::handleLoginOut);
        //http服务,请求由Router分发
        vertx.createHttpServer().requestHandler(router::accept).listen(8080);
    }

    private void handleLoginIn(RoutingContext context) {
        String username = context.request().getParam("param1");
        String password = context.request().getParam("param2");

//        if (StringUtils.isBlank(password) || StringUtils.isBlank(username)){
//            context.response().setStatusCode(400).end();
//        }

        jdbc.getConnection(conn -> {
            if (conn.failed()) {
                System.out.println("数据库连接异常");
                context.response().setStatusCode(500).end();
                return;
            }
            final SQLConnection connection = conn.result();
            connection.queryWithParams(QUERY, new JsonArray().add(username).add(password), resultSetAsyncResult -> {
               if (resultSetAsyncResult.failed()) {
                   System.err.println("Cannot retrieve the data from the database");
                   context.response().setStatusCode(500).end();
               } else if (resultSetAsyncResult.result().getNumRows() < 1) {
                   System.out.println("用户不存在");
                   context.response().setStatusCode(403);
                   context.response().putHeader("content-type", "application/json; charset=utf-8")
                           .end(Json.encodePrettily(new JsonArray().add("ERROR:403").add("check")));
               } else {
                   System.out.println("用户存在");
                   context.response().setStatusCode(200);
                   context.response().putHeader("content-type", "application/json; charset=utf-8")
                           .end(Json.encodePrettily(resultSetAsyncResult.result().getResults()));

               }

               connection.close(done -> {
                   if (done.failed()) {
                       System.out.printf("can not close database connect");
                   }
               });
            });
        });
        //check in
        //if(isCheckIn())
        //return 403
    }

    private void handleLoginOut(RoutingContext context) {
        String username = context.request().getParam("param1");

//        if (StringUtils.isBlank(username)) {
//            context.response().setStatusCode(403).end();
//        }

        //check out
        context.response().setStatusCode(200).end();
    }


}
