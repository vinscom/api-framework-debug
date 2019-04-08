package in.erail.debug.service;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import in.erail.server.Server;
import in.erail.glue.Glue;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author vinay
 */
@ExtendWith(VertxExtension.class)
@Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
public class ClusterMapServiceTest {

  @Test
  public void testProcess(VertxTestContext testContext) {

    Server server = Glue.instance().resolve("/in/erail/server/Server");

    server
            .getVertx()
            .sharedData()
            .<String, String>rxGetClusterWideMap("dummayMap")
            .flatMapCompletable((m) -> {
              return m.rxPut("dummayKey", "dummayValue");
            })
            .andThen(WebClient
                    .create(server.getVertx())
                    .get(server.getHttpServerOptions().getPort(), server.getHttpServerOptions().getHost(), "/v1/debug/clustermap/dump?mapName=dummayMap")
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                    .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                    .rxSend())
            .doOnSuccess(response -> assertEquals(response.statusCode(), 200, response.statusMessage()))
            .doOnSuccess(response -> assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*"))
            .doOnSuccess(response -> assertEquals("dummayValue", response.bodyAsJsonObject().getString("dummayKey")))
            .subscribe(t -> testContext.completeNow(), err -> testContext.failNow(err));
  }

}
