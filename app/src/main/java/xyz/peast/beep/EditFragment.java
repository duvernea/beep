package xyz.peast.beep;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by duverneay on 9/22/16.
 */
public class EditFragment extends Fragment {

    private static final String TAG = EditFragment.class.getSimpleName();

    Context mContext;
    Activity mActivity;

    Button mNoEffectButton;
    Button mChipmunkButton;
    Button mSlomoButton;
    Button mEchoButton;

    String mRecordFileName;
    String mRecordFilePath;
    Boolean mIsPlaying = false;

    int mPlaybackRate=1;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_edit, container, false);


        mContext = getActivity();
        mActivity = getActivity();

        Bundle bundle = this.getArguments();
        mRecordFileName = bundle.getString(RecordActivity.RECORD_FILE_UNIQUE_NAME) + ".wav";

        String recordDir = mContext.getFilesDir().getAbsolutePath();
        mRecordFilePath = recordDir + "/" + mRecordFileName;
        Log.i(TAG, "recordfilepath: " + mRecordFilePath);

        mNoEffectButton = (Button) rootView.findViewById(R.id.noeffect_button);
        mChipmunkButton = (Button) rootView.findViewById(R.id.chipmunk_button);
        mSlomoButton = (Button) rootView.findViewById(R.id.slomo_button);
        mEchoButton = (Button) rootView.findViewById(R.id.echo_button);

        mNoEffectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "mIsPlaying start: " + mIsPlaying);
                    mIsPlaying = true;
                    ((RecordActivity) mActivity).onFileChange(mRecordFilePath, 0, 0);
                    ((RecordActivity) mActivity).onPlayPause(mRecordFilePath, mIsPlaying, 0);
            }
        });
        mChipmunkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((RecordActivity) mActivity).onFileChange(mRecordFilePath, 0, 0);
                mPlaybackRate -=1;
                // TODO determine max and min shift
                ((RecordActivity) mActivity).setPitchShift(mPlaybackRate);

            }
        });
        return rootView;
    }
}