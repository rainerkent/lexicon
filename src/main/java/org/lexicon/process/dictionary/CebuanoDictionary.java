package org.lexicon.process.dictionary;

import static org.lexicon.util.ResourceUtil.CEBUANO_ROOTS_JSON;

import java.util.HashSet;
import java.util.Set;

import org.lexicon.process.GsonHelper;
import org.lexicon.util.ResourceUtil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class CebuanoDictionary {

    private static final int MIN_ROOT_LENGTH = 3;

    private static CebuanoDictionary instance;

    private Set<String> roots;

    private static CebuanoDictionary getInstance() {
        if (instance == null) {
            instance = new CebuanoDictionary();
        }
        return instance;
    }

    private CebuanoDictionary() {
        Gson gson = GsonHelper.createGson();
        JsonElement wordsJson = ResourceUtil.parseJson(CEBUANO_ROOTS_JSON);
        roots = gson.fromJson(wordsJson, new TypeToken<Set<String>>() {}.getType());
    }

    public static boolean isRootWord(String root) {
        CebuanoDictionary instance = getInstance();
        if (root.length() < MIN_ROOT_LENGTH) { return false; }
        return instance.roots.contains(root);
    }

    // public boolean isRootWordWithType(String root, String type) {
    // if (root.length() < MIN_ROOT_LENGTH) {
    // return false;
    // }
    //
    // for (String value : roots) {
    // if (value.indexOf(root) == 0 && value.indexOf(type) > (root.length() - 1))
    // return true;
    // }
    //
    // return false;
    // }

    public static Set<String> getOtherForms(String word) {
        String[][] cases = { { "siy", "sy" }, { "kuw", "kw" }, { "riy", "ry" }, { "duw", "dw" }, { "piy", "py" },
                { "diy", "dy" }, { "yig", "yg" } };

        Set<String> forms = new HashSet<>();
        for (String[] aCase : cases) {
            if (word.contains(aCase[0])) {
                forms.add(word.replaceAll(aCase[0], aCase[1]));
            }
            if (word.contains(aCase[1])) {
                forms.add(word.replaceAll(aCase[1], aCase[0]));
            }
        }

        return forms;
    }

}
