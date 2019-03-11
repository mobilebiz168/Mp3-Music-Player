package com.kabouzeid.ringdroid.view;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.kabouzeid.gramophone.R;
import com.kabouzeid.ringdroid.RingdroidPreferences;
import com.kabouzeid.ringdroid.soundfile.SoundFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class RingdroidSelectActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private android.support.v7.widget.SearchView mFilter;
    private ListView listView;
    private SimpleCursorAdapter mAdapter;
    private boolean mWasGetContentIntent;
    private Cursor mInternalCursor;
    private Cursor mExternalCursor;

    // Request code
    public static final int REQUEST_CODE_EDIT = 1;

    // Context menu
    private static final int CMD_EDIT = 4;
    private static final int CMD_DELETE = 5;
    private static final int CMD_SET_AS_DEFAULT = 6;

    private static final String[] INTERNAL_COLUMNS = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_MUSIC,
            "\"" + MediaStore.Audio.Media.INTERNAL_CONTENT_URI + "\""
    };

    private static final String[] EXTERNAL_COLUMNS = new String[]{
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.IS_RINGTONE,
            MediaStore.Audio.Media.IS_ALARM,
            MediaStore.Audio.Media.IS_NOTIFICATION,
            MediaStore.Audio.Media.IS_MUSIC,
            "\"" + MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "\""
    };

    private static final int INTERNAL_CURSOR_ID = 0;
    private static final int EXTERNAL_CURSOR_ID = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String status = Environment.getExternalStorageState();
        if (status.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            showFinalAlert(getResources().getText(R.string.sdcard_readonly));
            return;
        }
        if (status.equals(Environment.MEDIA_SHARED)) {
            showFinalAlert(getResources().getText(R.string.sdcard_shared));
            return;
        }
        if (!status.equals(Environment.MEDIA_MOUNTED)) {
            showFinalAlert(getResources().getText(R.string.no_sdcard));
            return;
        }

        Intent intent = getIntent();
        if (intent.getAction() != null) {
            mWasGetContentIntent = intent.getAction().equals(
                    Intent.ACTION_GET_CONTENT);
        } else {
            mWasGetContentIntent = false;
        }

        // Inflate UI
        setContentView(R.layout.ringdroid_media_select);
        listView = (ListView) findViewById(R.id.listView);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Set toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        try {
            mAdapter = new SimpleCursorAdapter(
                    this,
                    // Use a template that displays a text view
                    R.layout.ringdroid_media_select_row,
                    null,
                    // Map from database columns...
                    new String[]{
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media._ID,
                            MediaStore.Audio.Media._ID},
                    // To widget ids in the row layout...
                    new int[]{
                            R.id.row_artist,
                            R.id.row_title,
                            R.id.row_icon,
                            R.id.row_options_button},
                    0);

            listView.setAdapter(mAdapter);
            listView.setItemsCanFocus(true);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent,
                                        View view,
                                        int position,
                                        long id) {
                    startRingdroidEditor();
                }
            });

            mInternalCursor = null;
            mExternalCursor = null;
            getLoaderManager().initLoader(INTERNAL_CURSOR_ID, null, this);
            getLoaderManager().initLoader(EXTERNAL_CURSOR_ID, null, this);
        } catch (SecurityException | IllegalArgumentException e) {
            // No permission to retrieve audio?
            Log.e("Ringdroid", e.toString());
        }

        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.row_options_button) {
                    // Get the arrow ImageView and set the onClickListener to open the context menu.
                    ImageView iv = (ImageView) view;
                    iv.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            openContextMenu(v);
                        }
                    });
                    return true;
                } else if (view.getId() == R.id.row_icon) {
                    setSoundIconFromCursor((ImageView) view, cursor);
                    return true;
                }

                return false;
            }
        });

        // Long-press opens a context menu
        registerForContextMenu(listView);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent dataIntent) {
        if (requestCode != REQUEST_CODE_EDIT) {
            return;
        }

        if (resultCode != RESULT_OK) {
            return;
        }

        setResult(RESULT_OK, dataIntent);
        //finish();  // TODO(nfaralli): why would we want to quit the app here?
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.ringdroid_list_options_menu, menu);

        final MenuItem searchItem = menu.findItem(R.id.action_search_filter);
        mFilter = (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(searchItem);

        if (mFilter != null) {
            mFilter.setOnQueryTextListener(new android.support.v7.widget.SearchView.OnQueryTextListener() {
                public boolean onQueryTextChange(String newText) {
                    refreshListView();
                    return true;
                }

                public boolean onQueryTextSubmit(String query) {
                    refreshListView();
                    return true;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_filter:
                switch (RingdroidPreferences.getInstance().getFilterToShow(this)) {
                    case RingdroidPreferences.SHOW_ALL:
                        item.getSubMenu().findItem(R.id.action_show_all).setChecked(true);
                        break;
                    case RingdroidPreferences.SHOW_RINGTONE:
                        item.getSubMenu().findItem(R.id.action_show_ringtone).setChecked(true);
                        break;
                    case RingdroidPreferences.SHOW_NOTIFICATION:
                        item.getSubMenu().findItem(R.id.action_show_notification).setChecked(true);
                        break;
                    case RingdroidPreferences.SHOW_ALARM:
                        item.getSubMenu().findItem(R.id.action_show_alarm).setChecked(true);
                        break;
                    case RingdroidPreferences.SHOW_MUSIC:
                        item.getSubMenu().findItem(R.id.action_show_music).setChecked(true);
                        break;
                }
                return true;
            case R.id.action_show_all:
                RingdroidPreferences.getInstance().setFilterToShow(this, RingdroidPreferences.SHOW_ALL);
                refreshListView();
                return true;
            case R.id.action_show_ringtone:
                RingdroidPreferences.getInstance().setFilterToShow(this, RingdroidPreferences.SHOW_RINGTONE);
                refreshListView();
                return true;
            case R.id.action_show_notification:
                RingdroidPreferences.getInstance().setFilterToShow(this, RingdroidPreferences.SHOW_NOTIFICATION);
                refreshListView();
                return true;
            case R.id.action_show_alarm:
                RingdroidPreferences.getInstance().setFilterToShow(this, RingdroidPreferences.SHOW_ALARM);
                refreshListView();
                return true;
            case R.id.action_show_music:
                RingdroidPreferences.getInstance().setFilterToShow(this, RingdroidPreferences.SHOW_MUSIC);
                refreshListView();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        Cursor c = mAdapter.getCursor();
        String title = c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));

        menu.setHeaderTitle(title);
        menu.add(0, CMD_EDIT, 0, R.string.edit);
        menu.add(0, CMD_DELETE, 0, R.string.btn_delete);

        // Add items to the context menu item based on file type
        if (0 != c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE))) {
            menu.add(0, CMD_SET_AS_DEFAULT, 0, R.string.set_as_default_ringtone);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_NOTIFICATION))) {
            menu.add(0, CMD_SET_AS_DEFAULT, 0, R.string.set_as_default_notification);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case CMD_EDIT:
                startRingdroidEditor();
                return true;
            case CMD_DELETE:
                confirmDelete();
                return true;
            case CMD_SET_AS_DEFAULT:
                setAsDefaultRingtoneOrNotification();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    private void setSoundIconFromCursor(ImageView view, Cursor cursor) {
        if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            view.setImageResource(R.drawable.ringdroid_ic_phone_in_talk_black_24dp);
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_ALARM))) {
            view.setImageResource(R.drawable.ringdroid_ic_alarm_black_24dp);
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            view.setImageResource(R.drawable.ringdroid_ic_notifications_black_24dp);
        } else if (0 != cursor.getInt(cursor.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_MUSIC))) {
            view.setImageResource(R.drawable.ringdroid_ic_music_note_black_24dp);
        }
    }

    private void setAsDefaultRingtoneOrNotification() {

        // Check permission and request if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                return;
            }
        }

        Cursor c = mAdapter.getCursor();

        // If the item is a ringtone then set the default ringtone,
        // otherwise it has to be a notification so set the default notification sound
        if (0 != c.getInt(c.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_RINGTONE))) {
            RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_RINGTONE,
                    getUri());
            Toast.makeText(
                    this,
                    R.string.default_ringtone_success_message,
                    Toast.LENGTH_SHORT)
                    .show();
        } else {
            RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_NOTIFICATION,
                    getUri());
            Toast.makeText(
                    this,
                    R.string.default_notification_success_message,
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private int getUriIndex(Cursor c) {
        int uriIndex;
        String[] columnNames = {
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI.toString(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString()
        };

        for (String columnName : Arrays.asList(columnNames)) {
            uriIndex = c.getColumnIndex(columnName);
            if (uriIndex >= 0) {
                return uriIndex;
            }
            // On some phones and/or Android versions, the column name includes the double quotes.
            uriIndex = c.getColumnIndex("\"" + columnName + "\"");
            if (uriIndex >= 0) {
                return uriIndex;
            }
        }
        return -1;
    }

    private Uri getUri() {
        //Get the uri of the item that is in the row
        Cursor c = mAdapter.getCursor();
        int uriIndex = getUriIndex(c);
        if (uriIndex == -1) {
            return null;
        }
        String itemUri = c.getString(uriIndex) + "/" +
                c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        return (Uri.parse(itemUri));
    }

    private void confirmDelete() {
        // See if the selected list item was created by Ringdroid to
        // determine which alert message to show
        Cursor c = mAdapter.getCursor();
        String artist = c.getString(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.ARTIST));
        CharSequence ringdroidArtist =
                getResources().getText(R.string.default_artist_name);

        CharSequence message;
        if (artist.equals(ringdroidArtist)) {
            message = getResources().getText(
                    R.string.confirm_delete_ringdroid);
        } else {
            message = getResources().getText(
                    R.string.confirm_delete_non_ringdroid);
        }

        CharSequence title;
        if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_RINGTONE))) {
            title = getResources().getText(R.string.delete_ringtone);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_ALARM))) {
            title = getResources().getText(R.string.delete_alarm);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_NOTIFICATION))) {
            title = getResources().getText(R.string.delete_notification);
        } else if (0 != c.getInt(c.getColumnIndexOrThrow(
                MediaStore.Audio.Media.IS_MUSIC))) {
            title = getResources().getText(R.string.delete_music);
        } else {
            title = getResources().getText(R.string.delete_audio);
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.btn_delete,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                onDelete();
                            }
                        })
                .setNegativeButton(
                        R.string.btn_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                            }
                        })
                .setCancelable(true)
                .show();
    }

    private void onDelete() {
        Cursor c = mAdapter.getCursor();
        int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        String filename = c.getString(dataIndex);

        int uriIndex = getUriIndex(c);
        if (uriIndex == -1) {
            showFinalAlert(getResources().getText(R.string.unable_to_delete_file));
            return;
        }

        if (!new File(filename).delete()) {
            showFinalAlert(getResources().getText(R.string.unable_to_delete_file));
        }

        String itemUri = c.getString(uriIndex) + "/" +
                c.getString(c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
        getContentResolver().delete(Uri.parse(itemUri), null, null);
    }

    private void showFinalAlert(CharSequence message) {
        new AlertDialog.Builder(this)
                .setTitle(getResources().getText(R.string.error))
                .setMessage(message)
                .setPositiveButton(
                        R.string.btn_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    private void startRingdroidEditor() {
        Cursor c = mAdapter.getCursor();
        int dataIndex = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
        String filename = c.getString(dataIndex);
        try {
            Intent intent = new Intent(Intent.ACTION_EDIT, Uri.parse(filename));
            intent.putExtra("was_get_content_intent", mWasGetContentIntent);
            intent.setClassName(getPackageName(), RingdroidEditActivity.class.getName());
            startActivityForResult(intent, REQUEST_CODE_EDIT);
        } catch (Exception e) {
            Log.e("Ringdroid", "Couldn't start editor");
        }
    }

    private void refreshListView() {
        mInternalCursor = null;
        mExternalCursor = null;
        Bundle args = new Bundle();
        args.putString("filter", mFilter.getQuery().toString());
        getLoaderManager().restartLoader(INTERNAL_CURSOR_ID, args, this);
        getLoaderManager().restartLoader(EXTERNAL_CURSOR_ID, args, this);
    }


    /* Implementation of LoaderCallbacks.onCreateLoader */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Log.d(getClass().getSimpleName(), "onCreateLoader");

        ArrayList<String> selectionArgsList = new ArrayList<>();
        Uri baseUri;
        String[] projection;
        String selection;

        switch (id) {
            case INTERNAL_CURSOR_ID:
                baseUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
                projection = INTERNAL_COLUMNS;
                break;
            case EXTERNAL_CURSOR_ID:
                baseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                projection = EXTERNAL_COLUMNS;
                break;
            default:
                return null;
        }

        selection = "(";
        for (String extension : SoundFile.getSupportedExtensions()) {
            selectionArgsList.add("%." + extension);
            if (selection.length() > 1) {
                selection += " OR ";
            }
            selection += "(_DATA LIKE ?)";
        }
        selection += ")";

        selection = "(" + selection + ") AND (_DATA NOT LIKE ?)";
        selectionArgsList.add("%espeak-data/scratch%");

        // filter if user search
        String filter = args != null ? args.getString("filter") : null;
        if (filter != null && filter.length() > 0) {
            filter = "%" + filter + "%";
            selection =
                    "(" + selection + " AND " +
                            "((TITLE LIKE ?) OR (ARTIST LIKE ?) OR (ALBUM LIKE ?)))";
            selectionArgsList.add(filter);
            selectionArgsList.add(filter);
            selectionArgsList.add(filter);
        }

        // query base on preference
        switch (RingdroidPreferences.getInstance().getFilterToShow(this)) {
            case RingdroidPreferences.SHOW_ALL:
                selection = selection + " AND ((is_ringtone + \" != 0\") " +
                        "OR (is_alarm + \" != 0\") " +
                        "OR (is_music + \" != 0\") " +
                        "OR (is_notification + \" != 0\"))";
                break;
            case RingdroidPreferences.SHOW_RINGTONE:
                selection = selection + " AND ((is_ringtone + \" != 0\"))";
                break;
            case RingdroidPreferences.SHOW_NOTIFICATION:
                selection = selection + " AND ((is_notification + \" != 0\"))";
                break;
            case RingdroidPreferences.SHOW_ALARM:
                selection = selection + " AND ((is_alarm + \" != 0\"))";
                break;
            case RingdroidPreferences.SHOW_MUSIC:
                selection = selection + " AND ((is_music + \" != 0\"))";
                break;
        }
        Log.d(this.getClass().getSimpleName(), selection);

        String[] selectionArgs =
                selectionArgsList.toArray(new String[selectionArgsList.size()]);

        return new CursorLoader(
                this,
                baseUri,
                projection,
                selection,
                selectionArgs,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER
        );
    }

    /* Implementation of LoaderCallbacks.onLoadFinished */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Log.d(getClass().getSimpleName(), "onLoadFinished");

        switch (loader.getId()) {
            case INTERNAL_CURSOR_ID:
                mInternalCursor = data;
                break;
            case EXTERNAL_CURSOR_ID:
                mExternalCursor = data;
                break;
            default:
                return;
        }
        // TODO: should I use a mutex/synchronized block here?
        if (mInternalCursor != null && mExternalCursor != null) {
            Cursor mergeCursor = new MergeCursor(new Cursor[]{mInternalCursor, mExternalCursor});
            mAdapter.swapCursor(mergeCursor);
        }
    }

    /* Implementation of LoaderCallbacks.onLoaderReset */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(getClass().getSimpleName(), "onLoaderReset");

        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed.  We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

}
