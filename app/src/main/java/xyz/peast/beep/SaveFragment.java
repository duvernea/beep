package xyz.peast.beep;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Calendar;

import xyz.peast.beep.adapters.Board;
import xyz.peast.beep.adapters.BoardSpinnerAdapter;
import xyz.peast.beep.data.BeepDbContract;
import xyz.peast.beep.services.BitmapImageService;

/**
 * Created by duvernea on 7/30/16.
 */
public class SaveFragment extends Fragment implements LocationListener {

    private static final String TAG = SaveFragment.class.getSimpleName();
    private Context mContext;
    private Activity mActivity;

    // Permission Request Code
    public static final int PERMISSIONS_REQUEST_READ_EXTERNAL= 10;

    // KEYs for onSaveInstanceState
    private static final String IMAGE_FILE_NAME = "image_file_name";
    private static final String IMAGE_FILE_URI = "image_file_uri";
    private static final String IMAGE_FILE_PATH = "image_file_path";

    // Request Code for Photo Picker Intent
    private static final int SELECT_PHOTO = 1;

    // Service for saving, compressing, resizing images
    private Intent mServiceIntent;

    // Views
    private Spinner mBoardSpinner;
    private ImageView mBeepImage;
    private Button mSaveButton;
    private EditText mBeepNameEditText;
    private Button mReplayButton;
    private AdView mAdView;
    // Spinner for Board selection and creation
    private ArrayList<Board> mSpinnerItems;
    private BoardSpinnerAdapter mBoardSpinnerAdapter;

    // Selected Image in Picker - Uri and Path
    private Uri mImageUri = null;
    private String mImagePath = null;
    // Image Bitmap and Image FileName
    private Bitmap mImageBitmap;
    private String mImageFileName;

    // Location variables
    private LocationManager mLocationManager;
    private Location mMostRecentLocation;

    // Image loading
    Handler mImageHandler;

    // Audio variables
    private boolean mIsPlaying;

    private String mRecordFileName;
    private int mNumberOfBoards;

    // SaveCallback interface - implemented in RecordActivity
    public interface SaveCallback{
        public void onSaveNextButton(String beepName, String audioFile, Uri imageUri,
                                     String boardname, int boardkey);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log.d(TAG, "onCreateView");
        mContext = getActivity();
        mActivity = getActivity();

        View rootView = inflater.inflate(R.layout.fragment_save, container, false);

        // Record File Unique Name is generated in RecordActivity and passed into the Fragment
        Bundle bundle = this.getArguments();
        mRecordFileName = bundle.getString(RecordActivity.RECORD_FILE_UNIQUE_NAME) + ".wav";

        // Get Location manager, so that GPS coordinates can be saved
        mLocationManager = (LocationManager)
                mContext.getSystemService(Context.LOCATION_SERVICE);

        // Initialize views
        mBoardSpinner = (Spinner) rootView.findViewById(R.id.board_name_spinner);
        mBeepImage = (ImageView) rootView.findViewById(R.id.beep_image);
        mSaveButton = (Button) rootView.findViewById(R.id.save_button);
        mBeepNameEditText = (EditText) rootView.findViewById(R.id.beep_name_edittext);
        mReplayButton = (Button) rootView.findViewById(R.id.replay_button);

        // Initilize Ads
        mAdView = (AdView) rootView.findViewById(R.id.adview);
        final AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("839737069995AAD5519D71B8B267924D")
                .build();
        mAdView.loadAd(adRequest);

        // EditText for setting Beep Name - onClick opens Dialog for entering text
        mBeepNameEditText.setClickable(true);
        mBeepNameEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createBeepNameDialog();
            }
        });
        // Spinner for setting Board Name - onItemSelected
        mBoardSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Spinner Item Selected");
                if (position == mNumberOfBoards) {
                    createBoardNameDialog();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // BeepImage selected, launch the photo picker
        mBeepImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                boolean permissionReadExternal = Utility.hasReadExternalPermission(mContext);
                Log.d(TAG, "hasRecordAudioPermission: " + permissionReadExternal);

                if (permissionReadExternal) {
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);

                } else {
                    requestReadExternalPermission();
                }
            }
        });

        // Save Button onClick - run the Callback in RecordActivity
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO name, board, time, audio file, image uri, etc
                insertContent();
                int spinnerSelectedItemPosition  = mBoardSpinner.getSelectedItemPosition();
                Log.d(TAG, "spinner selected item position " + spinnerSelectedItemPosition);
                Board selected = mSpinnerItems.get(spinnerSelectedItemPosition);
                int selectedKey = selected.getKey();
                String boardname = selected.getName();

                ((SaveCallback) getActivity()).onSaveNextButton(mBeepNameEditText.getText().toString(),
                        mRecordFileName,
                        mImageUri,
                        boardname,
                        selectedKey
                );
            }
        });
        // Populate spinner with board data from database
        getAndPopulateBoardData();

        // Audio File Name path
        String recordDir = mContext.getFilesDir().getAbsolutePath();
        final String filePath = recordDir + "/" + mRecordFileName;
        Log.d(TAG, "filePath: " + filePath);

        // Replay audio button
        mReplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsPlaying = true;
                Log.d(TAG, "mIsPlaying play: " + mIsPlaying);
                ((RecordActivity) mActivity).onFileChange(filePath, 0, 0);
                ((RecordActivity) mActivity).onPlayPause(filePath, mIsPlaying, 0);
            }
        });
        // Set the Image Uri, Path, and restore bitmap if previous state saved
        if (savedInstanceState != null) {
            mImageUri = Uri.parse(savedInstanceState.getString(IMAGE_FILE_URI));
            mImagePath = savedInstanceState.getString(IMAGE_FILE_PATH);
        }
        if (mImagePath != null) {
            // Downsample bitmap
            Bitmap bitmap = Utility.subsampleBitmap(mContext, mImagePath, 360, 360);
            // Center crop bitmap
            mImageBitmap = Utility.centerCropBitmap(mContext, bitmap);
            mBeepImage.setImageBitmap(mImageBitmap);
        }
        mImageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "handler handleMessage");
                Bundle reply = msg.getData();
                Bitmap bitmap = reply.getParcelable(Constants.IMAGE_BITMAP_FROM_SERVICE);
                // do whatever with the bundle here
                mBeepImage.setImageBitmap(bitmap);
            }
        };

        return rootView;
    }
    // Callback after image selected in photo picker
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult run");
        switch (requestCode) {
            case SELECT_PHOTO:
                if (resultCode == Activity.RESULT_OK) {

                    mImageUri = data.getData();
                    mImagePath = Utility.getRealPathFromURI(mContext, mImageUri);
                    int imageSize = (int) mContext.getResources().getDimension(R.dimen.image_size_save_activity);

                    Intent intent = new Intent(mContext, BitmapImageService.class);
                    intent.putExtra(Constants.IMAGE_MESSENGER, new Messenger(mImageHandler));
                    intent.putExtra(Utility.ORIGINAL_IMAGE_FILE_URI, mImageUri.toString());

                    intent.putExtra(Constants.IMAGE_MIN_SIZE, imageSize);

                    mContext.startService(intent);
                }
        }
    }
    // Insert beep into database
    void insertContent() {
        String beepName = mBeepNameEditText.getText().toString();
        int spinnerSelectedItemPosition  = mBoardSpinner.getSelectedItemPosition();
        Board selected = mSpinnerItems.get(spinnerSelectedItemPosition);
        int selectedKey = selected.getKey();

        Utility.insertNewBeep(mContext, beepName, mRecordFileName,
                mMostRecentLocation, selectedKey, mImageUri);
    }
    // Save Image items
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(IMAGE_FILE_NAME, mImageFileName);
        outState.putString(IMAGE_FILE_PATH, mImagePath);
        if (mImageUri != null) {
            outState.putString(IMAGE_FILE_URI, mImageUri.toString());
        }
        Log.d(TAG, "onSaveInstanceState");
    }
    private void getAndPopulateBoardData() {
        // Get the data to populate the Board Spinner
        String[] mProjection =
                {
                        BeepDbContract.BoardEntry._ID,
                        BeepDbContract.BoardEntry.COLUMN_NAME
                };
        Cursor cursor = mContext.getContentResolver().query(BeepDbContract.BoardEntry.CONTENT_URI,
                mProjection,
                null,
                null,
                null);

        Log.d(TAG, "Cursor count, #boards returned" + cursor.getCount());

        mNumberOfBoards = cursor.getCount();
        cursor.moveToFirst();
        // Populate the Board Spinner
        mSpinnerItems = new ArrayList<Board>();

        for(int i = 0; i < cursor.getCount(); i++){
            String row = cursor.getString(
                    cursor.getColumnIndex(BeepDbContract.BoardEntry.COLUMN_NAME));
            String key = cursor.getString(
                    cursor.getColumnIndex(BeepDbContract.BoardEntry._ID));
            Board temp = new Board(Integer.parseInt(key), row, "temp", 242);
            mSpinnerItems.add(temp);
            cursor.moveToNext();
        }
        // Add item for creating new cursor
        Board createNew = new Board(-1, "Create New", "N/A", 0);
        mSpinnerItems.add(createNew);

        mBoardSpinnerAdapter = new BoardSpinnerAdapter(mContext,
                R.layout.spinner_row, R.id.spinner_item_textview, mSpinnerItems);

        mBoardSpinner.setAdapter(mBoardSpinnerAdapter);
    }
    private void createBeepNameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        String dialogTitle = getResources().getString(R.string.dialog_new_beep_name);
        builder.setTitle(dialogTitle);
        final EditText input = new EditText(mContext);
        input.setMaxLines(1);
        input.setSingleLine();
        input.setText(mBeepNameEditText.getText());
        input.setSelectAllOnFocus(true);

        FrameLayout container = new FrameLayout(mContext);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Resources resources = mContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float marginDpLeft = 16;
        float marginDpRight = 64;
        float pxLeft = marginDpLeft * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        float pxRight = marginDpRight * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);

        params.leftMargin = (int) pxLeft;
        params.rightMargin = (int) pxRight;
        input.setLayoutParams(params);
        container.addView(input);

        int maxLength = getResources().getInteger(R.integer.max_beep_size);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(container);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newBeepName = input.getText().toString();
                mBeepNameEditText.setText(newBeepName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }
    private void createBoardNameDialog() {

        Log.d(TAG, "create new board selected on spinner");
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

        String dialogTitle = getResources().getString(R.string.dialog_new_beep_name);
        builder.setTitle(dialogTitle);
        final EditText input = new EditText(mContext);
        input.setMaxLines(1);
        input.setSingleLine();

        FrameLayout container = new FrameLayout(mContext);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Resources resources = mContext.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float marginDpLeft = 16;
        float marginDpRight = 64;
        float pxLeft = marginDpLeft * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        float pxRight = marginDpRight * ((float) metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);

        params.leftMargin = (int) pxLeft;
        params.rightMargin = (int) pxRight;
        input.setLayoutParams(params);
        container.addView(input);

        int maxLength = getResources().getInteger(R.integer.max_board_size);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(container);

        String positiveButtonText = getResources().getString(R.string.dialog_positive_button);
        String negativeButtonText = getResources().getString(R.string.dialog_negative_button);

        builder.setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newBoardName = input.getText().toString();
                ContentValues contentValues = new ContentValues();
                contentValues.put(BeepDbContract.BoardEntry.COLUMN_NAME, newBoardName);
                long currentTime = Calendar.getInstance().getTimeInMillis();
                contentValues.put(BeepDbContract.BoardEntry.COLUMN_DATE_CREATED, currentTime);
                // TODO - need default image resources to use
                String tempImageUri = "";
                contentValues.put(BeepDbContract.BoardEntry.COLUMN_IMAGE, tempImageUri);
                Uri uri = mContext.getContentResolver().insert(BeepDbContract.BoardEntry.CONTENT_URI, contentValues);
                int insertedRow = (int) ContentUris.parseId(uri);
                Log.d(TAG, "inserted Row into Board db: " + insertedRow);
                //mSpinnerItems.add(mSpinnerItems.size()-1, newBoard);
                Board newBoardz = new Board(insertedRow, newBoardName, tempImageUri, currentTime);
                mSpinnerItems.add(mSpinnerItems.size() - 1, newBoardz);
                mBoardSpinnerAdapter.notifyDataSetChanged();
                mNumberOfBoards += 1;
                Utility.updateWidgets(mActivity);

            }
        });
        builder.setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        Dialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();
    }

    // Get location - currently just uses last known location to save battery and avoid network/GPS issues
    private void getLocation() {
        // Get GPS coordinates
        try {
            Location locationGPS = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location locationNetwork = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (locationGPS == null || locationNetwork == null) {
                if (locationGPS == null) {
                    if (locationNetwork == null) {
                        mMostRecentLocation = null;
                    }
                    else {
                        mMostRecentLocation = locationNetwork;
                    }
                }
                else {
                    mMostRecentLocation = locationGPS;
                }
            }
            else if (locationGPS.getTime() > locationNetwork.getTime()) {
                mMostRecentLocation = locationGPS;
            }
            else {
                mMostRecentLocation = locationNetwork;
            }
            if (mMostRecentLocation != null) {
                Log.d(TAG, "mostRecentLocation Lat: " + mMostRecentLocation.getLatitude());
                Log.d(TAG, "mostRecentLocation Long: " + mMostRecentLocation.getLongitude());
            }
            else {
                Log.d(TAG, "mostRecentLocation is NULL");

            }
        }
        catch (SecurityException e) {
            // Handle if GPS not enabled
            mMostRecentLocation = null;
        }
    }
    // Remove updates - don't care
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.v("Location Changed", location.getLatitude() + " and " + location.getLongitude());
            try {
                mLocationManager.removeUpdates(this);
                // update the contentvalues and insert
            }
            catch (SecurityException e) {
                // Handle if GPS not enabled
            }
        }
    }

    private void requestReadExternalPermission(){

        // The dangerous READ External permission is NOT already granted.
        // Check if the user has been asked about this permission already and denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Log.d(TAG, "permission has previously been denied.  Explain why need");
                // TODO Show UI to explain to the user why we need to read external
            }
            // Fire off an async request to actually get the permission
            // This will show the standard permission request dialog UI

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_READ_EXTERNAL);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(TAG, "onRequestPermissionResult");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_READ_EXTERNAL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO add call to image picker
                    Log.d(TAG, "permission granted, create intent");
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, SELECT_PHOTO);

                } else {
                    // Permission Denied
                    Toast.makeText(mContext,
                            getResources().getString(R.string.need_external_permission), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    // Required functions
    public void onProviderDisabled(String arg0) {}
    public void onProviderEnabled(String arg0) {}
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {}
}