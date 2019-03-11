package com.kabouzeid.gramophone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;

import com.kabouzeid.gramophone.util.Util;

import java.util.Date;

public class RateHelper {

    // Dialog will show after x day set in this variable (dialog already show but user click no)
    private static final short DATE_DURATION_TO_SHOW_DIALOG = 1;

    // Dialog will show after x times set in this variable (first time show dialog)
    private static final short TIME_TO_SHOW_DIALOG_FIRST_TIME = 4;

    public static void showRateDialogIfNecessary(Context context) {
        if (RatePreferences.getInstance().isAlreadyDisplayedRateDialog(context)) {
            if (!(RatePreferences.getInstance().isRated(context))) {
                Date currentDate = new Date(System.currentTimeMillis());
                Date oldDate = getDateWhenRateDialogShow(context);
                int differenceInDay = -1;
                if (oldDate != null) {
                    differenceInDay = Util.getDaysBetweenDates(oldDate, currentDate);
                }

                if (differenceInDay >= DATE_DURATION_TO_SHOW_DIALOG) {
                    showDialog(context);
                }
            }
        } else {
            RatePreferences.getInstance().addCounterForRate(context, 1);
            if (RatePreferences.getInstance().getCounterForRate(context) >= TIME_TO_SHOW_DIALOG_FIRST_TIME) {
                RatePreferences.getInstance().setCounterForRate(context, 0);
                RatePreferences.getInstance().setAlreadyDisplayedRateDialog(context, true);
                showDialog(context);
            }
        }
    }

    private static void showDialog(final Context context) {
        RatePreferences.getInstance().setDateWhenRateDialogShow(context, System.currentTimeMillis());

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        RatePreferences.getInstance().setRate(context, true);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse(context.getResources().getString(
                                        R.string.app_link)));
                        context.startActivity(browserIntent);
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setTitle(context.getString(R.string.dg_rate_title))
                .setMessage(context.getString(R.string.dg_rate_msg))
                .setCancelable(false)
                .setPositiveButton(context.getString(R.string.dg_rate_btn_yes), dialogClickListener)
                .setNegativeButton(context.getString(R.string.dg_rate_btn_no), dialogClickListener).show();
    }

    private static Date getDateWhenRateDialogShow(Context context) {
        long l = RatePreferences.getInstance().getDateWhenRateDialogShow(context);
        if (l == 0) {
            return null;
        } else {
            return new Date(l);
        }
    }

}
