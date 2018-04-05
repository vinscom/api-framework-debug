package in.erail.debug.service;

import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import in.erail.server.Server;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.Rule;
import in.erail.glue.Glue;
import in.erail.security.SecurityTools;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.op.SetOptions;

/**
 *
 * @author vinay
 */
@RunWith(VertxUnitRunner.class)
public class TopSubscriberServiceTest {

  @Rule
  public Timeout rule = Timeout.seconds(2000);

  /**
   * To enable test, enable redis
   *
   * @param context
   */
  @Test
  public void testProcess(TestContext context) {

    RedisClient redisClient = Glue.instance().resolve("/io/vertx/redis/RedisClient");
    if (redisClient == null) {
      System.out.println("TopSubscriberServiceTest: Redis disabled. Skipping Test");
      return;
    }

    Async async = context.async();
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
            .doOnComplete(() -> {
              server
                      .getVertx()
                      .createHttpClient()
                      .get(server.getPort(), server.getHost(), "/v1/debug/subscriber/top/10")
                      .putHeader(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString())
                      .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                      .handler(response -> {
                        context.assertEquals(response.statusCode(), 200, response.statusMessage());
                        context.assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*");
                        response.bodyHandler((event) -> {
                          JsonArray result = event.toJsonArray();
                          context.assertEquals(9, result.size());
                          async.complete();
                        });
                      })
                      .end();
            })
            .subscribe();
  }

}
