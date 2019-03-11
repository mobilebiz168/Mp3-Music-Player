package com.kabouzeid.ringdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class RingdroidPreferences {

    private static RingdroidPreferences instance;
    private SharedPreferences pref;
    private static final String PREFERENCE_NAME = "RingdroidPreferences";

    public static RingdroidPreferences getInstance() {
        if (instance == null) {
            instance = new RingdroidPreferences();
        }
        return instance;
    }


    // Filter in ringtone list fragment
    public final static int SHOW_ALL = 1;
    public final static int SHOW_RINGTONE = 2;
    public final static int SHOW_NOTIFICATION = 3;
    public final static int SHOW_ALARM = 4;
    public final static int SHOW_MUSIC = 5;

    public int getFilterToShow(Context context) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getInt("FilterToShow", SHOW_ALL);
    }

    public void setFilterToShow(Context context, int i) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putInt("FilterToShow", i);
        editor.apply();
    }

}
