package in.erail.debug.service;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.Ints;
import in.erail.common.FrameworkConstants;
import in.erail.service.RESTServiceImpl;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.eventbus.Message;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.op.ScanOptions;
import java.util.PriorityQueue;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author vinay
 */
public class TopSubscriberService extends RESTServiceImpl {

  private RedisClient mRedisClient;
  private String mScanCountParamName = "count";
  private Integer mDefaultReturnResultCount = 100;
  private String mGlobalUniqueString;
  private Integer mDefaultScanCount = 100000;

  @Override
  public void process(Message<JsonObject> pMessage) {

    JsonObject pathParam = pMessage
            .body()
            .getJsonObject(FrameworkConstants.RoutingContext.Json.PATH_PARAM);

    Integer returnResultCount = Ints.tryParse(pathParam.getString(getScanCountParamName()));

    if (returnResultCount == null) {
      returnResultCount = getDefaultReturnResultCount();
    }

    ScanOptions scanOptions = new ScanOptions();
    scanOptions.setCount(getDefaultScanCount());
    scanOptions.setMatch(getGlobalUniqueString() + "*");

    Subject<String> cursors = BehaviorSubject.createDefault("0").toSerialized();

    PriorityQueue<JsonArray> topSub = new PriorityQueue<JsonArray>((o1, o2) -> {
      Long v1 = o1.getLong(1);
      Long v2 = o2.getLong(1);
      return v2.compareTo(v1);
    });

    cursors
            .concatMap(cursor -> {
              return getRedisClient()
                      .rxScan(cursor, scanOptions)
                      .toObservable();
            })
            .flatMap((jsonArrayData) -> {

              String next = jsonArrayData.getString(0);
              JsonArray data = jsonArrayData.getJsonArray(1);

              if (!next.equals("0")) {
                cursors.onNext(next);
              } else {
                cursors.onComplete();
              }
              
              return Observable.fromIterable(data);
            })
            .flatMapSingle((key) -> {
              String k = (String) key;

              return getRedisClient()
                      .rxGet(k)
                      .map((v) -> {
                        return new JsonArray().add(k).add(Long.valueOf(v));
                      });
            })
            .reduce(topSub, (acc, item) -> {
              topSub.add(item);
              return topSub;
            })
            .flatMapObservable((q) -> {
              return Observable.create((e) -> {
                JsonArray item = null;
                while ((!e.isDisposed()) && ((item = q.poll()) != null)) {
                  e.onNext(item);
                }
                e.onComplete();
              });
            })
            .take(returnResultCount)
            .reduce(new JsonArray(), (acc, item) -> acc.add(item))
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

  public String getScanCountParamName() {
    return mScanCountParamName;
  }

  public void setScanCountParamName(String pScanCountParamName) {
    this.mScanCountParamName = pScanCountParamName;
  }

  public Integer getDefaultReturnResultCount() {
    return mDefaultReturnResultCount;
  }

  public void setDefaultReturnResultCount(Integer pDefaultReturnResultCount) {
    this.mDefaultReturnResultCount = pDefaultReturnResultCount;
  }

  public String getGlobalUniqueString() {
    return mGlobalUniqueString;
  }

  public void setGlobalUniqueString(String pGlobalUniqueString) {
    this.mGlobalUniqueString = pGlobalUniqueString;
  }

  public Integer getDefaultScanCount() {
    return mDefaultScanCount;
  }

  public void setDefaultScanCount(Integer pDefaultScanCount) {
    this.mDefaultScanCount = pDefaultScanCount;
  }

}
