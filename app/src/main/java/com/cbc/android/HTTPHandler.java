package com.cbc.android;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HTTPHandler {
    private Context      context = null;
    private RequestQueue queue   = null;
    /*
     * The getMessage returns null, so the standard error is not much use. The delves around in error to
     * find a useful description of the error.
     */
    public String getErrorMessage(VolleyError error) {
        if (error.networkResponse != null) {
            String data = new String(error.networkResponse.data);
            int status = error.networkResponse.statusCode;
            return "Volley error Status " + status + " message " + error.getMessage() + " data " + data;
        } else {
            return  "Volley error message " + error.getMessage();
        }
    }
    public void log(VolleyError error, boolean stack) {
        if (stack) error.printStackTrace();

        Log.e("", getErrorMessage(error));
    }
    public void log(VolleyError error) {
        log(error, false);
    }
    private class Listeners  implements  Response.Listener<String>, Response.ErrorListener {
        private Response.Listener<String> valid = null;
        private Response.ErrorListener    error = null;
        private boolean                   complete = true;
        @Override
        public void onErrorResponse(VolleyError error) {
            if (this.error != null) {
                this.error.onErrorResponse(error);
            }
            else {
                log(error, true);
            }
            complete = true;
        }
        @Override
        public void onResponse(String response) {
            if (valid != null)
                valid.onResponse(response);
            else
                Log.i("","Response is: "+ response);
            complete = true;
        }
        protected Listeners(Response.Listener<String> valid, Response.ErrorListener error) {
            this.valid = valid;
            this.error = error;
        }
        protected boolean isComplete() {
            return complete;
        }
    }
    public class Request extends StringRequest {
        private Map<String,String> params  = new HashMap<String, String>();
        private Map<String,String> headers = new HashMap<String, String>();

        Listeners listeners = null;
        URL       url       = null;

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            for (Map.Entry<String, String> e : response.headers.entrySet()) {
                Log.i("", "Header key " + e.getKey() + "=" + e.getValue());
            }
            Map<String, String> responseHeaders = response.headers;
            String rawCookies = response.headers.get("Set-Cookie");
            return super.parseNetworkResponse(response);
        }
        public Request(int method, String url, Listeners listeners) {
            super(method, url, listeners, listeners);
            this.listeners = listeners;
        }
        protected Map<String,String> getParams(){
            return params;
        }
        public Map<String,String> getHeaders(){
            return headers;
        }
        public void addParameter(String name, String value) {
            params.put(name, value);
        }
        public void addHeader(String name, String value) {
            headers.put(name, value);
        }
        public boolean isComplete() {
            return  listeners.isComplete();
        }
        public void setUrl(URL url) {
            String origin = null;
            this.url = url;

            origin = url.getProtocol() + "://" + url.getHost();

            if (url.getPort() != -1) origin += ":" + url.getPort();

            addHeader("origin", origin);
        }
        public void setUrl(String url) throws MalformedURLException {
            setUrl(new URL(url));
        }
        public void send() {
            listeners.complete = false;
            queue.add(this);
        }
        public void reset() throws Exception {
            if (!isComplete()) throw new Exception("Request " + url.getFile() + " not complete");

            params.clear();
        }
    }
    public HTTPHandler(Context context, RequestQueue queue) {
        this.context = context;
        this.queue   = queue;
    }
    public HTTPHandler(Context context) {
        this(context, Volley.newRequestQueue(context));
    }
    private Request getRequest(int method, String url, Listeners listeners) {
        Request request = null;

        try {
            request = new Request(method, url, listeners);
            request.setUrl(url);
        } catch (Exception e) {
            Log.e("", e.getMessage(), e);
        }
        return request;
    }
    public Request getRequest(int method, String url, Response.Listener<String> listener, @Nullable Response.ErrorListener errorListener) {
        return getRequest(method, url, new Listeners(listener, errorListener));
    }
    public Request getRequest(int method, String url, Response.Listener<String> listener) {
        return getRequest(method, url, new Listeners(listener, null));
    }
    public Request getRequest(int method, String url) {
        return getRequest(method, url, new Listeners(null, null));
    }
}
