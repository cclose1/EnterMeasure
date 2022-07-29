package com.cbc.entermeasure;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cbc.android.Alert;
import com.cbc.android.HTTPHandler;
import com.cbc.android.IntentHandler;
import com.cbc.android.KeyValueStore;
import com.cbc.android.LabelledText;

import java.net.CookieHandler;
import java.net.CookieManager;

public class AccessServer extends AppCompatActivity {
    Alert               alert       = null;
    IntentHandler       intent      = null;
    HTTPHandler         httpHandler = null;
    HTTPHandler.Request request     = null;
    LabelledText        server      = null;
    LabelledText        user        = null;
    LabelledText        password    = null;
    KeyValueStore       properties  = null;

    private String getURL() {
        boolean ssl = ((CheckBox) findViewById(R.id.usessl)).isChecked();

        if (ssl) return "https://" + server.getText() + ":8443/Recorder/Record";

        return "http://" + server.getText() + ":8080/Recorder/Record";
    }
    private HTTPHandler.Request getRequest() {
        return httpHandler.getRequest(Request.Method.POST, getURL(), listeners, listeners);
    }
    private class Listeners  implements  Response.Listener<String>, Response.ErrorListener {

        @Override
        public void onErrorResponse(VolleyError error) {
            httpHandler.log(error);
            alert.display("Server Error", httpHandler.getErrorMessage(error));
        }
        @Override
        public void onResponse(String response) {
            Log.i("", response);
            alert.display("Response", response);
        }
    }
    Listeners listeners = new Listeners();

    private void addConnectParameter(LabelledText fld) {
        String name = fld.getLabel().getText().toString();

        properties.setValue(name, fld.getText());
        request.addParameter(name.toLowerCase(), fld.getText());
    }
    public void onClick(View view) {
        try {
            switch (view.getId()) {
                case R.id.connect:
                    request = getRequest();

                    request.addParameter("action", "login");
                    addConnectParameter(user);
                    addConnectParameter(password);
                    request.send();
                    break;
                case R.id.send:
                    request = getRequest();
                    request.addParameter("action",  "updates");
                    request.addParameter("user",    user.getText());
                    request.addParameter("updates", intent.getStringExtra("Data"));
                    request.send();
                    break;
                default:
                    Log.w("", "On click on id " + view.getId() + " unexpected");
            }
        } catch (Exception e) {
            Log.e("", "On click on id " + view.getId() + " failed with " + e.getMessage(), e);
        }
    }
    private void onComplete(View view) {
        finish();
    }
    @Override
    public void onBackPressed() {
        onComplete(null);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*
             * Make menu back arrow behave as Complete button;
             */
            case android.R.id.home:
                onComplete(null);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    LabelledText setField(int labId, int fldId, String defaultValue) {
        LabelledText fld = new LabelledText(findViewById(labId), findViewById(fldId), "", alert);

        fld.setBackgroundColor(Color.rgb(230,255,255));
        fld.setText(properties.getValue(fld.getLabel().getText().toString(), defaultValue));

        if (fldId == R.id.password) fld.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        return fld;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_server);

        alert       = new Alert(this);
        properties  = new KeyValueStore(this, "AccessServer");
        server      = setField(R.id.serverLab,   R.id.server,   "10.0.2.2");
        user        = setField(R.id.userLab,     R.id.user,     "cclose");
        password    = setField(R.id.passwordLab, R.id.password, "");
        intent      = new IntentHandler(getIntent());
        httpHandler = new HTTPHandler(this);
        CookieHandler.setDefault(new CookieManager());
    }
}