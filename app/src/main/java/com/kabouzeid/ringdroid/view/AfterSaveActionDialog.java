/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kabouzeid.ringdroid.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Message;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;

import com.kabouzeid.gramophone.R;


public class AfterSaveActionDialog extends Dialog {

    private Message mResponse;

    public AfterSaveActionDialog(final Context context, Message response) {
        super(context);

        // Inflate our UI from its XML layout description.
        setContentView(R.layout.ringdroid_after_save_action);

        setTitle(R.string.success);

        ((Button) findViewById(R.id.button_make_default))
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {

                        // Check permission and request if necessary
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!Settings.System.canWrite(context)) {
                                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                                intent.setData(Uri.parse("package:" + context.getPackageName()));
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                context.startActivity(intent);

                                return;
                            }
                        }

                        closeAndSendResult(R.id.button_make_default);
                    }
                });

        ((Button) findViewById(R.id.button_do_nothing))
                .setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        closeAndSendResult(R.id.button_do_nothing);
                    }
                });

        mResponse = response;
    }

    private void closeAndSendResult(int clickedButtonId) {
        mResponse.arg1 = clickedButtonId;
        mResponse.sendToTarget();
        dismiss();
    }

}
