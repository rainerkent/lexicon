package org.lexicon.process;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Helper class to create Gson singleton
 *
 */
public class GsonHelper {

    private static Gson mGsonInstance;

    public static Gson createGson() {
        if (mGsonInstance == null)
            mGsonInstance = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

        return mGsonInstance;
    }

    private GsonHelper() {};

}
