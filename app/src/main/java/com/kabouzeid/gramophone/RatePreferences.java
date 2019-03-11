package com.kabouzeid.gramophone;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class RatePreferences {

    private static RatePreferences instance;
    private SharedPreferences pref;
    private static final String PREFERENCE_NAME = "RatePreference";

    public static RatePreferences getInstance() {
        if (instance == null) {
            instance = new RatePreferences();
        }
        return instance;
    }


    public long getDateWhenRateDialogShow(Context context) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getLong("DateWhenRateDialogShow", 0);
    }

    public void setDateWhenRateDialogShow(Context context, long l) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putLong("DateWhenRateDialogShow", l);
        editor.apply();
    }


    public int getCounterForRate(Context context) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getInt("CounterForRate", 0);
    }

    public void setCounterForRate(Context context, int l) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putInt("CounterForRate", l);
        editor.apply();
    }

    public void addCounterForRate(Context context, int numAdd) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        int i = pref.getInt("CounterForRate", 0);

        Editor editor = pref.edit();
        editor.putInt("CounterForRate", i + numAdd);
        editor.apply();
    }


    public boolean isRated(Context context) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean("Rated", false);
    }

    public void setRate(Context context, boolean b) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean("Rated", b);
        editor.apply();
    }


    public boolean isAlreadyDisplayedRateDialog(Context context) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean("AlreadyDisplayedRateDialog", false);
    }

    public void setAlreadyDisplayedRateDialog(Context context, boolean b) {
        pref = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        Editor editor = pref.edit();
        editor.putBoolean("AlreadyDisplayedRateDialog", b);
        editor.apply();
    }

}
