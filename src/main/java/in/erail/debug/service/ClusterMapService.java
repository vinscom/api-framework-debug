package in.erail.debug.service;

import in.erail.common.FrameworkConstants;
import in.erail.service.RESTServiceImpl;
import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import java.util.Map;

/**
 *
 * @author vinay
 */
public class ClusterMapService extends RESTServiceImpl {

  @Override
  public void process(Message<JsonObject> pMessage) {

    String mapName = pMessage
            .body()
            .getJsonObject(FrameworkConstants.RoutingContext.Json.QUERY_STRING_PARAM)
            .getString("mapName");

    getVertx()
            .sharedData()
            .<String, String>rxGetClusterWideMap(mapName)
            .flatMapObservable((m) -> {
              return Observable.<Map.Entry<String,String>>create((e) -> {
                m
                        .getDelegate()
                        .entries((Object k) -> {
                          AsyncResult<Map<String, String>> entriesMap = (AsyncResult<Map<String, String>>) k;
                          entriesMap
                                  .result()
                                  .entrySet()
                                  .forEach((entry) -> {
                                    e.onNext(entry);
                                  });
                          e.onComplete();
                        });
              });
            })
            .reduce(new JsonObject(), (s,entry) -> {
              s.put(entry.getKey(), entry.getValue());
              return s;
            })
            .subscribe((result) -> {
              pMessage.reply(new JsonObject().put(FrameworkConstants.RoutingContext.Json.BODY, result));
            });
    
  }

}
