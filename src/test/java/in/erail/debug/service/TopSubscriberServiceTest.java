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
import in.erail.security.SecurityTools;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.redis.RedisClientInstance;
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

    RedisClientInstance redisClientInst = Glue.instance().resolve("/io/vertx/redis/RedisClientInstance");
    if (!redisClientInst.isEnable()) {
      System.out.println("TopSubscriberServiceTest: Redis disabled. Skipping Test");
      return;
    }

    Async async = context.async();
    Server server = Glue.instance().resolve("/in/erail/server/Server");
    SecurityTools secTools = Glue.instance().resolve("/in/erail/security/SecurityTools");

    Observable
            .range(1, 10000)
            .map(t -> Integer.toString(t))
            .flatMapSingle((t) -> {
              SetOptions opt = new SetOptions();
              opt.setEX(100);
              return redisClientInst
                      .getRedisClient()
                      .rxSetWithOptions(secTools.getGlobalUniqueString() + "testsubcriber" + t, t, opt);
            })
            .doOnComplete(() -> {
              server
                      .getVertx()
                      .createHttpClient()
                      .get(server.getPort(), server.getHost(), "/v1/debug/subscriber/top/2")
                      .putHeader("content-type", "application/json")
                      .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                      .handler(response -> {
                        context.assertEquals(response.statusCode(), 200, response.statusMessage());
                        context.assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*");
                        response.bodyHandler((event) -> {
                          JsonArray result = event.toJsonArray();
                          context.assertEquals(2, result.size());
                          System.out.println(result.toString());
                          async.complete();
                        });
                      })
                      .end();
            })
            .subscribe();
  }

}
