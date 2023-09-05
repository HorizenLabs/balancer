package io.horizen.helpers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MyGsonManager {
    private static final Gson gsonInstance = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static Gson getGson() {
        return gsonInstance;
    }
}
