package com.example.starter;

import io.vertx.core.*;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

import io.vertx.pgclient.*;
import io.vertx.sqlclient.*;
import io.vertx.sqlclient.impl.SqlClientInternal;

import java.util.List;
import java.util.ArrayList;

import com.example.starter.model.*;

public class MainVerticle extends VerticleBase implements Handler<HttpServerRequest> {
  private static final int NUM_CORES = 8;
  private HttpServer server;
  private SqlClientInternal client;
  private Throwable database_err;

  private PreparedQuery<RowSet<Row>> GET_USERS;

  @Override
  public Future<?> start() throws Exception {
    int port = 8080;
    server = vertx.createHttpServer(new HttpServerOptions()
      .setStrictThreadMode(true))
      .requestHandler(MainVerticle.this);

    PgConnectOptions options = new PgConnectOptions();
    options.setDatabase("tes");
    options.setHost("localhost");
    options.setPort(5432);
    options.setUser("postgres");
    options.setPassword("fuji123");
    options.setCachePreparedStatements(true);
    options.setPreparedStatementCacheMaxSize(1024);
    options.setPipeliningLimit(256);
    Future<?> client_init = initClients(options);

    return client_init.transform(ar -> {
      database_err = ar.cause();
      return server.listen(port);
    });
  }

  private Future<?> initClients(PgConnectOptions options){
    return PgConnection.connect(vertx, options)
      .flatMap(conn -> {
      client = (SqlClientInternal) conn;
      List<Future<?>> list = new ArrayList<>();

      Future<PreparedStatement> get_users = conn.prepare("select * from users;")
        .andThen(onSuccess(val -> GET_USERS = val.query()));
      list.add(get_users);

      return Future.join(list);
    });
  }

  public static <T> Handler<AsyncResult<T>> onSuccess(Handler<T> handler){
    return ar -> {
      if (ar.succeeded()){
        handler.handle(ar.result());
      }
    };
  }

  @Override
  public void handle(HttpServerRequest request){
    try {
      switch (request.path()) {
        case "/":

          request.response()
            .putHeader("content-type", "application/json")
            .end("hellooo");

          break;

        case "/db":
          HttpServerResponse resp = request.response();
          GET_USERS.execute().onComplete(res2 -> {
          if (res2.succeeded()){
            RowIterator<Row> result_set = res2.result().iterator();
            if (!result_set.hasNext()){
              resp.setStatusCode(404).end();
              return;
            }

            Row row = result_set.next();
            User data = new User(row.getString(0));
            resp.putHeader("content-type", "application/json")
              .end(data.toJson());
          }
        });

          break;

        default:
          System.out.println("default");
          break;
      }
    } catch (Exception e){
      System.out.println("error");
    }
  }

  @Override
  public Future<?> stop() throws Exception {
    return server != null ? server.close() : super.stop();
  }

  public static void main(String[] args) throws Exception {
    int event_loop_pool_size = NUM_CORES;
    String size_prop = System.getProperty("vertx.eventLoopPoolSize");
    if (size_prop != null){
      try {
        event_loop_pool_size = Integer.parseInt(size_prop);
      } catch (NumberFormatException e){
        e.printStackTrace();
      }
    }
    Vertx vertx = Vertx.vertx(new VertxOptions()
      .setEventLoopPoolSize(event_loop_pool_size)
      .setPreferNativeTransport(true)
      .setDisableTCCL(true));

    vertx.exceptionHandler(err -> {
      err.printStackTrace();
    });

    vertx.deployVerticle(
      MainVerticle.class.getName(),
      new DeploymentOptions().setInstances(event_loop_pool_size))
    .onComplete(event -> {
      if (event.succeeded()){
          System.out.println("server running on port 8080");
        }  else {
          System.out.println("failed to run server");
        }
    });

  }

}
