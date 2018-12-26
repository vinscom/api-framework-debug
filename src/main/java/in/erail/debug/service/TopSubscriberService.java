package in.erail.debug.service;

import java.util.List;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.net.MediaType;
import com.google.common.primitives.Ints;

import in.erail.model.RequestEvent;
import in.erail.model.ResponseEvent;
import in.erail.service.RESTServiceImpl;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.redis.RedisClient;
import io.vertx.redis.op.ScanOptions;

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

  @SuppressWarnings("unchecked")
  @Override
  public Maybe<ResponseEvent> process(RequestEvent pRequest) {

    Integer returnResultCount = Ints.tryParse(pRequest.getPathParameters().get(getScanCountParamName()));

    if (returnResultCount == null) {
      returnResultCount = getDefaultReturnResultCount();
    }

    ScanOptions scanOptions = new ScanOptions();
    scanOptions.setCount(getDefaultScanCount());
    scanOptions.setMatch(getGlobalUniqueString() + "*");

    Subject<String> cursors = BehaviorSubject.createDefault("0").toSerialized();

    MinMaxPriorityQueue<JsonArray> topSub
            = MinMaxPriorityQueue
                    .<JsonArray>orderedBy((o1, o2) -> {
                      Long v1 = o1.getLong(1);
                      Long v2 = o2.getLong(1);
                      return v2.compareTo(v1);
                    })
                    .maximumSize(returnResultCount)
                    .create();

    return cursors
            .subscribeOn(Schedulers.io())
            .concatMap(cursor -> {
              return getRedisClient()
                      .rxScan(cursor, scanOptions)
                      .toObservable();
            })
            .flatMapSingle((jsonArrayData) -> {

              String next = jsonArrayData.getString(0);
              JsonArray keys = jsonArrayData.getJsonArray(1);

              if (!next.equals("0")) {
                cursors.onNext(next);
              } else {
                cursors.onComplete();
              }

              return Single.<JsonArray>just(keys);
            })
            .flatMap((keys) -> {

              if (keys.size() == 0) {
                return Observable.<JsonArray>empty();
              }

              return getRedisClient()
                      .rxMgetMany((List<String>) keys.getList())
                      .flatMapObservable((values) -> {
                        return Observable.<JsonArray>create((e) -> {
                          for (int i = 0; i < keys.size(); i++) {
                            String value = values.getString(i);
                            if (value == null || "0".equals(value)) {
                              continue;
                            }
                            e.onNext(new JsonArray().add(keys.getString(i)).add(Long.valueOf(value)));
                          }
                          e.onComplete();
                        });
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
            .map((t) -> {
              return new ResponseEvent().setBody(t.toBuffer().getBytes()).setContentType(MediaType.JSON_UTF_8);
            })
            .toMaybe();

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
