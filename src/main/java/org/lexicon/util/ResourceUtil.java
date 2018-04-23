package org.lexicon.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class ResourceUtil {

    public static final String CEBUANO_ROOTS_JSON = "json/cebuano_roots.json";
    public static final String CEBUANO_STEMMER_JSON = "json/cebuano_stemmer.json";
    public static final String CEBUANO_STOP_JSON = "json/cebuano_stop_words.json";
    public static final String ENGLISH_WORDS_JSON = "json/english_words.json";
    public static final String TRANSLATIONS_JSON = "json/translations.json";

    public static JsonElement parseJson(String resourceName) {
        JsonParser parser = new JsonParser();
        return parser.parse(readFile(resourceName));
    }

    public static String readFile(String resourceName) {
        String result = "";
        try {
            InputStream in = ClassLoaderUtil.getResourceAsStream(resourceName, ResourceUtil.class);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }
            result = sb.toString();
            br.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private ResourceUtil() {};
}
