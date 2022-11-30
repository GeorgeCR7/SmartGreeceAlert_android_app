package com.example.smartgreecealert.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import androidx.appcompat.app.AlertDialog;

import com.example.smartgreecealert.R;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class Language {
    public static String language;
    private Context context;
    private Map<String, Integer> locales;
    private Activity activity;

    public Language(Context context) {
        this.context = context;
        activity = getActivity(context);

        locales = new HashMap<>();
        locales.put("en", 0);
        locales.put("el", 1);
        locales.put("de", 2);
    }

    public void setLocale(String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        context.getResources().updateConfiguration(config,
                context.getResources().getDisplayMetrics());

        //Save data to shared preferences.
        SharedPreferences.Editor editor = context.getSharedPreferences("Settings", MODE_PRIVATE).edit();
        editor.putString("My_Lang", lang);
        editor.apply();
        language = lang;
    }

    //Load language saved in shared preferences.
    public void loadLocale() {
        SharedPreferences prefs = context.getSharedPreferences("Settings", Activity.MODE_PRIVATE);
        language = prefs.getString("My_Lang", "");
        setLocale(language);
    }

    public void showChangeLanguageDialog() {
        //Array of languages to display in alert dialog.
        String eng = context.getResources().getString(R.string.engLanguage);
        String gr = context.getResources().getString(R.string.grLanguage);
        String de = context.getResources().getString(R.string.deLanguage);
        final String[] langList = {eng, gr, de};
        Map<String, Integer> locales = getLocales();

        AlertDialog.Builder mBuilder = new AlertDialog.Builder(context);
        mBuilder.setTitle(R.string.chooseLanguage);

        int initialPos = 0;
        if (!Language.language.isEmpty()) {
            Integer initialPosObj = locales.get(language);
            if (initialPosObj != null) {
                initialPos = initialPosObj;
            }
        }

        mBuilder.setSingleChoiceItems(langList, initialPos, null
        ).setPositiveButton("OK", (dialogInterface, i) -> {

            int selectedPosition = ((AlertDialog) dialogInterface).getListView().getCheckedItemPosition();
            setLocale(Language.getKeyByValue(locales, selectedPosition));
            activity.recreate();

        }).setNegativeButton(R.string.cancelDialog, (dialogInterface, which) -> {
            //Dismiss alert dialog when language selected.
            dialogInterface.dismiss();

        }).create().show();
    }

    /**
     * Gets key from value in map
     *
     * @param map Map<T, E>
     * @param value E
     * @return <T, E> T
     */
    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Map<String, Integer> getLocales() {
        return locales;
    }

    public Activity getActivity(Context context)
    {
        if (context == null)
        {
            return null;
        }
        else if (context instanceof ContextWrapper)
        {
            if (context instanceof Activity)
            {
                return (Activity) context;
            }
            else
            {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }
        return null;
    }
}
