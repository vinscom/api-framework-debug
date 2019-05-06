package in.erail.debug.service;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import in.erail.server.Server;
import in.erail.glue.Glue;
import in.erail.security.SecurityTools;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.op.SetOptions;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 *
 * @author vinay
 */
@ExtendWith(VertxExtension.class)
@Timeout(value = 10, timeUnit = TimeUnit.MINUTES)
public class TopSubscriberServiceTest {

  /**
   * To enable test, enable redis
   *
   * @param context
   */
  @Test
  public void testProcess(VertxTestContext testContext) {

    RedisClient redisClient = Glue.instance().resolve("/io/vertx/redis/RedisClient");
    if (redisClient == null) {
      System.out.println("TopSubscriberServiceTest: Redis disabled. Skipping Test");
      testContext.completeNow();
      return;
    }

    Server server = Glue.instance().resolve("/in/erail/server/Server");
    SecurityTools secTools = Glue.instance().resolve("/in/erail/security/SecurityTools");
    SetOptions opt = new SetOptions().setEX(100);

    Observable
            .range(0, 10)
            .map(t -> Integer.toString(t))
            .flatMapSingle((t) -> {
              return redisClient
                      .rxSetWithOptions(secTools.getGlobalUniqueString() + "testsubcriber" + t, t, opt);
            })
            .ignoreElements()
            .andThen(WebClient
                    .create(server.getVertx())
                    .get(server.getHttpServerOptions().getPort(), server.getHttpServerOptions().getHost(), "/v1/debug/subscriber/top/10")
                    .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                    .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                    .rxSend())
            .doOnSuccess(response -> assertEquals(response.statusCode(), 200, response.statusMessage()))
            .doOnSuccess(response -> assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*"))
            .doOnSuccess(response -> {
              JsonArray result = response.bodyAsJsonArray();
              assertEquals(9, result.size());
            })
            .subscribe(t -> testContext.completeNow(), err -> testContext.failNow(err));
  }

}
