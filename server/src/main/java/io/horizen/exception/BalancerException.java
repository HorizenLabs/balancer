package io.horizen.exception;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.horizen.helpers.MyGsonManager;

import java.util.HashMap;
import java.util.Map;

public class BalancerException extends Exception {

    private static final Map<Integer, String> errorDict = new HashMap<>();
    static {
        errorDict.put(100, "Generic error");
        errorDict.put(101, "Can not add ownership");
        errorDict.put(102, "Could not get ownership for given sc address");
        errorDict.put(103, "Could not get owner sc addresses");
        errorDict.put(104, "Can not create proposal");
        errorDict.put(105, "Can not get active proposal");
        errorDict.put(106, "Rosetta error");
        errorDict.put(107, "Snapshot error");
    }

    private final int code;
    private final String description;
    private final String detail;

    public BalancerException(int code, String detail) {
        this.code = code;
        if (errorDict.containsKey(code)) {
            this.description = errorDict.get(code);
        } else {
            this.description = "Invalid error code: " + code + ". Pls add it to the catalog";
        }
        this.detail = detail;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getDetail() {
        return detail;
    }

    @Override
    public String toString() {
        Gson gson = MyGsonManager.getGson();

        JsonObject errorObject = new JsonObject();
        errorObject.addProperty("code", this.code);
        errorObject.addProperty("description", this.description);
        errorObject.addProperty("detail", this.detail);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("error", errorObject);

        return gson.toJson(jsonObject);
    }
}
