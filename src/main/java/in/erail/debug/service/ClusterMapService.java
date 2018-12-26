package in.erail.debug.service;

import com.google.common.net.MediaType;
import in.erail.model.RequestEvent;
import in.erail.model.ResponseEvent;
import in.erail.service.RESTServiceImpl;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.core.AsyncResult;
import io.vertx.core.json.JsonObject;
import java.util.Map;

/**
 *
 * @author vinay
 */
public class ClusterMapService extends RESTServiceImpl {

  @SuppressWarnings("unchecked")
  @Override
  public Maybe<ResponseEvent> process(RequestEvent pRequest) {

    String mapName = pRequest.getQueryStringParameters().get("mapName");

    return getVertx()
            .sharedData()
            .<String, String>rxGetClusterWideMap(mapName)
            .flatMapObservable((m) -> {
              return Observable.<Map.Entry<String, String>>create((e) -> {
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
            .reduce(new JsonObject(), (s, entry) -> {
              s.put(entry.getKey(), entry.getValue());
              return s;
            })
            .map((result) -> {
              return new ResponseEvent()
                      .setBody(result.toBuffer().getBytes())
                      .setContentType(MediaType.JSON_UTF_8);
            })
            .toMaybe();
  }

}
