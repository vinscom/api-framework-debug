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
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.redis.RedisClientInstance;
import io.vertx.redis.op.SetOptions;

/**
 *
 * @author vinay
 */
@RunWith(VertxUnitRunner.class)
public class RedisScanServiceTest {

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
      System.out.println("RedisScanServiceTest: Redis disabled. Skipping Test");
      return;
    }

    Async async = context.async();
    Server server = Glue.instance().resolve("/in/erail/server/Server");

    Observable
            .range(1, 10)
            .map(t -> Integer.toString(t))
            .flatMapSingle((t) -> {
              SetOptions opt = new SetOptions();
              opt.setEX(100);
              return redisClientInst
                      .getRedisClient()
                      .rxSetWithOptions("testscan" + t, t, opt);
            })
            .doOnComplete(() -> {
              server
                      .getVertx()
                      .createHttpClient()
                      .get(server.getPort(), server.getHost(), "/v1/debug/redis/scan?match=testscan*&count=2")
                      .putHeader("content-type", "application/json")
                      .putHeader(HttpHeaders.ORIGIN, "https://test.com")
                      .handler(response -> {
                        context.assertEquals(response.statusCode(), 200, response.statusMessage());
                        context.assertEquals(response.getHeader(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN.toString()), "*");
                        response.bodyHandler((event) -> {
                          JsonArray result = event.toJsonArray();
                          context.assertNotNull(result.getString(0));
                          context.assertEquals(2, result.getJsonArray(1).size());
                          async.complete();
                        });
                      })
                      .end();
            })
            .subscribe();

  }

}
