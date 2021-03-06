package com.ksyun.media.rtc.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ksyun.media.player.IMediaPlayer;
import com.ksyun.media.rtc.demo.filter.DemoAudioFilter;
import com.ksyun.media.rtc.demo.filter.DemoFilter;
import com.ksyun.media.rtc.demo.filter.DemoFilter2;
import com.ksyun.media.rtc.demo.filter.DemoFilter3;
import com.ksyun.media.rtc.demo.filter.DemoFilter4;
import com.ksyun.media.rtc.kit.KSYRtcStreamer;
import com.ksyun.media.rtc.kit.RTCClient;
import com.ksyun.media.rtc.kit.RTCConstants;
import com.ksyun.media.streamer.capture.CameraCapture;
import com.ksyun.media.streamer.capture.camera.CameraTouchHelper;
import com.ksyun.media.streamer.filter.audio.AudioFilterBase;
import com.ksyun.media.streamer.filter.audio.AudioReverbFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyProFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgBeautyToneCurveFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilter;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterBase;
import com.ksyun.media.streamer.filter.imgtex.ImgTexFilterMgt;
import com.ksyun.media.streamer.kit.KSYStreamer;
import com.ksyun.media.streamer.kit.StreamerConstants;
import com.ksyun.media.streamer.logstats.StatsLogReport;
import com.ksyun.media.streamer.util.gles.GLRender;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class RTCActivity extends Activity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "RTCActivity";

    private GLSurfaceView mCameraPreviewView;
    private CameraTouchHelper mCameraTouchHelper;
    //private TextureView mCameraPreviewView;
    private CameraHintView mCameraHintView;
    private Chronometer mChronometer;
    private View mDeleteView;
    private View mSwitchCameraView;
    private View mFlashView;
    private TextView mShootingText;
    private TextView mRecordingText;
    private TextView mCaptureSceenShot;
    private CheckBox mWaterMarkCheckBox;
    private CheckBox mBeautyCheckBox;
    private CheckBox mReverbCheckBox;
    private CheckBox mAudioPreviewCheckBox;
    private CheckBox mBgmCheckBox;
    private CheckBox mMuteCheckBox;
    private CheckBox mAudioOnlyCheckBox;
    private CheckBox mFrontMirrorCheckBox;
    private TextView mUrlTextView;
    private TextView mDebugInfoTextView;
    //RTC params
    private EditText mRTCFpsEditText;
    private EditText mRTCBitrateEditText;
    private EditText mRTCLocalURIEditText;
    private Button mRTCRegistButton;
    private TextView mRTCRegistStatusTextView;
    private EditText mRTCRemoteURIEditText;
    private Button mRTCRemoteCallButton;
    private TextView mRTCRemoteCallStatusTextView;
    private Button mRTCAnswerRemoteCallButton;
    private Button mRTCRejectRemoteCallButton;
    private View mRTCRemoteReciveLayout;
    private TextView mRTCIncommingStatusTextView;

    private View mBeautyChooseView;
    private AppCompatSpinner mBeautySpinner;
    private LinearLayout mBeautyGrindLayout;
    private TextView mGrindText;
    private AppCompatSeekBar mGrindSeekBar;
    private LinearLayout mBeautyWhitenLayout;
    private TextView mWhitenText;
    private AppCompatSeekBar mWhitenSeekBar;
    private LinearLayout mBeautyRuddyLayout;
    private TextView mRuddyText;
    private AppCompatSeekBar mRuddySeekBar;

    private int mLastRotation;
    private OrientationEventListener mOrientationEventListener;

    private ButtonObserver mObserverButton;
    private CheckBoxObserver mCheckBoxObserver;

    private KSYRtcStreamer mStreamer;
    private Handler mMainHandler;
    private Timer mTimer;

    private boolean mAutoStart;
    private boolean mIsLandscape;
    private boolean mPrintDebugInfo = false;
    private boolean mRecording = false;
    private boolean mIsFileRecording = false;
    private boolean mIsFlashOpened = false;
    private String mUrl;
    private String mDebugInfo = "";
    private String mBgmPath = "/sdcard/test.mp3";
    private String mLogoPath = "file:///sdcard/test.png";
    private String mRecordUrl = "/sdcard/test.mp4";

    private boolean mHWEncoderUnsupported;
    private boolean mSWEncoderUnsupported;

    //rtc
    //从server拿到健全串
    private final String RTC_AUTH_SERVER = "http://rtc.vcloud.ks-live.com:6002/rtcauth";
    private final String RTC_AUTH_URI = "https://rtc.vcloud.ks-live.com:6001";
    private final String RTC_UINIQUE_NAME = "apptest";

    private boolean mIsRegisted;
    private boolean mIsConnected;

    private AuthHttpTask mRTCAuthTask;
    private AuthHttpTask.KSYOnHttpResponse mRTCAuthResponse;

    private final static int PERMISSION_REQUEST_CAMERA_AUDIOREC = 1;
    private static final String START_STRING = "开始直播";
    private static final String STOP_STRING = "停止直播";
    private static final String START_RECORDING = "开始录制";
    private static final String STOP_RECORDING = "停止录制";

    public final static String URL = "url";
    public final static String FRAME_RATE = "framerate";
    public final static String VIDEO_BITRATE = "video_bitrate";
    public final static String AUDIO_BITRATE = "audio_bitrate";
    public final static String VIDEO_RESOLUTION = "video_resolution";
    public final static String ORIENTATION = "orientation";
    public final static String ENCODE_TYPE = "encode_type";
    public final static String ENCODE_METHOD = "encode_method";
    public final static String ENCODE_SCENE = "encode_scene";
    public final static String ENCODE_PROFILE = "encode_profile";
    public final static String START_AUTO = "start_auto";
    public static final String SHOW_DEBUGINFO = "show_debuginfo";

    public static void startActivity(Context context, int fromType,
                                     String rtmpUrl, int frameRate,
                                     int videoBitrate, int audioBitrate,
                                     int videoResolution, int orientation,
                                     int encodeType, int encodeMethod,
                                     int encodeScene, int encodeProfile,
                                     boolean startAuto, boolean showDebugInfo) {
        Intent intent = new Intent(context, RTCActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("type", fromType);
        intent.putExtra(URL, rtmpUrl);
        intent.putExtra(FRAME_RATE, frameRate);
        intent.putExtra(VIDEO_BITRATE, videoBitrate);
        intent.putExtra(AUDIO_BITRATE, audioBitrate);
        intent.putExtra(VIDEO_RESOLUTION, videoResolution);
        intent.putExtra(ORIENTATION, orientation);
        intent.putExtra(ENCODE_TYPE, encodeType);
        intent.putExtra(ENCODE_METHOD, encodeMethod);
        intent.putExtra(ENCODE_SCENE, encodeScene);
        intent.putExtra(ENCODE_PROFILE, encodeProfile);
        intent.putExtra(START_AUTO, startAuto);
        intent.putExtra(SHOW_DEBUGINFO, showDebugInfo);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.camera_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraHintView = (CameraHintView) findViewById(R.id.camera_hint);
        mCameraPreviewView = (GLSurfaceView) findViewById(R.id.camera_preview);
        //mCameraPreviewView = (TextureView) findViewById(R.id.camera_preview);
        mUrlTextView = (TextView) findViewById(R.id.url);
        mChronometer = (Chronometer) findViewById(R.id.chronometer);
        mDebugInfoTextView = (TextView) findViewById(R.id.debuginfo);

        mObserverButton = new ButtonObserver();
        mShootingText = (TextView) findViewById(R.id.click_to_shoot);
        mShootingText.setOnClickListener(mObserverButton);
        mRecordingText = (TextView) findViewById(R.id.click_to_record);
        mRecordingText.setOnClickListener(mObserverButton);
        mCaptureSceenShot = (TextView) findViewById(R.id.click_to_capture_screenshot);
        mCaptureSceenShot.setOnClickListener(mObserverButton);
        mDeleteView = findViewById(R.id.backoff);
        mDeleteView.setOnClickListener(mObserverButton);
        mSwitchCameraView = findViewById(R.id.switch_cam);
        mSwitchCameraView.setOnClickListener(mObserverButton);
        mFlashView = findViewById(R.id.flash);
        mFlashView.setOnClickListener(mObserverButton);

        mCheckBoxObserver = new CheckBoxObserver();
        mBeautyCheckBox = (CheckBox) findViewById(R.id.click_to_switch_beauty);
        mBeautyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mReverbCheckBox = (CheckBox) findViewById(R.id.click_to_select_audio_filter);
        mReverbCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mBgmCheckBox = (CheckBox) findViewById(R.id.bgm);
        mBgmCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioPreviewCheckBox = (CheckBox) findViewById(R.id.ear_mirror);
        mAudioPreviewCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mMuteCheckBox = (CheckBox) findViewById(R.id.mute);
        mMuteCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mWaterMarkCheckBox = (CheckBox) findViewById(R.id.watermark);
        mWaterMarkCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mFrontMirrorCheckBox = (CheckBox) findViewById(R.id.front_camera_mirror);
        mFrontMirrorCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);
        mAudioOnlyCheckBox = (CheckBox) findViewById(R.id.audio_only);
        mAudioOnlyCheckBox.setOnCheckedChangeListener(mCheckBoxObserver);

        mBeautyChooseView = findViewById(R.id.beauty_choose);
        mBeautySpinner = (AppCompatSpinner) findViewById(R.id.beauty_spin);
        mBeautyGrindLayout = (LinearLayout) findViewById(R.id.beauty_grind);
        mGrindText = (TextView) findViewById(R.id.grind_text);
        mGrindSeekBar = (AppCompatSeekBar) findViewById(R.id.grind_seek_bar);
        mBeautyWhitenLayout = (LinearLayout) findViewById(R.id.beauty_whiten);
        mWhitenText = (TextView) findViewById(R.id.whiten_text);
        mWhitenSeekBar = (AppCompatSeekBar) findViewById(R.id.whiten_seek_bar);
        mBeautyRuddyLayout = (LinearLayout) findViewById(R.id.beauty_ruddy);
        mRuddyText = (TextView) findViewById(R.id.ruddy_text);
        mRuddySeekBar = (AppCompatSeekBar) findViewById(R.id.ruddy_seek_bar);

        //RTC View init
        mRTCFpsEditText = (EditText) findViewById(R.id.rtc_fps);
        mRTCBitrateEditText = (EditText) findViewById(R.id.rtc_bitrate);
        mRTCLocalURIEditText = (EditText) findViewById(R.id.rtc_local_uri);
        mRTCRegistButton = (Button) findViewById(R.id.rtc_register);
        mRTCRegistButton.setOnClickListener(mObserverButton);
        mRTCRegistStatusTextView = (TextView) findViewById(R.id.rtc_regist_status);
        mRTCRemoteURIEditText = (EditText) findViewById(R.id.rtc_remote_uri);
        mRTCRemoteCallButton = (Button) findViewById(R.id.rtc_remote_call);
        mRTCRemoteCallButton.setOnClickListener(mObserverButton);
        mRTCRemoteCallStatusTextView = (TextView) findViewById(R.id.rtc_remote_call_status);
        mRTCAnswerRemoteCallButton = (Button) findViewById(R.id.rtc_answer_call);
        mRTCAnswerRemoteCallButton.setOnClickListener(mObserverButton);
        mRTCRejectRemoteCallButton = (Button) findViewById(R.id.rtc_reject_call);
        mRTCRejectRemoteCallButton.setOnClickListener(mObserverButton);
        mRTCRemoteReciveLayout = findViewById(R.id.rtc_remote_receive);
        mRTCIncommingStatusTextView = (TextView) findViewById(R.id.rtc_incomming_status);

        mMainHandler = new Handler();
        mStreamer = new KSYRtcStreamer(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            String url = bundle.getString(URL);
            if (!TextUtils.isEmpty(url)) {
                mUrl = url;
                mUrlTextView.setText(mUrl);
                mStreamer.setUrl(url);
            }

            int frameRate = bundle.getInt(FRAME_RATE, 0);
            if (frameRate > 0) {
                mStreamer.setPreviewFps(frameRate);
                mStreamer.setTargetFps(frameRate);
            }

            int videoBitrate = bundle.getInt(VIDEO_BITRATE, 0);
            if (videoBitrate > 0) {
                mStreamer.setVideoKBitrate(videoBitrate * 3 / 4, videoBitrate, videoBitrate / 4);
            }

            int audioBitrate = bundle.getInt(AUDIO_BITRATE, 0);
            if (audioBitrate > 0) {
                mStreamer.setAudioKBitrate(audioBitrate);
            }

            int videoResolution = bundle.getInt(VIDEO_RESOLUTION, 0);
            mStreamer.setPreviewResolution(videoResolution);
            mStreamer.setTargetResolution(videoResolution);

            int encode_type = bundle.getInt(ENCODE_TYPE);
            mStreamer.setVideoCodecId(encode_type);

            int encode_method = bundle.getInt(ENCODE_METHOD);
            mStreamer.setEncodeMethod(encode_method);

            int encodeScene = bundle.getInt(ENCODE_SCENE);
            mStreamer.setVideoEncodeScene(encodeScene);

            int encodeProfile = bundle.getInt(ENCODE_PROFILE);
            mStreamer.setVideoEncodeProfile(encodeProfile);

            int orientation = bundle.getInt(ORIENTATION, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            if (orientation == ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
                int rotation = getDisplayRotation();
                mIsLandscape = (rotation % 180) != 0;
                mStreamer.setRotateDegrees(rotation);
                mLastRotation = rotation;
                mOrientationEventListener = new OrientationEventListener(this,
                        SensorManager.SENSOR_DELAY_NORMAL) {
                    @Override
                    public void onOrientationChanged(int orientation) {
                        int rotation = getDisplayRotation();
                        if (rotation != mLastRotation) {
                            Log.d(TAG, "Rotation changed " + mLastRotation + "->" + rotation);
                            mIsLandscape = (rotation % 180) != 0;
                            mStreamer.setRotateDegrees(rotation);
                            hideWaterMark();
                            if (mWaterMarkCheckBox.isChecked()) {
                                showWaterMark();
                            }
                            mLastRotation = rotation;
                        }
                    }
                };
            } else if (orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                mIsLandscape = true;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                mStreamer.setRotateDegrees(90);
            } else {
                mIsLandscape = false;
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                mStreamer.setRotateDegrees(0);
            }

            mAutoStart = bundle.getBoolean(START_AUTO, false);
            mPrintDebugInfo = bundle.getBoolean(SHOW_DEBUGINFO, false);
        }
        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.setEnableRepeatLastFrame(false);  // disable repeat last frame in background
        mStreamer.setEnableAutoRestart(true, 3000);
        mStreamer.setCameraFacing(CameraCapture.FACING_FRONT);
        mStreamer.setFrontCameraMirror(mFrontMirrorCheckBox.isChecked());
        mStreamer.setMuteAudio(mMuteCheckBox.isChecked());
        mStreamer.setEnableAudioPreview(mAudioPreviewCheckBox.isChecked());
        if (mStreamer.isAudioPreviewing() != mAudioPreviewCheckBox.isChecked()) {
            mAudioPreviewCheckBox.setChecked(mStreamer.isAudioPreviewing());
        }
        mStreamer.setOnInfoListener(mOnInfoListener);
        mStreamer.setOnErrorListener(mOnErrorListener);
        mStreamer.setOnLogEventListener(mOnLogEventListener);

        // set beauty filter
        initBeautyUI();
        if (mStreamer.getVideoEncodeMethod() == StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
            mBeautyCheckBox.setChecked(true);
        }
        mStreamer.getImgTexFilterMgt().setOnErrorListener(new ImgTexFilterBase.OnErrorListener() {
            @Override
            public void onError(ImgTexFilterBase filter, int errno) {
                Toast.makeText(RTCActivity.this, "当前机型不支持该滤镜",
                        Toast.LENGTH_SHORT).show();
                mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                        ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            }
        });

        // add RGBA buffer filter to ImgTexFilterMgt, this would cause performance drop,
        // only valid after Android 4.4
        //RGBABufDemoFilter demoFilter = new RGBABufDemoFilter(mStreamer.getGLRender());
        //mStreamer.getImgTexFilterMgt().setExtraFilter(demoFilter);

        // touch focus and zoom support
        mCameraTouchHelper = new CameraTouchHelper();
        mCameraTouchHelper.setCameraCapture(mStreamer.getCameraCapture());
        mCameraPreviewView.setOnTouchListener(mCameraTouchHelper);
        // set CameraHintView to show focus rect and zoom ratio
        mCameraTouchHelper.setCameraHintView(mCameraHintView);

        startCameraPreviewWithPermCheck();
        if (mWaterMarkCheckBox.isChecked()) {
            showWaterMark();
        }
        if (mAutoStart) {
            startStream();
        }

        //for rtc sub screen
        mCameraTouchHelper.addTouchListener(mRTCSubScreenTouchListener);

        //set rtc info
        mStreamer.getRtcClient().setRTCErrorListener(mRTCErrorListener);
        mStreamer.getRtcClient().setRTCEventListener(mRTCEventListener);
        if (!mIsRegisted) {
            mRTCRemoteCallButton.setEnabled(false);
        }
    }

    private void initBeautyUI() {
        String[] items = new String[]{"DISABLE", "BEAUTY_SOFT", "SKIN_WHITEN", "BEAUTY_ILLUSION",
                "BEAUTY_DENOISE", "BEAUTY_SMOOTH", "BEAUTY_PRO", "DEMO_FILTER", "GROUP_FILTER",
                "ToneCurve", "复古", "胶片"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mBeautySpinner.setAdapter(adapter);
        mBeautySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                TextView textView = ((TextView) parent.getChildAt(0));
                if (textView != null) {
                    textView.setTextColor(getResources().getColor(R.color.font_color_35));
                }
                if (position == 0) {
                    mStreamer.getImgTexFilterMgt().setFilter((ImgFilterBase) null);
                } else if (position <= 5) {
                    mStreamer.getImgTexFilterMgt().setFilter(
                            mStreamer.getGLRender(), position + 15);
                } else if (position == 6) {
                    mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(),
                            ImgTexFilterMgt.KSY_FILTER_BEAUTY_PRO);
                } else if (position == 7) {
                    mStreamer.getImgTexFilterMgt().setFilter(
                            new DemoFilter(mStreamer.getGLRender()));
                } else if (position == 8) {
                    List<ImgTexFilter> groupFilter = new LinkedList<>();
                    groupFilter.add(new DemoFilter2(mStreamer.getGLRender()));
                    groupFilter.add(new DemoFilter3(mStreamer.getGLRender()));
                    groupFilter.add(new DemoFilter4(mStreamer.getGLRender()));
                    mStreamer.getImgTexFilterMgt().setFilter(groupFilter);
                } else if (position == 9) {
                    ImgBeautyToneCurveFilter acvFilter = new ImgBeautyToneCurveFilter(mStreamer.getGLRender());
                    acvFilter.setFromCurveFileInputStream(
                            RTCActivity.this.getResources().openRawResource(R.raw.tone_cuver_sample));

                    mStreamer.getImgTexFilterMgt().setFilter(acvFilter);
                } else if (position == 10) {
                    ImgBeautyToneCurveFilter acvFilter = new ImgBeautyToneCurveFilter(mStreamer.getGLRender());
                    acvFilter.setFromCurveFileInputStream(
                            RTCActivity.this.getResources().openRawResource(R.raw.fugu));

                    mStreamer.getImgTexFilterMgt().setFilter(acvFilter);
                } else if (position == 11) {
                    ImgBeautyToneCurveFilter acvFilter = new ImgBeautyToneCurveFilter(mStreamer.getGLRender());
                    acvFilter.setFromCurveFileInputStream(
                            RTCActivity.this.getResources().openRawResource(R.raw.jiaopian));

                    mStreamer.getImgTexFilterMgt().setFilter(acvFilter);
                }
                List<ImgFilterBase> filters = mStreamer.getImgTexFilterMgt().getFilter();
                if (filters != null && !filters.isEmpty()) {
                    final ImgFilterBase filter = filters.get(0);
                    mBeautyGrindLayout.setVisibility(filter.isGrindRatioSupported() ?
                            View.VISIBLE : View.GONE);
                    mBeautyWhitenLayout.setVisibility(filter.isWhitenRatioSupported() ?
                            View.VISIBLE : View.GONE);
                    mBeautyRuddyLayout.setVisibility(filter.isRuddyRatioSupported() ?
                            View.VISIBLE : View.GONE);
                    SeekBar.OnSeekBarChangeListener seekBarChangeListener =
                            new SeekBar.OnSeekBarChangeListener() {
                                @Override
                                public void onProgressChanged(SeekBar seekBar, int progress,
                                                              boolean fromUser) {
                                    if (!fromUser) {
                                        return;
                                    }
                                    float val = progress / 100.f;
                                    if (seekBar == mGrindSeekBar) {
                                        filter.setGrindRatio(val);
                                    } else if (seekBar == mWhitenSeekBar) {
                                        filter.setWhitenRatio(val);
                                    } else if (seekBar == mRuddySeekBar) {
                                        if (filter instanceof ImgBeautyProFilter) {
                                            val = progress / 50.f - 1.0f;
                                        }
                                        filter.setRuddyRatio(val);
                                    }
                                }

                                @Override
                                public void onStartTrackingTouch(SeekBar seekBar) {
                                }

                                @Override
                                public void onStopTrackingTouch(SeekBar seekBar) {
                                }
                            };
                    mGrindSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                    mWhitenSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                    mRuddySeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
                    mGrindSeekBar.setProgress((int) (filter.getGrindRatio() * 100));
                    mWhitenSeekBar.setProgress((int) (filter.getWhitenRatio() * 100));
                    int ruddyVal = (int) (filter.getRuddyRatio() * 100);
                    if (filter instanceof ImgBeautyProFilter) {
                        ruddyVal = (int) (filter.getRuddyRatio() * 50 + 50);
                    }
                    mRuddySeekBar.setProgress(ruddyVal);
                } else {
                    mBeautyGrindLayout.setVisibility(View.GONE);
                    mBeautyWhitenLayout.setVisibility(View.GONE);
                    mBeautyRuddyLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
        mBeautySpinner.setPopupBackgroundResource(R.color.transparent1);
        mBeautySpinner.setSelection(4);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mOrientationEventListener != null &&
                mOrientationEventListener.canDetectOrientation()) {
            mOrientationEventListener.enable();
        }
        mStreamer.setDisplayPreview(mCameraPreviewView);
        mStreamer.onResume();
        mCameraHintView.hideAll();

        // camera may be occupied by other app in background
        startCameraPreviewWithPermCheck();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOrientationEventListener != null) {
            mOrientationEventListener.disable();
        }
        mStreamer.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mCameraTouchHelper.removeAllTouchListener();
        if (mIsConnected) {
            mStreamer.getRtcClient().stopCall();
        }

        if (mIsRegisted) {
            mStreamer.getRtcClient().unRegisterRTC();
        }

        if (mMainHandler != null) {
            mMainHandler.removeCallbacksAndMessages(null);
            mMainHandler = null;
        }
        if (mTimer != null) {
            mTimer.cancel();
        }
        mStreamer.setOnLogEventListener(null);
        mStreamer.release();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                onBackoffClick();
                return true;
            default:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    private int getDisplayRotation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (rotation) {
            case Surface.ROTATION_0:
                return 0;
            case Surface.ROTATION_90:
                return 90;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_270:
                return 270;
        }
        return 0;
    }

    private void startStream() {
        mStreamer.startStream();
        mShootingText.setText(STOP_STRING);
        mShootingText.postInvalidate();
        mRecording = true;
    }

    //start recording to a local file
    private void startRecord() {
        mStreamer.startRecord(mRecordUrl);
        mRecordingText.setText(STOP_RECORDING);
        mRecordingText.postInvalidate();
        mIsFileRecording = true;
    }

    private void stopRecord() {
        mStreamer.stopRecord();
        mRecordingText.setText(START_RECORDING);
        mRecordingText.postInvalidate();
        mIsFileRecording = false;
        stopChronometer();
    }

    private void stopChronometer() {
        if (mRecording || mIsFileRecording) {
            return;
        }
        mChronometer.setBase(SystemClock.elapsedRealtime());
        mChronometer.stop();
    }

    private void stopStream() {
        mStreamer.stopStream();
        mShootingText.setText(START_STRING);
        mShootingText.postInvalidate();
        mRecording = false;
        stopChronometer();
    }

    private void beginInfoUploadTimer() {
        if (mPrintDebugInfo && mTimer == null) {
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    updateDebugInfo();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDebugInfoTextView.setText(mDebugInfo);
                        }
                    });
                }
            }, 100, 1000);
        }
    }

    private void updateDebugInfo() {
        if (mStreamer == null) return;
        mDebugInfo = String.format(Locale.getDefault(),
                "RtmpHostIP()=%s DroppedFrameCount()=%d \n " +
                        "ConnectTime()=%d DnsParseTime()=%d \n " +
                        "UploadedKB()=%d EncodedFrames()=%d \n" +
                        "CurrentKBitrate=%d Version()=%s RTCLibVersion=%s",
                mStreamer.getRtmpHostIP(), mStreamer.getDroppedFrameCount(),
                mStreamer.getConnectTime(), mStreamer.getDnsParseTime(),
                mStreamer.getUploadedKBytes(), mStreamer.getEncodedFrames(),
                mStreamer.getCurrentUploadKBitrate(), mStreamer.getVersion(),
                mStreamer.getKSYRTClibVersion());
    }

    //show watermark in specific location
    private void showWaterMark() {
        if (!mIsLandscape) {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.08f, 0.04f, 0.20f, 0, 0.8f);
            mStreamer.showWaterMarkTime(0.03f, 0.01f, 0.35f, Color.WHITE, 1.0f);
        } else {
            mStreamer.showWaterMarkLogo(mLogoPath, 0.05f, 0.09f, 0, 0.20f, 0.8f);
            mStreamer.showWaterMarkTime(0.01f, 0.03f, 0.22f, Color.WHITE, 1.0f);
        }
    }

    private void hideWaterMark() {
        mStreamer.hideWaterMarkLogo();
        mStreamer.hideWaterMarkTime();
    }

    // Example to handle camera related operation
    private void setCameraAntiBanding50Hz() {
        Camera.Parameters parameters = mStreamer.getCameraCapture().getCameraParameters();
        if (parameters != null) {
            parameters.setAntibanding(Camera.Parameters.ANTIBANDING_50HZ);
            mStreamer.getCameraCapture().setCameraParameters(parameters);
        }
    }

    private KSYStreamer.OnInfoListener mOnInfoListener = new KSYStreamer.OnInfoListener() {
        @Override
        public void onInfo(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_CAMERA_INIT_DONE:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_INIT_DONE");
                    setCameraAntiBanding50Hz();
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_STREAM_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_STREAM_SUCCESS");
                    mShootingText.setText(STOP_STRING);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    beginInfoUploadTimer();
                    break;
                case StreamerConstants.KSY_STREAMER_OPEN_FILE_SUCCESS:
                    Log.d(TAG, "KSY_STREAMER_OPEN_FILE_SUCCESS");
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.start();
                    break;
                case StreamerConstants.KSY_STREAMER_FRAME_SEND_SLOW:
                    Log.d(TAG, "KSY_STREAMER_FRAME_SEND_SLOW " + msg1 + "ms");
                    Toast.makeText(RTCActivity.this, "Network not good!",
                            Toast.LENGTH_SHORT).show();
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_RAISE:
                    Log.d(TAG, "BW raise to " + msg1 / 1000 + "kbps");
                    break;
                case StreamerConstants.KSY_STREAMER_EST_BW_DROP:
                    Log.d(TAG, "BW drop to " + msg1 / 1000 + "kpbs");
                    break;
                default:
                    Log.d(TAG, "OnInfo: " + what + " msg1: " + msg1 + " msg2: " + msg2);
                    break;
            }
        }
    };

    private void handleEncodeError() {
        int encodeMethod = mStreamer.getVideoEncodeMethod();
        if (encodeMethod == StreamerConstants.ENCODE_METHOD_HARDWARE) {
            mHWEncoderUnsupported = true;
            if (mSWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_SOFTWARE);
                Log.e(TAG, "Got HW encoder error, switch to SOFTWARE mode");
            }
        } else if (encodeMethod == StreamerConstants.ENCODE_METHOD_SOFTWARE) {
            mSWEncoderUnsupported = true;
            if (mHWEncoderUnsupported) {
                mStreamer.setEncodeMethod(
                        StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT);
                onBeautyChecked(mBeautyCheckBox.isChecked());
                Log.e(TAG, "Got SW encoder error, switch to SOFTWARE_COMPAT mode");
            } else {
                mStreamer.setEncodeMethod(StreamerConstants.ENCODE_METHOD_HARDWARE);
                Log.e(TAG, "Got SW encoder error, switch to HARDWARE mode");
            }
        }
    }

    private KSYStreamer.OnErrorListener mOnErrorListener = new KSYStreamer.OnErrorListener() {
        @Override
        public void onError(int what, int msg1, int msg2) {
            switch (what) {
                case StreamerConstants.KSY_STREAMER_ERROR_DNS_PARSE_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_DNS_PARSE_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_PUBLISH_FAILED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_PUBLISH_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_CONNECT_BREAKED:
                    Log.d(TAG, "KSY_STREAMER_ERROR_CONNECT_BREAKED");
                    break;
                case StreamerConstants.KSY_STREAMER_ERROR_AV_ASYNC:
                    Log.d(TAG, "KSY_STREAMER_ERROR_AV_ASYNC " + msg1 + "ms");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNSUPPORTED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_ENCODER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_UNKNOWN");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_START_FAILED");
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_SERVER_DIED");
                    break;
                //Camera was disconnected due to use by higher priority user.
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED:
                    Log.d(TAG, "KSY_STREAMER_CAMERA_ERROR_EVICTED");
                    break;
                default:
                    Log.d(TAG, "what=" + what + " msg1=" + msg1 + " msg2=" + msg2);
                    break;
            }
            switch (what) {
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_AUDIO_RECORDER_ERROR_UNKNOWN:
                    break;
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_START_FAILED:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_EVICTED:
                case StreamerConstants.KSY_STREAMER_CAMERA_ERROR_SERVER_DIED:
                    mStreamer.stopCameraPreview();
                    break;
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_CLOSE_FAILED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_ERROR_UNKNOWN:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_OPEN_FAILED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_FORMAT_NOT_SUPPORTED:
                case StreamerConstants.KSY_STREAMER_FILE_PUBLISHER_WRITE_FAILED:
                    stopRecord();
                    break;
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNSUPPORTED:
                case StreamerConstants.KSY_STREAMER_VIDEO_ENCODER_ERROR_UNKNOWN: {
                    handleEncodeError();
                    stopStream();
                    mMainHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            startStream();
                        }
                    }, 3000);
                }
                break;
                default:
                    if (mStreamer.getEnableAutoRestart()) {
                        mShootingText.setText(START_STRING);
                        mShootingText.postInvalidate();
                        mRecording = false;
                        stopChronometer();
                    } else {
                        stopStream();
                        mMainHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startStream();
                            }
                        }, 3000);
                    }
                    break;
            }
        }
    };

    private StatsLogReport.OnLogEventListener mOnLogEventListener =
            new StatsLogReport.OnLogEventListener() {
                @Override
                public void onLogEvent(StringBuilder singleLogContent) {
                    Log.i(TAG, "***onLogEvent : " + singleLogContent.toString());
                }
            };

    private void onSwitchCamera() {
        mStreamer.switchCamera();
        mCameraHintView.hideAll();
    }

    private void onFlashClick() {
        if (mIsFlashOpened) {
            mStreamer.toggleTorch(false);
            mIsFlashOpened = false;
        } else {
            mStreamer.toggleTorch(true);
            mIsFlashOpened = true;
        }
    }

    private void onBackoffClick() {
        new AlertDialog.Builder(RTCActivity.this).setCancelable(true)
                .setTitle("结束直播?")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        mChronometer.stop();
                        mRecording = false;
                        RTCActivity.this.finish();
                    }
                }).show();
    }

    private void onShootClick() {
        if (mRecording) {
            stopStream();
        } else {
            startStream();
        }
    }

    private void onRecordClick() {
        if (mIsFileRecording) {
            stopRecord();
        } else {
            startRecord();
        }
    }

    private boolean[] mChooseFilter = {false, false};

    private void showChooseAudioFilter() {
        AlertDialog alertDialog;
        alertDialog = new AlertDialog.Builder(this)
                .setTitle("请选择音频滤镜")
                .setMultiChoiceItems(
                        new String[]{"REVERB", "DEMO",}, mChooseFilter,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                if (isChecked) {
                                    mChooseFilter[which] = true;
                                }
                            }
                        }
                ).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mChooseFilter[0] && mChooseFilter[1]) {
                            List<AudioFilterBase> filters = new LinkedList<>();
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            filters.add(reverbFilter);
                            filters.add(demofilter);
                            mStreamer.getAudioFilterMgt().setFilter(filters);
                        } else if (mChooseFilter[0]) {
                            AudioReverbFilter reverbFilter = new AudioReverbFilter();
                            mStreamer.getAudioFilterMgt().setFilter(reverbFilter);
                        } else if (mChooseFilter[1]) {
                            DemoAudioFilter demofilter = new DemoAudioFilter();
                            mStreamer.getAudioFilterMgt().setFilter(demofilter);
                        } else {
                            mStreamer.getAudioFilterMgt().setFilter((AudioFilterBase) null);
                        }
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void onBeautyChecked(boolean isChecked) {
        if (mStreamer.getVideoEncodeMethod() == StreamerConstants.ENCODE_METHOD_SOFTWARE_COMPAT) {
            mStreamer.getImgTexFilterMgt().setFilter(mStreamer.getGLRender(), isChecked ?
                    ImgTexFilterMgt.KSY_FILTER_BEAUTY_DENOISE :
                    ImgTexFilterMgt.KSY_FILTER_BEAUTY_DISABLE);
            mStreamer.setEnableImgBufBeauty(isChecked);
        } else {
            mBeautyChooseView.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private void onAudioFilterChecked(boolean isChecked) {
        showChooseAudioFilter();
    }

    private void onBgmChecked(boolean isChecked) {
        if (isChecked) {
            // use KSYMediaPlayer
            mStreamer.getAudioPlayerCapture().getMediaPlayer()
                    .setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(IMediaPlayer iMediaPlayer) {
                            Log.d(TAG, "End of the currently playing music");
                        }
                    });
            mStreamer.getAudioPlayerCapture().getMediaPlayer()
                    .setOnErrorListener(new IMediaPlayer.OnErrorListener() {
                        @Override
                        public boolean onError(IMediaPlayer iMediaPlayer, int what, int extra) {
                            Log.e(TAG, "OnErrorListener, Error:" + what + ", extra:" + extra);
                            return false;
                        }
                    });
            mStreamer.setEnableAudioMix(true);
            mStreamer.startBgm(mBgmPath, true);
            mStreamer.getAudioPlayerCapture().getMediaPlayer().setVolume(0.4f, 0.4f);
        } else {
            mStreamer.stopBgm();
        }
    }

    private void onAudioPreviewChecked(boolean isChecked) {
        mStreamer.setEnableAudioPreview(isChecked);
    }

    private void onMuteChecked(boolean isChecked) {
        mStreamer.setMuteAudio(isChecked);
    }

    private void onWaterMarkChecked(boolean isChecked) {
        if (isChecked) {
            showWaterMark();
        } else {
            hideWaterMark();
        }
    }

    private void onFrontMirrorChecked(boolean isChecked) {
        mStreamer.setFrontCameraMirror(isChecked);
    }

    private void onAudioOnlyChecked(boolean isChecked) {
        mStreamer.setAudioOnly(isChecked);
    }

    private void onCaptureScreenShotClick() {
        mStreamer.requestScreenShot(new GLRender.ScreenShotListener() {
            @Override
            public void onBitmapAvailable(Bitmap bitmap) {
                BufferedOutputStream bos = null;
                try {
                    Date date = new Date();
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
                    final String filename = "/sdcard/screenshot" + dateFormat.format(date) + ".jpg";

                    bos = new BufferedOutputStream(new FileOutputStream(filename));
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(RTCActivity.this, "保存截图到 " + filename,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (bos != null) try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private class ButtonObserver implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            switch (view.getId()) {
                case R.id.switch_cam:
                    onSwitchCamera();
                    break;
                case R.id.backoff:
                    onBackoffClick();
                    break;
                case R.id.flash:
                    onFlashClick();
                    break;
                case R.id.click_to_shoot:
                    onShootClick();
                    break;
                case R.id.click_to_record:
                    onRecordClick();
                    break;
                case R.id.click_to_capture_screenshot:
                    onCaptureScreenShotClick();
                    break;
                case R.id.rtc_register:
                case R.id.rtc_remote_call:
                case R.id.rtc_answer_call:
                case R.id.rtc_reject_call:
                    onRtcClick(view.getId());
                    break;
                default:
                    break;
            }
        }
    }

    private class CheckBoxObserver implements CompoundButton.OnCheckedChangeListener {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            switch (buttonView.getId()) {
                case R.id.click_to_switch_beauty:
                    onBeautyChecked(isChecked);
                    break;
                case R.id.click_to_select_audio_filter:
                    onAudioFilterChecked(isChecked);
                    break;
                case R.id.bgm:
                    onBgmChecked(isChecked);
                    break;
                case R.id.ear_mirror:
                    onAudioPreviewChecked(isChecked);
                    break;
                case R.id.mute:
                    onMuteChecked(isChecked);
                    break;
                case R.id.watermark:
                    onWaterMarkChecked(isChecked);
                    break;
                case R.id.front_camera_mirror:
                    onFrontMirrorChecked(isChecked);
                    break;
                case R.id.audio_only:
                    onAudioOnlyChecked(isChecked);
                    break;
                default:
                    break;
            }
        }
    }

    private void startCameraPreviewWithPermCheck() {
        int cameraPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int audioPerm = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (cameraPerm != PackageManager.PERMISSION_GRANTED ||
                audioPerm != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                Log.e(TAG, "No CAMERA or AudioRecord permission, please check");
                Toast.makeText(this, "No CAMERA or AudioRecord permission, please check",
                        Toast.LENGTH_LONG).show();
            } else {
                String[] permissions = {Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
                ActivityCompat.requestPermissions(this, permissions,
                        PERMISSION_REQUEST_CAMERA_AUDIOREC);
            }
        } else {
            mStreamer.startCameraPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CAMERA_AUDIOREC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mStreamer.startCameraPreview();
                } else {
                    Log.e(TAG, "No CAMERA or AudioRecord permission");
                    Toast.makeText(this, "No CAMERA or AudioRecord permission",
                            Toast.LENGTH_LONG).show();
                }
                break;
            }
        }
    }

    //rtc
    private void onRtcClick(int clickId) {
        switch (clickId) {
            case R.id.rtc_register:
                onRTCRegisterClick();
                break;
            case R.id.rtc_remote_call:
                onRTCRemoteCallClick();
                break;
            case R.id.rtc_answer_call:
                onRTCAnswerRemoteCall();
                break;
            case R.id.rtc_reject_call:
                onRTCRejectRemoteCall();
                break;
            default:
                break;
        }
    }

    private void onRTCRegisterClick() {
        if (mRTCAuthResponse == null) {
            mRTCAuthResponse = new AuthHttpTask.KSYOnHttpResponse() {
                @Override
                public void onHttpResponse(int responseCode, final String response) {
                    if (mMainHandler != null) {
                        if (responseCode == 200) {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    doRegister(response);
                                }
                            });
                        } else {
                            mMainHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    doAuthFailed();
                                }
                            });
                        }
                    }
                }
            };
        }
        if (!mIsRegisted) {
            //注册
            if (TextUtils.isEmpty(mRTCLocalURIEditText.getText())) {
                Toast.makeText(this, "you must set the local uri before register", Toast
                        .LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(mRTCFpsEditText.getText())) {
                Toast.makeText(this, "you must set the local fps before register", Toast
                        .LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(mRTCBitrateEditText.getText())) {
                Toast.makeText(this, "you must set the local bitrate before register", Toast
                        .LENGTH_SHORT).show();
                return;
            }

            mRTCAuthTask = new AuthHttpTask(mRTCAuthResponse);
            mRTCAuthTask.execute(RTC_AUTH_SERVER + "?uid=" + mRTCLocalURIEditText.getText().toString());
            //do not registed when registing
            mRTCRegistButton.setEnabled(false);
            mRTCRegistStatusTextView.setText("register Waiting...");
        } else {
            doUnRegister();
        }

    }

    private void onRTCRemoteCallClick() {
        if (!mIsConnected && mIsRegisted) {
            if (TextUtils.isEmpty(mRTCRemoteURIEditText.getText())) {
                Toast.makeText(this, "you must set the remote uri before call", Toast
                        .LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.equals(mRTCRemoteURIEditText.getText(), mRTCLocalURIEditText.getText())) {
                Toast.makeText(this, "Don't call yourself, pls.", Toast
                        .LENGTH_SHORT).show();
                return;
            }
            mStreamer.getRtcClient().startCall(mRTCRemoteURIEditText.getText().toString());
            //can not call when calling
            mRTCRemoteCallButton.setEnabled(false);
            mRTCRemoteCallStatusTextView.setText("start calling Waiting...");

        } else {
            mStreamer.getRtcClient().stopCall();
            mRTCRemoteCallStatusTextView.setText("stop calling Waiting...");
            //can not call when stop calling
            mRTCRemoteCallButton.setEnabled(false);
        }
    }

    private void onRTCAnswerRemoteCall() {
        mStreamer.getRtcClient().answerCall();
        mRTCRemoteReciveLayout.setVisibility(View.GONE);
        //can not call
        mRTCRemoteCallButton.setEnabled(false);
        mRTCRemoteCallStatusTextView.setText("answerer calling Waiting...");
    }

    private void onRTCRejectRemoteCall() {
        mStreamer.getRtcClient().rejectCall();
        mRTCRemoteReciveLayout.setVisibility(View.GONE);
        mIsConnected = false;
    }

    private void doRegister(String authString) {
        //must set before register
        if (!mIsLandscape) {
            mStreamer.setRTCSubScreenRect(0.65f, 0.f, 0.35f, 0.3f,
                    RTCConstants.SCALING_MODE_CENTER_CROP);
        } else {
            mStreamer.setRTCSubScreenRect(0.65f, 0.f, 0.3f, 0.35f,
                    RTCConstants.SCALING_MODE_CENTER_CROP);
        }
        mStreamer.getRtcClient().setRTCAuthInfo(RTC_AUTH_URI, authString,
                mRTCLocalURIEditText.getText().toString());
        mStreamer.getRtcClient().setRTCUniqueName(RTC_UINIQUE_NAME);
        //has default value
        mStreamer.setRTCMainScreen(RTCConstants.RTC_MAIN_SCREEN_CAMERA);
        mStreamer.getRtcClient().openChattingRoom(false);
        mStreamer.getRtcClient().setRTCResolutionScale(0.5f);
        mStreamer.getRtcClient().setRTCFps(Integer.parseInt(mRTCFpsEditText.getText().toString()));
        mStreamer.getRtcClient().setRTCVideoBitrate(256 * 1024);
        mStreamer.getRtcClient().registerRTC();
    }

    private void doUnRegister() {
        mStreamer.getRtcClient().unRegisterRTC();
        //can not register when unregistering
        mRTCRegistButton.setEnabled(false);
        mRTCRegistButton.setText("unregister waiting...");
        //disable call
        mRTCRemoteCallButton.setEnabled(false);
        mRTCRemoteReciveLayout.setVisibility(View.GONE);
    }

    private void doAuthFailed() {
        //register failed
        mRTCRegistStatusTextView.setText("Auth failed, pls. try again");
        //can register again
        mIsRegisted = false;
        mRTCRegistButton.setEnabled(true);
    }

    private void doRegisteredFailed(int failed) {
        //register failed
        mRTCRegistStatusTextView.setText("register failed error_type:" + String.valueOf(failed));
        //can register again
        mIsRegisted = false;
        if (mIsConnected) {
            mIsConnected = false;
        }
        mRTCRegistButton.setEnabled(true);
    }

    private void doRegisteredSuccess() {
        mIsRegisted = true;
        //can unregisted
        mRTCRegistButton.setEnabled(true);
        mRTCRegistStatusTextView.setText("registered");
        mRTCRegistButton.setText("unregister");
        //enable call
        mRTCRemoteCallButton.setEnabled(true);
    }

    private void doUnRegisteredResult() {
        mIsRegisted = false;
        if (mIsConnected) {
            mIsConnected = false;
        }
        //can register again
        mRTCRegistButton.setEnabled(true);
        mRTCRegistButton.setText("register");
        mRTCRegistStatusTextView.setText("unregistered");
        //can not call
        mRTCRemoteCallButton.setEnabled(false);
        mRTCRemoteCallButton.setText("start call");
        mRTCRemoteCallStatusTextView.setText("disconnected");
    }

    private void doStartCallSuccess() {
        mIsConnected = true;
        //can stop call
        mRTCRemoteCallButton.setEnabled(true);
        mRTCRemoteCallButton.setText("stop call");
        mRTCRemoteCallStatusTextView.setText("connected");
    }

    private void doStopCallResult() {
        mIsConnected = false;
        //can start call again
        mRTCRemoteCallButton.setEnabled(true);
        mRTCRemoteCallButton.setText("start call");
        mRTCRemoteCallStatusTextView.setText("disconnected");
    }

    private void doStartCallFailed(int status) {
        mIsConnected = false;
        //if remote receive visible need hide
        mRTCRemoteReciveLayout.setVisibility(View.GONE);
        //can start again
        mRTCRemoteCallButton.setEnabled(true);
        mRTCRemoteCallButton.setText("start call");
        mRTCRemoteCallStatusTextView.setText("disconnected");
        Toast.makeText(this, "call failed: " + status, Toast
                .LENGTH_SHORT).show();

    }

    private void doRTCCallBreak() {
        mIsConnected = false;
        //can start call
        mRTCRemoteCallButton.setEnabled(true);
        mRTCRemoteCallButton.setText("start call");
        mRTCRemoteCallStatusTextView.setText("disconnected");
        Toast.makeText(this, "call break", Toast
                .LENGTH_SHORT).show();
    }

    private void doReceiveRemoteCall(final String remoteUri) {
        //当前版本支持1对1的call
        if (mIsConnected) {
            Toast.makeText(RTCActivity.this, "[shit]I am in a call, so auto hangup the " +
                    "incomming new " +
                    "call", Toast
                    .LENGTH_SHORT).show();
            mStreamer.getRtcClient().rejectCall();
        } else {
            //can not start call
            mRTCRemoteCallButton.setEnabled(false);

            mRTCRemoteReciveLayout.setVisibility(View.VISIBLE);
            mRTCRemoteReciveLayout.postInvalidate();
            mRTCIncommingStatusTextView.setText("Someone is calling you: " + remoteUri);
        }
    }

    private boolean checkRTCStats() {
        if (mIsConnected) {
            Toast.makeText(this, "must stop rtc call before close", Toast
                    .LENGTH_LONG).show();
            return false;
        }
        if (mIsRegisted) {
            Toast.makeText(this, "must unregister rtc before close", Toast
                    .LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    private RTCClient.RTCEventChangedListener mRTCEventListener = new RTCClient.RTCEventChangedListener() {
        @Override
        public void onEventChanged(int event, Object arg1) {
            switch (event) {
                case RTCClient.RTC_EVENT_REGISTED:
                    doRegisteredSuccess();
                    break;
                case RTCClient.RTC_EVENT_STARTED:
                    doStartCallSuccess();
                    break;
                case RTCClient.RTC_EVENT_CALL_COMMING:
                    doReceiveRemoteCall(String.valueOf(arg1));
                    break;
                case RTCClient.RTC_EVENT_STOPPED:
                    Log.d(TAG, "stop result:" + arg1);
                    doStopCallResult();
                    break;
                case RTCClient.RTC_EVENT_UNREGISTED:
                    Log.d(TAG, "unregister result:" + arg1);
                    doUnRegisteredResult();
                    break;
                default:
                    break;
            }

        }
    };

    private RTCClient.RTCErrorListener mRTCErrorListener = new RTCClient.RTCErrorListener() {
        @Override
        public void onError(int errorType, int arg1) {
            switch (errorType) {
                case RTCClient.RTC_ERROR_AUTH_FAILED:
                    doAuthFailed();
                    break;
                case RTCClient.RTC_ERROR_REGISTED_FAILED:
                    doRegisteredFailed(arg1);
                    break;
                case RTCClient.RTC_ERROR_SERVER_ERROR:
                case RTCClient.RTC_ERROR_CONNECT_FAIL:
                    doRTCCallBreak();
                    break;
                case RTCClient.RTC_ERROR_STARTED_FAILED:
                    doStartCallFailed(arg1);
                    break;
                default:
                    break;
            }
        }
    };

    /***********************************
     * for rtc sub move&switch
     ********************************/
    private float mSubTouchStartX;
    private float mSubTouchStartY;
    private float mLastRawX;
    private float mLastRawY;
    private float mLastX;
    private float mLastY;
    private float mSubMaxX = 0;   //小窗可移动的最大X轴距离
    private float mSubMaxY = 0;  //小窗可以移动的最大Y轴距离
    private boolean mIsSubMoved = false;  //小窗是否移动过了，如果移动过了，ACTION_UP时不触发大小窗内容切换
    private int SUB_TOUCH_MOVE_MARGIN = 30;  //触发移动的最小距离

    private CameraTouchHelper.OnTouchListener mRTCSubScreenTouchListener = new CameraTouchHelper.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent event) {
            //获取相对屏幕的坐标，即以屏幕左上角为原点
            mLastRawX = event.getRawX();
            mLastRawY = event.getRawY();
            // 预览区域的大小
            int width = view.getWidth();
            int height = view.getHeight();
            //小窗的位置信息
            RectF subRect = mStreamer.getRTCSubScreenRect();
            int left = (int) (subRect.left * width);
            int right = (int) (subRect.right * width);
            int top = (int) (subRect.top * height);
            int bottom = (int) (subRect.bottom * height);
            int subWidth = right - left;
            int subHeight = bottom - top;


            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    //只有在小屏区域才触发位置改变
                    if (isSubScreenArea(event.getX(), event.getY(), left, right, top, bottom)) {
                        //获取相对sub区域的坐标，即以sub左上角为原点
                        mSubTouchStartX = event.getX() - left;
                        mSubTouchStartY = event.getY() - top;
                        mLastX = event.getX();
                        mLastY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    int moveX = (int) Math.abs(event.getX() - mLastX);
                    int moveY = (int) Math.abs(event.getY() - mLastY);
                    if (mSubTouchStartX > 0f && mSubTouchStartY > 0f && (
                            (moveX > SUB_TOUCH_MOVE_MARGIN) ||
                                    (moveY > SUB_TOUCH_MOVE_MARGIN))) {
                        //触发移动
                        mIsSubMoved = true;
                        updateSubPosition(width, height, subWidth, subHeight);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    //未移动并且在小窗区域，则触发大小窗切换
                    if (!mIsSubMoved && isSubScreenArea(event.getX(), event.getY(), left, right,
                            top, bottom)) {
                        mStreamer.switchRTCMainScreen();
                    }

                    mIsSubMoved = false;
                    mSubTouchStartX = 0f;
                    mSubTouchStartY = 0f;
                    mLastX = 0f;
                    mLastY = 0f;
                    break;
            }

            return true;
        }
    };

    /**
     * 是否在小窗区域移动
     *
     * @param x      当前点击的相对小窗左上角的x坐标
     * @param y      当前点击的相对小窗左上角的y坐标
     * @param left   小窗左上角距离预览区域左上角的x轴距离
     * @param right  小窗右上角距离预览区域左上角的x轴距离
     * @param top    小窗左上角距离预览区域左上角的y轴距离
     * @param bottom 小窗右上角距离预览区域左上角的y轴距离
     * @return
     */
    private boolean isSubScreenArea(float x, float y, int left, int right, int top, int bottom) {
        if (!mIsConnected) {
            return false;
        }
        if (x > left && x < right &&
                y > top && y < bottom) {
            return true;
        }

        return false;
    }

    /**
     * 触发移动小窗
     *
     * @param screenWidth 预览区域width
     * @param sceenHeight 预览区域height
     * @param subWidth    小窗区域width
     * @param subHeight   小窗区域height
     */
    private void updateSubPosition(int screenWidth, int sceenHeight, int subWidth, int subHeight) {
        mSubMaxX = screenWidth - subWidth;
        mSubMaxY = sceenHeight - subHeight;

        //更新浮动窗口位置参数
        float newX = (mLastRawX - mSubTouchStartX);
        float newY = (mLastRawY - mSubTouchStartY);

        //不能移出预览区域最左边和最上边
        if (newX < 0) {
            newX = 0;
        }

        if (newY < 0) {
            newY = 0;
        }

        //不能移出预览区域最右边和最下边
        if (newX > mSubMaxX) {
            newX = mSubMaxX;
        }

        if (newY > mSubMaxY) {
            newY = mSubMaxY;
        }
        //小窗的width和height不发生变化
        RectF subRect = mStreamer.getRTCSubScreenRect();
        float width = subRect.width();
        float height = subRect.height();

        float left = newX / screenWidth;
        float top = newY / sceenHeight;

        mStreamer.setRTCSubScreenRect(left, top, width, height, RTCConstants.SCALING_MODE_CENTER_CROP);
    }
}
