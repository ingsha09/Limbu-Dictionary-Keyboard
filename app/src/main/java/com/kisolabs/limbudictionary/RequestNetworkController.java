package com.kisolabs.limbudictionary;

import android.app.Activity;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RequestNetworkController {
  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String PUT = "PUT";
  public static final String DELETE = "DELETE";

  public static final int REQUEST_PARAM = 0;
  public static final int REQUEST_BODY = 1;

  private static final int SOCKET_TIMEOUT = 15000;
  private static final int READ_TIMEOUT = 25000;

  protected OkHttpClient client;
  private static RequestNetworkController mInstance;

  public static synchronized RequestNetworkController getInstance() {
    if (mInstance == null) {
      mInstance = new RequestNetworkController();
    }
    return mInstance;
  }

  private OkHttpClient getClient() {
    if (client == null) {
      // Secure OkHttpClient relying on default Android system certificate validation
      client = new OkHttpClient.Builder()
          .connectTimeout(SOCKET_TIMEOUT, TimeUnit.MILLISECONDS)
          .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
          .writeTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
          .build();
    }
    return client;
  }

  public void execute(final RequestNetwork requestNetwork, String method, String url,
      final String tag, final RequestNetwork.RequestListener requestListener) {

    Request.Builder reqBuilder = new Request.Builder();
    Headers.Builder headerBuilder = new Headers.Builder();

    if (requestNetwork.getHeaders() != null && requestNetwork.getHeaders().size() > 0) {
      for (HashMap.Entry<String, Object> header : requestNetwork.getHeaders().entrySet()) {
        headerBuilder.add(header.getKey(), String.valueOf(header.getValue()));
      }
    }

    try {
      if (requestNetwork.getRequestType() == REQUEST_PARAM) {
        if (method.equalsIgnoreCase(GET)) {
          HttpUrl parsedUrl = HttpUrl.parse(url);
          if (parsedUrl == null) {
            throw new IllegalArgumentException("Unexpected or malformed URL: " + url);
          }

          HttpUrl.Builder httpBuilder = parsedUrl.newBuilder();

          if (requestNetwork.getParams() != null && requestNetwork.getParams().size() > 0) {
            for (HashMap.Entry<String, Object> param : requestNetwork.getParams().entrySet()) {
              httpBuilder.addQueryParameter(param.getKey(), String.valueOf(param.getValue()));
            }
          }

          reqBuilder.url(httpBuilder.build()).headers(headerBuilder.build()).get();
        } else {
          FormBody.Builder formBuilder = new FormBody.Builder();
          if (requestNetwork.getParams() != null && requestNetwork.getParams().size() > 0) {
            for (HashMap.Entry<String, Object> param : requestNetwork.getParams().entrySet()) {
              formBuilder.add(param.getKey(), String.valueOf(param.getValue()));
            }
          }

          RequestBody reqBody = formBuilder.build();
          reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody);
        }
      } else {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        String jsonPayload = new Gson().toJson(requestNetwork.getParams());
        RequestBody reqBody = RequestBody.create(jsonPayload, mediaType);

        if (method.equalsIgnoreCase(GET)) {
          reqBuilder.url(url).headers(headerBuilder.build()).get();
        } else {
          reqBuilder.url(url).headers(headerBuilder.build()).method(method, reqBody);
        }
      }

      Request req = reqBuilder.build();

      getClient().newCall(req).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, final IOException e) {
          Activity act = requestNetwork.getActivity();
          if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            act.runOnUiThread(() -> requestListener.onErrorResponse(tag, e.getMessage() != null ? e.getMessage() : "Network error"));
          }
        }

        @Override
        public void onResponse(Call call, final Response response) throws IOException {
          final String responseBody = (response.body() != null) ? response.body().string().trim() : "";
          Activity act = requestNetwork.getActivity();

          if (act != null && !act.isFinishing() && !act.isDestroyed()) {
            act.runOnUiThread(() -> {
              Headers headers = response.headers();
              HashMap<String, Object> headerMap = new HashMap<>();
              for (String name : headers.names()) {
                headerMap.put(name, headers.get(name) != null ? headers.get(name) : "");
              }
              requestListener.onResponse(tag, responseBody, headerMap);
            });
          }
        }
      });
    } catch (Exception e) {
      if (requestListener != null) {
        requestListener.onErrorResponse(tag, e.getMessage() != null ? e.getMessage() : "Execution error");
      }
    }
  }
}
