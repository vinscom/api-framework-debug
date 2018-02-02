package in.erail.debug.service;

import com.google.common.net.HttpHeaders;
import in.erail.server.Server;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import in.erail.glue.Glue;
import io.netty.handler.codec.http.HttpHeaderNames;

/**
 *
 * @author vinay
 */
@RunWith(VertxUnitRunner.class)
public class ClusterMapServiceTest {

  @Rule
  public Timeout rule = Timeout.seconds(2000);

  @Test
  public void testProcess(TestContext context) {

    Async async = context.async();

    Server server = Glue.instance().resolve("/in/erail/server/Server");

    server
            .getVertx()
            .sharedData()
            .<String, String>rxGetClusterWideMap("dummayMap")
            .flatMapCompletable((m) -> {
              return m.rxPut("dummayKey", "dummayValue");
            })
            .subscribe(() -> {
              server
                      .getVertx()
                      .createHttpClient()
                      .get(server.getPort(), server.getHost(), "/v1/debug/clustermap/dump?mapName=dummayMap")
                      .putHeader("content-type", "application/json")
                      .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                      .handler(response -> {
                        context.assertEquals(response.statusCode(), 200, response.statusMessage());
                        context.assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*");
                        response.bodyHandler((event) -> {
                          context.assertEquals("dummayValue", event.toJsonObject().getString("dummayKey"));
                          async.complete();
                        });
                      })
                      .end();
            });
  }

}
