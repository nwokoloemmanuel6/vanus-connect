package com.linkall.sink.mysql;

import com.linkall.vance.common.config.ConfigUtil;
import com.linkall.vance.common.config.SecretUtil;
import com.linkall.vance.core.Sink;
import com.linkall.vance.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MySqlSink implements Sink {
  private static final Logger LOGGER = LoggerFactory.getLogger(MySqlSink.class);
  private SqlWriter sqlWriter;
  private final MySqlConfig config;

  public MySqlSink() {
    config =
        new MySqlConfig(
            SecretUtil.getString("host"),
            SecretUtil.getString("port"),
            SecretUtil.getString("username"),
            SecretUtil.getString("password"),
            SecretUtil.getString("dbName"),
            ConfigUtil.getString("table_name"),
            ConfigUtil.getString("insert_mode"),
            ConfigUtil.getString("commit_interval"));
  }

  @Override
  public void start() throws Exception {
    HttpServer server = HttpServer.createHttpServer();
    server.ceHandler(
        event -> {
          LOGGER.info("receive a new event: {}", event);
          if (!"application/json".equals(event.getDataContentType())) {
            LOGGER.info(
                "only process contentType application/json, now contentType: {}",
                event.getDataContentType());
            return;
          }
          JsonObject data = new JsonObject(new String(event.getData().toBytes()));
          if (sqlWriter == null) {
            try {
              this.sqlWriter = getSqlWriter(data);
            } catch (SQLException e) {
              LOGGER.error("get sql writer fail", e);
            }
          }
          try {
            sqlWriter.add(data);
          } catch (SQLException e) {
            LOGGER.error("write data has error", e);
          }
        });
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  if (sqlWriter == null) {
                    return;
                  }
                  try {
                    sqlWriter.close();
                  } catch (Exception e) {
                    LOGGER.error("sql writer close error", e);
                  }
                }));
    server.listen();
  }

  public SqlWriter getSqlWriter(JsonObject data) throws SQLException {
    List<String> columnNames = new ArrayList<>(data.fieldNames());
    TableMetadata meta = new TableMetadata(config.getTableName(), columnNames);
    SqlWriter sqlWriter = new SqlWriter(this.config, meta);
    sqlWriter.init();
    return sqlWriter;
  }
}
