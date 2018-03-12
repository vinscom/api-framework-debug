package in.erail.debug.service;

import in.erail.common.FrameworkConstants;
import in.erail.service.RESTServiceImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;

/**
 *
 * @author vinay
 */
public class GlueBrowserService  extends RESTServiceImpl {

  @Override
  public void process(Message<JsonObject> pMessage) {
    pMessage.reply(new JsonObject().put(FrameworkConstants.RoutingContext.Json.BODY, new JsonObject()));
  }
  
}
