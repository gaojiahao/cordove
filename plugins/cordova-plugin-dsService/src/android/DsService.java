package org.apache.cordova.dsService;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This class echoes a string called from JavaScript.
 */
public class DsService extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("sayHello")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        } else if(action.equals("login")){
            this.login(args);
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    private void login(JSONArray args) {
        String url = args.getString(0);
        String uid = args.getString(1);
        JsonObject credentials = new JsonObject();
        credentials.addProperty("username", uid);
    }
}
