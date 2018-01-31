package in.erail.debug.service;

import in.erail.common.FrameworkConstants;
import in.erail.service.RESTServiceImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;

/**
 *
 * @author vinay
 */
public class ClusterMapService extends RESTServiceImpl {

  @Override
  public void process(Message<JsonObject> pMessage) {
    //JsonObject body = pMessage.body().getJsonObject(FrameworkConstants.RoutingContext.Json.BODY);
    JsonObject resp = new JsonObject();
    resp.put(FrameworkConstants.RoutingContext.Json.BODY, new JsonObject());
    pMessage.reply(resp);
  }

}
