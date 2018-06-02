package com.sweetmilkcake.ijkplayerdemo.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.sweetmilkcake.ijkplayerdemo.R;
import com.sweetmilkcake.ijkplayerdemo.widget.media.IjkVideoView;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class PlayActivity extends AppCompatActivity implements GestureDetectorController.IGestureListener {

    private static final String TAG = PlayActivity.class.getSimpleName();

    private static final long CONTROL_PANEL_UPDATE_INTERNAL = 1000;
    private static final long CONTROL_PANEL_UPDATE_INITIAL_INTERVAL = 100;
    private final Handler mControlPanelHandler = new Handler();

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> mScheduleFuture;

    private IjkVideoView mVideoView;
    private RelativeLayout mLoadingLayout;
    private TextView mLoadingText;
    private String mUrl;

    private FrameLayout mTopLayout;
    private LinearLayout mBottomLayout;
    private ImageView mBackButton;
    private TextView mVideoNameView;
    private TextView mSysTimeView;
    private CheckBox mPlayPauseButton;
    private TextView mPlaybackTimeView;
    private SeekBar mSeekBar;
    private static final int SEEK_BAR_MAX = 1000;
    private Formatter mFormatter;
    private StringBuilder mFormatterBuilder;

    private boolean mIsPanelShowing = false;
    private boolean mIsDragging = false;
    private int mBatteryLevel;
    private ImageView mBatteryView;
    private boolean mIsMove = false;
    private long mScrollProgress;
    private boolean mIsHorizontalScroll;
    private boolean mIsVerticalScroll;
    private GestureDetectorController mGestureDetectorController;
    private TextView mDragHorizontalView;
    private TextView mDragVerticalView;

    private int mCurrentLight;
    private int mMaxLight = 255;
    private int mCurrentVolume;
    private int mMaxVolume = 10;
    private AudioManager mAudioManager;

    private static final int AUTO_HIDE_TIME = 5000;
    private static final int AFTER_DRAGGLE_HIDE_TIME = 3000;

    private static final int DIRECTION_UP = 0;
    private static final int DIRECTION_DOWN = 1;
    private static final int DIRECTION_LEFT = 2;
    private static final int DIRECTION_RIGHT = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        mUrl = getIntent().getStringExtra("url");

        // Init Ijkplayer
        mVideoView = (IjkVideoView) findViewById(R.id.video_view);
//        IjkMediaPlayer.loadLibrariesOnce(null);
//        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        mLoadingLayout = (RelativeLayout) findViewById(R.id.rl_loading_layout);
        mLoadingText = (TextView) findViewById(R.id.tv_loading_info);
        mLoadingText.setText(R.string.loading);


        mVideoView.setVideoURI(Uri.parse(mUrl));
        mVideoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer iMediaPlayer) {
                mVideoView.start();
                scheduleControlPanelUpdate();
            }
        });
        mVideoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer iMediaPlayer, int i, int i1) {
                Toast.makeText(PlayActivity.this, "Video Play Error", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        mVideoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
                switch (i) {
                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        mLoadingLayout.setVisibility(View.VISIBLE);
                        break;
                    case IjkMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        mLoadingLayout.setVisibility(View.GONE);
                        break;
                }
                return false;
            }
        });

        initView();
        initGesture();
        initAudio();
        initLight();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBatteryReceiver != null) {
            unregisterReceiver(mBatteryReceiver);
            mBatteryReceiver = null;
        }
        if (mAudioManager != null) {
            mAudioManager.abandonAudioFocus(null);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopControlPanelUpdate();
        mExecutorService.shutdown();
    }

    private BroadcastReceiver mBatteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mBatteryLevel = intent.getIntExtra("level", 0);
            Log.d(TAG, ">> mBatteryReceiver onReceive mBatteryLevel=" + mBatteryLevel);
        }
    };

    private final Runnable mUpdateControlPanelTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            updateCurrentBattery();
            updateCurrentSysTime();
        }
    };

    private void scheduleControlPanelUpdate() {
        stopControlPanelUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(
                    new Runnable() {
                        @Override
                        public void run() {
                            mControlPanelHandler.post(mUpdateControlPanelTask);
                        }
                    }, CONTROL_PANEL_UPDATE_INITIAL_INTERVAL,
                    CONTROL_PANEL_UPDATE_INTERNAL, TimeUnit.MILLISECONDS);
        }
    }

    private void stopControlPanelUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    private void updateProgress() {
        int currentPosition = mVideoView.getCurrentPosition();
        int duration = mVideoView.getDuration();
        if (mSeekBar != null) {
            if (duration > 0) {
                long pos = (long) currentPosition * SEEK_BAR_MAX / duration;
                mSeekBar.setProgress((int) pos);
            }
            int perent = mVideoView.getBufferPercentage();
            mSeekBar.setSecondaryProgress(perent);
            mPlaybackTimeView.setText(timeToString(currentPosition) + " / " + timeToString(duration));
        }
    }

    private void updateCurrentBattery() {
        Log.d(TAG, ">> setCurrentBattery level " + mBatteryLevel);
        if (0 < mBatteryLevel && mBatteryLevel <= 10) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_10);
        } else if (10 < mBatteryLevel && mBatteryLevel <= 20) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_20);
        } else if (20 < mBatteryLevel && mBatteryLevel <= 50) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_50);
        } else if (50 < mBatteryLevel && mBatteryLevel <= 80) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_80);
        } else if (80 < mBatteryLevel && mBatteryLevel <= 100) {
            mBatteryView.setBackgroundResource(R.drawable.ic_battery_100);
        }
    }

    private void updateCurrentSysTime() {
        mSysTimeView.setText(getCurrentSysTime());
    }

    private void initView() {
        mTopLayout = (FrameLayout) findViewById(R.id.fl_player_top_layout);
        mBottomLayout = (LinearLayout) findViewById(R.id.ll_player_bottom_layout);
        mBackButton = (ImageView) findViewById(R.id.iv_player_back);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mVideoNameView = (TextView) findViewById(R.id.tv_player_video_name);
        mVideoNameView.setText("Just Test");
        mSysTimeView = (TextView) findViewById(R.id.tv_sys_time);
        mSysTimeView.setText(getCurrentSysTime());
        mBatteryView = (ImageView) findViewById(R.id.iv_battery);
        mPlayPauseButton = (CheckBox) findViewById(R.id.cb_play_pause);
        mPlayPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlayPause();
            }
        });
        mPlaybackTimeView = (TextView) findViewById(R.id.tv_playback_time);
        mSeekBar = (SeekBar) findViewById(R.id.sb_player_seekbar);
        mSeekBar.setMax(SEEK_BAR_MAX);
        mSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        mFormatterBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatterBuilder, Locale.getDefault());
    }

    private void initGesture() {
        mDragHorizontalView = (TextView) findViewById(R.id.tv_horiontal_gesture);
        mDragVerticalView = (TextView) findViewById(R.id.tv_vertical_gesture);
        mGestureDetectorController = new GestureDetectorController(this, this);
    }

    private void initAudio() {
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) * 10;
        mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) * 10;
    }

    private void initLight() {
        mCurrentLight = getBrightness();
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }
            long duration = mVideoView.getDuration();
            long newPosition = (duration * progress) / SEEK_BAR_MAX;
            mPlaybackTimeView.setText(timeToString((int) newPosition) + " / " + timeToString((int) duration));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            stopControlPanelUpdate();
            mIsDragging = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            scheduleControlPanelUpdate();
            mIsDragging = false;
            int progress = seekBar.getProgress();
            long duration = mVideoView.getDuration();
            long newPosition = (duration * progress) / SEEK_BAR_MAX;
            mVideoView.seekTo((int) newPosition);
            mControlPanelHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideTopBottomLayout();
                }
            }, AFTER_DRAGGLE_HIDE_TIME);
        }
    };

    private void toggleTopAndBottomLayout() {
        if (mIsPanelShowing) {
            hideTopBottomLayout();
        } else {
            showTopBottomLayout();
            mControlPanelHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideTopBottomLayout();
                }
            }, AUTO_HIDE_TIME);
        }
    }

    private void showTopBottomLayout() {
        mIsPanelShowing = true;
        mTopLayout.setVisibility(View.VISIBLE);
        mBottomLayout.setVisibility(View.VISIBLE);
    }

    private void hideTopBottomLayout() {
        if (mIsDragging == true) {
            return;
        }
        mIsPanelShowing = false;
        mTopLayout.setVisibility(View.GONE);
        mBottomLayout.setVisibility(View.GONE);
    }

    private void togglePlayPause() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            updatePlayPauseStatus(false);
        } else {
            mVideoView.start();
            updatePlayPauseStatus(true);
        }
    }

    private void updatePlayPauseStatus(boolean isPlaying) {
        mPlayPauseButton.invalidate();
        mPlayPauseButton.setChecked(isPlaying);
        mPlayPauseButton.refreshDrawableState();
    }

    @Override
    public void onScrollStart(GestureDetectorController.ScrollType type) {
        mIsMove = true;
        switch (type) {
            case HORIZONTAL:
                mDragHorizontalView.setVisibility(View.VISIBLE);
                mScrollProgress = -1;
                mIsHorizontalScroll = true;
                break;
            case VERTICAL_LEFT:
                updateVerticalDrawable(mDragVerticalView, R.drawable.ic_brightness);
                updateVerticalText(mCurrentLight, mMaxLight);
                mDragVerticalView.setVisibility(View.VISIBLE);
                mIsVerticalScroll = true;
                break;
            case VERTICAL_RIGHT:
                if (mCurrentVolume > 0) {
                    updateVerticalDrawable(mDragVerticalView, R.drawable.ic_volume_normal);
                } else {
                    updateVerticalDrawable(mDragVerticalView, R.drawable.ic_volume_no);
                }
                updateVerticalText(mCurrentVolume, mMaxVolume);
                mDragVerticalView.setVisibility(View.VISIBLE);
                mIsVerticalScroll = true;
                break;
        }
    }

    @Override
    public void onScrollHorizontal(float x1, float x2) {
        int width = getResources().getDisplayMetrics().widthPixels;
        int MAX_SEEK_STEP = 300000;
        int offset = (int) (x2 / width * MAX_SEEK_STEP) + mVideoView.getCurrentPosition();
        long progress = Math.max(0, Math.min(mVideoView.getDuration(), offset));
        mScrollProgress = progress;
        if (x1 <= x2) {
            updateHorizontalText(progress, DIRECTION_RIGHT);
        } else {
            updateHorizontalText(progress, DIRECTION_LEFT);
        }
    }

    @Override
    public void onScrollVerticalLeft(float y1, float y2) {
        int height = getResources().getDisplayMetrics().heightPixels;
        int offset = (int) (mMaxLight * y1) / height;
        if (Math.abs(offset) > 0) {
            mCurrentLight += offset;
            mCurrentLight = Math.max(0, Math.min(mMaxLight, mCurrentLight));
            setBrightness(mCurrentLight);
            updateVerticalText(mCurrentLight, mMaxLight);
        }
    }

    @Override
    public void onScrollVerticalRight(float y1, float y2) {
        int height = getResources().getDisplayMetrics().heightPixels;
        int offset = (int) (mMaxVolume * y1) / height;
        if (Math.abs(offset) > 0) {
            mCurrentVolume += offset;
            mCurrentVolume = Math.max(0, Math.min(mMaxVolume, mCurrentVolume));
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume / 10, 0);
            updateVerticalText(mCurrentVolume, mMaxVolume);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (mIsMove == false) {
                toggleTopAndBottomLayout();
            } else {
                mIsMove = false;
            }

            if (mIsHorizontalScroll) {
                mIsHorizontalScroll = false;
                mVideoView.seekTo((int) mScrollProgress);
                mDragHorizontalView.setVisibility(View.GONE);
            }
            if (mIsVerticalScroll) {
                mDragVerticalView.setVisibility(View.GONE);
                mIsVerticalScroll = false;
            }
        }
        return mGestureDetectorController.onTouchEvent(event);
    }

    private void updateVerticalDrawable(TextView textView, int drawableId) {
        textView.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(drawableId, null), null, null, null);
    }

    private void updateVerticalText(int current, int total) {
        NumberFormat formater = NumberFormat.getPercentInstance();
        formater.setMaximumFractionDigits(0);
        String percent = formater.format((double) (current) / (double) total);
        mDragVerticalView.setText(percent);
    }

    private void updateHorizontalText(long current, int direction) {
        String currentStr = timeToString((int) current);
        String durationStr = " / " + timeToString((int) mVideoView.getDuration());
        String allStr = currentStr + durationStr;
        SpannableString spannableString = new SpannableString(allStr);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.text_current_progress, null)), 0, currentStr.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.text_progress, null)), currentStr.length() + 1, allStr.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        mDragHorizontalView.setText(spannableString);
        if (direction == DIRECTION_LEFT) {
            Drawable backward = getResources().getDrawable(R.drawable.ic_progress_backward, null);
            mDragHorizontalView.setCompoundDrawablesWithIntrinsicBounds(null, backward, null, null);
        } else if (direction == DIRECTION_RIGHT) {
            Drawable forward = getResources().getDrawable(R.drawable.ic_progress_forward, null);
            mDragHorizontalView.setCompoundDrawablesWithIntrinsicBounds(null, forward, null, null);
        }
    }

    private String getCurrentSysTime() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm");
        Date curDate = new Date(System.currentTimeMillis());
        String str = format.format(curDate);
        return str;
    }

    private String timeToString(int timeMs) {
        int totalSeconds = timeMs / 1000;
        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = (totalSeconds / 3600);
        mFormatterBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%02d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int getBrightness() {
        return Settings.System.getInt(this.getContentResolver(), "screen_brightness", -1);
    }

    private void setBrightness(int param) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float currentBrightness = param / (float) mMaxLight;
        lp.screenBrightness = currentBrightness;
        getWindow().setAttributes(lp);
    }
}
