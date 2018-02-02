package in.erail.debug.service;

import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import in.erail.common.FrameworkConstants;
import in.erail.service.RESTServiceImpl;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.op.ScanOptions;

/**
 *
 * @author vinay
 */
public class RedisScanService extends RESTServiceImpl {

  private RedisClient mRedisClient;
  private String mScanMatchParamName = "pattern";
  private String mScanCountParamName = "count";
  private String mScanCursorParamName = "cursor";
  private Integer mDefaultScanCount = 10000;

  @Override
  public void process(Message<JsonObject> pMessage) {

    JsonObject queryParam = pMessage
            .body()
            .getJsonObject(FrameworkConstants.RoutingContext.Json.QUERY_STRING_PARAM);

    String match = queryParam.getString(getScanMatchParamName());
    Integer count = Ints.tryParse(queryParam.getString(getScanCountParamName()));
    String cursor = queryParam.getString(getScanCursorParamName());

    if (count == null) {
      count = getDefaultScanCount();
    }

    if (Strings.isNullOrEmpty(cursor)) {
      cursor = "0";
    }

    ScanOptions scanOptions = new ScanOptions();
    scanOptions.setCount(count);

    if (!Strings.isNullOrEmpty(match)) {
      scanOptions.setMatch(match);
    }

    getRedisClient()
            .rxScan(cursor, scanOptions)
            .subscribe((result) -> {
              pMessage.reply(new JsonObject().put(FrameworkConstants.RoutingContext.Json.BODY, result));
            });

  }

  public RedisClient getRedisClient() {
    return mRedisClient;
  }

  public void setRedisClient(RedisClient pRedisClient) {
    this.mRedisClient = pRedisClient;
  }

  public String getScanMatchParamName() {
    return mScanMatchParamName;
  }

  public void setScanMatchParamName(String pScanMatchParamName) {
    this.mScanMatchParamName = pScanMatchParamName;
  }

  public String getScanCountParamName() {
    return mScanCountParamName;
  }

  public void setScanCountParamName(String pScanCountParamName) {
    this.mScanCountParamName = pScanCountParamName;
  }

  public Integer getDefaultScanCount() {
    return mDefaultScanCount;
  }

  public void setDefaultScanCount(Integer pDefaultScanCount) {
    this.mDefaultScanCount = pDefaultScanCount;
  }

  public String getScanCursorParamName() {
    return mScanCursorParamName;
  }

  public void setScanCursorParamName(String pScanCursorParamName) {
    this.mScanCursorParamName = pScanCursorParamName;
  }

}
