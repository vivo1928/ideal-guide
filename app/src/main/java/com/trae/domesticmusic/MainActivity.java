package com.trae.domesticmusic;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.Manifest;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.media.MediaPlayer;
import android.media.MediaMetadata;
import android.media.PlaybackParams;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.media.audiofx.PresetReverb;
import android.media.audiofx.BassBoost;
import android.net.Uri;
import android.os.PowerManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String PREFS_NAME = "tinghanxinyu_player";
    private static final String PREF_PLAYLIST = "playlist";
    private static final String PREF_IMPORTED_PLAYLISTS = "imported_playlists";
    private static final String PREF_SUBTITLE_EFFECT = "subtitle_effect";
    private static final String PREF_LYRIC_COLOR = "lyric_color";
    private static final String PREF_LYRIC_OFFSET = "lyric_offset";
    private static final String PREF_TIMBRE = "timbre";
    private static final String PREF_EQ_BANDS = "eq_bands";
    private static final String PREF_VOICE_EFFECT = "voice_effect";
    private static final String PREF_LOVE_UNLOCKED = "love_unlocked";
    private static final String LOVE_PASSWORD = "526118";
    private static final String ACTION_PLAY_PAUSE = "com.trae.domesticmusic.PLAY_PAUSE";
    private static final String ACTION_PREVIOUS = "com.trae.domesticmusic.PREVIOUS";
    private static final String ACTION_NEXT = "com.trae.domesticmusic.NEXT";
    private static final String ACTION_FAST_FORWARD = "com.trae.domesticmusic.FAST_FORWARD";
    private static final int NOTIFICATION_ID = 1024;
    private static final int BG = Color.parseColor("#000000");
    private static final int CARD = Color.parseColor("#111111");
    private static final int CARD2 = Color.parseColor("#1E1E1E");
    private static final int LINE = Color.parseColor("#333333");
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.parseColor("#BDBDBD");

    private static final int BLUE = Color.parseColor("#1E90FF");
    private static final int GREEN = Color.parseColor("#4CAF50");

    private FrameLayout root;
    private LinearLayout content;
    private FrameLayout pageContainer;
    private TextView[] pageTabViews;
    private View page1View, page2View, page3View, page4View;
    private SeekBar seekBar;
    private ImageView albumArtView;
    private ImageView fullPlayerCoverView;
    private Track currentFullPlayerTrack;
    private LinearLayout bottomArea;
    private LinearLayout playerCtrl;
    private int currentPageIndex = 0;
    private boolean addedFromPlaylist = false;
    private String lastAlbumCoverTrack = "";
    private TextView currentTrackText;
    private TextView currentTimeView;
    private TextView durationView;
    private ProgressBar loadingBar;
    private LoveMenuButton heartMenuButton;
    private LinearLayout activeLyricsBox;
    private ScrollView activeLyricsScroll;
    private final List<LyricLine> activeLyricLines = new ArrayList<LyricLine>();
    private final List<TextView> activeLyricViews = new ArrayList<TextView>();
    private int activeLyricIndex = -1;
    private boolean activeLyricsTimed = false;

    private final Handler handler = new Handler();
    private final ExecutorService worker = Executors.newCachedThreadPool();
    private final MusicResolver resolver = new MusicResolver();
    private final List<Track> playlist = new ArrayList<Track>();
    private final List<SearchResult> lastSearchResults = new ArrayList<SearchResult>();
    private int repeatMode = 0; // 0=顺序播放, 1=单曲循环, 2=全部循环
    private SharedPreferences prefs;
    private MediaPlayer mediaPlayer;
    private MediaSession mediaSession;
    private Equalizer equalizer;
    private PresetReverb presetReverb;
    private BassBoost bassBoost;
    private int currentVoiceEffect = 0; // 0=默认, 1=机器人, 2=花栗鼠, 3=教堂, 4=大厅, 5=房间, 6=回声, 7=低音增强
    private NotificationManager notificationManager;
    private BroadcastReceiver mediaActionReceiver;
    private BroadcastReceiver noisyAudioReceiver;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private int currentIndex = -1;
    private android.view.SurfaceHolder currentSurfaceHolder;
    private String currentUrl = "";
    private String currentMediaTitle = "未播放";
    private String currentMediaArtist = "听含新宇";
    private int subtitleEffect = 1;
    private int lyricColor = Color.WHITE;
    private int lyricOffsetMs = 1000;
    private short timbrePreset = -1;
    private float playbackSpeed = 1.0f;
    private boolean userSeeking = false;
    private boolean showingMainPage = false;
    private Runnable previousPageAction = null;
    private String lastSearchKeyword = ""; // 保存上次搜索关键词，返回后不消失

    // 爱的空间歌曲列表
    private static class LoveTrack {
        String title;
        String artist;
        String assetFile; // assets/love/ 下的文件名
        LoveTrack(String title, String artist, String assetFile) {
            this.title = title; this.artist = artist; this.assetFile = assetFile;
        }
    }
    private final List<LoveTrack> loveTrackList = new ArrayList<LoveTrack>();
    private int currentLoveTrackIndex = -1; // 当前播放的爱的空间歌曲索引
    private final List<SeekBar> loveSeekBars = new ArrayList<SeekBar>();

    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            handler.postDelayed(this, 200);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 硬件加速已在AndroidManifest.xml中启用
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        subtitleEffect = prefs.getInt(PREF_SUBTITLE_EFFECT, 1);
        lyricColor = prefs.getInt(PREF_LYRIC_COLOR, Color.WHITE);
        lyricOffsetMs = prefs.getInt(PREF_LYRIC_OFFSET, 1000);
        timbrePreset = (short) prefs.getInt(PREF_TIMBRE, -1);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TingHanXinYu::PlayerWakeLock");
        wakeLock.setReferenceCounted(false);
        setupMediaSession();
        // 注册通知栏按钮广播接收器，避免点击时跳转打开APP
        mediaActionReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                handleIntentAction(intent);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAY_PAUSE);
        filter.addAction(ACTION_PREVIOUS);
        filter.addAction(ACTION_NEXT);
        filter.addAction(ACTION_FAST_FORWARD);
        registerReceiver(mediaActionReceiver, filter);
        // 注册耳机拔出检测
        noisyAudioReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        updatePlaybackState();
                        showPlayerNotification();
                    }
                }
            }
        };
        registerReceiver(noisyAudioReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        loadPlaylist();
        buildRoot();
        showMainPage();
        handler.post(progressTicker);
        handleIntentAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentAction(intent);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        hidePlayerNotification();
        if (mediaActionReceiver != null) {
            try { unregisterReceiver(mediaActionReceiver); } catch (Exception ignored) {}
            mediaActionReceiver = null;
        }
        if (noisyAudioReceiver != null) {
            try { unregisterReceiver(noisyAudioReceiver); } catch (Exception ignored) {}
            noisyAudioReceiver = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        abandonAudioFocus();
        releaseWakeLock();
        worker.shutdownNow();
        releasePlayer();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        Object tag = content.getTag();
        if (tag != null && !"main".equals(tag)) {
            showMainPage();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                togglePlayback();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                playFromMediaSession();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                pauseFromMediaSession();
                return true;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                nextTrack();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                previousTrack();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                fastForward15Seconds();
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                    int prev = Math.max(0, mediaPlayer.getCurrentPosition() - 15000);
                    mediaPlayer.seekTo(prev);
                }
                updatePlaybackState();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void buildRoot() {
        root = new FrameLayout(this);
        root.setContentDescription("听含新宇主界面");
        root.addView(new GradientBackground(this), new FrameLayout.LayoutParams(-1, -1));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setPadding(dp(18), dp(18), dp(18), dp(12));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(content, new ScrollView.LayoutParams(-1, -2));
        FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(-1, -1);
        scrollParams.setMargins(0, 0, 0, dp(50));
        root.addView(scrollView, scrollParams);

        addHeartButton();
        setContentView(root);
    }

    private void setupMediaSession() {
        mediaSession = new MediaSession(this, "TingHanXinYuPlayer");
        mediaSession.setActive(true);
        mediaSession.setFlags(MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mediaSession.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { playFromMediaSession(); }
            @Override public void onPause() { pauseFromMediaSession(); }
            @Override public void onStop() {
                pauseFromMediaSession();
                hidePlayerNotification();
            }
            @Override public void onSkipToNext() { nextTrack(); }
            @Override public void onSkipToPrevious() { previousTrack(); }
            @Override public void onFastForward() { fastForward15Seconds(); }
            @Override public void onRewind() {
                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                    int prev = Math.max(0, mediaPlayer.getCurrentPosition() - 15000);
                    mediaPlayer.seekTo(prev);
                }
                updatePlaybackState();
            }
            @Override public void onSeekTo(long pos) {
                if (mediaPlayer != null) mediaPlayer.seekTo((int) pos);
                updatePlaybackState();
            }
        });
        updatePlaybackState();
    }

    private void handleIntentAction(Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        String action = intent.getAction();
        if (ACTION_PLAY_PAUSE.equals(action)) togglePlayback();
        else if (ACTION_PREVIOUS.equals(action)) previousTrack();
        else if (ACTION_NEXT.equals(action)) nextTrack();
        else if (ACTION_FAST_FORWARD.equals(action)) fastForward15Seconds();
    }

    private void playFromMediaSession() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            requestAudioFocus();
            acquireWakeLock();
            mediaPlayer.start();
            updatePlaybackState();
            showPlayerNotification();
        } else if (!playlist.isEmpty()) {
            playPlaylistTrack(Math.max(0, currentIndex));
        }
    }

    private void pauseFromMediaSession() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) mediaPlayer.pause();
        releaseWakeLock();
        updatePlaybackState();
        showPlayerNotification();
    }

    private void fastForward15Seconds() {
        if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
            int next = Math.min(mediaPlayer.getDuration(), mediaPlayer.getCurrentPosition() + 15000);
            mediaPlayer.seekTo(next);
        }
        updatePlaybackState();
    }

    private void bindSeekBar(SeekBar bar) {
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                    mediaPlayer.seekTo(mediaPlayer.getDuration() * seekBar.getProgress() / 1000);
                    updateActiveLyric(mediaPlayer.getCurrentPosition());
                }
                userSeeking = false;
            }
        });
        bar.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public boolean performAccessibilityAction(View host, int action, Bundle args) {
                if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
                    if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                        int newPos = Math.min(mediaPlayer.getCurrentPosition() + 5000, mediaPlayer.getDuration());
                        mediaPlayer.seekTo(newPos);
                        updatePlaybackState();
                        updateActiveLyric(newPos);
                        host.setContentDescription("快进到 " + formatTime(newPos));
                        host.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
                    }
                    return true;
                } else if (action == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
                    if (mediaPlayer != null) {
                        int newPos = Math.max(mediaPlayer.getCurrentPosition() - 5000, 0);
                        mediaPlayer.seekTo(newPos);
                        updatePlaybackState();
                        updateActiveLyric(newPos);
                        host.setContentDescription("快退到 " + formatTime(newPos));
                        host.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
                    }
                    return true;
                }
                return super.performAccessibilityAction(host, action, args);
            }
        });
    }

    private void applyPlaybackSpeed() {
        if (mediaPlayer == null) return;
        try {
            PlaybackParams params = mediaPlayer.getPlaybackParams();
            params.setSpeed(playbackSpeed);
            mediaPlayer.setPlaybackParams(params);
            if (!mediaPlayer.isPlaying()) mediaPlayer.pause();
        } catch (Exception error) {
            toast("当前设备不支持倍速");
        }
    }

    private void setupEqualizer() {
        releaseEqualizer();
        if (mediaPlayer == null) return;
        try {
            int sessionId = mediaPlayer.getAudioSessionId();
            equalizer = new Equalizer(0, sessionId);
            equalizer.setEnabled(true);
            applyTimbre();
            applyStoredEqBands();
            presetReverb = new PresetReverb(0, sessionId);
            presetReverb.setEnabled(false);
            bassBoost = new BassBoost(0, sessionId);
            bassBoost.setEnabled(false);
            // 恢复存储的魔音效果
            int savedVoice = prefs.getInt(PREF_VOICE_EFFECT, 0);
            if (savedVoice > 0) { currentVoiceEffect = savedVoice; applyVoiceEffect(); }
        } catch (Exception ignored) {
            equalizer = null;
            presetReverb = null;
            bassBoost = null;
        }
    }

    private void applyStoredEqBands() {
        if (equalizer == null) return;
        String saved = prefs.getString(PREF_EQ_BANDS, "");
        if (saved.length() == 0) return;
        try {
            String[] parts = saved.split(",");
            short numBands = equalizer.getNumberOfBands();
            for (int i = 0; i < Math.min(parts.length, numBands); i++) {
                equalizer.setBandLevel((short) i, Short.parseShort(parts[i]));
            }
        } catch (Exception ignored) {}
    }

    private void applyTimbre() {
        if (equalizer == null) return;
        try {
            if (timbrePreset >= 0 && timbrePreset < equalizer.getNumberOfPresets()) {
                equalizer.usePreset(timbrePreset);
                equalizer.setEnabled(true);
            } else {
                equalizer.setEnabled(false);
            }
        } catch (Exception ignored) {}
    }

    private void releaseEqualizer() {
        if (equalizer != null) {
            try { equalizer.release(); } catch (Exception ignored) {}
            equalizer = null;
        }
        if (presetReverb != null) {
            try { presetReverb.release(); } catch (Exception ignored) {}
            presetReverb = null;
        }
        if (bassBoost != null) {
            try { bassBoost.release(); } catch (Exception ignored) {}
            bassBoost = null;
        }
    }

    private PendingIntent mediaActionIntent(String action, int requestCode) {
        Intent intent = new Intent(action);
        intent.setPackage(getPackageName());
        return PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void updatePlaybackState() {
        if (mediaSession == null) return;
        long actions = PlaybackState.ACTION_PLAY
            | PlaybackState.ACTION_PAUSE
            | PlaybackState.ACTION_PLAY_PAUSE
            | PlaybackState.ACTION_STOP
            | PlaybackState.ACTION_SKIP_TO_NEXT
            | PlaybackState.ACTION_SKIP_TO_PREVIOUS
            | PlaybackState.ACTION_FAST_FORWARD
            | PlaybackState.ACTION_REWIND
            | PlaybackState.ACTION_SEEK_TO;
        int state = mediaPlayer != null && mediaPlayer.isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        long pos = mediaPlayer != null ? mediaPlayer.getCurrentPosition() : 0;
        mediaSession.setPlaybackState(new PlaybackState.Builder()
            .setActions(actions)
            .setState(state, pos, state == PlaybackState.STATE_PLAYING ? 1f : 0f)
            .build());
        MediaMetadata.Builder metaBuilder = new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, currentMediaTitle)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, currentMediaArtist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "听含新宇");
        if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
            metaBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, mediaPlayer.getDuration());
        }
        mediaSession.setMetadata(metaBuilder.build());
    }

    private void showPlayerNotification() {
        if (notificationManager == null || mediaSession == null) return;
        boolean playing = mediaPlayer != null && mediaPlayer.isPlaying();
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 10, openIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
            .setSmallIcon(getApplicationInfo().icon)
            .setContentTitle(currentMediaTitle)
            .setContentText(currentMediaArtist)
            .setContentIntent(contentIntent)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "上一首", mediaActionIntent(ACTION_PREVIOUS, 11))
            .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, playing ? "暂停" : "播放", mediaActionIntent(ACTION_PLAY_PAUSE, 12))
            .addAction(android.R.drawable.ic_media_next, "下一首", mediaActionIntent(ACTION_NEXT, 13))
            .addAction(android.R.drawable.ic_media_ff, "快进", mediaActionIntent(ACTION_FAST_FORWARD, 14))
            .setStyle(new Notification.MediaStyle()
                .setMediaSession(mediaSession.getSessionToken())
                .setShowActionsInCompactView(0, 1, 2));
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void hidePlayerNotification() {
        if (notificationManager != null) notificationManager.cancel(NOTIFICATION_ID);
    }

    private void addHeartButton() {
        heartMenuButton = new LoveMenuButton(this);
        heartMenuButton.setContentDescription("爱心");
        heartMenuButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                showLovePasswordDialog();
            }
        });
        // 长按爱心按钮5秒可清除已解锁状态，重新启用密码
        heartMenuButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                prefs.edit().putBoolean(PREF_LOVE_UNLOCKED, false).apply();
                toast("已重新启用密码保护");
                return true;
            }
        });
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(110), dp(44), Gravity.RIGHT | Gravity.BOTTOM);
        params.setMargins(0, 0, dp(8), dp(104));
        root.addView(heartMenuButton, params);
    }

    private View lovePasswordOverlay;
    private void showLovePasswordDialog() {
        if (lovePasswordOverlay != null && lovePasswordOverlay.getParent() != null) return;
        final View overlay = new View(this);
        overlay.setBackgroundColor(Color.BLACK);
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(-1, -1);
        root.addView(overlay, overlayParams);

        final EditText pwdInput = new EditText(this);
        pwdInput.setHint("请输入密码");
        pwdInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pwdInput.setTextColor(TEXT);
        pwdInput.setHintTextColor(MUTED);
        pwdInput.setBackgroundColor(CARD2);
        pwdInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        pwdInput.setSingleLine(true);
        pwdInput.setGravity(Gravity.CENTER);
        pwdInput.setTextSize(20);
        pwdInput.setContentDescription("密码输入框");
        pwdInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pwdInput.setFocusable(true);
        pwdInput.setFocusableInTouchMode(true);
        // 限制最多6位
        pwdInput.setFilters(new android.text.InputFilter[]{
            new android.text.InputFilter.LengthFilter(6)
        });

        Button confirmBtn = new Button(this);
        confirmBtn.setText("确 定");
        confirmBtn.setAllCaps(false);
        confirmBtn.setTextColor(Color.WHITE);
        confirmBtn.setBackgroundColor(BLUE);
        confirmBtn.setPadding(dp(20), dp(12), dp(20), dp(12));
        confirmBtn.setTextSize(16);
        confirmBtn.setContentDescription("确定");

        Button cancelBtn = new Button(this);
        cancelBtn.setText("取 消");
        cancelBtn.setAllCaps(false);
        cancelBtn.setTextColor(TEXT);
        cancelBtn.setBackgroundColor(CARD2);
        cancelBtn.setPadding(dp(20), dp(12), dp(20), dp(12));
        cancelBtn.setTextSize(16);
        cancelBtn.setContentDescription("取消");

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams btnLp1 = new LinearLayout.LayoutParams(0, dp(48), 1);
        btnLp1.setMargins(dp(6), 0, dp(6), 0);
        btnRow.addView(cancelBtn, btnLp1);
        LinearLayout.LayoutParams btnLp2 = new LinearLayout.LayoutParams(0, dp(48), 1);
        btnLp2.setMargins(dp(6), 0, dp(6), 0);
        btnRow.addView(confirmBtn, btnLp2);

        TextView tip = new TextView(this);
        tip.setText("请输入爱的空间密码");
        tip.setTextColor(MUTED);
        tip.setTextSize(14);
        tip.setGravity(Gravity.CENTER);
        tip.setContentDescription("请输入爱的空间密码");

        LinearLayout cardBox = new LinearLayout(this);
        cardBox.setOrientation(LinearLayout.VERTICAL);
        cardBox.setBackgroundColor(CARD);
        cardBox.setPadding(dp(20), dp(24), dp(20), dp(20));

        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(-1, dp(52));
        inputLp.setMargins(0, 0, 0, dp(14));
        cardBox.addView(pwdInput, inputLp);
        cardBox.addView(btnRow, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout wrap = new LinearLayout(this);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setGravity(Gravity.CENTER);
        wrap.addView(tip, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams tipLp = (LinearLayout.LayoutParams) tip.getLayoutParams();
        tipLp.bottomMargin = dp(16);
        tip.setLayoutParams(tipLp);
        wrap.addView(cardBox, new LinearLayout.LayoutParams(-1, -2));

        FrameLayout.LayoutParams wrapParams = new FrameLayout.LayoutParams(-1, -1);
        wrapParams.gravity = Gravity.CENTER;
        wrap.setPadding(dp(32), 0, dp(32), 0);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        root.addView(wrap, wrapParams);

        // 点击黑色背景不取消，必须通过按钮关闭
        overlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pwdInput.requestFocus();
            }
        });

        Runnable dismiss = new Runnable() {
            @Override public void run() {
                // 隐藏键盘
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(pwdInput.getWindowToken(), 0);
                root.removeView(wrap);
                root.removeView(overlay);
                lovePasswordOverlay = null;
            }
        };
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { dismiss.run(); }
        });
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String input = pwdInput.getText().toString().trim();
                if (LOVE_PASSWORD.equals(input)) {
                    dismiss.run();
                    animateLoveSpaceEntry();
                } else {
                    toast("密码错误，请重试");
                    pwdInput.setText("");
                    pwdInput.requestFocus();
                }
            }
        });
        pwdInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    confirmBtn.performClick();
                    return true;
                }
                return false;
            }
        });

        // 自动检测密码：输入满6位自动判断
        pwdInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String input = s.toString().trim();
                if (input.length() == 6) {
                    if (LOVE_PASSWORD.equals(input)) {
                        // 密码正确：自动进入，带动画
                        pwdInput.postDelayed(new Runnable() {
                            @Override public void run() {
                                if (lovePasswordOverlay == null) return;
                                dismiss.run();
                                animateLoveSpaceEntry();
                            }
                        }, 200);
                    } else {
                        // 密码错误：弹回首页
                        pwdInput.postDelayed(new Runnable() {
                            @Override public void run() {
                                if (lovePasswordOverlay == null) return;
                                dismiss.run();
                                switchToPage(0);
                                toast("密码错误");
                            }
                        }, 200);
                    }
                }
            }
        });

        lovePasswordOverlay = overlay;
        // 自动弹出键盘
        pwdInput.post(new Runnable() {
            @Override public void run() {
                pwdInput.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(pwdInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    // ===== 爱的空间进入动画 =====
    private void animateLoveSpaceEntry() {
        // 第一步：显示白色闪光覆盖层
        final View whiteFlash = new View(this);
        whiteFlash.setBackgroundColor(Color.WHITE);
        whiteFlash.setAlpha(0f);
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(-1, -1);
        root.addView(whiteFlash, fp);

        // 白光快速亮起
        whiteFlash.animate().alpha(1f).setDuration(180).withEndAction(new Runnable() {
            @Override public void run() {
                // 第二步：白光慢慢熄灭
                whiteFlash.animate().alpha(0f).setDuration(500).setStartDelay(50).withEndAction(new Runnable() {
                    @Override public void run() {
                        root.removeView(whiteFlash);
                        // 第三步：碎片汇聚动画
                        showConvergencePieces();
                    }
                });
            }
        });
    }

    private void showConvergencePieces() {
        final int cols = 8;
        final int rows = 12;
        final int screenW = root.getWidth();
        final int screenH = root.getHeight();
        if (screenW <= 0 || screenH <= 0) {
            // 屏幕尺寸未就绪，直接切换
            switchToPage(3);
            return;
        }
        final int pieceW = (int) Math.ceil((float) screenW / cols);
        final int pieceH = (int) Math.ceil((float) screenH / rows);

        final FrameLayout pieceContainer = new FrameLayout(this);
        pieceContainer.setClipChildren(false);
        root.addView(pieceContainer, new FrameLayout.LayoutParams(-1, -1));

        final int totalPieces = cols * rows;
        final View[] pieces = new View[totalPieces];
        final long baseDelay = 80;
        final long animDuration = 550;

        // 计算每块碎片的最终位置和起始偏移方向
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int idx = r * cols + c;
                View piece = new View(this);

                // 根据位置给不同深浅的颜色（模拟碎片感）
                float hue = 210f + (r + c) * 4f; // 蓝紫色调变化
                float[] hsv = {hue % 360, 0.7f, 0.55f + (r % 3) * 0.15f};
                int pieceColor = Color.HSVToColor(hsv);
                piece.setBackgroundColor(pieceColor);

                int finalX = c * pieceW;
                int finalY = r * pieceH;

                // 计算从哪个方向飞入：基于碎片在屏幕中的位置
                float centerX = cols / 2f;
                float centerY = rows / 2f;
                float dx = c - centerX;
                float dy = r - centerY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < 0.5f) dist = 0.5f;

                // 归一化方向向量，碎片从远处飞入
                float nx = dx / dist;
                float ny = dy / dist;
                float flyDist = (float)(Math.sqrt(screenW * screenW + screenH * screenH)) * 0.8f;
                float startX = finalX + nx * flyDist;
                float startY = finalY + ny * flyDist;

                piece.setTranslationX(startX - finalX);
                piece.setTranslationY(startY - finalY);
                piece.setScaleX(0.3f);
                piece.setScaleY(0.3f);
                piece.setAlpha(0.9f);
                piece.setRotation((float)(Math.random() * 60 - 30)); // 随机旋转

                FrameLayout.LayoutParams pp = new FrameLayout.LayoutParams(pieceW + 1, pieceH + 1);
                pp.leftMargin = finalX;
                pp.topMargin = finalY;
                pieceContainer.addView(piece, pp);
                pieces[idx] = piece;
            }
        }

        // 第四步：所有碎片从四面八方汇聚到最终位置
        for (int i = 0; i < totalPieces; i++) {
            final View piece = pieces[i];
            // 随机微小延迟，让汇聚更自然
            int delay = (int)(Math.random() * 150);
            piece.animate()
                .translationX(0)
                .translationY(0)
                .scaleX(1f)
                .scaleY(1f)
                .rotation(0)
                .alpha(0.9f)
                .setDuration(animDuration)
                .setStartDelay(baseDelay + delay)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(2.5f));
        }

        // 碎片汇聚完成后，整体淡出并显示实际页面
        handler.postDelayed(new Runnable() {
            @Override public void run() {
                // 碎片整体淡出
                pieceContainer.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                    @Override public void run() {
                        root.removeView(pieceContainer);
                        switchToPage(3);
                        // 动画结束后随机播放一首爱的空间歌曲
                        if (!loveTrackList.isEmpty()) {
                            int randomIdx = (int)(Math.random() * loveTrackList.size());
                            playLoveTrack(randomIdx);
                        }
                    }
                });
            }
        }, baseDelay + animDuration + 200);
    }

    private void showMainPage() {
        // 防重入：如果正在构建主页，跳过重复调用
        if (showingMainPage) return;
        // 首次加载直接构建，不需要过渡动画
        if (pageContainer == null) {
            showingMainPage = true;
            doShowMainPage();
            showingMainPage = false;
            return;
        }
        // 返回主页：淡出 → 重建 → 淡入，视觉流畅
        showingMainPage = true;
        content.animate().alpha(0f).setDuration(160).withEndAction(new Runnable() {
            @Override public void run() {
                doShowMainPage();
                content.setAlpha(0f);
                content.animate().alpha(1f).setDuration(160).withEndAction(new Runnable() {
                    @Override public void run() { showingMainPage = false; }
                }).start();
            }
        }).start();
    }

    private void doShowMainPage() {
        content.setTag("main");
        content.removeAllViews();
        previousPageAction = null;

        // 清除详情页的控件引用
        seekBar = null;
        currentTimeView = null;
        durationView = null;
        fullPlayerCoverView = null;
        currentFullPlayerTrack = null;
        repeatModeButton = null;

        addHeader("听含新宇", "聚合搜索 · 歌单播放 · 多音源解析");

        // 首次创建页面容器和四个标签页
        if (pageContainer == null) {
            pageContainer = new FrameLayout(this);
            pageContainer.setLayoutParams(new LinearLayout.LayoutParams(-1, -1, 1f));

            page1View = buildPage1();
            pageContainer.addView(page1View);

            page2View = buildPage2();
            page2View.setVisibility(View.GONE);
            pageContainer.addView(page2View);

            page3View = buildPage3();
            page3View.setVisibility(View.GONE);
            pageContainer.addView(page3View);

            page4View = buildPage4();
            page4View.setVisibility(View.GONE);
            pageContainer.addView(page4View);
        }
        // 切回第1页
        page1View.setVisibility(View.VISIBLE);
        page2View.setVisibility(View.GONE);
        page3View.setVisibility(View.GONE);
        if (page4View != null) page4View.setVisibility(View.GONE);
        content.addView(pageContainer);

        // 底部标签栏：首次创建，之后复用
        if (pageTabViews == null) {
            final String[] tabLabels = {"搜索", "功能", "歌单", "爱的空间"};
            // 使用底部LinearLayout均匀分布4个标签
            LinearLayout tabBar = new LinearLayout(this);
            tabBar.setOrientation(LinearLayout.HORIZONTAL);
            tabBar.setGravity(Gravity.CENTER);
            pageTabViews = new TextView[4];
            for (int i = 0; i < 4; i++) {
                final int tabIdx = i;
                TextView tab = new TextView(this);
                tab.setText(tabLabels[i]);
                tab.setGravity(Gravity.CENTER);
                tab.setTextSize(10);
                tab.setTextColor(i == 0 ? TEXT : MUTED);
                android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                circle.setColor(i == 0 ? BLUE : CARD);
                circle.setStroke(dp(1), i == 0 ? BLUE : MUTED);
                tab.setBackground(circle);
                tab.setClickable(true);
                tab.setFocusable(true);
                tab.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                tab.setContentDescription(tabLabels[i] + "标签");
                tab.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (tabIdx == 3) {
                            showLovePasswordDialog();
                            return;
                        }
                        switchToPage(tabIdx);
                    }
                });
                pageTabViews[i] = tab;
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(dp(30), dp(30));
                tp.setMargins(dp(14), 0, dp(14), dp(10));
                tabBar.addView(tab, tp);
            }
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            barParams.setMargins(0, 0, 0, 0);
            root.addView(tabBar, barParams);
        } else {
            for (TextView tv : pageTabViews) {
                if (tv != null) tv.setVisibility(View.VISIBLE);
            }
            switchToPage(0);
        }
        currentPageIndex = 0;
        lastAlbumCoverTrack = "";
        handler.post(new Runnable() {
            @Override public void run() { updateMiniPlayer(); }
        });
        updateHeartVisibility();
    }

    private View buildPage1() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout searchCard = card();
        final EditText searchInput = new EditText(this);
        searchInput.setHint("搜索歌名或歌手");
        searchInput.setSingleLine(true);
        searchInput.setTextColor(TEXT);
        searchInput.setHintTextColor(MUTED);
        searchInput.setBackgroundColor(CARD2);
        searchInput.setPadding(dp(12), dp(8), dp(12), dp(8));
        searchInput.setFocusableInTouchMode(true);
        searchInput.clearFocus();
        searchCard.addView(searchInput, lp(-1, dp(48), 0, 0, 0, 8));
        // 第一行音源选择
        LinearLayout sourceBar = new LinearLayout(this);
        sourceBar.setOrientation(LinearLayout.HORIZONTAL);
        final String[] sourceNames = {"全网", "QQ", "网易云", "酷狗", "酷我", "咪咕"};
        final String[] sourceKeys = {"all", "tx", "wy", "kg", "kw", "mg"};
        final int[] selectedSource = {0};
        final Button[] xmBtnHolder = new Button[1];
        final Button[] blBtnHolder = new Button[1];
        final Button[] dyBtnHolder = new Button[1];
        for (int i = 0; i < sourceNames.length; i++) {
            final int idx = i;
            Button srcBtn = new Button(this);
            srcBtn.setText(sourceNames[i]);
            srcBtn.setAllCaps(false);
            srcBtn.setTextSize(12);
            srcBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
            srcBtn.setMinHeight(dp(32));
            srcBtn.setMinWidth(0);
            srcBtn.setTextColor(i == 0 ? BG : MUTED);
            srcBtn.setBackgroundColor(i == 0 ? TEXT : CARD2);
            srcBtn.setContentDescription("选择" + sourceNames[i] + "音源");
            srcBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    selectedSource[0] = idx;
                    // 更新第一行按钮样式
                    for (int j = 0; j < sourceBar.getChildCount(); j++) {
                        Button btn = (Button) sourceBar.getChildAt(j);
                        btn.setTextColor(j == idx ? BG : MUTED);
                        btn.setBackgroundColor(j == idx ? TEXT : CARD2);
                    }
                    // 取消第二行选中
                    if (xmBtnHolder[0] != null) {
                        xmBtnHolder[0].setTextColor(MUTED);
                        xmBtnHolder[0].setBackgroundColor(CARD2);
                    }
                    if (blBtnHolder[0] != null) {
                        blBtnHolder[0].setTextColor(MUTED);
                        blBtnHolder[0].setBackgroundColor(CARD2);
                    }
                    if (dyBtnHolder[0] != null) {
                        dyBtnHolder[0].setTextColor(MUTED);
                        dyBtnHolder[0].setBackgroundColor(CARD2);
                    }
                    String keyword = searchInput.getText().toString().trim();
                    if (keyword.length() == 0) { toast("请输入关键词"); return; }
                    toast("正在搜索：" + keyword + "（" + sourceNames[idx] + "）");
                    searchInside(sourceKeys[idx], keyword);
                    v.setContentDescription("已选择" + sourceNames[idx] + "音源");
                    v.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
                }
            });
            sourceBar.addView(srcBtn, new LinearLayout.LayoutParams(0, dp(32), 1));
        }
        searchCard.addView(sourceBar, lp(-1, -2, 0, 0, 0, 8));

        // 第二行：喜马拉雅 + 哔哩哔哩 + 抖音（并排）
        LinearLayout xmBar = new LinearLayout(this);
        xmBar.setOrientation(LinearLayout.HORIZONTAL);

        Button ximalayaBtn = new Button(this);
        ximalayaBtn.setText("喜马拉雅");
        ximalayaBtn.setAllCaps(false);
        ximalayaBtn.setTextSize(12);
        ximalayaBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        ximalayaBtn.setMinHeight(dp(32));
        ximalayaBtn.setMinWidth(0);
        ximalayaBtn.setTextColor(MUTED);
        ximalayaBtn.setBackgroundColor(CARD2);
        ximalayaBtn.setContentDescription("选择喜马拉雅音源");
        xmBtnHolder[0] = ximalayaBtn;
        ximalayaBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // 取消第一行所有选中
                for (int j = 0; j < sourceBar.getChildCount(); j++) {
                    Button btn = (Button) sourceBar.getChildAt(j);
                    btn.setTextColor(MUTED);
                    btn.setBackgroundColor(CARD2);
                }
                // 取消同排选中
                if (blBtnHolder[0] != null) {
                    blBtnHolder[0].setTextColor(MUTED);
                    blBtnHolder[0].setBackgroundColor(CARD2);
                }
                if (dyBtnHolder[0] != null) {
                    dyBtnHolder[0].setTextColor(MUTED);
                    dyBtnHolder[0].setBackgroundColor(CARD2);
                }
                ximalayaBtn.setTextColor(BG);
                ximalayaBtn.setBackgroundColor(TEXT);
                String keyword = searchInput.getText().toString().trim();
                if (keyword.length() == 0) { toast("请输入关键词"); return; }
                showXimalayaTypeDialog(keyword);
                v.setContentDescription("已选择喜马拉雅音源");
                v.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        });
        xmBar.addView(ximalayaBtn, new LinearLayout.LayoutParams(0, dp(32), 1));

        Button bilibiliBtn = new Button(this);
        bilibiliBtn.setText("哔哩哔哩");
        bilibiliBtn.setAllCaps(false);
        bilibiliBtn.setTextSize(12);
        bilibiliBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        bilibiliBtn.setMinHeight(dp(32));
        bilibiliBtn.setMinWidth(0);
        bilibiliBtn.setTextColor(MUTED);
        bilibiliBtn.setBackgroundColor(CARD2);
        bilibiliBtn.setContentDescription("选择哔哩哔哩音源");
        blBtnHolder[0] = bilibiliBtn;
        bilibiliBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // 取消第一行所有选中
                for (int j = 0; j < sourceBar.getChildCount(); j++) {
                    Button btn = (Button) sourceBar.getChildAt(j);
                    btn.setTextColor(MUTED);
                    btn.setBackgroundColor(CARD2);
                }
                // 取消同排选中
                if (xmBtnHolder[0] != null) {
                    xmBtnHolder[0].setTextColor(MUTED);
                    xmBtnHolder[0].setBackgroundColor(CARD2);
                }
                if (dyBtnHolder[0] != null) {
                    dyBtnHolder[0].setTextColor(MUTED);
                    dyBtnHolder[0].setBackgroundColor(CARD2);
                }
                bilibiliBtn.setTextColor(BG);
                bilibiliBtn.setBackgroundColor(TEXT);
                String keyword = searchInput.getText().toString().trim();
                if (keyword.length() == 0) { toast("请输入关键词"); return; }
                showBilibiliTypeDialog(keyword);
                v.setContentDescription("已选择哔哩哔哩音源");
                v.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        });
        xmBar.addView(bilibiliBtn, new LinearLayout.LayoutParams(0, dp(32), 1));

        Button douyinBtn = new Button(this);
        douyinBtn.setText("抖音");
        douyinBtn.setAllCaps(false);
        douyinBtn.setTextSize(12);
        douyinBtn.setPadding(dp(4), dp(4), dp(4), dp(4));
        douyinBtn.setMinHeight(dp(32));
        douyinBtn.setMinWidth(0);
        douyinBtn.setTextColor(MUTED);
        douyinBtn.setBackgroundColor(CARD2);
        douyinBtn.setContentDescription("选择抖音音源");
        dyBtnHolder[0] = douyinBtn;
        douyinBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // 取消第一行所有选中
                for (int j = 0; j < sourceBar.getChildCount(); j++) {
                    Button btn = (Button) sourceBar.getChildAt(j);
                    btn.setTextColor(MUTED);
                    btn.setBackgroundColor(CARD2);
                }
                // 取消同排选中
                if (xmBtnHolder[0] != null) {
                    xmBtnHolder[0].setTextColor(MUTED);
                    xmBtnHolder[0].setBackgroundColor(CARD2);
                }
                if (blBtnHolder[0] != null) {
                    blBtnHolder[0].setTextColor(MUTED);
                    blBtnHolder[0].setBackgroundColor(CARD2);
                }
                douyinBtn.setTextColor(BG);
                douyinBtn.setBackgroundColor(TEXT);
                String keyword = searchInput.getText().toString().trim();
                if (keyword.length() == 0) { toast("请输入关键词"); return; }
                showDouyinTypeDialog(keyword);
                v.setContentDescription("已选择抖音音源");
                v.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_SELECTED);
            }
        });
        xmBar.addView(douyinBtn, new LinearLayout.LayoutParams(0, dp(32), 1));
        searchCard.addView(xmBar, lp(-1, -2, 0, 0, 0, 8));

        // 回车键直接搜索（使用当前选中的音源）
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    String keyword = searchInput.getText().toString().trim();
                    if (keyword.length() == 0) { toast("请输入关键词"); return true; }
                    toast("正在搜索：" + keyword);
                    searchInside(sourceKeys[selectedSource[0]], keyword);
                    return true;
                }
                return false;
            }
        });
        // 正在播放指示器
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            final Track nowPlaying = playlist.get(currentIndex);
            Button nowBtn = new Button(this);
            nowBtn.setText("正在播放：" + nowPlaying.title + " - " + nowPlaying.artist);
            nowBtn.setAllCaps(false);
            nowBtn.setTextColor(TEXT);
            nowBtn.setBackgroundColor(BLUE);
            nowBtn.setPadding(dp(12), dp(8), dp(12), dp(8));
            nowBtn.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            nowBtn.setSingleLine(true);
            nowBtn.setEllipsize(android.text.TextUtils.TruncateAt.END);
            nowBtn.setContentDescription("正在播放：" + nowPlaying.title + "，歌手：" + nowPlaying.artist);
            nowBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showFullPlayer(nowPlaying);
                }
            });
            searchCard.addView(nowBtn, lp(-1, dp(44), 0, 6, 0, 0));
        }
        page.addView(searchCard);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        page.addView(spacer);

        bottomArea = new LinearLayout(this);
        bottomArea.setOrientation(LinearLayout.HORIZONTAL);
        bottomArea.setGravity(Gravity.BOTTOM);
        bottomArea.setVisibility(View.GONE); // 默认不显示，播放时才显示
        albumArtView = new ImageView(this);
        albumArtView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        albumArtView.setContentDescription("专辑封面");
        albumArtView.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentIndex >= 0 && currentIndex < playlist.size())
                    showFullPlayer(playlist.get(currentIndex));
                else toast("当前没有正在播放的歌曲");
            }
        });
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(dp(120), dp(120));
        artParams.gravity = Gravity.BOTTOM;
        albumArtView.setLayoutParams(artParams);
        albumArtView.setBackgroundColor(CARD);
        bottomArea.addView(albumArtView);
        View rightSpacer = new View(this);
        rightSpacer.setLayoutParams(new LinearLayout.LayoutParams(dp(60), dp(120)));
        bottomArea.addView(rightSpacer);

        playerCtrl = card();
        playerCtrl.setVisibility(View.GONE); // 默认不显示，播放时才显示
        seekBar = new SeekBar(this);
        seekBar.setContentDescription("进度条");
        seekBar.setMax(1000);
        bindSeekBar(seekBar);
        playerCtrl.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout timeRow = row();
        currentTimeView = small("00:00");
        durationView = small("--:--");
        timeRow.addView(currentTimeView, new LinearLayout.LayoutParams(0, -2, 1));
        durationView.setGravity(Gravity.RIGHT);
        timeRow.addView(durationView, new LinearLayout.LayoutParams(0, -2, 1));
        playerCtrl.addView(timeRow);
        LinearLayout controls = row();
        controls.addView(controlButton("上一首", new View.OnClickListener() {
            @Override public void onClick(View v) { previousTrack(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        controls.addView(controlButton("播放/暂停", new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlayback(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        controls.addView(controlButton("下一首", new View.OnClickListener() {
            @Override public void onClick(View v) { nextTrack(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        playerCtrl.addView(controls);
        playerCtrl.addView(pillButton("播放详情 / 歌词", new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentIndex >= 0 && currentIndex < playlist.size()) showFullPlayer(playlist.get(currentIndex));
                else toast("当前没有正在播放的歌曲");
            }
        }), lp(-1, -2, 0, 10, 0, 0));
        page.addView(bottomArea);
        page.addView(playerCtrl);
        return page;
    }

    private View buildPage2() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        LinearLayout moreCard = card();
        TextView moreTitle = text("更多功能", 20, true);
        moreCard.addView(moreTitle, lp(-1, -2, 0, 0, 0, 12));
        moreCard.addView(pillButton("播放列表", new View.OnClickListener() {
            @Override public void onClick(View v) { showPlaylistPage(); }
        }));
        moreCard.addView(pillButton("导入外部歌单", new View.OnClickListener() {
            @Override public void onClick(View v) { showImportPlaylistDialog(); }
        }));
        moreCard.addView(pillButton("手动输入歌曲 ID", new View.OnClickListener() {
            @Override public void onClick(View v) { showManualPlayDialog(); }
        }));
        moreCard.addView(pillButton("音乐设置", new View.OnClickListener() {
            @Override public void onClick(View v) { showMusicSettingsDialog(); }
        }));
        moreCard.addView(pillButton("外部导入歌单查看", new View.OnClickListener() {
            @Override public void onClick(View v) { showImportedPlaylists(); }
        }));
        page.addView(moreCard);
        return page;
    }

    private View buildPage3() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        ScrollView page3Scroll = new ScrollView(this);
        LinearLayout playlistCard = card();
        TextView playlistTitle = text("推荐歌单", 20, true);
        playlistCard.addView(playlistTitle, lp(-1, -2, 0, 0, 0, 10));
        TextView playlistHint = small("点击歌单加载歌曲，长按批量下载");
        playlistHint.setGravity(Gravity.CENTER);
        playlistCard.addView(playlistHint, lp(-1, -2, 0, 0, 0, 8));

        // 歌单数据：名称、平台、排行榜ID（QQ音乐官方排行榜，实时拉取真实歌曲）
        // 所有ID均已验证可正常返回真实歌曲数据
        final String[][] playlists = {
            {"华语热歌榜", "tx", "4"},
            {"流行指数榜", "tx", "26"},
            {"新歌榜", "tx", "27"},
            {"内地榜", "tx", "5"},
            {"港台榜", "tx", "6"},
            {"欧美榜", "tx", "3"},
            {"韩国榜", "tx", "16"},
            {"日本榜", "tx", "17"},
            {"网络歌曲榜", "tx", "28"},
            {"说唱榜", "tx", "52"},
            {"抖音热歌榜", "tx", "23"},
            {"古风榜", "tx", "65"},
            {"电音榜", "tx", "62"},
            {"粤语榜", "tx", "29"},
            {"民谣榜", "tx", "33"},
            {"摇滚榜", "tx", "35"},
            {"DJ舞曲榜", "tx", "67"},
            {"影视金曲榜", "tx", "32"},
            {"K歌金曲榜", "tx", "36"},
            {"动漫榜", "tx", "58"},
            {"经典老歌榜", "tx", "25"},
            {"轻音乐榜", "tx", "34"},
            {"游戏音乐榜", "tx", "30"},
            {"爵士蓝调榜", "tx", "59"},
            {"R&B流行榜", "tx", "60"},
            {"情歌对唱榜", "tx", "61"},
            {"原创音乐榜", "tx", "63"},
            {"国风新韵榜", "tx", "64"},
            {"综艺热歌榜", "tx", "66"},
            {"电影原声榜", "tx", "70"},
            {"年度热歌榜", "tx", "26"},
            {"全球流行榜", "tx", "3"},
        };

        // 32种主题色，用于歌单封面
        final int[] colors = {
            0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
            0xFF8BC34A, 0xFFFF9800, 0xFFFF5722, 0xFF795548, 0xFF607D8B, 0xFFE040FB, 0xFF536DFE, 0xFF448AFF,
            0xFF18FFFF, 0xFF69F0AE, 0xFFEEFF41, 0xFFFFD740, 0xFFFF6E40, 0xFFF44336, 0xFFD500F9, 0xFF2979FF,
            0xFF00E5FF, 0xFF76FF03, 0xFFFFEA00, 0xFFFFAB40, 0xFFFF5252, 0xFFC51162, 0xFFAA00FF, 0xFF2962FF,
        };

        // 2列网格布局
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        for (int row = 0; row < playlists.length; row += 2) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 2; col++) {
                int idx = row + col;
                if (idx >= playlists.length) break;
                final String[] pl = playlists[idx];
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(6), dp(6), dp(6), dp(6));
                cell.setBackgroundColor(CARD2);
                cell.setContentDescription("歌单：" + pl[0] + "，点击加载，长按批量下载");

                // 封面：用首字+彩色背景替代网络图片
                TextView coverText = new TextView(this);
                String firstChar = pl[0].length() > 0 ? pl[0].substring(0, 1) : "歌";
                coverText.setText(firstChar);
                coverText.setTextColor(0xFFFFFFFF);
                coverText.setTextSize(32);
                coverText.setGravity(Gravity.CENTER);
                android.graphics.drawable.GradientDrawable coverBg = new android.graphics.drawable.GradientDrawable();
                coverBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                coverBg.setCornerRadius(dp(12));
                coverBg.setColor(colors[idx % colors.length]);
                coverText.setBackground(coverBg);
                coverText.setContentDescription("歌单封面" + pl[0]);
                LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(140), dp(140));
                coverParams.gravity = Gravity.CENTER;
                cell.addView(coverText, coverParams);

                // 歌单名
                TextView name = small(pl[0]);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);
                name.setPadding(dp(4), dp(6), dp(4), dp(2));
                cell.addView(name, lp(dp(140), -2, 0, 0, 0, 0));

                cell.setClickable(true);
                cell.setFocusable(true);
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        loadPlaylistSongs(pl[0], pl[1], pl[2]);
                    }
                });
                cell.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        showBatchDownloadPlaylist(pl[0], pl[1], pl[2]);
                        return true;
                    }
                });
                rowLayout.addView(cell, new LinearLayout.LayoutParams(0, -2, 1));
            }
            grid.addView(rowLayout);
        }
        playlistCard.addView(grid);
        page3Scroll.addView(playlistCard);
        page.addView(page3Scroll);
        return page;
    }

    private View buildPage4() {
        // 初始化爱的空间歌曲列表
        loveTrackList.clear();
        loveTrackList.add(new LoveTrack("零距离的思念", "TINY7", "love_song.flac"));
        loveTrackList.add(new LoveTrack("爱情讯息", "郭静", "love_song_2.flac"));
        loveSeekBars.clear();

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);

        LinearLayout loveCard = card();

        // 照片放在最上面
        ImageView photoView = new ImageView(this);
        try {
            photoView.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("love_photo.jpg")));
        } catch (Exception ignored) {}
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setContentDescription("周新和王雨涵的合照");
        loveCard.addView(photoView, lp(-1, dp(280), 0, 0, 0, 16));

        // 跑马灯在照片下面
        LoveMarqueeView loveView = new LoveMarqueeView(this);
        loveView.setContentDescription("爱心跑马灯");
        loveCard.addView(loveView, lp(-1, dp(160), 0, 0, 0, 16));

        TextView readable = small("周新爱王雨涵\n王宇含爱周新");
        readable.setTextSize(20);
        readable.setGravity(Gravity.CENTER);
        readable.setFocusable(true);
        readable.setContentDescription("爱心文字");
        loveCard.addView(readable, lp(-1, -2, 0, 0, 0, 18));

        // 歌曲列表标题
        TextView songListTitle = text("♫ 爱的空间音乐盒（" + loveTrackList.size() + "首）", 16, true);
        songListTitle.setGravity(Gravity.CENTER);
        loveCard.addView(songListTitle, lp(-1, -2, 0, 0, 0, 12));

        // 每首歌一行：标题+歌手 + 进度条
        for (int i = 0; i < loveTrackList.size(); i++) {
            final int idx = i;
            final LoveTrack lt = loveTrackList.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(dp(12), dp(10), dp(12), dp(10));
            row.setBackgroundColor(CARD2);

            // 歌曲信息行
            LinearLayout infoRow = new LinearLayout(this);
            infoRow.setOrientation(LinearLayout.HORIZONTAL);
            infoRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView titleTv = text(lt.title, 16, true);
            titleTv.setSingleLine(true);
            titleTv.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1f);
            infoRow.addView(titleTv, titleLp);

            TextView artistTv = small(lt.artist);
            artistTv.setPadding(dp(8), 0, 0, 0);
            infoRow.addView(artistTv, new LinearLayout.LayoutParams(-2, -2));

            // 播放按钮
            Button playBtn = new Button(this);
            playBtn.setText("▶");
            playBtn.setTextColor(TEXT);
            playBtn.setAllCaps(false);
            playBtn.setBackgroundColor(BLUE);
            playBtn.setTextSize(14);
            playBtn.setPadding(dp(12), dp(6), dp(12), dp(6));
            playBtn.setContentDescription("播放" + lt.title);
            playBtn.setMinWidth(0);
            playBtn.setMinimumWidth(0);
            playBtn.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    playLoveTrack(idx);
                }
            });
            infoRow.addView(playBtn, new LinearLayout.LayoutParams(-2, -2));

            row.addView(infoRow, new LinearLayout.LayoutParams(-1, -2));

            // 进度条
            SeekBar bar = new SeekBar(this);
            bar.setMax(1000);
            bar.setPadding(dp(4), dp(4), dp(4), 0);
            bar.setContentDescription(lt.title + "进度");
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
                @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (currentLoveTrackIndex == idx && mediaPlayer != null) {
                        mediaPlayer.seekTo(mediaPlayer.getDuration() * seekBar.getProgress() / 1000);
                    }
                    userSeeking = false;
                }
            });
            loveSeekBars.add(bar);
            row.addView(bar, new LinearLayout.LayoutParams(-1, dp(32)));

            loveCard.addView(row, lp(-1, -2, 0, 0, 0, 6));
        }

        page.addView(loveCard);
        return page;
    }

    private void switchToPage(int pageIndex) {
        if (pageIndex < 0 || pageIndex > 3 || pageContainer == null) return;
        page1View.setVisibility(pageIndex == 0 ? View.VISIBLE : View.GONE);
        page2View.setVisibility(pageIndex == 1 ? View.VISIBLE : View.GONE);
        page3View.setVisibility(pageIndex == 2 ? View.VISIBLE : View.GONE);
        if (page4View != null) page4View.setVisibility(pageIndex == 3 ? View.VISIBLE : View.GONE);
        updatePageTabs(pageIndex);
        // 无障碍：通知页面切换
        String[] labels = {"搜索", "功能", "歌单", "爱的空间"};
        View cur = pageIndex == 0 ? page1View : pageIndex == 1 ? page2View : pageIndex == 2 ? page3View : page4View;
        if (cur != null) {
            cur.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            cur.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED);
        }
    }

    private void updatePageTabs(int pageIndex) {
        if (pageIndex < 0 || pageIndex > 3) return;
        currentPageIndex = pageIndex;
        if (pageTabViews == null) return;
        String[] labels = {"搜索", "功能", "歌单", "爱的空间"};
        for (int i = 0; i < pageTabViews.length; i++) {
            if (i == pageIndex) {
                pageTabViews[i].setTextColor(TEXT);
            } else {
                pageTabViews[i].setTextColor(MUTED);
            }
            android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
            circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            circle.setColor(i == pageIndex ? BLUE : CARD);
            circle.setStroke(dp(1), i == pageIndex ? BLUE : MUTED);
            pageTabViews[i].setBackground(circle);
            pageTabViews[i].setContentDescription(labels[i] + "标签");
        }
    }

    private void loadPlaylistSongs(String name, String source, String topId) {
        toast("正在加载歌单：" + name);
        playlist.clear();
        addedFromPlaylist = true;
        playlist.add(new Track("", "正在加载歌单 " + name + "...", "请稍候", "", source, ""));
        showPlaylistPage();
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<Track> tracks = resolver.fetchToplist(topId);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            playlist.clear();
                            playlist.addAll(tracks);
                            showPlaylistPage();
                            toast("歌单加载完成：" + name + "（" + playlist.size() + "首）");
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            playlist.clear();
                            toast("歌单加载失败：" + name + "（" + e.getMessage() + "）");
                        }
                    });
                }
            }
        });
    }

    private void showBatchDownloadPlaylist(String name, final String source, final String topId) {
        showQualityPicker(new QualityPickerCallback() {
            @Override public void onQualityPicked(final String quality, final String label) {
                toast("正在获取歌单...");
                worker.execute(new Runnable() {
                    @Override public void run() {
                        try {
                            final List<Track> tracks = resolver.fetchToplist(topId);
                            final boolean[] checked = new boolean[tracks.size()];
                            for (int i = 0; i < checked.length; i++) checked[i] = true;
                            runOnUiThread(new Runnable() {
                                @Override public void run() {
                                    final LinearLayout box = new LinearLayout(MainActivity.this);
                                    box.setOrientation(LinearLayout.VERTICAL);
                                    box.setPadding(dp(12), dp(4), dp(12), dp(4));
                                    final ScrollView scroll = new ScrollView(MainActivity.this);
                                    for (int i = 0; i < tracks.size(); i++) {
                                        final int idx = i;
                                        final Track t = tracks.get(i);
                                        final String artist = t.artist == null || t.artist.length() == 0 ? "未知歌手" : t.artist;
                                        TextView tv = new TextView(MainActivity.this);
                                        tv.setText("☑  " + t.title + "  -  " + artist);
                                        tv.setTextColor(GREEN);
                                        tv.setTextSize(16);
                                        tv.setPadding(dp(6), dp(10), dp(6), dp(10));
                                        tv.setFocusable(true);
                                        tv.setClickable(true);
                                        tv.setContentDescription(t.title + "，" + artist + "，已选中");
                                        tv.setOnClickListener(new View.OnClickListener() {
                                            @Override public void onClick(View v) {
                                                checked[idx] = !checked[idx];
                                                TextView tv = (TextView) v;
                                                tv.setText((checked[idx] ? "☑  " : "☐  ") + t.title + "  -  " + artist);
                                                tv.setTextColor(checked[idx] ? GREEN : MUTED);
                                                v.setContentDescription(t.title + "，" + artist + "，" + (checked[idx] ? "已选中" : "未选中"));
                                            }
                                        });
                                        box.addView(tv);
                                    }
                                    scroll.addView(box);
                                    FrameLayout bottomBar = new FrameLayout(MainActivity.this);
                                    bottomBar.setPadding(dp(16), dp(12), dp(16), dp(12));
                                    Button confirmBtn = new Button(MainActivity.this);
                                    confirmBtn.setText("确定");
                                    confirmBtn.setTextColor(TEXT);
                                    confirmBtn.setBackgroundColor(BLUE);
                                    confirmBtn.setAllCaps(false);
                                    confirmBtn.setTextSize(16);
                                    confirmBtn.setPadding(dp(24), dp(8), dp(24), dp(8));
                                    confirmBtn.setContentDescription("确定批量下载");
                                    FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                                    bottomBar.addView(confirmBtn, btnParams);
                                    LinearLayout root = new LinearLayout(MainActivity.this);
                                    root.setOrientation(LinearLayout.VERTICAL);
                                    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
                                    root.addView(bottomBar);
                                    final android.app.AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("勾选歌曲：" + name + "（" + tracks.size() + "首）- " + label)
                                        .setView(root)
                                        .setCancelable(true)
                                        .create();
                                    confirmBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override public void onClick(View v) {
                                            dialog.dismiss();
                                            int count = 0;
                                            for (int i = 0; i < checked.length; i++) {
                                                if (checked[i]) { downloadTrack(tracks.get(i), quality, label); count++; }
                                            }
                                            toast("开始批量下载 " + count + " 首歌曲（" + label + "）");
                                        }
                                    });
                                    dialog.show();
                                }
                            });
                        } catch (Exception e) {
                            runOnUiThread(new Runnable() {
                                @Override public void run() { toast("获取歌单失败：" + e.getMessage()); }
                            });
                        }
                    }
                });
            }
        });
    }

    private void showBatchDownloadPlaylist() {
        final List<Track> tracks = new ArrayList<Track>(playlist);
        showQualityPicker(new QualityPickerCallback() {
            @Override public void onQualityPicked(final String quality, final String label) {
                final boolean[] checked = new boolean[tracks.size()];
                for (int i = 0; i < checked.length; i++) checked[i] = true;
                final LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(dp(12), dp(4), dp(12), dp(4));
                final ScrollView scroll = new ScrollView(MainActivity.this);
                for (int i = 0; i < tracks.size(); i++) {
                    final int idx = i;
                    final Track t = tracks.get(i);
                    final String artist = t.artist == null || t.artist.length() == 0 ? "未知歌手" : t.artist;
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText("☑  " + t.title + "  -  " + artist);
                    tv.setTextColor(GREEN);
                    tv.setTextSize(16);
                    tv.setPadding(dp(6), dp(10), dp(6), dp(10));
                    tv.setFocusable(true);
                    tv.setClickable(true);
                    tv.setContentDescription(t.title + "，" + artist + "，已选中");
                    tv.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            checked[idx] = !checked[idx];
                            TextView tv = (TextView) v;
                            tv.setText((checked[idx] ? "☑  " : "☐  ") + t.title + "  -  " + artist);
                            tv.setTextColor(checked[idx] ? GREEN : MUTED);
                            v.setContentDescription(t.title + "，" + artist + "，" + (checked[idx] ? "已选中" : "未选中"));
                        }
                    });
                    box.addView(tv);
                }
                scroll.addView(box);
                FrameLayout bottomBar = new FrameLayout(MainActivity.this);
                bottomBar.setPadding(dp(16), dp(12), dp(16), dp(12));
                Button confirmBtn = new Button(MainActivity.this);
                confirmBtn.setText("确定");
                confirmBtn.setTextColor(TEXT);
                confirmBtn.setBackgroundColor(BLUE);
                confirmBtn.setAllCaps(false);
                confirmBtn.setTextSize(16);
                confirmBtn.setPadding(dp(24), dp(8), dp(24), dp(8));
                confirmBtn.setContentDescription("确定批量下载");
                FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                bottomBar.addView(confirmBtn, btnParams);
                LinearLayout root = new LinearLayout(MainActivity.this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
                root.addView(bottomBar);
                final android.app.AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("勾选歌曲（" + tracks.size() + "首）- " + label)
                    .setView(root)
                    .setCancelable(true)
                    .create();
                confirmBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                        int count = 0;
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) { downloadTrack(tracks.get(i), quality, label); count++; }
                        }
                        toast("开始批量下载 " + count + " 首歌曲（" + label + "）");
                    }
                });
                dialog.show();
            }
        });
    }

    private void showBatchDownloadSearch(final List<SearchResult> results) {
        showQualityPicker(new QualityPickerCallback() {
            @Override public void onQualityPicked(final String quality, final String label) {
                final boolean[] checked = new boolean[results.size()];
                for (int i = 0; i < checked.length; i++) checked[i] = true;
                // 构建勾选列表
                final LinearLayout box = new LinearLayout(MainActivity.this);
                box.setOrientation(LinearLayout.VERTICAL);
                box.setPadding(dp(12), dp(4), dp(12), dp(4));
                final ScrollView scroll = new ScrollView(MainActivity.this);
                for (int i = 0; i < results.size(); i++) {
                    final int idx = i;
                    final SearchResult sr = results.get(i);
                    final String artist = sr.artist == null || sr.artist.length() == 0 ? "未知歌手" : sr.artist;
                    TextView tv = new TextView(MainActivity.this);
                    tv.setText("☑  " + sr.name + "  -  " + artist);
                    tv.setTextColor(GREEN);
                    tv.setTextSize(16);
                    tv.setPadding(dp(6), dp(10), dp(6), dp(10));
                    tv.setFocusable(true);
                    tv.setClickable(true);
                    tv.setContentDescription(sr.name + "，" + artist + "，已选中，点击可取消选中");
                    tv.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            checked[idx] = !checked[idx];
                            TextView tv = (TextView) v;
                            tv.setText((checked[idx] ? "☑  " : "☐  ") + sr.name + "  -  " + artist);
                            tv.setTextColor(checked[idx] ? GREEN : MUTED);
                            v.setContentDescription(sr.name + "，" + artist + "，" + (checked[idx] ? "已选中，点击可取消选中" : "未选中，点击可选中"));
                        }
                    });
                    box.addView(tv);
                }
                scroll.addView(box);
                // 底部确定按钮容器
                FrameLayout bottomBar = new FrameLayout(MainActivity.this);
                bottomBar.setPadding(dp(16), dp(12), dp(16), dp(12));
                Button confirmBtn = new Button(MainActivity.this);
                confirmBtn.setText("确定");
                confirmBtn.setTextColor(TEXT);
                confirmBtn.setBackgroundColor(BLUE);
                confirmBtn.setAllCaps(false);
                confirmBtn.setTextSize(16);
                confirmBtn.setPadding(dp(24), dp(8), dp(24), dp(8));
                confirmBtn.setContentDescription("确定批量下载");
                FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(-2, -2, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                bottomBar.addView(confirmBtn, btnParams);
                // 整体布局
                LinearLayout root = new LinearLayout(MainActivity.this);
                root.setOrientation(LinearLayout.VERTICAL);
                root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
                root.addView(bottomBar);
                final android.app.AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("勾选歌曲（" + results.size() + "首）- " + label)
                    .setView(root)
                    .setCancelable(true)
                    .create();
                confirmBtn.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        dialog.dismiss();
                        int count = 0;
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) {
                                downloadTrack(results.get(i).toTrack(), quality, label);
                                count++;
                            }
                        }
                        toast("开始批量下载 " + count + " 首歌曲（" + label + "）");
                    }
                });
                dialog.show();
            }
        });
    }

    private interface QualityPickerCallback {
        void onQualityPicked(String quality, String label);
    }

    private void showQualityPicker(final QualityPickerCallback callback) {
        final String[] qualities = {"flac", "320k", "192k", "128k"};
        final String[] labels = {"无损 FLAC", "高品质 320k", "标准 192k", "低品质 128k"};
        new AlertDialog.Builder(this)
            .setTitle("选择下载音质")
            .setItems(labels, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    callback.onQualityPicked(qualities[which], labels[which]);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showPlaylistPage() {
        content.setTag("playlist");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("播放列表");

        LinearLayout card = card();
        if (playlist.isEmpty()) {
            TextView empty = text("播放列表为空\n可通过搜索结果添加歌曲", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(130)));
        } else {
            card.addView(pillButton("批量下载（" + playlist.size() + "首）", new View.OnClickListener() {
                @Override public void onClick(View v) { showBatchDownloadPlaylist(); }
            }));
            for (int i = 0; i < playlist.size(); i++) {
                final int index = i;
                card.addView(trackRow(playlist.get(i), new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        Track track = playlist.get(index);
                        if (isCurrentTrack(track)) showFullPlayer(track);
                        else playPlaylistTrack(index);
                    }
                }));
            }
        }
        content.addView(card);
    }

    private void showSearchDialog() {
        final String[] names = {"全网搜索", "QQ音乐", "网易云音乐", "酷狗音乐", "酷我音乐", "咪咕音乐", "喜马拉雅", "哔哩哔哩", "抖音"};
        final String[] engNames = {"All Source", "QQ Music", "Netease", "Kugou", "Kuwo", "Migu", "Ximalaya", "Bilibili", "Douyin"};
        final String[] keys = {"all", "tx", "wy", "kg", "kw", "mg", "xm", "bl", "dy"};

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(16), dp(12), dp(16), dp(12));

        // 顶部提示
        TextView tip = text("想听什么音乐？", 20, true);
        tip.setGravity(Gravity.CENTER);
        container.addView(tip, lp(-1, -2, 0, 0, 0, 16));

        // 输入框（没有搜索按钮，只有完成键关闭键盘）
        final EditText input = new EditText(this);
        input.setHint("输入歌名或歌手");
        input.setSingleLine(true);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackgroundColor(CARD2);
        input.setPadding(dp(16), dp(14), dp(16), dp(14));
        input.setTextSize(17);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setContentDescription("歌名输入框");
        // 恢复上次输入的内容
        if (lastSearchKeyword.length() > 0) {
            input.setText(lastSearchKeyword);
            input.setSelection(lastSearchKeyword.length());
        }
        // 点击完成键只关闭键盘，不搜索
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE ||
                    (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    return true;
                }
                return false;
            }
        });
        container.addView(input, lp(-1, dp(54), 0, 0, 0, 20));

        // 分割线 + 提示
        TextView sourceTip = text("选择音源搜索", 14, false);
        sourceTip.setTextColor(MUTED);
        sourceTip.setGravity(Gravity.CENTER);
        container.addView(sourceTip, lp(-1, -2, 0, 0, 0, 12));

        // 音源网格（两列）
        final AlertDialog[] dialogHolder = new AlertDialog[1];
        int cols = 2;
        int rows = (int) Math.ceil((double) names.length / cols);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < cols; c++) {
                final int idx = r * cols + c;
                if (idx >= names.length) {
                    View spacer = new View(this);
                    row.addView(spacer, new LinearLayout.LayoutParams(0, dp(76), 1f));
                    continue;
                }
                LinearLayout cell = new LinearLayout(this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(8), dp(10), dp(8), dp(10));
                cell.setBackgroundColor(CARD2);
                cell.setClickable(true);
                cell.setFocusable(true);
                cell.setContentDescription(names[idx] + "，" + engNames[idx]);

                TextView cn = new TextView(this);
                cn.setText(names[idx]);
                cn.setTextColor(TEXT);
                cn.setTextSize(16);
                cn.setGravity(Gravity.CENTER);
                cn.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                cell.addView(cn);

                TextView en = new TextView(this);
                en.setText(engNames[idx]);
                en.setTextColor(MUTED);
                en.setTextSize(11);
                en.setGravity(Gravity.CENTER);
                cell.addView(en);

                cell.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String keyword = input.getText().toString().trim();
                        if (keyword.length() == 0) {
                            toast("请先输入歌名");
                            input.requestFocus();
                            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            return;
                        }
                        lastSearchKeyword = keyword;
                        dialogHolder[0].dismiss();
                        if ("bl".equals(keys[idx])) {
                            showBilibiliTypeDialog(keyword);
                        } else if ("dy".equals(keys[idx])) {
                            showDouyinTypeDialog(keyword);
                        } else {
                            toast("正在搜索：" + keyword + "（" + names[idx] + "）");
                            searchInside(keys[idx], keyword);
                        }
                    }
                });

                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dp(76), 1f);
                cp.setMargins(dp(4), dp(4), dp(4), dp(4));
                row.addView(cell, cp);
            }
            container.addView(row);
        }

        dialogHolder[0] = new AlertDialog.Builder(this)
            .setTitle("搜索")
            .setView(container)
            .setNegativeButton("取消", null)
            .create();
        dialogHolder[0].setCanceledOnTouchOutside(true);
        dialogHolder[0].show();
    }

    private void showXimalayaTypeDialog(final String keyword) {
        final String[] items = {"搜索单集", "搜索专辑"};
        new AlertDialog.Builder(this)
            .setTitle("喜马拉雅搜索：" + keyword)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) {
                        toast("正在搜索单集：" + keyword);
                        searchInside("xm", keyword);
                    } else {
                        toast("正在搜索专辑：" + keyword);
                        searchXimalayaAlbums(keyword);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showBilibiliTypeDialog(final String keyword) {
        final String[] items = {"搜索视频", "搜索音频", "搜索番剧全集", "搜索用户"};
        new AlertDialog.Builder(this)
            .setTitle("哔哩哔哩搜索：" + keyword)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) {
                        toast("正在搜索视频：" + keyword);
                        searchInside("blv", keyword);
                    } else if (which == 1) {
                        toast("正在搜索音频：" + keyword);
                        searchInside("bla", keyword);
                    } else if (which == 2) {
                        toast("正在搜索番剧全集：" + keyword);
                        searchBilibiliBangumi(keyword);
                    } else {
                        toast("正在搜索用户：" + keyword);
                        searchBilibiliUserForCollection(keyword);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showDouyinTypeDialog(final String keyword) {
        final String[] items = {"搜索视频", "粘贴视频链接"};
        new AlertDialog.Builder(this)
            .setTitle("抖音：" + keyword)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) {
                        toast("正在搜索视频：" + keyword);
                        searchDouyinVideo(keyword);
                    } else {
                        // 弹出输入框让用户粘贴抖音视频分享链接
                        showDouyinVideoLinkDialog();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /** 弹出输入框让用户粘贴抖音视频分享链接 */
    private void showDouyinVideoLinkDialog() {
        final EditText input = new EditText(this);
        input.setHint("粘贴抖音视频分享链接");
        input.setMinLines(1);
        input.setMaxLines(3);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setPadding(dp(20), dp(12), dp(20), dp(12));
        // 尝试从剪贴板获取内容作为默认值
        try {
            Object cmObj = getSystemService(CLIPBOARD_SERVICE);
            if (cmObj instanceof android.content.ClipboardManager) {
                android.content.ClipboardManager cm = (android.content.ClipboardManager) cmObj;
                if (cm.hasPrimaryClip()) {
                    String clip = cm.getPrimaryClip().getItemAt(0).getText().toString().trim();
                    if (clip != null && clip.length() > 0) {
                        // 如果剪贴板内容看起来像抖音链接，自动填入
                        if (clip.contains("douyin.com") || clip.contains("iesdouyin.com")) {
                            input.setText(clip);
                            input.setSelection(clip.length());
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        new AlertDialog.Builder(this)
            .setTitle("解析抖音视频")
            .setMessage("粘贴抖音分享链接，支持：\n1. App分享的短链接（https://v.douyin.com/...）\n2. 网页版视频链接\n3. 直接复制分享的整段文字")
            .setView(input)
            .setPositiveButton("确定", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    String text = input.getText().toString().trim();
                    if (text.length() == 0) {
                        toast("请粘贴抖音视频分享链接");
                        return;
                    }
                    toast("正在解析视频链接...");
                    parseDouyinVideoLink(text);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void searchBilibiliVideoPages(final String bvid) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> pages = resolver.getBilibiliVideoPages(bvid);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (pages.isEmpty()) {
                                toast("未找到分P信息，请检查BV号是否正确");
                                return;
                            }
                            playlist.clear();
                            for (SearchResult sr : pages) {
                                playlist.add(sr.toTrack());
                            }
                            savePlaylist();
                            currentIndex = 0;
                            playPlaylistTrack(0);
                            toast("已加载 " + pages.size() + " 个分P");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载分P失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    /** 哔哩哔哩：搜索番剧/影视全集 */
    private void searchBilibiliBangumi(final String keyword) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    // 先检查是否是BV号，如果是直接获取分P
                    String cleanKeyword = keyword.trim();
                    if (cleanKeyword.startsWith("BV") || cleanKeyword.startsWith("bv") || cleanKeyword.contains("bilibili.com/video/")) {
                        String bvid = cleanKeyword;
                        if (bvid.contains("bilibili.com/video/")) {
                            int idx = bvid.indexOf("BV");
                            if (idx < 0) idx = bvid.indexOf("bv");
                            if (idx >= 0) {
                                bvid = bvid.substring(idx);
                                int end = bvid.indexOf("?");
                                if (end > 0) bvid = bvid.substring(0, end);
                                end = bvid.indexOf("/");
                                if (end > 0) bvid = bvid.substring(0, end);
                            }
                        }
                        final List<SearchResult> pages = resolver.getBilibiliVideoPages(bvid);
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                loading(false);
                                if (pages.isEmpty()) {
                                    toast("未找到视频信息");
                                    return;
                                }
                                playlist.clear();
                                for (SearchResult sr : pages) {
                                    playlist.add(sr.toTrack());
                                }
                                savePlaylist();
                                currentIndex = 0;
                                playPlaylistTrack(0);
                                toast("已加载 " + pages.size() + " 个视频");
                            }
                        });
                        return;
                    }
                    // 搜索番剧和影视
                    final List<SearchResult> bangumiList = resolver.searchBilibiliBangumi(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (bangumiList.isEmpty()) {
                                toast("未找到相关番剧/影视：" + keyword);
                                return;
                            }
                            showBilibiliBangumiPicker(bangumiList, keyword);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("搜索番剧失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void showBilibiliBangumiPicker(final List<SearchResult> bangumiList, final String keyword) {
        String[] items = new String[bangumiList.size()];
        for (int i = 0; i < bangumiList.size(); i++) {
            SearchResult item = bangumiList.get(i);
            items[i] = item.name + "\n" + item.artist + (item.album != null && item.album.length() > 0 ? " · " + item.album : "");
        }
        new AlertDialog.Builder(this)
            .setTitle("选择番剧/影视：" + keyword)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    SearchResult selected = bangumiList.get(which);
                    loadBilibiliBangumiEpisodes(selected.id, selected.name, selected.coverUrl, selected.type);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void loadBilibiliBangumiEpisodes(final String mediaId, final String title, final String cover, final String mediaType) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> episodes = resolver.getBilibiliBangumiEpisodes(mediaId, title, cover, mediaType);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (episodes.isEmpty()) {
                                toast("该番剧暂无剧集");
                                return;
                            }
                            playlist.clear();
                            for (SearchResult sr : episodes) {
                                playlist.add(sr.toTrack());
                            }
                            savePlaylist();
                            currentIndex = 0;
                            playPlaylistTrack(0);
                            toast("已加载 " + title + " 的 " + episodes.size() + " 集");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载剧集失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    /** 抖音：搜索视频 */
    private void searchDouyinVideo(final String keyword) {
        // 先判断是否是抖音链接或视频ID
        String trimmed = keyword.trim();
        if (trimmed.contains("douyin.com") || trimmed.contains("iesdouyin.com") || trimmed.matches("\\d{15,}")) {
            parseDouyinVideoLink(trimmed);
            return;
        }
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> results = resolver.searchDouyinVideos(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (results.isEmpty()) {
                                toast("未找到相关视频：" + keyword);
                                return;
                            }
                            // 像哔哩哔哩一样展示搜索结果列表
                            showSourceSearchResults(results);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("搜索视频失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    /** 抖音：搜索音频（抖音BGM） */
    private void searchDouyinAudio(final String keyword) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> results = resolver.searchDouyinMusic(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (results.isEmpty()) {
                                toast("未找到相关抖音音乐：" + keyword);
                                return;
                            }
                            // 展示搜索结果列表
                            showSourceSearchResults(results);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("搜索音乐失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    /** 抖音：搜索用户 */
    private void searchDouyinUser(final String keyword) {
        String trimmed = keyword.trim();
        if (trimmed.contains("douyin.com/user/") || trimmed.contains("iesdouyin.com/user/")) {
            toast("正在解析用户主页...");
            return;
        }
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> users = resolver.searchDouyinUsers(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (users.isEmpty()) {
                                toast("未找到相关用户：" + keyword);
                                return;
                            }
                            // 展示用户列表，点击后加载该用户视频
                            showDouyinUserPicker(users, keyword);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("搜索用户失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void showDouyinUserPicker(final List<SearchResult> users, final String keyword) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("用户搜索结果");
        LinearLayout card = card();
        TextView tip = small("关键词：" + keyword + "，找到 " + users.size() + " 位用户，点击查看视频");
        card.addView(tip, lp(-1, -2, 0, 0, 0, 8));
        for (int i = 0; i < users.size(); i++) {
            final SearchResult user = users.get(i);
            final boolean isBl = "bl".equals(user.source);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));
            row.setBackgroundColor(CARD);
            // 头像/封面
            ImageView avatar = new ImageView(this);
            avatar.setImageResource(android.R.drawable.ic_menu_gallery);
            avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            avatar.setBackgroundColor(CARD2);
            loadCover(user.coverUrl, avatar);
            row.addView(avatar, new LinearLayout.LayoutParams(dp(44), dp(44)));
            // 文本信息
            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setPadding(dp(10), 0, 0, 0);
            TextView name = text(user.name, 15, true);
            name.setSingleLine(true);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            String sourceLabel = isBl ? "[B站] " : "[抖音] ";
            String subText = sourceLabel + (user.artist != null ? user.artist : "");
            if (user.album != null && user.album.length() > 0) subText += " | " + user.album;
            TextView desc = small(subText);
            desc.setSingleLine(true);
            desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
            textCol.addView(name, new LinearLayout.LayoutParams(-1, -2));
            textCol.addView(desc, new LinearLayout.LayoutParams(-1, -2));
            row.addView(textCol, new LinearLayout.LayoutParams(0, -2, 1f));
            row.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (isBl) {
                        // B站用户
                        loadBilibiliUserCollection(user.id, user.name);
                    } else if (user.id != null && user.id.length() > 5 && user.id.startsWith("MS4w")) {
                        // 抖音用户，有有效sec_uid
                        loadDouyinUserVideos(user.id, user.name);
                    } else {
                        // 没有有效sec_uid，直接搜索该用户名的视频
                        toast("正在搜索 " + user.name + " 的视频...");
                        searchDouyinVideo(user.name);
                    }
                }
            });
            card.addView(row, lp(-1, -2, 0, 0, 0, 2));
        }
        content.addView(card);
    }

    private void loadDouyinUserVideos(final String secUid, final String userName) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> videos = resolver.getDouyinUserVideos(secUid, userName);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (videos.isEmpty()) {
                                toast("该用户暂无公开视频");
                                return;
                            }
                            // 展示视频列表
                            showSourceSearchResults(videos);
                            toast("已加载 " + userName + " 的 " + videos.size() + " 个视频");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载用户视频失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    /** 抖音：解析视频链接（支持短链和完整链接） */
    private void parseDouyinVideoLink(final String linkText) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final SearchResult result = resolver.parseDouyinVideo(linkText);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (result == null) {
                                toast("无法解析该抖音链接，请检查链接是否正确");
                                return;
                            }
                            playlist.clear();
                            playlist.add(result.toTrack());
                            savePlaylist();
                            currentIndex = 0;
                            playPlaylistTrack(0);
                            toast("已加载：" + result.name);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("解析抖音失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void searchBilibiliUserForCollection(final String keyword) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> users = resolver.searchBilibiliUsers(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (users.isEmpty()) {
                                toast("未找到用户：" + keyword);
                                return;
                            }
                            showBilibiliUserPicker(users, keyword);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("用户搜索失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void showBilibiliUserPicker(final List<SearchResult> users, final String keyword) {
        String[] items = new String[users.size()];
        for (int i = 0; i < users.size(); i++) {
            items[i] = users.get(i).name + "\n" + users.get(i).artist;
        }
        new AlertDialog.Builder(this)
            .setTitle("选择用户：" + keyword)
            .setItems(items, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    SearchResult user = users.get(which);
                    loadBilibiliUserCollection(user.id, user.name);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /** 加载作者主页视频列表（B站mid/抖音sec_uid） */
    private void loadAuthorVideos(final Track track) {
        final String authorId = track.authorId;
        final String authorName = track.artist == null || track.artist.length() == 0 ? "未知作者" : track.artist;
        if (authorId == null || authorId.length() == 0) {
            toast("没有作者信息");
            return;
        }
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    List<SearchResult> videos;
                    if ("bl".equals(track.source)) {
                        // B站：用mid获取用户所有视频
                        videos = resolver.getBilibiliUserVideos(authorId);
                    } else if ("dy".equals(track.source)) {
                        // 抖音：用sec_uid获取用户所有视频
                        videos = resolver.getDouyinUserVideos(authorId, authorName);
                    } else {
                        throw new Exception("不支持该平台");
                    }
                    if (videos.isEmpty()) {
                        throw new Exception("该作者暂无视频");
                    }
                    final List<SearchResult> finalVideos = videos;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            // 保存返回动作
                            final Runnable savedBackAction = previousPageAction;
                            previousPageAction = new Runnable() {
                                @Override public void run() {
                                    previousPageAction = savedBackAction;
                                    if (currentFullPlayerTrack != null) {
                                        showFullPlayer(currentFullPlayerTrack);
                                    } else {
                                        showMainPage();
                                    }
                                }
                            };
                            // 显示作者视频列表
                            content.setTag("search");
                            content.removeAllViews();
                            updateHeartVisibility();
                            addTopBackButton("作者：" + authorName);
                            LinearLayout card = card();
                            card.addView(pillButton("批量下载（" + finalVideos.size() + "首）", new View.OnClickListener() {
                                @Override public void onClick(View v) { showBatchDownloadSearch(finalVideos); }
                            }));
                            for (int i = 0; i < finalVideos.size(); i++) {
                                final int index = i;
                                card.addView(trackRow(finalVideos.get(i).toTrack(), new View.OnClickListener() {
                                    @Override public void onClick(View v) {
                                        Track t = finalVideos.get(index).toTrack();
                                        if (isCurrentTrack(t)) {
                                            showFullPlayer(t);
                                        } else {
                                            playlist.clear();
                                            for (SearchResult sr : finalVideos) {
                                                playlist.add(sr.toTrack());
                                            }
                                            savePlaylist();
                                            currentIndex = index;
                                            playPlaylistTrack(currentIndex);
                                        }
                                    }
                                }));
                            }
                            content.addView(card);
                            toast("已加载 " + authorName + " 的 " + finalVideos.size() + " 个视频");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载作者视频失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void loadBilibiliUserCollection(final String mid, final String userName) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> videos = resolver.getBilibiliUserVideos(mid);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (videos.isEmpty()) {
                                toast("该用户暂无视频");
                                return;
                            }
                            playlist.clear();
                            for (SearchResult sr : videos) {
                                playlist.add(sr.toTrack());
                            }
                            savePlaylist();
                            currentIndex = 0;
                            playPlaylistTrack(0);
                            toast("已加载 " + userName + " 的 " + videos.size() + " 个视频");
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载视频失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void searchXimalayaAlbums(final String keyword) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final org.json.JSONArray albums = resolver.searchXimalayaAlbums(keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            showXimalayaAlbumResults(albums, keyword);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("专辑搜索失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void searchInside(final String source, final String keyword) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> results = resolver.search(source, keyword);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            showSourceSearchResults(results);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            showSearchError(source, keyword, error.getMessage() == null ? "搜索失败" : error.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void showSearchError(String source, String keyword, String message) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("搜索结果");
        LinearLayout card = card();
        TextView title = text("没有搜索到歌曲", 18, true);
        title.setGravity(Gravity.CENTER);
        TextView detail = small("平台：" + sourceName(source) + "\n关键词：" + keyword + "\n原因：" + message + "\n\n建议换一个平台或关键词再试。");
        detail.setGravity(Gravity.CENTER);
        card.addView(title, lp(-1, -2, 0, 10, 0, 8));
        card.addView(detail, lp(-1, -2, 0, 0, 0, 10));
        card.addView(pillButton("重新搜索", new View.OnClickListener() {
            @Override public void onClick(View v) { showSearchDialog(); }
        }));
        content.addView(card);
    }

    private void showSourceSearchResults(List<SearchResult> results) {
        lastSearchResults.clear();
        lastSearchResults.addAll(results);
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("搜索结果");
        LinearLayout card = card();
        if (results.isEmpty()) {
            TextView empty = text("没有搜索到结果", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else {
            card.addView(pillButton("批量下载（" + results.size() + "首）", new View.OnClickListener() {
                @Override public void onClick(View v) { showBatchDownloadSearch(results); }
            }));
            for (int i = 0; i < results.size(); i++) {
                final int index = i;
                final SearchResult sr = results.get(i);
                card.addView(trackRow(sr.toTrack(), new View.OnClickListener() {
                    @Override public void onClick(View v) { playOrOpenSearchResult(index); }
                }));
                // 视频结果：在行下方添加作者主页入口
                if (("bl".equals(sr.source) || "dy".equals(sr.source))
                        && sr.authorId != null && sr.authorId.length() > 0) {
                    final String authorId = sr.authorId;
                    final String authorName = sr.artist == null || sr.artist.length() == 0 ? "未知作者" : sr.artist;
                    LinearLayout authorRow = row();
                    authorRow.setPadding(dp(10), 0, dp(10), dp(8));
                    authorRow.setBackgroundColor(CARD2);
                    TextView authorText = small("作者：" + authorName);
                    authorText.setTextColor(0xFFFF6B6B);
                    authorText.setPadding(dp(10), dp(6), dp(10), dp(6));
                    authorText.setClickable(true);
                    authorText.setFocusable(true);
                    authorText.setContentDescription("查看作者 " + authorName + " 的主页");
                    authorText.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            Track t = sr.toTrack();
                            t.authorId = authorId;
                            loadAuthorVideos(t);
                        }
                    });
                    authorRow.addView(authorText, new LinearLayout.LayoutParams(-2, -2));
                    card.addView(authorRow, lp(-1, -2, 0, 0, 0, 4));
                }
            }
        }
        content.addView(card);
    }

    private void showXimalayaAlbumResults(final org.json.JSONArray albums, final String keyword) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("专辑搜索结果");
        LinearLayout card = card();
        if (albums == null || albums.length() == 0) {
            TextView empty = text("没有搜索到专辑", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else {
            TextView tip = small("关键词：" + keyword + "，共 " + albums.length() + " 张专辑");
            card.addView(tip, lp(-1, -2, 0, 0, 0, 8));
            for (int i = 0; i < albums.length(); i++) {
                try {
                    final org.json.JSONObject album = albums.getJSONObject(i);
                    final String albumId = album.optString("id", "");
                    final String title = album.optString("title", "未知专辑");
                    final String nickname = album.optString("nickname", "未知主播");
                    String coverRaw = album.optString("cover_path", album.optString("coverUrl", ""));
                    if (coverRaw.startsWith("//")) coverRaw = "https:" + coverRaw;
                    final String cover = coverRaw;
                    final int trackCount = album.optInt("tracks", album.optInt("trackCount", album.optInt("tracksCount", album.optInt("track_count", 0))));

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(dp(8), dp(8), dp(8), dp(8));
                    row.setBackgroundColor(CARD2);
                    row.setClickable(true);
                    row.setFocusable(true);

                    ImageView iv = new ImageView(this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    int size = dp(64);
                    LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(size, size);
                    ivLp.setMargins(0, 0, dp(12), 0);
                    row.addView(iv, ivLp);
                    if (cover.length() > 0) {
                        loadCover(cover, iv);
                    }

                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setGravity(Gravity.CENTER_VERTICAL);

                    TextView tvTitle = text(title, 15, true);
                    tvTitle.setMaxLines(2);
                    tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    info.addView(tvTitle);

                    TextView tvInfo = small(nickname + " · " + trackCount + " 集");
                    info.addView(tvInfo);

                    row.addView(info, new LinearLayout.LayoutParams(0, -1, 1f));

                    row.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            toast("正在加载专辑：" + title);
                            loadAlbumTracks(albumId, title, nickname);
                        }
                    });

                    card.addView(row, lp(-1, -2, 0, 0, 0, 8));
                } catch (Exception e) {}
            }
        }
        content.addView(card);
    }

    private void loadAlbumTracks(final String albumId, final String albumTitle, final String albumArtist) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final java.util.List<SearchResult> tracks = resolver.getXimalayaAlbumTracks(albumId, albumTitle, albumArtist);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            showAlbumTracks(tracks, albumTitle, albumArtist);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载专辑失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void showAlbumTracks(java.util.List<SearchResult> tracks, String albumTitle, String albumArtist) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("专辑：" + albumTitle);
        LinearLayout card = card();
        if (tracks.isEmpty()) {
            TextView empty = text("该专辑暂无单集", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else {
            int playableCount = 0;
            for (SearchResult sr : tracks) {
                if (sr.playUrl != null && sr.playUrl.length() > 0) playableCount++;
            }
            String tipText = "共 " + tracks.size() + " 集";
            if (playableCount < tracks.size()) {
                tipText += "，其中 " + playableCount + " 集可免费播放";
            }
            TextView tip = small(tipText);
            card.addView(tip, lp(-1, -2, 0, 0, 0, 8));
            for (int i = 0; i < tracks.size(); i++) {
                final int index = i;
                final SearchResult sr = tracks.get(i);
                final boolean canPlay = sr.playUrl != null && sr.playUrl.length() > 0;
                Track track = sr.toTrack();
                // fallback的tracks可能artist不对，用原始专辑播讲人覆盖
                if (albumArtist != null && albumArtist.length() > 0 && !canPlay) {
                    track.artist = albumArtist;
                }
                LinearLayout row = trackRow(track, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!canPlay) {
                            toast("该单集为付费内容，暂不可播放");
                            return;
                        }
                        playFromAlbumTracks(tracks, index);
                    }
                });
                // 对付费不可播的单集添加标记
                if (!canPlay) {
                    TextView paidMark = text(" [付费]", 14, false);
                    paidMark.setTextColor(0xFFFFA500);
                    // 在row的第二个子view（meta）后面添加标记
                    if (row.getChildCount() >= 2) {
                        LinearLayout metaRow = new LinearLayout(this);
                        metaRow.setOrientation(LinearLayout.HORIZONTAL);
                        // 移除原来的meta
                        View oldMeta = row.getChildAt(1);
                        row.removeViewAt(1);
                        metaRow.addView(oldMeta);
                        metaRow.addView(paidMark);
                        row.addView(metaRow, lp(-1, -2, 0, 6, 0, 0));
                    }
                }
                card.addView(row);
            }
        }
        content.addView(card);
    }

    private void playFromAlbumTracks(java.util.List<SearchResult> results, int index) {
        // 过滤掉没有播放链接的，避免加入播放列表
        java.util.List<SearchResult> playable = new java.util.ArrayList<SearchResult>();
        for (SearchResult r : results) {
            if (r.playUrl != null && r.playUrl.length() > 0) {
                playable.add(r);
            }
        }
        if (playable.isEmpty()) {
            toast("没有可播放的单集");
            return;
        }
        // 重新计算index
        SearchResult target = results.get(index);
        int newIndex = 0;
        for (int i = 0; i < playable.size(); i++) {
            if (playable.get(i).id.equals(target.id)) {
                newIndex = i;
                break;
            }
        }
        playlist.clear();
        for (SearchResult r : playable) {
            playlist.add(r.toTrack());
        }
        savePlaylist();
        currentIndex = newIndex;
        playPlaylistTrack(currentIndex);
    }

    // 显示喜马拉雅专辑详情页
    private void showXimalayaAlbumDetailPage(final String albumId, final String albumTitle) {
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final org.json.JSONObject detail = resolver.getXimalayaAlbumDetail(albumId);
                    final String author = detail.optString("author", detail.optString("nickname", ""));
                    final java.util.List<SearchResult> tracks = resolver.getXimalayaAlbumTracks(albumId, albumTitle, author);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            renderXimalayaAlbumDetailPage(albumId, albumTitle, detail, tracks, author);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast("加载专辑详情失败：" + (error.getMessage() == null ? "未知错误" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    private void renderXimalayaAlbumDetailPage(String albumId, String albumTitle, org.json.JSONObject detail, final java.util.List<SearchResult> tracks, String albumArtist) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("专辑详情");
        LinearLayout card = card();

        // 解析专辑详情
        String title = detail.optString("title", albumTitle);
        String nickname = detail.optString("nickname", "未知主播");
        String intro = detail.optString("intro", detail.optString("description", detail.optString("detailRichIntro", "暂无介绍")));
        int trackCount = detail.optInt("tracks", detail.optInt("trackCount", detail.optInt("tracksCount", tracks.size())));
        String coverRaw = detail.optString("cover_path", detail.optString("coverUrl", detail.optString("cover", "")));
        if (coverRaw.startsWith("//")) coverRaw = "https:" + coverRaw;
        final String cover = coverRaw;
        String author = (albumArtist != null && albumArtist.length() > 0) ? albumArtist : nickname;

        // 专辑封面
        if (cover.length() > 0) {
            ImageView coverView = new ImageView(this);
            coverView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            coverView.setBackgroundColor(CARD2);
            card.addView(coverView, lp(-1, dp(200), 0, 0, 0, 12));
            loadCover(cover, coverView);
        }

        // 专辑标题
        TextView tvTitle = text(title, 20, true);
        tvTitle.setGravity(Gravity.CENTER);
        card.addView(tvTitle, lp(-1, -2, 0, 0, 0, 8));

        // 作者/播讲者
        TextView tvAuthor = small("作者/播讲：" + author);
        tvAuthor.setGravity(Gravity.CENTER);
        card.addView(tvAuthor, lp(-1, -2, 0, 0, 0, 4));

        // 集数
        int playableCount = 0;
        for (SearchResult sr : tracks) {
            if (sr.playUrl != null && sr.playUrl.length() > 0) playableCount++;
        }
        String countText = "共 " + trackCount + " 集";
        if (playableCount < tracks.size()) {
            countText += "，其中 " + playableCount + " 集可免费播放";
        }
        TextView tvCount = small(countText);
        tvCount.setGravity(Gravity.CENTER);
        card.addView(tvCount, lp(-1, -2, 0, 0, 0, 8));

        // 批量下载按钮（放在专辑介绍之前，确保醒目）
        if (!tracks.isEmpty()) {
            card.addView(pillButton("批量下载（" + tracks.size() + "集）", new View.OnClickListener() {
                @Override public void onClick(View v) {
                    showBatchDownloadSearch(tracks);
                }
            }), lp(-1, -2, 0, 0, 0, 12));
        }

        // 专辑介绍
        if (intro.length() > 0) {
            TextView tvIntro = small(intro);
            tvIntro.setLineSpacing(0, 1.3f);
            card.addView(tvIntro, lp(-1, -2, 0, 0, 0, 12));
        }

        // 单集列表
        if (tracks.isEmpty()) {
            TextView empty = text("该专辑暂无单集", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else {
            TextView tip = small("单集列表：");
            card.addView(tip, lp(-1, -2, 0, 0, 0, 8));
            for (int i = 0; i < tracks.size(); i++) {
                final int index = i;
                final SearchResult sr = tracks.get(i);
                final boolean canPlay = sr.playUrl != null && sr.playUrl.length() > 0;
                Track track = sr.toTrack();
                // fallback的tracks可能artist不对，用原始专辑播讲人覆盖
                if (!canPlay && author != null && author.length() > 0) {
                    track.artist = author;
                }
                LinearLayout row = trackRow(track, new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        if (!canPlay) {
                            toast("该单集为付费内容，暂不可播放");
                            return;
                        }
                        playFromAlbumTracks(tracks, index);
                    }
                });
                // 长按进入详情页
                row.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override public boolean onLongClick(View v) {
                        showFullPlayer(sr.toTrack());
                        return true;
                    }
                });
                // 对付费不可播的单集添加标记
                if (!canPlay) {
                    TextView paidMark = text(" [付费]", 14, false);
                    paidMark.setTextColor(0xFFFFA500);
                    if (row.getChildCount() >= 2) {
                        LinearLayout metaRow = new LinearLayout(this);
                        metaRow.setOrientation(LinearLayout.HORIZONTAL);
                        View oldMeta = row.getChildAt(1);
                        row.removeViewAt(1);
                        metaRow.addView(oldMeta);
                        metaRow.addView(paidMark);
                        row.addView(metaRow, lp(-1, -2, 0, 6, 0, 0));
                    }
                }
                card.addView(row);
            }
        }
        content.addView(card);
    }

    private void showImportPlaylistDialog() {
        final EditText input = new EditText(this);
        input.setHint("粘贴歌单链接或分享文字");
        input.setSingleLine(false);
        input.setMinLines(3);
        input.setTextColor(TEXT);
        input.setHintTextColor(MUTED);
        input.setBackgroundColor(CARD2);
        input.setPadding(dp(12), dp(10), dp(12), dp(10));
        input.setContentDescription("歌单链接输入框");
        new AlertDialog.Builder(this)
            .setTitle("导入外部歌单")
            .setMessage("支持：QQ音乐、网易云音乐、酷狗、酷我、咪咕、喜马拉雅、哔哩哔哩\n粘贴歌单链接或分享口令即可自动识别平台")
            .setView(input)
            .setPositiveButton("解析歌单", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    String text = input.getText().toString().trim();
                    if (text.length() == 0) { toast("请粘贴链接或分享文字"); return; }
                    parseUniversalPlaylist(text);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /** 自动识别平台并解析歌单 */
    private void parseUniversalPlaylist(String rawText) {
        // 检测平台
        String platform = null;
        String playlistId = null;
        if (rawText.contains("music.163.com") || rawText.contains("163cn.tv") || rawText.contains("netease")) {
            platform = "wy";
        } else if (rawText.contains("kugou.com") || rawText.contains("kugou") || rawText.contains("kg")) {
            platform = "kg";
        } else if (rawText.contains("kuwo.cn") || rawText.contains("kuwo")) {
            platform = "kw";
        } else if (rawText.contains("migu.cn") || rawText.contains("migu") || rawText.contains("migumusic")) {
            platform = "mg";
        } else {
            platform = "tx"; // 默认 QQ 音乐
        }
        // 提取 ID
        String[] patterns = {
            "playlist/(\\d+)",
            "playlist\\?id=(\\d+)",
            "taogeid=(\\d+)",
            "id=(\\d+)",
            "/(\\d{5,})",
            "album/(\\d+)",
            "playlist_detail/(\\d+)",
            "pid=(\\d+)",
            "globalId=(\\d+)",
        };
        for (String p : patterns) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(p).matcher(rawText);
            if (m.find()) { playlistId = m.group(1); break; }
        }
        if (playlistId == null) {
            String digits = rawText.replaceAll("[^0-9]", "");
            if (digits.length() >= 5) playlistId = digits;
        }
        if (playlistId == null) {
            toast("未能识别歌单 ID，请确认链接格式正确");
            return;
        }
        final String pid = playlistId;
        final String plat = platform;
        toast("正在解析" + platformName(plat) + "歌单...");
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final List<SearchResult> results = new ArrayList<SearchResult>();
                    String name = "外部歌单";
                    if ("wy".equals(plat)) {
                        name = parseNetEasePlaylist(pid, results);
                    } else if ("kg".equals(plat)) {
                        name = parseKugouPlaylist(pid, results);
                    } else if ("kw".equals(plat)) {
                        name = parseKuwoPlaylist(pid, results);
                    } else if ("mg".equals(plat)) {
                        name = parseMiguPlaylist(pid, results);
                    } else {
                        name = parseTXPlaylist(pid, results);
                    }
                    final String finalName = name;
                    runOnUiThread(new Runnable() {
                        @Override public void run() { showImportResults(finalName, results); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { toast("解析失败：" + e.getMessage()); }
                    });
                }
            }
        });
    }

    private String platformName(String p) {
        if ("wy".equals(p)) return "网易云音乐";
        if ("kg".equals(p)) return "酷狗";
        if ("kw".equals(p)) return "酷我";
        if ("mg".equals(p)) return "咪咕";
        return "QQ音乐";
    }

    /* ====== QQ 音乐歌单解析 ====== */
    private String parseTXPlaylist(String pid, List<SearchResult> results) throws Exception {
        String url = "https://c.y.qq.com/v8/fcg-bin/fcg_v8_playlist_cp.fcg?format=json&id=" + pid + "&type=1";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://y.qq.com/");
        String body = readStream(conn.getInputStream()); conn.disconnect();
        org.json.JSONObject json = new org.json.JSONObject(body);
        if (json.optInt("code", -1) != 0) throw new Exception("QQ音乐歌单解析失败，错误码：" + json.optInt("code"));
        org.json.JSONArray cdlist = json.optJSONObject("data").optJSONArray("cdlist");
        if (cdlist == null || cdlist.length() == 0) throw new Exception("歌单为空");
        org.json.JSONObject cd = cdlist.getJSONObject(0);
        String name = cd.optString("dissname", "QQ音乐歌单");
        org.json.JSONArray songlist = cd.optJSONArray("songlist");
        if (songlist == null) throw new Exception("歌单中没有歌曲");
        for (int i = 0; i < songlist.length(); i++) {
            org.json.JSONObject song = songlist.getJSONObject(i);
            SearchResult sr = new SearchResult();
            sr.id = song.optString("songmid", song.optString("mid", ""));
            sr.name = song.optString("songname", song.optString("name", "未知歌曲"));
            sr.artist = joinSingers(song.optJSONArray("singer"));
            sr.album = song.optString("albumname", song.optString("album", ""));
            sr.source = "tx";
            String albummid = song.optString("albummid", "");
            sr.coverUrl = albummid.length() > 0 ? "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg" : "";
            results.add(sr);
        }
        return name;
    }

    /* ====== 网易云音乐歌单解析 ====== */
    private String parseNetEasePlaylist(String pid, List<SearchResult> results) throws Exception {
        // 主方案：直接调用网易云API
        String url = "https://music.163.com/api/playlist/detail?id=" + pid;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://music.163.com/");
        String body = readStream(conn.getInputStream()); conn.disconnect();
        org.json.JSONObject json = new org.json.JSONObject(body);
        if (json.optInt("code", -1) == 200) {
            org.json.JSONObject result = json.optJSONObject("result");
            if (result != null) {
                String name = result.optString("name", "网易云歌单");
                org.json.JSONArray tracks = result.optJSONArray("tracks");
                if (tracks != null && tracks.length() > 0) {
                    for (int i = 0; i < tracks.length(); i++) {
                        org.json.JSONObject song = tracks.getJSONObject(i);
                        SearchResult sr = new SearchResult();
                        sr.id = String.valueOf(song.optInt("id", 0));
                        sr.name = song.optString("name", "未知歌曲");
                        sr.artist = joinSingers(song.optJSONArray("ar"));
                        sr.album = song.optJSONObject("al") != null ? song.optJSONObject("al").optString("name", "") : "";
                        sr.source = "wy";
                        String picUrl = song.optJSONObject("al") != null ? song.optJSONObject("al").optString("picUrl", "") : "";
                        sr.coverUrl = picUrl;
                        results.add(sr);
                    }
                    return name;
                }
            }
        }
        // Fallback：使用 meting 第三方API
        String metingUrl = "https://api.injahow.cn/meting/?type=playlist&id=" + pid;
        java.net.HttpURLConnection metingConn = (java.net.HttpURLConnection) new java.net.URL(metingUrl).openConnection();
        metingConn.setConnectTimeout(10000); metingConn.setReadTimeout(10000);
        metingConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        String metingBody = readStream(metingConn.getInputStream()); metingConn.disconnect();
        org.json.JSONArray metingArr = new org.json.JSONArray(metingBody);
        if (metingArr.length() == 0) throw new Exception("网易云歌单解析失败，错误码：" + json.optInt("code", -1) + "，且第三方API无数据");
        for (int i = 0; i < metingArr.length(); i++) {
            org.json.JSONObject song = metingArr.getJSONObject(i);
            SearchResult sr = new SearchResult();
            // 从url字段提取歌曲ID，例如：https://api.injahow.cn/meting/?server=netease&type=url&id=2755496359
            String songUrl = song.optString("url", "");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("id=(\\d+)").matcher(songUrl);
            sr.id = m.find() ? m.group(1) : "";
            sr.name = song.optString("name", "未知歌曲");
            sr.artist = song.optString("artist", "未知歌手");
            sr.album = "";
            sr.source = "wy";
            sr.coverUrl = song.optString("pic", "");
            results.add(sr);
        }
        return "网易云歌单";
    }

    /* ====== 酷狗歌单解析 ====== */
    private String parseKugouPlaylist(String pid, List<SearchResult> results) throws Exception {
        String url = "https://m.kugou.com/api/playlist/detail?id=" + pid;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://m.kugou.com/");
        conn.setRequestProperty("Cookie", "kg_mid=1");
        String body = readStream(conn.getInputStream()); conn.disconnect();
        org.json.JSONObject json = new org.json.JSONObject(body);
        if (json.optInt("status", -1) != 1) throw new Exception("酷狗歌单解析失败");
        org.json.JSONObject data = json.optJSONObject("data");
        if (data == null) throw new Exception("歌单为空");
        String name = data.optString("name", "酷狗歌单");
        org.json.JSONArray list = data.optJSONArray("list");
        if (list == null) list = data.optJSONArray("musicList");
        if (list == null) throw new Exception("歌单中没有歌曲");
        for (int i = 0; i < list.length(); i++) {
            org.json.JSONObject song = list.getJSONObject(i);
            SearchResult sr = new SearchResult();
            sr.id = song.optString("hash", song.optString("Hash", ""));
            sr.name = song.optString("name", song.optString("songname", "未知歌曲"));
            sr.artist = song.optString("singer", song.optString("singername", "未知歌手"));
            sr.album = song.optString("album", song.optString("albumname", ""));
            sr.source = "kg";
            sr.coverUrl = song.optString("cover", song.optString("coverUrl", ""));
            results.add(sr);
        }
        return name;
    }

    /* ====== 酷我歌单解析 ====== */
    private String parseKuwoPlaylist(String pid, List<SearchResult> results) throws Exception {
        String url = "https://www.kuwo.cn/api/www/playlist/playListInfo?pid=" + pid + "&pn=1&rn=200&httpsStatus=1";
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://www.kuwo.cn/");
        conn.setRequestProperty("csrf", "1");
        conn.setRequestProperty("Cookie", "kw_token=1");
        String body = readStream(conn.getInputStream()); conn.disconnect();
        org.json.JSONObject json = new org.json.JSONObject(body);
        if (json.optInt("code", -1) != 200) throw new Exception("酷我歌单解析失败，错误码：" + json.optInt("code"));
        org.json.JSONObject data = json.optJSONObject("data");
        if (data == null) throw new Exception("歌单为空");
        String name = data.optString("name", "酷我歌单");
        org.json.JSONArray list = data.optJSONArray("musicList");
        if (list == null) throw new Exception("歌单中没有歌曲");
        for (int i = 0; i < list.length(); i++) {
            org.json.JSONObject song = list.getJSONObject(i);
            SearchResult sr = new SearchResult();
            sr.id = String.valueOf(song.optInt("rid", 0));
            sr.name = song.optString("name", "未知歌曲");
            sr.artist = song.optString("artist", "未知歌手");
            sr.album = song.optString("album", "");
            sr.source = "kw";
            sr.coverUrl = song.optString("pic", song.optString("albumpic", ""));
            results.add(sr);
        }
        return name;
    }

    /* ====== 咪咕歌单解析 ====== */
    private String parseMiguPlaylist(String pid, List<SearchResult> results) throws Exception {
        String url = "https://c.musicapp.migu.cn/MIGUM2.0/v1.0/content/resourceinfo.do?resourceType=2021&resourceId=" + pid;
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
        conn.setConnectTimeout(10000); conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://m.music.migu.cn/");
        String body = readStream(conn.getInputStream()); conn.disconnect();
        org.json.JSONObject json = new org.json.JSONObject(body);
        if (!"000000".equals(json.optString("code"))) throw new Exception("咪咕歌单解析失败");
        org.json.JSONArray resource = json.optJSONArray("resource");
        if (resource == null || resource.length() == 0) throw new Exception("歌单为空");
        org.json.JSONObject res = resource.getJSONObject(0);
        String name = res.optString("title", "咪咕歌单");
        org.json.JSONArray list = res.optJSONArray("songList");
        if (list == null) throw new Exception("歌单中没有歌曲");
        for (int i = 0; i < list.length(); i++) {
            org.json.JSONObject song = list.getJSONObject(i);
            SearchResult sr = new SearchResult();
            sr.id = song.optString("copyrightId", song.optString("contentId", ""));
            sr.name = song.optString("songName", song.optString("name", "未知歌曲"));
            StringBuilder sb = new StringBuilder();
            org.json.JSONArray singers = song.optJSONArray("singerName");
            if (singers != null) {
                for (int j = 0; j < singers.length(); j++) {
                    if (j > 0) sb.append("/");
                    sb.append(singers.optString(j, ""));
                }
            }
            if (sb.length() == 0) sb.append(song.optString("singerName", "未知歌手"));
            sr.artist = sb.toString();
            sr.album = song.optString("albumName", song.optString("album", ""));
            sr.source = "mg";
            sr.coverUrl = song.optString("albumImg", song.optString("coverUrl", ""));
            results.add(sr);
        }
        return name;
    }

    private String joinSingers(org.json.JSONArray singers) {
        if (singers == null || singers.length() == 0) return "未知歌手";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < singers.length(); i++) {
            if (i > 0) sb.append("/");
            org.json.JSONObject s = singers.optJSONObject(i);
            if (s != null) sb.append(s.optString("name", ""));
        }
        return sb.length() == 0 ? "未知歌手" : sb.toString();
    }

    private String readStream(java.io.InputStream stream) throws Exception {
        if (stream == null) return "";
        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read;
        while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);
        reader.close();
        return builder.toString();
    }

    private void showImportResults(final String playlistName, final List<SearchResult> songs) {
        final boolean[] checked = new boolean[songs.size()];
        for (int i = 0; i < checked.length; i++) checked[i] = true;
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(4), dp(8), dp(4));
        ScrollView scroll = new ScrollView(this);
        for (int i = 0; i < songs.size(); i++) {
            final int idx = i;
            final SearchResult sr = songs.get(i);
            TextView tv = small("☑ " + sr.name + " - " + sr.artist);
            tv.setPadding(dp(4), dp(6), dp(4), dp(6));
            tv.setContentDescription(sr.name + "，" + sr.artist + "，已选中");
            tv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    checked[idx] = !checked[idx];
                    ((TextView) v).setText((checked[idx] ? "☑ " : "☐ ") + sr.name + " - " + sr.artist);
                    v.setContentDescription(sr.name + "，" + sr.artist + "，" + (checked[idx] ? "已选中" : "未选中"));
                }
            });
            box.addView(tv);
        }
        scroll.addView(box);
        new AlertDialog.Builder(this)
            .setTitle("导入：" + playlistName + "（" + songs.size() + "首）")
            .setView(scroll)
            .setPositiveButton("全选导入", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int which) {
                    int count = 0;
                    java.util.List<Track> importedTracks = new java.util.ArrayList<Track>();
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            Track t = songs.get(i).toTrack();
                            playlist.add(t);
                            importedTracks.add(t);
                            count++;
                        }
                    }
                    savePlaylist();
                    saveImportedPlaylist(playlistName, importedTracks);
                    toast("已导入 " + count + " 首歌曲");
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void saveImportedPlaylist(String name, java.util.List<Track> tracks) {
        try {
            org.json.JSONArray saved = new org.json.JSONArray(prefs.getString(PREF_IMPORTED_PLAYLISTS, "[]"));
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("name", name);
            obj.put("time", System.currentTimeMillis());
            org.json.JSONArray arr = new org.json.JSONArray();
            for (Track t : tracks) arr.put(t.toJson());
            obj.put("tracks", arr);
            saved.put(obj);
            prefs.edit().putString(PREF_IMPORTED_PLAYLISTS, saved.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void showImportedPlaylists() {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("外部导入歌单");
        LinearLayout card = card();
        try {
            org.json.JSONArray saved = new org.json.JSONArray(prefs.getString(PREF_IMPORTED_PLAYLISTS, "[]"));
            if (saved.length() == 0) {
                TextView empty = text("暂无导入的歌单", 16, false);
                empty.setGravity(Gravity.CENTER);
                card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
            } else {
                for (int i = saved.length() - 1; i >= 0; i--) {
                    final int idx = i;
                    org.json.JSONObject obj = saved.getJSONObject(i);
                    final String name = obj.optString("name", "未知歌单");
                    final org.json.JSONArray tracks = obj.optJSONArray("tracks");
                    int count = tracks == null ? 0 : tracks.length();
                    long time = obj.optLong("time", 0);
                    String timeStr = "";
                    if (time > 0) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault());
                        timeStr = sdf.format(new java.util.Date(time));
                    }

                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setPadding(dp(12), dp(12), dp(12), dp(12));
                    row.setBackgroundColor(CARD2);
                    row.setClickable(true);
                    row.setFocusable(true);

                    LinearLayout info = new LinearLayout(this);
                    info.setOrientation(LinearLayout.VERTICAL);
                    info.setGravity(Gravity.CENTER_VERTICAL);
                    TextView tvName = text(name, 16, true);
                    tvName.setMaxLines(1);
                    tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    info.addView(tvName);
                    TextView tvInfo = small(count + " 首 · " + timeStr);
                    info.addView(tvInfo);
                    row.addView(info, new LinearLayout.LayoutParams(0, -1, 1f));

                    row.setOnClickListener(new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            showImportedPlaylistSongs(name, tracks);
                        }
                    });

                    card.addView(row, lp(-1, -2, 0, 0, 0, 8));
                }
            }
        } catch (Exception e) {
            TextView empty = text("读取失败", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        }
        content.addView(card);
    }

    private void showImportedPlaylistSongs(String name, final org.json.JSONArray tracks) {
        content.setTag("search");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton(name);
        LinearLayout card = card();
        if (tracks == null || tracks.length() == 0) {
            TextView empty = text("该歌单没有歌曲", 16, false);
            empty.setGravity(Gravity.CENTER);
            card.addView(empty, new LinearLayout.LayoutParams(-1, dp(120)));
        } else {
            TextView tip = small("共 " + tracks.length() + " 首");
            card.addView(tip, lp(-1, -2, 0, 0, 0, 8));
            // 批量下载按钮
            card.addView(pillButton("批量下载（" + tracks.length() + "首）", new View.OnClickListener() {
                @Override public void onClick(View v) {
                    java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
                    for (int i = 0; i < tracks.length(); i++) {
                        try {
                            Track t = Track.fromJson(tracks.getJSONObject(i));
                            SearchResult sr = new SearchResult();
                            sr.id = t.id;
                            sr.name = t.title;
                            sr.artist = t.artist;
                            sr.album = t.album;
                            sr.source = t.source;
                            sr.coverUrl = t.coverUrl;
                            results.add(sr);
                        } catch (Exception ignored) {}
                    }
                    if (results.isEmpty()) {
                        toast("没有可下载的歌曲");
                        return;
                    }
                    showBatchDownloadSearch(results);
                }
            }));
            for (int i = 0; i < tracks.length(); i++) {
                try {
                    final int index = i;
                    final Track track = Track.fromJson(tracks.getJSONObject(i));
                    View row = trackRow(track, new View.OnClickListener() {
                        @Override public void onClick(View v) {
                            // 将歌单歌曲加入播放列表并播放
                            playlist.clear();
                            for (int j = 0; j < tracks.length(); j++) {
                                try {
                                    playlist.add(Track.fromJson(tracks.getJSONObject(j)));
                                } catch (Exception ignored) {}
                            }
                            savePlaylist();
                            currentIndex = index;
                            playPlaylistTrack(currentIndex);
                        }
                    });
                    // 长按下载
                    row.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override public boolean onLongClick(View v) {
                            showQualityPicker(new QualityPickerCallback() {
                                @Override public void onQualityPicked(String quality, String label) {
                                    downloadTrack(track, quality, label);
                                    toast("开始下载：" + track.title + "（" + label + "）");
                                }
                            });
                            return true;
                        }
                    });
                    card.addView(row);
                } catch (Exception ignored) {}
            }
        }
        content.addView(card);
    }

    private void showManualPlayDialog() {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(8), dp(18), dp(4));
        final EditText input = new EditText(this);
        input.setHint("歌曲 ID / hash / songmid");
        input.setContentDescription("歌曲ID输入框");
        final Spinner source = new Spinner(this);
        source.setContentDescription("选择歌曲 ID 所属平台");
        source.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item,
            new String[]{"网易云", "QQ音乐", "酷我", "酷狗", "咪咕"}));
        box.addView(input);
        box.addView(source);
        new AlertDialog.Builder(this)
            .setTitle("手动播放")
            .setView(box)
            .setNegativeButton("取消", null)
            .setPositiveButton("播放", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    String id = input.getText().toString().trim();
                    if (id.length() == 0) return;
                    Track track = new Track(id, "歌曲 " + id, "手动输入", "", platformKey(source.getSelectedItemPosition()));
                    addTrack(track);
                    playPlaylistTrack(playlist.size() - 1);
                }
            }).show();
    }

    private void showMusicSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("音乐设置")
            .setItems(new String[]{"歌曲设置（魔音）", "自定义均衡器", "复制当前播放链接", "清空播放列表", "字幕过渡效果", "歌词颜色", "歌词校准", "倍速", "音色", "关于"}, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) showVoiceEffectDialog();
                    if (which == 1) showCustomEqualizerDialog();
                    if (which == 2) copyCurrentUrl();
                    if (which == 3) {
                        playlist.clear();
                        currentIndex = -1;
                        savePlaylist();
                        toast("播放列表已清空");
                    }
                    if (which == 4) showSubtitleEffectDialog();
                    if (which == 5) showLyricColorDialog();
                    if (which == 6) showLyricOffsetDialog();
                    if (which == 7) showSpeedDialog();
                    if (which == 8) showTimbreDialog();
                    if (which == 9) toast("听含新宇 · 聚合音源版");
                }
            }).show();
    }

    private void showCustomEqualizerDialog() {
        final short numBands;
        final short minLevel;
        final short maxLevel;
        if (equalizer != null) {
            numBands = equalizer.getNumberOfBands();
            minLevel = equalizer.getBandLevelRange()[0];
            maxLevel = equalizer.getBandLevelRange()[1];
        } else {
            numBands = 7;
            minLevel = -1500;
            maxLevel = 1500;
        }
        // 各频段的说明
        final String[] bandLabels = {"超低音", "低音", "中低音", "中音", "中高音", "高音", "超高音"};
        final String[] bandDescs = {"60Hz 重低音", "230Hz 低音", "910Hz 中低音", "3.6kHz 中音", "5kHz 中高音", "10kHz 高音", "14kHz 超高音"};

        // 读取存储的EQ值
        final short[] storedLevels = new short[numBands];
        String saved = prefs.getString(PREF_EQ_BANDS, "");
        String[] parts = saved.length() > 0 ? saved.split(",") : new String[0];
        for (int i = 0; i < numBands; i++) {
            if (i < parts.length && parts[i].length() > 0) {
                try { storedLevels[i] = Short.parseShort(parts[i]); } catch (Exception e) { storedLevels[i] = (short)0; }
            } else if (equalizer != null) {
                try { storedLevels[i] = (short)equalizer.getBandLevel((short)i); } catch (Exception e) { storedLevels[i] = (short)0; }
            } else {
                storedLevels[i] = (short)0;
            }
        }

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(8), dp(8), dp(8));

        for (short i = 0; i < numBands; i++) {
            final int idx = i;
            String label;
            String desc;
            if (i < bandLabels.length) {
                label = bandLabels[i];
                desc = bandDescs[i];
            } else {
                int freq = equalizer != null ? equalizer.getCenterFreq(i) / 1000 : (i * 2000 + 60);
                label = freq + "Hz";
                desc = freq + "Hz";
            }

            TextView bandLabel = small(label + "（" + desc + "）");
            bandLabel.setPadding(0, dp(4), 0, dp(2));
            box.addView(bandLabel);

            SeekBar bar = new SeekBar(this);
            bar.setMax(maxLevel - minLevel);
            bar.setProgress(storedLevels[i] - minLevel);
            bar.setContentDescription(label + "，" + desc);
            bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        short level = (short) (progress + minLevel);
                        storedLevels[idx] = level;
                        // 保存到prefs
                        StringBuilder sb = new StringBuilder();
                        for (int j = 0; j < numBands; j++) {
                            if (j > 0) sb.append(",");
                            sb.append(storedLevels[j]);
                        }
                        prefs.edit().putString(PREF_EQ_BANDS, sb.toString()).apply();
                        if (equalizer != null) {
                            try { equalizer.setBandLevel((short) idx, level); } catch (Exception ignored) {}
                        }
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
            box.addView(bar);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(box);

        new AlertDialog.Builder(this)
            .setTitle("自定义均衡器")
            .setView(scroll)
            .setPositiveButton("重置", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface d, int which) {
                    prefs.edit().putString(PREF_EQ_BANDS, "").apply();
                    if (equalizer != null) {
                        try {
                            for (short i = 0; i < numBands; i++) equalizer.setBandLevel(i, (short) 0);
                        } catch (Exception ignored) {}
                    }
                    toast("均衡器已重置");
                }
            })
            .setNegativeButton("关闭", null)
            .show();
    }

    private void showPlayerMoreDialog() {
        new AlertDialog.Builder(this)
            .setTitle("更多")
            .setItems(new String[]{"歌词颜色", "歌词动画", "歌词校准", "倍速", "音色"}, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    if (which == 0) showLyricColorDialog();
                    if (which == 1) showSubtitleEffectDialog();
                    if (which == 2) showLyricOffsetDialog();
                    if (which == 3) showSpeedDialog();
                    if (which == 4) showTimbreDialog();
                }
            }).show();
    }

    private void showSubtitleEffectDialog() {
        final String[] effects = {"无动画", "淡入", "上滑弹入", "缩放弹入", "逐句高亮", "左滑进入", "右滑进入", "轻微翻转", "呼吸放大"};
        new AlertDialog.Builder(this)
            .setTitle("字幕过渡效果")
            .setSingleChoiceItems(effects, subtitleEffect, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    subtitleEffect = which;
                    prefs.edit().putInt(PREF_SUBTITLE_EFFECT, subtitleEffect).apply();
                    toast("已切换为：" + effects[which]);
                    dialog.dismiss();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showLyricColorDialog() {
        final String[] names = {"白色", "粉色", "玫红", "天蓝", "深蓝", "薄荷绿", "青色", "金色", "橙色", "紫色", "红色"};
        final int[] colors = {
            Color.WHITE,
            Color.parseColor("#FF7BB8"),
            Color.parseColor("#FF2D86"),
            Color.parseColor("#64B5F6"),
            Color.parseColor("#448AFF"),
            Color.parseColor("#66E0A3"),
            Color.parseColor("#26E6DA"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#FF9F43"),
            Color.parseColor("#B388FF"),
            Color.parseColor("#FF6B6B")
        };
        new AlertDialog.Builder(this)
            .setTitle("歌词颜色")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    lyricColor = colors[which];
                    prefs.edit().putInt(PREF_LYRIC_COLOR, lyricColor).apply();
                    toast("歌词颜色：" + names[which]);
                    if (mediaPlayer != null) updateActiveLyric(mediaPlayer.getCurrentPosition());
                }
            })
            .show();
    }

    private void showLyricOffsetDialog() {
        final String[] names = {"提前 1.0 秒", "提前 0.5 秒", "正常", "延后 0.5 秒", "延后 1.0 秒", "延后 1.5 秒", "延后 2.0 秒"};
        final int[] offsets = {-1000, -500, 0, 500, 1000, 1500, 2000};
        int checked = 2;
        for (int i = 0; i < offsets.length; i++) if (offsets[i] == lyricOffsetMs) checked = i;
        new AlertDialog.Builder(this)
            .setTitle("歌词校准")
            .setSingleChoiceItems(names, checked, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    lyricOffsetMs = offsets[which];
                    prefs.edit().putInt(PREF_LYRIC_OFFSET, lyricOffsetMs).apply();
                    toast("歌词校准：" + names[which]);
                    if (mediaPlayer != null) updateActiveLyric(mediaPlayer.getCurrentPosition());
                    dialog.dismiss();
                }
            }).show();
    }

    private void showSpeedDialog() {
        final String[] names = {"0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "1.75x", "2.0x"};
        final float[] speeds = {0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        int checked = 2;
        for (int i = 0; i < speeds.length; i++) if (Math.abs(speeds[i] - playbackSpeed) < 0.01f) checked = i;
        new AlertDialog.Builder(this)
            .setTitle("倍速")
            .setSingleChoiceItems(names, checked, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    playbackSpeed = speeds[which];
                    applyPlaybackSpeed();
                    toast("倍速：" + names[which]);
                    dialog.dismiss();
                }
            }).show();
    }

    private String speedLabel() {
        return String.format(Locale.CHINA, "%.2fx", playbackSpeed).replace(".00", ".0");
    }

    private void showTimbreDialog() {
        final String[] fallback = {"默认", "正常", "流行", "摇滚", "古典", "舞曲", "低音增强", "人声增强", "爵士", "嘻哈", "重金属", "民谣", "现场", "派对", "俱乐部"};
        if (equalizer == null) {
            int checked = timbrePreset < 0 ? 0 : timbrePreset + 1;
            if (checked >= fallback.length) checked = 0;
            new AlertDialog.Builder(this)
                .setTitle("音色（需播放后生效）")
                .setSingleChoiceItems(fallback, checked, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        timbrePreset = (short) (which - 1);
                        prefs.edit().putInt(PREF_TIMBRE, timbrePreset).apply();
                        if (equalizer != null) applyTimbre();
                        toast("音色：" + fallback[which]);
                        dialog.dismiss();
                    }
                }).show();
            return;
        }
        short count = equalizer.getNumberOfPresets();
        final String[] names = new String[count + 1];
        final String[] presets = new String[count + 1];
        names[0] = "默认";
        presets[0] = "";
        for (short i = 0; i < count; i++) {
            presets[i + 1] = equalizer.getPresetName(i);
            names[i + 1] = translatePresetName(presets[i + 1]);
        }
        if (count == 0) {
            int checked = timbrePreset < 0 ? 0 : timbrePreset + 1;
            if (checked >= fallback.length) checked = 0;
            new AlertDialog.Builder(this)
                .setTitle("音色")
                .setSingleChoiceItems(fallback, checked, new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        timbrePreset = (short) (which - 1);
                        prefs.edit().putInt(PREF_TIMBRE, timbrePreset).apply();
                        if (equalizer != null) applyTimbre();
                        toast("音色：" + fallback[which]);
                        dialog.dismiss();
                    }
                }).show();
            return;
        }
        int checked = timbrePreset < 0 ? 0 : timbrePreset + 1;
        new AlertDialog.Builder(this)
            .setTitle("音色")
            .setSingleChoiceItems(names, checked, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    timbrePreset = (short) (which - 1);
                    prefs.edit().putInt(PREF_TIMBRE, timbrePreset).apply();
                    if (equalizer != null) applyTimbre();
                    toast("音色：" + names[which]);
                    dialog.dismiss();
                }
            }).show();
    }

    private String translatePresetName(String eng) {
        if (eng == null) return "未知";
        String lower = eng.toLowerCase();
        if (lower.contains("normal")) return "正常";
        if (lower.contains("classic")) return "古典";
        if (lower.contains("dance")) return "舞曲";
        if (lower.contains("flat")) return "平坦";
        if (lower.contains("folk")) return "民谣";
        if (lower.contains("heavy")) return "重金属";
        if (lower.contains("hip")) return "嘻哈";
        if (lower.contains("jazz")) return "爵士";
        if (lower.contains("pop")) return "流行";
        if (lower.contains("rock")) return "摇滚";
        if (lower.contains("bass")) return "低音增强";
        if (lower.contains("treble")) return "高音增强";
        if (lower.contains("vocal")) return "人声增强";
        if (lower.contains("loud")) return "响度增强";
        if (lower.contains("live")) return "现场";
        if (lower.contains("small")) return "小房间";
        if (lower.contains("large")) return "大厅";
        if (lower.contains("party")) return "派对";
        if (lower.contains("club")) return "俱乐部";
        if (lower.contains("concert")) return "音乐会";
        if (lower.contains("custom")) return "自定义";
        return eng; // 无法翻译则保留原文
    }

    private void showVoiceEffectDialog() {
        final String[] effects = {"默认", "机器人", "花栗鼠", "教堂", "大厅", "房间", "回声", "低音增强"};
        final String[] descs = {"还原声音", "低沉金属音", "尖锐卡通音", "教堂混响", "大厅混响", "小房间混响", "悠远回声", "重低音增强"};
        String title = (mediaPlayer == null || !mediaPlayer.isPlaying()) ? "歌曲设置（魔音，需播放后生效）" : "歌曲设置（魔音）";
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setSingleChoiceItems(effects, currentVoiceEffect, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    currentVoiceEffect = which;
                    prefs.edit().putInt(PREF_VOICE_EFFECT, which).apply();
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) applyVoiceEffect();
                    toast("魔音：" + effects[which] + " - " + descs[which]);
                    dialog.dismiss();
                }
            }).show();
    }

    private void applyVoiceEffect() {
        // 重置所有效果
        if (presetReverb != null) { try { presetReverb.setEnabled(false); } catch (Exception ignored) {} }
        if (bassBoost != null) { try { bassBoost.setEnabled(false); } catch (Exception ignored) {} }
        // 重置音高
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.media.PlaybackParams params = mediaPlayer.getPlaybackParams();
                params.setPitch(1.0f);
                params.setSpeed(playbackSpeed);
                mediaPlayer.setPlaybackParams(params);
            } catch (Exception ignored) {}
        }

        switch (currentVoiceEffect) {
            case 1: // 机器人：低音高 + 小房间混响
                applyPitch(0.6f);
                if (presetReverb != null) {
                    try { presetReverb.setPreset(PresetReverb.PRESET_SMALLROOM); presetReverb.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            case 2: // 花栗鼠：高音高
                applyPitch(1.8f);
                break;
            case 3: // 教堂
                if (presetReverb != null) {
                    try { presetReverb.setPreset(PresetReverb.PRESET_LARGEROOM); presetReverb.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            case 4: // 大厅
                if (presetReverb != null) {
                    try { presetReverb.setPreset(PresetReverb.PRESET_MEDIUMHALL); presetReverb.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            case 5: // 房间
                if (presetReverb != null) {
                    try { presetReverb.setPreset(PresetReverb.PRESET_SMALLROOM); presetReverb.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            case 6: // 回声
                if (presetReverb != null) {
                    try { presetReverb.setPreset(PresetReverb.PRESET_LARGEHALL); presetReverb.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            case 7: // 低音增强
                if (bassBoost != null) {
                    try { bassBoost.setStrength((short) 1000); bassBoost.setEnabled(true); } catch (Exception ignored) {}
                }
                break;
            default: break; // 默认：不做任何事
        }
    }

    private void applyPitch(float pitch) {
        if (mediaPlayer != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.media.PlaybackParams params = new android.media.PlaybackParams();
                params.setPitch(pitch);
                params.setSpeed(playbackSpeed);
                mediaPlayer.setPlaybackParams(params);
            } catch (Exception ignored) {}
        }
    }

    private void showLovePage() {
        content.setTag("love");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("爱心");

        LinearLayout card = card();

        // 照片放在最上面
        ImageView photoView = new ImageView(this);
        try {
            photoView.setImageBitmap(BitmapFactory.decodeStream(getAssets().open("love_photo.jpg")));
        } catch (Exception ignored) {}
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoView.setContentDescription("周新和王雨涵的合照");
        card.addView(photoView, lp(-1, dp(280), 0, 0, 0, 16));

        // 跑马灯在照片下面
        LoveMarqueeView loveView = new LoveMarqueeView(this);
        loveView.setContentDescription("爱心跑马灯");
        card.addView(loveView, lp(-1, dp(160), 0, 0, 0, 16));

        TextView readable = small("周新爱王雨涵\n王宇含爱周新");
        readable.setTextSize(20);
        readable.setGravity(Gravity.CENTER);
        readable.setFocusable(true);
        readable.setContentDescription("爱心文字");
        card.addView(readable, lp(-1, -2, 0, 0, 0, 18));

        card.addView(pillButton("播放内置歌曲：零距离的思念", new View.OnClickListener() {
            @Override public void onClick(View v) { playLoveTrack(0); }
        }));
        content.addView(card);
    }

    private void playLoveTrack(int index) {
        if (index < 0 || index >= loveTrackList.size()) return;
        currentLoveTrackIndex = index;
        final LoveTrack lt = loveTrackList.get(index);
        currentMediaTitle = lt.title;
        currentMediaArtist = lt.artist + " · 本地内置";
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        if (currentSurfaceHolder != null && currentSurfaceHolder.getSurface().isValid()) {
            try { mediaPlayer.setDisplay(currentSurfaceHolder); } catch (Exception ignored) {}
        }
        try {
            File out = new File(getCacheDir(), lt.assetFile);
            if (!out.exists() || out.length() == 0) {
                InputStream input = getAssets().open("love/" + lt.assetFile);
                FileOutputStream output = new FileOutputStream(out);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = input.read(buffer)) != -1) output.write(buffer, 0, len);
                output.close();
                input.close();
            }
            mediaPlayer.setDataSource(out.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    setupEqualizer();
                    applyPlaybackSpeed();
                    applyVoiceEffect();
                    mp.start();
                    updatePlaybackState();
                    updateLoveProgress();
                    showPlayerNotification();
                    updateHeartVisibility();
                    toast("正在播放：" + lt.title);
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) {
                    updatePlaybackState();
                    showPlayerNotification();
                    nextLoveTrack();
                }
            });
            mediaPlayer.prepareAsync();
        } catch (Exception error) {
            toast("内置歌曲播放失败：" + error.getMessage());
        }
    }

    // 随机播放下一首爱的空间歌曲
    private void nextLoveTrack() {
        if (loveTrackList.isEmpty()) return;
        if (loveTrackList.size() == 1) {
            playLoveTrack(0);
            return;
        }
        // 随机选一首，避免和当前重复
        int next;
        do {
            next = (int)(Math.random() * loveTrackList.size());
        } while (next == currentLoveTrackIndex && loveTrackList.size() > 1);
        playLoveTrack(next);
    }

    // 更新爱的空间进度条
    private void updateLoveProgress() {
        try {
            if (mediaPlayer != null && mediaPlayer.getDuration() > 0 && currentLoveTrackIndex >= 0) {
                if (!userSeeking && currentLoveTrackIndex < loveSeekBars.size() && loveSeekBars.get(currentLoveTrackIndex) != null) {
                    loveSeekBars.get(currentLoveTrackIndex).setProgress(
                        mediaPlayer.getCurrentPosition() * 1000 / mediaPlayer.getDuration());
                }
            }
        } catch (Exception ignored) {}
    }

    private void showFullPlayer(final Track track) {
        if (track == null) { toast("没有歌曲信息"); return; }
        // 确保歌名、歌手、专辑非空
        if (track.title == null || track.title.length() == 0) track.title = "未知歌曲";
        if (track.artist == null || track.artist.length() == 0) track.artist = "未知歌手";
        if (track.album == null || track.album.length() == 0) track.album = "未知专辑";
        currentFullPlayerTrack = track;
        // 保存当前页面以便返回时恢复
        Object curTag = content.getTag();
        if ("search".equals(curTag) && !lastSearchResults.isEmpty()) {
            final List<SearchResult> snapshot = new ArrayList<SearchResult>(lastSearchResults);
            previousPageAction = new Runnable() {
                @Override public void run() { showSourceSearchResults(snapshot); }
            };
        } else if ("playlist".equals(curTag)) {
            previousPageAction = new Runnable() {
                @Override public void run() { showPlaylistPage(); }
            };
        }
        content.setTag("fullPlayer");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("播放详情");

        final HorizontalScrollView pager = new HorizontalScrollView(this);
        pager.setHorizontalScrollBarEnabled(false);
        pager.setFillViewport(true);
        pager.setContentDescription("左右滑动切换播放和歌词");
        LinearLayout pages = new LinearLayout(this);
        pages.setOrientation(LinearLayout.HORIZONTAL);
        pager.addView(pages, new HorizontalScrollView.LayoutParams(-2, -1));

        LinearLayout card = card();
        pages.addView(card, new LinearLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels - dp(36), -2));
        Button moreButton = controlButton("更多", new View.OnClickListener() {
            @Override public void onClick(View v) { showPlayerMoreDialog(); }
        });
        moreButton.setContentDescription("更多");
        card.addView(moreButton, lp(-1, dp(42), 0, 0, 0, 10));
        final ImageView coverView = new ImageView(this);
        fullPlayerCoverView = coverView;
        coverView.setBackgroundColor(CARD2);
        coverView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        coverView.setContentDescription("专辑封面");
        // 哔哩哔哩/抖音视频：添加 SurfaceView 播放视频画面
        final boolean isVideoTrack = ("bl".equals(track.source) && track.id != null && (track.id.startsWith("BV") || track.id.startsWith("bv") || track.id.contains("|")))
            || ("dy".equals(track.source) && track.playUrl != null && track.playUrl.length() > 0 && !"audio".equals(track.type));
        if (isVideoTrack) {
            android.widget.FrameLayout videoContainer = new android.widget.FrameLayout(this);
            videoContainer.setLayoutParams(lp(-1, dp(260), 0, 0, 0, 16));
            videoContainer.addView(coverView, new android.widget.FrameLayout.LayoutParams(-1, -1));
            android.view.SurfaceView surfaceView = new android.view.SurfaceView(this);
            // 优化视频渲染性能
            surfaceView.setZOrderOnTop(false);
            surfaceView.getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);
            surfaceView.getHolder().addCallback(new android.view.SurfaceHolder.Callback() {
                @Override public void surfaceCreated(android.view.SurfaceHolder holder) {
                    if (holder.getSurface() != null && holder.getSurface().isValid()) {
                        currentSurfaceHolder = holder;
                        if (mediaPlayer != null) {
                            try { mediaPlayer.setDisplay(holder); } catch (Exception ignored) {}
                        }
                    }
                }
                @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) {}
                @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) {
                    currentSurfaceHolder = null;
                    // 不在这里调用 setDisplay(null)，避免切歌时旧 Surface 销毁干扰新 MediaPlayer
                }
            });
            videoContainer.addView(surfaceView, new android.widget.FrameLayout.LayoutParams(-1, -1));
            card.addView(videoContainer);
        } else {
            card.addView(coverView, lp(-1, dp(260), 0, 0, 0, 16));
        }
        loadCover(track.coverUrl, coverView);
        enrichCoverForView(track, coverView);

        TextView title = text(track.title, 24, true);
        title.setGravity(Gravity.CENTER);
        title.setContentDescription("歌曲：" + track.title);
        card.addView(title, lp(-1, -2, 0, 0, 0, 8));

        String artist = track.artist == null || track.artist.length() == 0 ? "未知歌手" : track.artist;
        String album = track.album == null || track.album.length() == 0 ? "未知专辑" : track.album;
        TextView meta = small(artist + " · " + album + " · " + sourceName(track.source));
        meta.setGravity(Gravity.CENTER);
        meta.setContentDescription("歌手：" + artist + "，专辑：" + album);
        card.addView(meta, lp(-1, -2, 0, 0, 0, 18));

        seekBar = new SeekBar(this);
        seekBar.setContentDescription("进度");
        seekBar.setMax(1000);
        seekBar.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, android.view.MotionEvent event) {
                int action = event.getAction();
                if (action == android.view.MotionEvent.ACTION_DOWN)
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                else if (action == android.view.MotionEvent.ACTION_UP || action == android.view.MotionEvent.ACTION_CANCEL)
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }
        });
        bindSeekBar(seekBar);
        card.addView(seekBar, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout timeRow = row();
        currentTimeView = small("00:00");
        durationView = small("--:--");
        timeRow.addView(currentTimeView, new LinearLayout.LayoutParams(0, -2, 1));
        durationView.setGravity(Gravity.RIGHT);
        timeRow.addView(durationView, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(timeRow, lp(-1, -2, 0, 0, 0, 14));

        LinearLayout controls = row();
        controls.addView(controlButton("上一首", new View.OnClickListener() {
            @Override public void onClick(View v) { previousTrack(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        controls.addView(controlButton("播放/暂停", new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlayback(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        controls.addView(controlButton("下一首", new View.OnClickListener() {
            @Override public void onClick(View v) { nextTrack(); }
        }), new LinearLayout.LayoutParams(0, dp(46), 1));
        card.addView(controls, lp(-1, -2, 0, 0, 0, 18));

        repeatModeButton = pillButton("播放模式：" + repeatModeLabel(), new View.OnClickListener() {
            @Override public void onClick(View v) { cycleRepeatMode(); }
        });
        repeatModeButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        repeatModeButton.setContentDescription("当前播放模式：" + repeatModeLabel() + "，点击切换");
        card.addView(repeatModeButton, lp(-1, -2, 0, 0, 0, 12));

        Button downloadButton = pillButton("下载当前歌曲", new View.OnClickListener() {
            @Override public void onClick(View v) { showDownloadQualityDialog(track); }
        });
        downloadButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        downloadButton.setContentDescription("下载当前歌曲");
        card.addView(downloadButton, lp(-1, -2, 0, 0, 0, 12));

        Button singerButton = pillButton("歌手介绍：" + artist, new View.OnClickListener() {
            @Override public void onClick(View v) { showSingerPage(track); }
        });
        singerButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        singerButton.setContentDescription("查看歌手 " + artist + " 的介绍和歌曲");
        card.addView(singerButton, lp(-1, -2, 0, 0, 0, 12));

        // 哔哩哔哩/抖音：如果有作者ID，显示作者主页按钮
        if (("bl".equals(track.source) || "dy".equals(track.source))
                && track.authorId != null && track.authorId.length() > 0) {
            Button authorButton = pillButton("作者主页：" + artist, new View.OnClickListener() {
                @Override public void onClick(View v) { loadAuthorVideos(track); }
            });
            authorButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            authorButton.setContentDescription("查看作者 " + artist + " 的主页和所有视频");
            card.addView(authorButton, lp(-1, -2, 0, 0, 0, 12));
        }

        // 喜马拉雅：如果有专辑ID，显示查看专辑按钮
        if ("xm".equals(track.source) && track.albumId != null && track.albumId.length() > 0) {
            Button albumButton = pillButton("查看专辑：" + track.album, new View.OnClickListener() {
                @Override public void onClick(View v) { showXimalayaAlbumDetailPage(track.albumId, track.album); }
            });
            albumButton.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            albumButton.setContentDescription("查看专辑 " + track.album);
            card.addView(albumButton, lp(-1, -2, 0, 0, 0, 12));
        }

        card.addView(pillButton("倍速：" + speedLabel(), new View.OnClickListener() {
            @Override public void onClick(View v) { showSpeedDialog(); }
        }), lp(-1, -2, 0, 0, 0, 12));

        TextView hint = small("往右滑查看歌词");
        hint.setGravity(Gravity.CENTER);
        hint.setContentDescription("往右滑查看歌词");
        card.addView(hint, lp(-1, -2, 0, 4, 0, 0));

        LinearLayout lyricPage = card();
        lyricPage.setContentDescription("歌词");
        pages.addView(lyricPage, new LinearLayout.LayoutParams(getResources().getDisplayMetrics().widthPixels - dp(36), -2));
        TextView lyricTitle = text("歌词", 24, true);
        lyricTitle.setGravity(Gravity.CENTER);
        lyricPage.addView(lyricTitle, lp(-1, -2, 0, 0, 0, 12));
        TextView lyricHint = small("正在播放的歌词会自动滚动到中间，不需要手动滑动。");
        lyricHint.setGravity(Gravity.CENTER);
        lyricPage.addView(lyricHint, lp(-1, -2, 0, 0, 0, 12));

        activeLyricsScroll = new ScrollView(this);
        activeLyricsScroll.setFillViewport(false);
        activeLyricsScroll.setContentDescription("歌词列表");
        final LinearLayout lyricsBox = new LinearLayout(this);
        lyricsBox.setOrientation(LinearLayout.VERTICAL);
        lyricsBox.setContentDescription("歌词");
        addLyricLines(lyricsBox, "正在加载歌词...");
        activeLyricsScroll.addView(lyricsBox, new ScrollView.LayoutParams(-1, -2));
        lyricPage.addView(activeLyricsScroll, lp(-1, dp(430), 0, 0, 0, 0));

        // 歌词底部进度条
        final LinearLayout lyricProgressRow = new LinearLayout(this);
        lyricProgressRow.setOrientation(LinearLayout.HORIZONTAL);
        lyricProgressRow.setGravity(Gravity.CENTER_VERTICAL);
        final SeekBar lyricSeekBar = new SeekBar(this);
        lyricSeekBar.setMax(1000);
        lyricSeekBar.setContentDescription("歌词进度");
        lyricSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            @Override public void onStartTrackingTouch(SeekBar seekBar) { userSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                    mediaPlayer.seekTo(mediaPlayer.getDuration() * seekBar.getProgress() / 1000);
                    updateActiveLyric(mediaPlayer.getCurrentPosition());
                }
                userSeeking = false;
            }
        });
        final TextView lyricPctView = small("0%");
        lyricPctView.setGravity(Gravity.CENTER);
        lyricPctView.setMinWidth(dp(40));
        final TextView lyricTimeView = small("00:00/00:00");
        lyricTimeView.setGravity(Gravity.CENTER);
        lyricPage.addView(lyricSeekBar, lp(-1, -2, 0, 8, 0, 0));
        lyricPage.addView(lyricProgressRow, lp(-1, -2, 0, 0, 0, 8));

        // Update lyric progress along with main progress
        final Handler lyricProgressHandler = new Handler();
        lyricProgressHandler.postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                        int pos = mediaPlayer.getCurrentPosition();
                        int dur = mediaPlayer.getDuration();
                        lyricSeekBar.setProgress(pos * 1000 / dur);
                        lyricPctView.setText((pos * 100 / dur) + "%");
                        lyricTimeView.setText(formatTime(pos) + "/" + formatTime(dur));
                    }
                } catch (Exception ignored) {}
                lyricProgressHandler.postDelayed(this, 1000);
            }
        }, 500);
        lyricProgressRow.addView(lyricPctView, new LinearLayout.LayoutParams(0, -2, 1));
        lyricProgressRow.addView(lyricTimeView, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(pager, lp(-1, -2, 0, 0, 0, 0));

        worker.execute(new Runnable() {
            @Override public void run() {
                final String lyricText = resolver.lyrics(track);
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        addLyricLines(lyricsBox, lyricText);
                        if (mediaPlayer != null) updateActiveLyric(mediaPlayer.getCurrentPosition());
                    }
                });
            }
        });
    }

    private void addLyricLines(LinearLayout lyricsBox, String lyricText) {
        lyricsBox.removeAllViews();
        activeLyricsBox = lyricsBox;
        activeLyricLines.clear();
        activeLyricViews.clear();
        activeLyricIndex = -1;
        activeLyricsTimed = false;
        String[] lines = (lyricText == null || lyricText.length() == 0 ? "暂无歌词" : lyricText).split("\\n");
        int visibleIndex = 0;
        for (String raw : lines) {
            final LyricLine parsed = parseLyricLine(raw);
            String line = parsed.text;
            if (line.length() == 0) continue;
            if (parsed.timeMs > 0) activeLyricsTimed = true;
            TextView lyricLine = small(line);
            lyricLine.setTextSize(16);
            lyricLine.setGravity(Gravity.CENTER);
            lyricLine.setFocusable(true);
            lyricLine.setContentDescription("歌词：" + line);
            lyricLine.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
            lyricLine.setPadding(dp(8), dp(7), dp(8), dp(7));
            lyricLine.setTextColor(MUTED);
            lyricLine.setAlpha(0.62f);
            lyricLine.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (mediaPlayer != null && parsed.timeMs >= 0) {
                        mediaPlayer.seekTo(parsed.timeMs);
                        updateActiveLyric(parsed.timeMs);
                    }
                }
            });
            lyricsBox.addView(lyricLine, lp(-1, -2, 0, 2, 0, 2));
            activeLyricLines.add(parsed);
            activeLyricViews.add(lyricLine);
            visibleIndex++;
        }
        if (visibleIndex == 0) {
            TextView empty = small("暂无歌词");
            empty.setGravity(Gravity.CENTER);
            empty.setFocusable(true);
            empty.setContentDescription("歌词：暂无歌词");
            lyricsBox.addView(empty);
        }
    }

    private LyricLine parseLyricLine(String raw) {
        if (raw == null) return new LyricLine(0, "");
        String line = raw.trim();
        int timeMs = 0;
        int close = line.indexOf(']');
        if (line.startsWith("[") && close > 0) {
            String time = line.substring(1, close);
            timeMs = parseLyricTime(time);
            line = line.substring(close + 1).trim();
        }
        return new LyricLine(timeMs, line);
    }

    private int parseLyricTime(String time) {
        try {
            String[] parts = time.split(":");
            if (parts.length < 2) return 0;
            int min = Integer.parseInt(parts[0]);
            float sec = Float.parseFloat(parts[1]);
            return (int) ((min * 60 + sec) * 1000);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void updateActiveLyric(int positionMs) {
        if (activeLyricLines.size() == 0 || activeLyricViews.size() != activeLyricLines.size()) return;
        if (!activeLyricsTimed) return;
        int syncedPosition = Math.max(0, positionMs + lyricOffsetMs);
        int target = -1;
        for (int i = 0; i < activeLyricLines.size(); i++) {
            if (activeLyricLines.get(i).timeMs <= syncedPosition) target = i;
            else break;
        }
        if (target < 0 || target == activeLyricIndex) return;
        activeLyricIndex = target;
        for (int i = 0; i < activeLyricViews.size(); i++) {
            TextView view = activeLyricViews.get(i);
            boolean active = i == target;
            view.setTextColor(active ? lyricColor : MUTED);
            view.setTextSize(active ? 19 : 15);
            view.setAlpha(active ? 1f : 0.58f);
            if (active) {
                applyCurrentLyricEffect(view);
                scrollToActiveLyric(view);
            }
        }
    }

    private void scrollToActiveLyric(final View view) {
        if (activeLyricsScroll == null || view == null) return;
        activeLyricsScroll.post(new Runnable() {
            @Override public void run() {
                int targetY = Math.max(0, view.getTop() - activeLyricsScroll.getHeight() / 2 + view.getHeight());
                activeLyricsScroll.smoothScrollTo(0, targetY);
            }
        });
    }

    private void applyCurrentLyricEffect(TextView view) {
        view.animate().cancel();
        if (subtitleEffect == 1) {
            view.setAlpha(0f);
            view.animate().alpha(1f).setDuration(260).start();
        } else if (subtitleEffect == 2) {
            view.setTranslationY(dp(18));
            view.animate().translationY(0).alpha(1f).setDuration(260).start();
        } else if (subtitleEffect == 3) {
            view.setScaleX(0.9f);
            view.setScaleY(0.9f);
            view.animate().scaleX(1.08f).scaleY(1.08f).setDuration(180).withEndAction(new Runnable() {
                @Override public void run() {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                }
            }).start();
        } else if (subtitleEffect == 4) {
            view.setAlpha(0.45f);
            view.animate().alpha(1f).setDuration(220).start();
        } else if (subtitleEffect == 5) {
            view.setTranslationX(-dp(42));
            view.setAlpha(0f);
            view.animate().translationX(0).alpha(1f).setDuration(280).start();
        } else if (subtitleEffect == 6) {
            view.setTranslationX(dp(42));
            view.setAlpha(0f);
            view.animate().translationX(0).alpha(1f).setDuration(280).start();
        } else if (subtitleEffect == 7) {
            view.setRotationX(38f);
            view.setAlpha(0f);
            view.animate().rotationX(0f).alpha(1f).setDuration(300).start();
        } else if (subtitleEffect == 8) {
            view.setScaleX(1f);
            view.setScaleY(1f);
            view.animate().scaleX(1.12f).scaleY(1.12f).setDuration(180).withEndAction(new Runnable() {
                @Override public void run() {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
                }
            }).start();
        }
    }

    private void applySubtitleEffect(TextView line, int index) {
        if (subtitleEffect == 1) {
            line.setAlpha(0f);
            line.animate().alpha(1f).setStartDelay(index * 35L).setDuration(260).start();
        } else if (subtitleEffect == 2) {
            line.setAlpha(0f);
            line.setTranslationY(dp(12));
            line.animate().alpha(1f).translationY(0).setStartDelay(index * 35L).setDuration(260).start();
        } else if (subtitleEffect == 3) {
            line.setAlpha(0f);
            line.setScaleX(0.96f);
            line.setScaleY(0.96f);
            line.animate().alpha(1f).scaleX(1f).scaleY(1f).setStartDelay(index * 35L).setDuration(260).start();
        } else if (subtitleEffect == 4) {
            line.setTextColor(index % 2 == 0 ? lyricColor : MUTED);
            line.setAlpha(0.72f);
            line.animate().alpha(1f).setStartDelay(index * 25L).setDuration(220).start();
        }
    }

    private void loadCover(final String coverUrl, final ImageView imageView) {
        if (coverUrl == null || coverUrl.length() == 0) {
            imageView.setImageDrawable(null);
            imageView.setContentDescription("暂无封面");
            return;
        }
        loadCoverFromUrl(coverUrl, imageView);
    }

    private void enrichCoverForView(final Track track, final ImageView imageView) {
        if (track == null) return;
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final String cover = resolver.albumCover(track);
                    if (cover != null && cover.length() > 0) {
                        track.coverUrl = cover;
                        if (currentIndex >= 0 && currentIndex < playlist.size() && isCurrentTrack(track)) {
                            playlist.get(currentIndex).coverUrl = cover;
                            savePlaylist();
                        }
                        runOnUiThread(new Runnable() {
                            @Override public void run() { loadCover(cover, imageView); }
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void playSearchResult(int index) {
        if (index < 0 || index >= lastSearchResults.size()) return;
        // 将全部搜索结果加入播放列表，确保自动播放下一首
        playlist.clear();
        for (SearchResult sr : lastSearchResults) {
            playlist.add(sr.toTrack());
        }
        savePlaylist();
        currentIndex = index;
        playPlaylistTrack(currentIndex);
    }

    private void playOrOpenSearchResult(int index) {
        if (index < 0 || index >= lastSearchResults.size()) return;
        Track track = lastSearchResults.get(index).toTrack();
        if (isCurrentTrack(track)) {
            showFullPlayer(track);
            return;
        }
        // 将全部搜索结果加入播放列表，确保自动播放下一首
        playlist.clear();
        for (SearchResult sr : lastSearchResults) {
            playlist.add(sr.toTrack());
        }
        savePlaylist();
        currentIndex = index;
        playPlaylistTrack(currentIndex);
    }

    private boolean isCurrentTrack(Track track) {
        if (track == null || currentIndex < 0 || currentIndex >= playlist.size()) return false;
        Track current = playlist.get(currentIndex);
        return track.id.equals(current.id) && track.source.equals(current.source);
    }

    private void showSingerPage(final Track track) {
        final String artistName = track.artist == null || track.artist.length() == 0 ? "未知歌手" : track.artist;
        // 保存当前页面以便返回，并链式恢复原始返回动作
        final Runnable savedBackAction = previousPageAction;
        previousPageAction = new Runnable() {
            @Override public void run() {
                previousPageAction = savedBackAction;
                if (currentFullPlayerTrack != null) {
                    showFullPlayer(currentFullPlayerTrack);
                } else {
                    showMainPage();
                }
            }
        };
        content.setTag("singer");
        content.removeAllViews();
        updateHeartVisibility();
        addTopBackButton("歌手介绍");

        LinearLayout card = card();
        // 歌手照片
        final ImageView singerPhoto = new ImageView(this);
        singerPhoto.setBackgroundColor(CARD2);
        singerPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        singerPhoto.setContentDescription(artistName + "的照片");
        card.addView(singerPhoto, lp(-1, dp(220), 0, 0, 0, 14));

        // 歌手名字
        TextView nameView = text(artistName, 26, true);
        nameView.setGravity(Gravity.CENTER);
        nameView.setContentDescription("歌手：" + artistName);
        card.addView(nameView, lp(-1, -2, 0, 0, 0, 10));

        // 加载中提示
        final TextView bioView = small("正在加载歌手信息...");
        bioView.setGravity(Gravity.CENTER);
        card.addView(bioView, lp(-1, -2, 0, 0, 0, 12));

        final LinearLayout songsCard = card();
        songsCard.addView(small("正在加载歌曲列表..."), lp(-1, -2, 0, 0, 0, 0));
        content.addView(card);
        content.addView(songsCard);

        // 异步加载歌手信息和歌曲
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    // 搜索歌手歌曲
                    final List<SearchResult> singerSongs = resolver.search("all", artistName);
                    // 获取歌手信息和照片
                    final SingerInfo singerInfo = fetchSingerInfo(artistName, track.source);

                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            // 设置歌手照片（优先使用API返回的picUrl）
                            String photoUrl = null;
                            if (singerInfo != null && singerInfo.photoUrl != null
                                    && singerInfo.photoUrl.length() > 0) {
                                photoUrl = singerInfo.photoUrl;
                            } else if (!singerSongs.isEmpty() && singerSongs.get(0).coverUrl != null
                                    && singerSongs.get(0).coverUrl.length() > 0) {
                                photoUrl = singerSongs.get(0).coverUrl;
                            }
                            if (photoUrl != null) {
                                loadCover(photoUrl, singerPhoto);
                            }

                            // 显示简介
                            if (singerInfo != null && singerInfo.bio != null && singerInfo.bio.length() > 0) {
                                bioView.setText(singerInfo.bio);
                            } else {
                                bioView.setText("暂无歌手简介");
                            }

                            // 更新歌曲列表卡片
                            songsCard.removeAllViews();
                            if (singerSongs.isEmpty()) {
                                songsCard.addView(small("未找到该歌手的歌曲"), lp(-1, -2, 0, 0, 0, 0));
                            } else {
                                // 批量下载按钮
                                songsCard.addView(pillButton("批量下载（" + singerSongs.size() + "首）", new View.OnClickListener() {
                                    @Override public void onClick(View v) {
                                        showBatchDownloadSearch(singerSongs);
                                    }
                                }));

                                // 歌曲列表
                                for (int i = 0; i < singerSongs.size(); i++) {
                                    final int idx = i;
                                    final SearchResult sr = singerSongs.get(i);
                                    songsCard.addView(trackRow(sr.toTrack(), new View.OnClickListener() {
                                        @Override public void onClick(View v) {
                                            // 将歌手歌曲加入播放列表并播放
                                            playlist.clear();
                                            for (SearchResult s : singerSongs) {
                                                playlist.add(s.toTrack());
                                            }
                                            savePlaylist();
                                            currentIndex = idx;
                                            playPlaylistTrack(currentIndex);
                                        }
                                    }));
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            bioView.setText("加载失败：" + (e.getMessage() != null ? e.getMessage() : "未知错误"));
                            songsCard.removeAllViews();
                            songsCard.addView(small("加载歌曲失败，请返回重试"), lp(-1, -2, 0, 0, 0, 0));
                        }
                    });
                }
            }
        });
    }

    private String fetchSingerBio(String artistName, String source) {
        SingerInfo info = fetchSingerInfo(artistName, source);
        return info != null ? info.bio : null;
    }

    private String fetchSingerPhotoFromApi(String artistName) {
        // 仅尝试从API获取歌手照片URL
        try {
            String encodedName = URLEncoder.encode(artistName, "UTF-8");
            String searchUrl = "https://music.163.com/api/search/get?s=" + encodedName + "&type=100&limit=1";
            String resp = httpGetString(searchUrl, "https://music.163.com/");
            if (resp != null) {
                JSONObject json = new JSONObject(resp);
                if (json.has("result")) {
                    JSONObject result = json.getJSONObject("result");
                    if (result.has("artists") && result.getJSONArray("artists").length() > 0) {
                        JSONObject artist = result.getJSONArray("artists").getJSONObject(0);
                        String photoUrl = artist.optString("picUrl", "");
                        if (photoUrl.length() > 0) return photoUrl;
                    }
                }
            }
        } catch (Exception ignored) {}

        // 备用：星海API
        try {
            String encodedName = URLEncoder.encode(artistName, "UTF-8");
            String url = "https://music-api.gdstudio.xyz/api.php?types=search&source=netease&name=" + encodedName + "&search_type=artist";
            String resp = httpGetString(url, null);
            if (resp != null) {
                JSONObject json = new JSONObject(resp);
                if (json.has("result")) {
                    Object resultObj = json.get("result");
                    if (resultObj instanceof JSONArray) {
                        JSONArray arr = (JSONArray) resultObj;
                        if (arr.length() > 0) {
                            JSONObject item = arr.getJSONObject(0);
                            String photoUrl = item.optString("picUrl", item.optString("img1v1Url", ""));
                            if (photoUrl.length() > 0) return photoUrl;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private static class SingerInfo {
        String bio;
        String photoUrl;
        SingerInfo(String bio, String photoUrl) { this.bio = bio; this.photoUrl = photoUrl; }
    }

    private SingerInfo fetchSingerInfo(String artistName, String source) {
        // 1. 先查内置数据库
        String normalized = normalizeName(artistName);
        SingerInfo builtin = builtinSingerDb.get(normalized);
        // 如果内置有数据但缺少照片，尝试从API获取照片后合并返回
        if (builtin != null) {
            if (builtin.photoUrl == null || builtin.photoUrl.length() == 0) {
                String apiPhoto = fetchSingerPhotoFromApi(artistName);
                if (apiPhoto != null) {
                    return new SingerInfo(builtin.bio, apiPhoto);
                }
            }
            return builtin;
        }

        // 2. 尝试通过网易云API获取歌手信息
        try {
            String encodedName = URLEncoder.encode(artistName, "UTF-8");
            // 搜索歌手
            String searchUrl = "https://music.163.com/api/search/get?s=" + encodedName + "&type=100&limit=1";
            String resp = httpGetString(searchUrl, "https://music.163.com/");
            if (resp != null) {
                JSONObject json = new JSONObject(resp);
                if (json.has("result")) {
                    JSONObject result = json.getJSONObject("result");
                    if (result.has("artists") && result.getJSONArray("artists").length() > 0) {
                        JSONObject artist = result.getJSONArray("artists").getJSONObject(0);
                        String photoUrl = artist.optString("picUrl", "");
                        String artistId = artist.optString("id", "");
                        String bio = null;

                        // 获取歌手简介
                        if (artistId.length() > 0) {
                            String descUrl = "https://music.163.com/api/artist/desc?id=" + artistId;
                            String descResp = httpGetString(descUrl, "https://music.163.com/");
                            if (descResp != null) {
                                JSONObject descJson = new JSONObject(descResp);
                                if (descJson.has("introduction")) {
                                    JSONArray introArr = descJson.getJSONArray("introduction");
                                    if (introArr.length() > 0) {
                                        StringBuilder sb = new StringBuilder();
                                        for (int i = 0; i < introArr.length(); i++) {
                                            JSONObject item = introArr.getJSONObject(i);
                                            String txt = item.optString("txt", "");
                                            if (txt.length() > 0) {
                                                if (sb.length() > 0) sb.append("\n");
                                                sb.append(txt);
                                            }
                                        }
                                        if (sb.length() > 0) bio = sb.toString();
                                    }
                                }
                                // 如果introduction为空，尝试briefDesc
                                if (bio == null && descJson.has("briefDesc")) {
                                    bio = descJson.optString("briefDesc", "");
                                }
                            }
                        }
                        if (bio != null || photoUrl.length() > 0) {
                            return new SingerInfo(bio, photoUrl);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        // 3. 尝试通过星海API获取
        try {
            String encodedName = URLEncoder.encode(artistName, "UTF-8");
            String url = "https://music-api.gdstudio.xyz/api.php?types=search&source=netease&name=" + encodedName + "&search_type=artist";
            String resp = httpGetString(url, null);
            if (resp != null) {
                JSONObject json = new JSONObject(resp);
                // 尝试解析星海返回的歌手信息
                if (json.has("result")) {
                    Object resultObj = json.get("result");
                    if (resultObj instanceof JSONArray) {
                        JSONArray arr = (JSONArray) resultObj;
                        if (arr.length() > 0) {
                            JSONObject item = arr.getJSONObject(0);
                            String photoUrl = item.optString("picUrl", item.optString("img1v1Url", ""));
                            if (photoUrl.length() > 0) {
                                return new SingerInfo(null, photoUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String httpGetString(String urlStr, String referer) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) TingHanXinYuPlayer/1.3");
            conn.setRequestProperty("Accept", "application/json, */*");
            if (referer != null) conn.setRequestProperty("Referer", referer);
            int code = conn.getResponseCode();
            if (code == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                return sb.toString();
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        return name.trim().toLowerCase(Locale.ROOT)
            .replaceAll("[·\\s\\-_\\.]", "")
            .replaceAll("（.*）", "")
            .replaceAll("\\(.*\\)", "");
    }

    private static final Map<String, SingerInfo> builtinSingerDb = new HashMap<String, SingerInfo>();
    static {
        builtinSingerDb.put("周杰伦", new SingerInfo(
            "周杰伦（Jay Chou），1979年1月18日出生于中国台湾省新北市，华语流行乐男歌手、音乐人、MV导演、演员。1997年签约阿尔法唱片公司，2000年发行首张个人专辑《JAY》正式出道。他以融合R&B、嘻哈、中国风等多元曲风著称，被誉为华语乐坛最具影响力的音乐人之一。代表作包括《青花瓷》《七里香》《夜曲》《双截棍》《晴天》《稻香》《听妈妈的话》等，曾获15座金曲奖，是华语乐坛获奖最多的男歌手。", ""));
        builtinSingerDb.put("林俊杰", new SingerInfo(
            "林俊杰（JJ Lin），1981年3月27日出生于新加坡，华语流行乐男歌手、音乐人、词曲创作人。2003年发行首张专辑《乐行者》出道，2004年凭《江南》一曲成名。他以出色的唱功和创作才华著称，被誉为\u201C行走的CD\u201D。代表作包括《江南》《一千年以后》《曹操》《修炼爱情》《不为谁而作的歌》《可惜没如果》等，曾获两届金曲奖最佳国语男歌手奖。", ""));        builtinSingerDb.put("陈奕迅", new SingerInfo(
            "陈奕迅（Eason Chan），1974年7月27日出生于中国香港，华语流行乐男歌手、演员。1995年参加第14届新秀歌唱大赛获得冠军出道。他以浑厚深情的嗓音和极强的歌曲演绎能力著称，被誉为\u201C歌神接班人\u201D。代表作包括《十年》《浮夸》《富士山下》《K歌之王》《爱情转移》《好久不见》《孤勇者》等，多次获得叱咤乐坛流行榜最受欢迎男歌手奖。", ""));        builtinSingerDb.put("邓紫棋", new SingerInfo(
            "邓紫棋（G.E.M.），本名邓诗颖，1991年8月16日出生于上海，华语流行乐女歌手、词曲作者、音乐制作人。2008年发行首张EP《G.E.M.》正式出道。她以强大的唱功和创作能力著称，是首位登上香港红馆开唱的90后女歌手。代表作包括《光年之外》《泡沫》《句号》《喜欢你》《来自天堂的魔鬼》《倒数》等，手握两项吉尼斯世界纪录。", ""));
        builtinSingerDb.put("薛之谦", new SingerInfo(
            "薛之谦，1983年7月17日出生于上海，中国内地流行乐男歌手、音乐制作人。2005年参加《我型我秀》正式出道，2006年发行首张同名专辑《薛之谦》。他以深情的词曲创作和独特的嗓音著称，是华语乐坛最具人气的创作歌手之一。代表作包括《演员》《认真的雪》《丑八怪》《绅士》《天外来物》《像风一样》等。", ""));
        builtinSingerDb.put("李荣浩", new SingerInfo(
            "李荣浩，1985年7月11日出生于安徽省蚌埠市，华语流行乐男歌手、音乐人、词曲创作人。2013年发行首张专辑《模特》出道，凭该专辑成为首位获得台湾金曲奖最佳新人奖的中国大陆歌手。他以全能创作才子的身份著称，包办词曲编曲制作。代表作包括《李白》《模特》《年少有为》《不将就》《喜剧之王》《麻雀》等。", ""));
        builtinSingerDb.put("许嵩", new SingerInfo(
            "许嵩（Vae），1986年5月14日出生于安徽省合肥市，中国内地流行乐男歌手、词曲创作人、音乐制作人，毕业于安徽医科大学。2006年开始在网络发表音乐作品，2009年发行首张全创作专辑《自定义》。他以文学性的歌词和独特曲风著称，被誉为\u201C音乐诗人\u201D。代表作包括《素颜》《有何不可》《清明雨上》《断桥残雪》《庐州月》《雅俗共赏》等。", ""));        builtinSingerDb.put("王菲", new SingerInfo(
            "王菲（Faye Wong），1969年8月8日出生于北京，华语流行乐女歌手、影视演员，国家一级演员。1989年以歌曲《仍是旧句子》在香港出道。她以空灵独特的嗓音和前卫的艺术风格著称，被誉为华语乐坛最具影响力的天后级歌手。代表作包括《红豆》《我愿意》《天空》《流年》《传奇》《因为爱情》《匆匆那年》等。", ""));
        builtinSingerDb.put("张杰", new SingerInfo(
            "张杰（Jason Zhang），1982年12月20日出生于四川成都，中国内地流行男歌手、音乐制作人。2004年获《我型我秀》全国总冠军出道。他以清亮高亢的嗓音和强大的现场演唱能力著称，是首位获得全美音乐奖年度国际艺人奖的华人歌手。代表作包括《这就是爱》《逆战》《天下》《明天过后》《勿忘心安》《三生三世》等。", ""));
        builtinSingerDb.put("陶喆", new SingerInfo(
            "陶喆（David Tao），1969年7月11日出生于中国香港，华语流行乐男歌手、音乐制作人，毕业于加州大学洛杉矶分校。1997年发行首张专辑《David Tao》出道。他将R&B引入华语乐坛，被誉为\u201C华语R&B教父\u201D。代表作包括《爱很简单》《今天你要嫁给我》《Melody》《小镇姑娘》《黑色柳丁》《普通朋友》等。", ""));        builtinSingerDb.put("蔡健雅", new SingerInfo(
            "蔡健雅（Tanya Chua），1975年1月28日出生于新加坡，华语流行乐女歌手、音乐制作人、词曲作者。1997年发行首张英语专辑《Bored》出道。她以细腻的情感表达和创作才华著称，曾四度获得台湾金曲奖最佳国语女歌手奖，是华语乐坛获奖最多的女歌手之一。代表作包括《红色高跟鞋》《达尔文》《Beautiful Love》《陌生人》《别找我麻烦》等。", ""));
        builtinSingerDb.put("袁娅维", new SingerInfo(
            "袁娅维（Tia Ray），1984年12月12日出生于湖南省怀化市，中国内地流行乐女歌手、词曲作者。2012年参加《中国好声音》获刘欢组四强出道。她以国际化的Soul/R&B唱腔和超强唱功著称，被誉为华语乐坛技术流代表。代表作包括《说散就散》《阿楚姑娘》《不亏不欠》《神预言》《月亮失眠了》等。", ""));
        builtinSingerDb.put("王力宏", new SingerInfo(
            "王力宏（Leehom Wang），1976年5月17日出生于美国纽约，华语流行乐男歌手、音乐制作人、演员，毕业于威廉姆斯学院和伯克利音乐学院。1995年发行首张专辑《情敌贝多芬》出道。他以融合中西音乐元素著称，被誉为\u201C华语乐坛一体机\u201D。代表作包括《唯一》《心中的日月》《花田错》《龙的传人》《大城小爱》《你不知道的事》等。", ""));        builtinSingerDb.put("孙燕姿", new SingerInfo(
            "孙燕姿（Stefanie Sun），1978年7月23日出生于新加坡，华语流行乐女歌手。2000年发行首张专辑《孙燕姿》出道。她以独特的嗓音和清新自然的气质著称，曾获新加坡十大杰出青年。代表作包括《天黑黑》《遇见》《我怀念的》《开始懂了》《绿光》《逆光》等。", ""));
        builtinSingerDb.put("蔡依林", new SingerInfo(
            "蔡依林（Jolin Tsai），1980年9月15日出生于中国台湾省新北市，华语流行乐女歌手、舞者。1999年发行首张专辑《Jolin 1019》出道。她以精湛的舞技和不断突破的音乐风格著称，被誉为\u201C亚洲舞娘\u201D。代表作包括《看我72变》《舞娘》《日不落》《倒带》《Play我呸》《玫瑰少年》等。", ""));        builtinSingerDb.put("张靓颖", new SingerInfo(
            "张靓颖（Jane Zhang），1984年10月11日出生于四川成都，中国内地流行乐女歌手。2005年获《超级女声》全国季军出道。她以出色的海豚音和宽广的音域著称，曾受邀演唱多部好莱坞电影中文主题曲。代表作包括《画心》《终于等到你》《我的梦》《如果这就是爱情》《第七感》等。", ""));
        builtinSingerDb.put("梁静茹", new SingerInfo(
            "梁静茹（Fish Leong），1978年6月16日出生于马来西亚，华语流行乐女歌手。1999年被李宗盛发掘发行首张专辑《一夜长大》。她以温暖治愈的嗓音和感人的情歌著称，被誉为\u201C情歌天后\u201D。代表作包括《勇气》《分手快乐》《可惜不是你》《暖暖》《宁夏》《会呼吸的痛》等。", ""));        builtinSingerDb.put("莫文蔚", new SingerInfo(
            "莫文蔚（Karen Mok），1970年6月2日出生于中国香港，华语流行乐女歌手、演员。1993年发行首张粤语专辑《Karen》出道。她以独特的嗓音和多元化的音乐风格著称，被誉为华语乐坛\u201C百变天后\u201D。代表作包括《盛夏的果实》《阴天》《忽然之间》《广岛之恋》《他不爱我》《电台情歌》等。", ""));        builtinSingerDb.put("朴树", new SingerInfo(
            "朴树，1973年11月8日出生于江苏南京，中国内地男歌手、音乐制作人。1996年签约麦田音乐，1999年发行首张专辑《我去2000年》。他以诗意的音乐风格和真诚的创作态度著称，是华语乐坛最具人文气质的音乐人之一。代表作包括《那些花儿》《白桦林》《生如夏花》《平凡之路》《清白之年》等。", ""));
        builtinSingerDb.put("汪峰", new SingerInfo(
            "汪峰，1971年6月29日出生于北京，中国内地摇滚男歌手、音乐人，毕业于中央音乐学院。1994年以\u201C鲍家街43号\u201D乐队主唱身份出道。他以充满力量的摇滚风格和深刻的社会观察著称，被誉为\u201C摇滚诗人\u201D。代表作包括《春天里》《怒放的生命》《飞得更高》《存在》《北京北京》《像梦一样自由》等。", ""));        builtinSingerDb.put("那英", new SingerInfo(
            "那英，1967年11月27日出生于辽宁沈阳，华语流行乐女歌手。1988年以翻唱苏芮歌曲进入歌坛。她以极具辨识度的嗓音和强大的唱功著称，是华语乐坛最具影响力的天后级歌手之一。代表作包括《征服》《默》《白天不懂夜的黑》《一笑而过》《梦一场》《春暖花开》等。", ""));
        builtinSingerDb.put("刘德华", new SingerInfo(
            "刘德华（Andy Lau），1961年9月27日出生于中国香港，华语流行乐男歌手、演员、制片人。香港\u201C四大天王\u201D之一，1985年发行首张专辑《只知道此刻爱你》。他以全面的演艺才能和勤奋敬业著称，是华语娱乐圈的常青树。代表作包括《忘情水》《一起走过的日子》《冰雨》《中国人》《练习》《爱你一万年》等。", ""));        builtinSingerDb.put("张学友", new SingerInfo(
            "张学友（Jacky Cheung），1961年7月10日出生于中国香港，华语流行乐男歌手、演员。香港\u201C四大天王\u201D之一，1984年获首届香港十八区业余歌唱大赛冠军出道。他以卓越的唱功和深情的演绎著称，被誉为\u201C歌神\u201D。代表作包括《吻别》《祝福》《一千个伤心的理由》《她来听我的演唱会》《饿狼传说》《如果这都不算爱》等。", ""));        builtinSingerDb.put("张惠妹", new SingerInfo(
            "张惠妹（A-Mei），1972年8月9日出生于台湾台东县卑南乡，华语流行乐女歌手。1996年发行首张专辑《姐妹》出道，即创下百万销量。她以极具爆发力的嗓音和强大的舞台魅力著称，被誉为\u201C华语天后\u201D。代表作包括《姐妹》《听海》《Bad Boy》《三天三夜》《记得》《我可以抱你吗》等。", ""));        builtinSingerDb.put("田馥甄", new SingerInfo(
            "田馥甄（Hebe Tien），1983年3月30日出生于台湾新竹，华语流行乐女歌手、演员。原为S.H.E组合成员，2010年以个人身份发行首张专辑《To Hebe》。她以空灵清澈的嗓音和文艺气质著称，是华语乐坛最具辨识度的女声之一。代表作包括《小幸运》《寂寞寂寞就好》《你就不要想起我》《魔鬼中的天使》《渺小》等。", ""));
        builtinSingerDb.put("毛不易", new SingerInfo(
            "毛不易，本名王维家，1994年10月1日出生于黑龙江省齐齐哈尔市，中国内地唱作男歌手。2017年参加《明日之子》获全国总冠军出道。他以深沉温暖的嗓音和走心的歌词著称，被誉为\u201C少年李宗盛\u201D。代表作包括《消愁》《像我这样的人》《平凡的一天》《不染》《一荤一素》《牧马城市》等。", ""));        builtinSingerDb.put("华晨宇", new SingerInfo(
            "华晨宇，1990年2月7日出生于湖北十堰，毕业于武汉音乐学院。2013年获《快乐男声》全国总冠军出道。他以独特的唱腔和极具实验性的音乐风格著称，是华语乐坛新生代实力唱作人。代表作包括《烟火里的尘埃》《异类》《齐天》《我管你》《好想爱这个世界啊》《寒鸦少年》等。", ""));
        builtinSingerDb.put("周深", new SingerInfo(
            "周深，1992年9月29日出生于湖南邵阳，毕业于乌克兰利沃夫国立音乐学院美声专业。2014年参加《中国好声音》出道。他以空灵纯净的嗓音和美声与流行结合的独特唱法著称，被誉为\u201C天籁之音\u201D。代表作包括《大鱼》《灯火里的中国》《小美满》《和光同尘》《Rubia》《若梦》等。", ""));        builtinSingerDb.put("赵雷", new SingerInfo(
            "赵雷，1986年7月20日出生于北京，中国内地民谣男歌手、音乐人。2010年参加《快乐男声》进入大众视野。他以质朴真诚的歌词和温暖的民谣风格著称，是华语民谣的代表人物。代表作包括《成都》《南方姑娘》《理想》《画》《三十岁的女人》《鼓楼》等。", ""));
        builtinSingerDb.put("陈粒", new SingerInfo(
            "陈粒，1990年7月26日出生于贵州省贵阳市，中国内地唱作音乐人。2014年退出空想家乐队独立发展。她以独特的嗓音和风格多变的音乐创作著称，是华语独立音乐的代表人物。代表作包括《奇妙能力歌》《小半》《易燃易爆炸》《走马》《如也》《虚拟》等。", ""));
        builtinSingerDb.put("李健", new SingerInfo(
            "李健，1974年9月23日出生于黑龙江哈尔滨，毕业于清华大学电子工程系。2001年以\u201C水木年华\u201D组合出道，后单飞发展。他以温润清澈的嗓音和诗意的音乐风格著称，被誉为\u201C音乐诗人\u201D。代表作包括《传奇》《贝加尔湖畔》《风吹麦浪》《异乡人》《父亲写的散文诗》《假如爱有天意》等。", ""));        builtinSingerDb.put("胡彦斌", new SingerInfo(
            "胡彦斌，1983年7月4日出生于上海，华语流行乐男歌手、音乐制作人。2001年发行首张专辑《文武双全》出道。他以出色的创作才华和R&B风格著称，是华语乐坛最具实力的唱作人之一。代表作包括《红颜》《月光》《男人KTV》《诀别诗》《你要的全拿走》《Waiting For You》等。", ""));
        builtinSingerDb.put("方大同", new SingerInfo(
            "方大同（Khalil Fong），1983年7月14日出生于美国夏威夷，华语流行乐男歌手、词曲创作人。2005年发行首张专辑《Soulboy》出道。他以融合西方R&B、灵魂乐与华语流行元素的独特风格著称。代表作包括《爱爱爱》《Love Song》《三人游》《悟空》《特别的人》《春风吹》等。", ""));
        builtinSingerDb.put("吴青峰", new SingerInfo(
            "吴青峰，1982年8月30日出生于台湾台北，华语流行乐男歌手、词曲创作人，苏打绿乐团主唱。他以独特的嗓音和极具文学性的词曲创作著称，是华语乐坛最具才华的创作人之一。代表作包括《小情歌》《无与伦比的美丽》《起风了》《起风了》《太空人》《歌颂者》等。", ""));
        builtinSingerDb.put("苏打绿", new SingerInfo(
            "苏打绿，中国台湾独立流行乐团，2001年成立于校园，2004年正式出道，由吴青峰、谢馨仪、史俊威、何景扬、刘家凯、龚钰祺六人组成。乐团以清新独特的音乐风格和吴青峰标志性的嗓音著称，曾获金曲奖最佳乐团奖。代表作包括《小情歌》《小宇宙》《无与伦比的美丽》《你在烦恼什么》《我好想你》等。", ""));
        builtinSingerDb.put("五月天", new SingerInfo(
            "五月天，中国台湾摇滚乐团，1997年3月29日成立，由主唱阿信、吉他手怪兽、吉他手石头、贝斯手玛莎、鼓手冠佑五人组成。他们以热血励志的摇滚风格和青春情怀著称，是华语乐坛最具影响力的乐团之一，首支登上纽约麦迪逊花园广场的华人乐团。代表作包括《倔强》《知足》《突然好想你》《恋爱ing》《温柔》《干杯》等。", ""));
        builtinSingerDb.put("告五人", new SingerInfo(
            "告五人，中国台湾流行摇滚乐团，2017年成立于宜兰，由男主唱潘云安、女主唱犬青和鼓手哲谦组成。乐团风格多元不设限，男女双主唱交错演绎是其特色。代表作包括《披星戴月的想你》《爱人错过》《爱在夏天》《带我去找夜生活》《一念之间》等。", ""));
        builtinSingerDb.put("萧敬腾", new SingerInfo(
            "萧敬腾（Jam Hsiao），1987年3月30日出生于台湾花莲，华语流行乐男歌手、演员。2007年参加《超级星光大道》踢馆赛一战成名。他以极具爆发力的嗓音和宽广的音域著称，被誉为\u201C省话一哥\u201D、\u201C雨神\u201D。代表作包括《王妃》《怎么说我不爱你》《新不了情》《海芋恋》《阿飞的小蝴蝶》等。", ""));        builtinSingerDb.put("杨丞琳", new SingerInfo(
            "杨丞琳（Rainie Yang），1984年6月4日出生于台湾台北，华语流行乐女歌手、演员。2000年以\u201C4 in Love\u201D组合出道，后单飞发展。她以甜美嗓音和多栖发展著称，是华语乐坛全面发展的艺人代表。代表作包括《暧昧》《雨爱》《带我走》《年轮说》《青春住了谁》等。", ""));        // 补充更多歌手别名
        builtinSingerDb.put("gem", builtinSingerDb.get("邓紫棋"));
        builtinSingerDb.put("g.e.m.", builtinSingerDb.get("邓紫棋"));
        builtinSingerDb.put("jjlin", builtinSingerDb.get("林俊杰"));
        builtinSingerDb.put("easonchan", builtinSingerDb.get("陈奕迅"));
        builtinSingerDb.put("jaychou", builtinSingerDb.get("周杰伦"));
        builtinSingerDb.put("tiaray", builtinSingerDb.get("袁娅维"));
        builtinSingerDb.put("tia", builtinSingerDb.get("袁娅维"));
        builtinSingerDb.put("tanyachua", builtinSingerDb.get("蔡健雅"));
        builtinSingerDb.put("tanya", builtinSingerDb.get("蔡健雅"));
        builtinSingerDb.put("fayewong", builtinSingerDb.get("王菲"));
        builtinSingerDb.put("jamhsiao", builtinSingerDb.get("萧敬腾"));
        builtinSingerDb.put("hebe", builtinSingerDb.get("田馥甄"));
        builtinSingerDb.put("hebetien", builtinSingerDb.get("田馥甄"));
        builtinSingerDb.put("jolin", builtinSingerDb.get("蔡依林"));
        builtinSingerDb.put("stefaniesun", builtinSingerDb.get("孙燕姿"));
        builtinSingerDb.put("stefanie", builtinSingerDb.get("孙燕姿"));
        builtinSingerDb.put("amei", builtinSingerDb.get("张惠妹"));
        builtinSingerDb.put("a-mei", builtinSingerDb.get("张惠妹"));
        builtinSingerDb.put("andylau", builtinSingerDb.get("刘德华"));
        builtinSingerDb.put("jackycheung", builtinSingerDb.get("张学友"));
        builtinSingerDb.put("leehom", builtinSingerDb.get("王力宏"));
        builtinSingerDb.put("leehomwang", builtinSingerDb.get("王力宏"));
        builtinSingerDb.put("davidtao", builtinSingerDb.get("陶喆"));
        builtinSingerDb.put("vae", builtinSingerDb.get("许嵩"));
        builtinSingerDb.put("khalil", builtinSingerDb.get("方大同"));
        builtinSingerDb.put("khalilfong", builtinSingerDb.get("方大同"));
        builtinSingerDb.put("zhangjiejason", builtinSingerDb.get("张杰"));
        builtinSingerDb.put("jasonzhang", builtinSingerDb.get("张杰"));
        builtinSingerDb.put("janice", builtinSingerDb.get("张靓颖"));
        builtinSingerDb.put("janezhang", builtinSingerDb.get("张靓颖"));
        builtinSingerDb.put("fish", builtinSingerDb.get("梁静茹"));
        builtinSingerDb.put("fishleong", builtinSingerDb.get("梁静茹"));
        builtinSingerDb.put("karenmok", builtinSingerDb.get("莫文蔚"));
        builtinSingerDb.put("maobuyi", builtinSingerDb.get("毛不易"));
        builtinSingerDb.put("huachenyu", builtinSingerDb.get("华晨宇"));
        builtinSingerDb.put("zhoushen", builtinSingerDb.get("周深"));
        builtinSingerDb.put("zhaolei", builtinSingerDb.get("赵雷"));
        builtinSingerDb.put("chenli", builtinSingerDb.get("陈粒"));
        builtinSingerDb.put("lijian", builtinSingerDb.get("李健"));
        builtinSingerDb.put("huyanbin", builtinSingerDb.get("胡彦斌"));
        builtinSingerDb.put("wuqingfeng", builtinSingerDb.get("吴青峰"));
        builtinSingerDb.put("sodagreen", builtinSingerDb.get("苏打绿"));
        builtinSingerDb.put("mayday", builtinSingerDb.get("五月天"));
        builtinSingerDb.put("announceit", builtinSingerDb.get("告五人"));
    }

    private void playPlaylistTrack(final int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentLoveTrackIndex = -1; // 切换到歌单播放，重置爱的空间状态
        currentIndex = index;
        final Track track = playlist.get(index);
        setCurrentTrack(track);
        loading(true);
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    if (track.coverUrl == null || track.coverUrl.length() == 0) {
                        track.coverUrl = resolver.albumCover(track);
                        savePlaylist();
                    }
                    final ResolvedPlayback playback = resolver.resolvePlayable(track, "320k");
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            if (!playback.track.id.equals(track.id) || !playback.track.source.equals(track.source)) {
                                playlist.set(currentIndex, playback.track);
                                savePlaylist();
                                setCurrentTrack(playback.track);
                                toast("原音源解析失败，已切换到可播放音源：" + sourceName(playback.track.source));
                            }
                            if (playback.url != null) {
                                toast("解析URL：" + playback.url.substring(0, Math.min(playback.url.length(), 80)));
                            }
                            startPlayer(playback.track, playback.url);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            loading(false);
                            toast(error.getMessage() == null ? "播放失败" : error.getMessage());
                        }
                    });
                }
            }
        });
    }

    private void startPlayer(final Track track, String url) {
        currentUrl = url;
        currentMediaTitle = track.title;
        currentMediaArtist = track.artist;
        releasePlayer();
        mediaPlayer = new MediaPlayer();
        // 保持屏幕常亮，提升视频播放体验
        mediaPlayer.setScreenOnWhilePlaying(true);
        // 设置音频流类型
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        if (currentSurfaceHolder != null && currentSurfaceHolder.getSurface().isValid()) {
            try { mediaPlayer.setDisplay(currentSurfaceHolder); } catch (Exception ignored) {}
        }
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) TingHanXinYuPlayer/1.3");
            headers.put("Accept", "*/*");
            headers.put("Connection", "keep-alive");
            headers.put("Referer", refererFor(track.source));
            if ("bl".equals(track.source)) {
                String cookie = resolver.getBiliCookie();
                if (cookie != null && cookie.length() > 0) {
                    headers.put("Cookie", cookie);
                }
            }
            mediaPlayer.setDataSource(this, Uri.parse(url), headers);
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override public void onPrepared(MediaPlayer mp) {
                    if (currentSurfaceHolder != null && currentSurfaceHolder.getSurface().isValid()) {
                        try { mp.setDisplay(currentSurfaceHolder); } catch (Exception ignored) {}
                    }
                    setupEqualizer();
                    applyPlaybackSpeed();
                    applyVoiceEffect();
                    requestAudioFocus();
                    acquireWakeLock();
                    mp.start();
                    updateMiniPlayer();
                    updateProgress();
                    updatePlaybackState();
                    showPlayerNotification();
                    updateHeartVisibility();
                }
            });
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override public void onCompletion(MediaPlayer mp) { nextTrack(); }
            });
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override public boolean onError(MediaPlayer mp, int what, int extra) {
                    String urlInfo = currentUrl != null ? currentUrl.substring(0, Math.min(currentUrl.length(), 60)) : "null";
                    toast("播放出错：" + what + "/" + extra + "\nURL=" + urlInfo);
                    return true;
                }
            });
            // 缓冲更新监听
            mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                @Override public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    // 缓冲进度更新，可用于显示缓冲状态
                }
            });
            // 信息监听，处理缓冲开始/结束
            mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
                @Override public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    switch (what) {
                        case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                            // 开始缓冲
                            break;
                        case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                            // 缓冲结束
                            break;
                        case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            // 视频开始渲染
                            break;
                    }
                    return false;
                }
            });
            mediaPlayer.prepareAsync();
            toast("正在加载：" + track.title);
        } catch (Exception error) {
            toast("无法播放链接：" + error.getMessage());
        }
    }

    private String refererFor(String source) {
        if ("tx".equals(source)) return "https://y.qq.com/";
        if ("kw".equals(source)) return "https://www.kuwo.cn/";
        if ("kg".equals(source)) return "https://www.kugou.com/";
        if ("mg".equals(source)) return "https://music.migu.cn/";
        if ("xm".equals(source)) return "https://www.ximalaya.com/";
        if ("bl".equals(source)) return "https://www.bilibili.com/";
        if ("dy".equals(source)) return "https://www.douyin.com/";
        return "https://music.163.com/";
    }

    private static String guessReferer(String url) {
        if (url.contains("bilibili.com")) return "https://www.bilibili.com/";
        if (url.contains("douyin.com")) return "https://www.douyin.com/";
        if (url.contains("ximalaya.com")) return "https://www.ximalaya.com/";
        if (url.contains("qq.com")) return "https://y.qq.com/";
        if (url.contains("kuwo.cn")) return "https://www.kuwo.cn/";
        if (url.contains("kugou.com")) return "https://www.kugou.com/";
        if (url.contains("migu.cn") || url.contains("migu")) return "https://music.migu.cn/";
        if (url.contains("music.163.com")) return "https://music.163.com/";
        return "https://music.163.com/";
    }

    private void previousTrack() {
        // 如果当前是爱的空间歌曲，随机切换
        if (currentLoveTrackIndex >= 0) {
            nextLoveTrack();
            return;
        }
        if (playlist.isEmpty()) {
            toast("播放列表为空");
            return;
        }
        currentIndex = currentIndex <= 0 ? playlist.size() - 1 : currentIndex - 1;
        playPlaylistTrack(currentIndex);
    }

    private void nextTrack() {
        // 如果当前是爱的空间歌曲，随机切换下一首
        if (currentLoveTrackIndex >= 0) {
            nextLoveTrack();
            return;
        }
        if (playlist.isEmpty()) {
            toast("播放列表为空");
            return;
        }
        if (repeatMode == 1) {
            // 单曲循环：重新播放当前歌曲
            playPlaylistTrack(currentIndex);
            return;
        }
        if (repeatMode == 2) {
            // 全部循环：到末尾回到开头
            currentIndex = currentIndex >= playlist.size() - 1 ? 0 : currentIndex + 1;
        } else {
            // 顺序播放：到末尾停止
            if (currentIndex >= playlist.size() - 1) {
                toast("播放列表已播完");
                updatePlaybackState();
                return;
            }
            currentIndex = currentIndex + 1;
        }
        playPlaylistTrack(currentIndex);
    }

    private String repeatModeLabel() {
        switch (repeatMode) {
            case 1: return "单曲循环";
            case 2: return "全部循环";
            default: return "顺序播放";
        }
    }

    private void cycleRepeatMode() {
        repeatMode = (repeatMode + 1) % 3;
        toast("播放模式：" + repeatModeLabel());
        updateRepeatButton();
    }

    private Button repeatModeButton;
    private void updateRepeatButton() {
        if (repeatModeButton != null) {
            repeatModeButton.setText("播放模式：" + repeatModeLabel());
        }
    }

    private void togglePlayback() {
        if (mediaPlayer == null) {
            if (!playlist.isEmpty()) playPlaylistTrack(Math.max(0, currentIndex));
            return;
        }
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            releaseWakeLock();
        } else {
            requestAudioFocus();
            acquireWakeLock();
            mediaPlayer.start();
        }
        updateMiniPlayer();
        updatePlaybackState();
        showPlayerNotification();
        updateHeartVisibility();
    }

    private void setCurrentTrack(Track track) {
        currentMediaTitle = track.title;
        currentMediaArtist = track.artist;
        if (currentTrackText != null) currentTrackText.setText(track.title);
    }

    private void addTrack(Track track) {
        playlist.add(track);
        savePlaylist();
    }

    private void showDownloadQualityDialog(final Track track) {
        if (track == null) {
            toast("当前没有可下载歌曲");
            return;
        }
        final String[] names = {"标准音质 128k", "较高音质 192k", "高音质 320k", "无损音质 FLAC"};
        final String[] qualities = {"128k", "192k", "320k", "flac"};
        new AlertDialog.Builder(this)
            .setTitle("选择下载音质")
            .setItems(names, new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    startDownloadWithPermission(track, qualities[which], names[which]);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void startDownloadWithPermission(Track track, String quality, String label) {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 2001);
            toast("请允许存储权限后再点一次下载");
            return;
        }
        downloadTrack(track, quality, label);
    }

    private void downloadTrack(final Track track, final String quality, final String label) {
        // 先解析下载链接，然后弹出选择：应用内下载 或 用其他应用下载
        final android.app.ProgressDialog resolvingDialog = new android.app.ProgressDialog(this);
        resolvingDialog.setMessage("正在解析下载链接...");
        resolvingDialog.setCancelable(false);
        resolvingDialog.show();

        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    // 解析下载链接
                    String downloadUrl = null;
                    Track resolvedTrack = track;
                    try {
                        downloadUrl = resolver.resolve(track.source, track.id, quality);
                    } catch (Exception e1) {
                        try {
                            final ResolvedPlayback playback = resolver.resolvePlayable(track, quality);
                            downloadUrl = playback.url;
                            resolvedTrack = playback.track;
                        } catch (Exception e2) {
                            throw new Exception("解析失败：" + (e2.getMessage() != null ? e2.getMessage() : "无可用链接"));
                        }
                    }
                    final String finalUrl = downloadUrl;
                    final Track finalTrack = resolvedTrack;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            resolvingDialog.dismiss();
                            // 弹出选择：应用内下载 或 用其他应用下载
                            showDownloadMethodDialog(finalTrack, quality, label, finalUrl);
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            resolvingDialog.dismiss();
                            toast("下载失败：" + (error.getMessage() == null ? "没有可下载链接" : error.getMessage()));
                        }
                    });
                }
            }
        });
    }

    // 弹出选择：应用内下载 或 用其他应用下载
    private void showDownloadMethodDialog(final Track track, final String quality, final String label, final String downloadUrl) {
        new AlertDialog.Builder(this)
            .setTitle("下载方式")
            .setMessage("歌曲：" + track.title + "\n音质：" + label + "\n\n选择下载方式：")
            .setPositiveButton("应用内下载", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    startDownloadWithProgress(track, quality, label, downloadUrl);
                }
            })
            .setNeutralButton("用其他应用下载", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    shareDownloadUrl(track, downloadUrl);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    // 用其他应用（浏览器等）打开下载链接
    private void shareDownloadUrl(Track track, String urlText) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(urlText), "audio/*");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 如果系统没有处理音频的app，降级为用浏览器打开
            if (intent.resolveActivity(getPackageManager()) == null) {
                intent.setDataAndType(Uri.parse(urlText), "*/*");
            }
            try {
                startActivity(Intent.createChooser(intent, "选择应用下载"));
            } catch (Exception e) {
                // 再降级：直接复制链接到剪贴板
                ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("下载链接", urlText));
                toast("已复制下载链接到剪贴板，请在浏览器中粘贴打开");
            }
        } catch (Exception e) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("下载链接", urlText));
            toast("已复制下载链接到剪贴板，请在浏览器中粘贴打开");
        }
    }

    // 应用内下载，带真实进度条
    private void startDownloadWithProgress(final Track track, final String quality, final String label, final String downloadUrl) {
        final android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("下载中");
        progressDialog.setMessage("歌曲：" + track.title + "\n音质：" + label + "\n正在连接...");
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setCancelable(false);
        progressDialog.show();

        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    final File file = saveMusicFileWithProgress(track, quality, downloadUrl, progressDialog);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progressDialog.dismiss();
                            toast("下载完成：" + file.getName());
                            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                        }
                    });
                } catch (final Exception error) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progressDialog.dismiss();
                            // 应用内下载失败，提示用户用其他方式
                            new AlertDialog.Builder(MainActivity.this)
                                .setTitle("下载失败")
                                .setMessage("应用内下载失败：" + (error.getMessage() != null ? error.getMessage() : "未知错误") + "\n\n是否尝试用其他应用下载？")
                                .setPositiveButton("用其他应用", new android.content.DialogInterface.OnClickListener() {
                                    @Override public void onClick(android.content.DialogInterface d, int w) {
                                        shareDownloadUrl(track, downloadUrl);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                        }
                    });
                }
            }
        });
    }

    private File saveMusicFileWithProgress(Track track, String quality, String urlText, final android.app.ProgressDialog progressDialog) throws Exception {
        File musicDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "听含新宇");
        if (!musicDir.exists() && !musicDir.mkdirs()) throw new Exception("无法创建音乐目录，请检查存储权限");
        String ext = "flac".equals(quality) || urlText.toLowerCase(Locale.ROOT).contains(".flac") ? ".flac" : ".mp3";
        String artist = track.artist == null || track.artist.length() == 0 ? "未知歌手" : track.artist;
        String base = safeFileName(track.title + " - " + artist + " [" + quality + "]");
        File out = new File(musicDir, base + ext);
        int index = 1;
        while (out.exists()) {
            out = new File(musicDir, base + " (" + index + ")" + ext);
            index++;
        }
        HttpURLConnection conn = (HttpURLConnection) new URL(urlText).openConnection();
        conn.setConnectTimeout(12000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) TingHanXinYuPlayer/1.4");
        conn.setRequestProperty("Accept", "*/*");
        conn.setRequestProperty("Referer", refererFor(track.source));
        conn.setInstanceFollowRedirects(true);
        int code = conn.getResponseCode();
        if (code >= 400) throw new Exception("下载链接不可用，HTTP " + code);
        final long totalBytes = conn.getContentLength();
        InputStream input = conn.getInputStream();
        FileOutputStream output = new FileOutputStream(out);
        byte[] buffer = new byte[16384];
        int len;
        long downloaded = 0;
        int lastPercent = 0;
        while ((len = input.read(buffer)) != -1) {
            output.write(buffer, 0, len);
            downloaded += len;
            if (totalBytes > 0) {
                final int percent = (int)(downloaded * 100 / totalBytes);
                if (percent > lastPercent) {
                    lastPercent = percent;
                    final long downloadedMB = downloaded / (1024 * 1024);
                    final long totalMB = totalBytes / (1024 * 1024);
                    final int finalPercent = percent;
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            progressDialog.setProgress(finalPercent);
                            progressDialog.setMessage("歌曲：" + track.title + "\n音质：" + quality + "\n已下载 " + downloadedMB + "MB / " + totalMB + "MB");
                        }
                    });
                }
            }
        }
        output.close();
        input.close();
        conn.disconnect();
        if (out.length() < 1024) throw new Exception("下载文件太小，可能不是音频");
        return out;
    }

    private String safeFileName(String name) {
        String value = name == null ? "未知歌曲" : name;
        value = value.replaceAll("[\\\\/:*?\"<>|\\n\\r\\t]", "_").trim();
        if (value.length() == 0) value = "未知歌曲";
        if (value.length() > 80) value = value.substring(0, 80);
        return value;
    }

    private void addHeader(String title, String subtitle) {
        TextView h = text(title, 34, true);
        h.setTextColor(TEXT);
        content.addView(h, lp(-1, -2, 0, 8, 0, 0));
        TextView s = small(subtitle);
        s.setTextColor(MUTED);
        content.addView(s, lp(-1, -2, 0, 0, 0, 16));
    }

    private void addTopBackButton(String title) {
        LinearLayout top = row();
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button back = controlButton("返回", new View.OnClickListener() {
            @Override public void onClick(View v) { goBack(); }
        });
        top.addView(back, new LinearLayout.LayoutParams(dp(82), dp(44)));
        TextView tv = text(title, 24, true);
        tv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        top.addView(tv, new LinearLayout.LayoutParams(0, dp(44), 1));
        content.addView(top, lp(-1, -2, 0, 0, 0, 14));
    }

    private void goBack() {
        if (previousPageAction != null) {
            Runnable action = previousPageAction;
            previousPageAction = null;
            action.run();
        } else {
            transitionToMain();
        }
    }

    private void transitionToMain() {
        showMainPage();
    }

    private LinearLayout trackRow(Track track, View.OnClickListener clickListener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(8), dp(14), dp(8));
        row.setBackgroundColor(CARD2);
        row.setOnClickListener(clickListener);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // 封面图
        ImageView coverView = new ImageView(this);
        coverView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int coverSize = dp(52);
        LinearLayout.LayoutParams coverLp = new LinearLayout.LayoutParams(coverSize, coverSize);
        coverLp.setMargins(0, 0, dp(10), 0);
        row.addView(coverView, coverLp);
        if (track.coverUrl != null && track.coverUrl.length() > 0) {
            loadCover(track.coverUrl, coverView);
        } else {
            coverView.setBackgroundColor(CARD);
        }

        // 文字信息（垂直布局）
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = text(track.title, 16, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        String artist = track.artist == null || track.artist.length() == 0 ? "未知歌手" : track.artist;
        String album = track.album == null || track.album.length() == 0 ? "未知专辑" : track.album;
        String src = sourceName(track.source);
        row.setContentDescription(track.title + "，歌手 " + artist + "，专辑 " + album + "，来源 " + src);
        TextView meta = text(artist + " · " + album + " · " + src, 13, false);
        meta.setTextColor(0xFFB0B0B0);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(title);
        info.addView(meta, lp(-1, -2, 0, 4, 0, 0));
        row.addView(info, new LinearLayout.LayoutParams(0, -1, 1f));
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundColor(CARD);
        content.addView(spacer(1), new LinearLayout.LayoutParams(1, dp(10)));
        return card;
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    private TextView text(String value, int sp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(value);
        tv.setTextColor(TEXT);
        tv.setTextSize(sp);
        if (bold) tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView small(String value) {
        TextView tv = text(value, 13, false);
        tv.setTextColor(MUTED);
        return tv;
    }

    private Button pillButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(TEXT);
        button.setAllCaps(false);
        button.setOnClickListener(listener);
        button.setBackgroundColor(CARD2);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setTextSize(16);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(54));
        button.setContentDescription(text);
        button.setLayoutParams(lp(-1, dp(54), 0, 7, 0, 7));
        return button;
    }

    private Button controlButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(TEXT);
        button.setBackgroundColor(CARD2);
        button.setOnClickListener(listener);
        button.setMinHeight(dp(44));
        button.setContentDescription(text);
        return button;
    }

    private View spacer(int size) {
        View view = new View(this);
        view.setBackgroundColor(Color.TRANSPARENT);
        return view;
    }

    private LinearLayout.LayoutParams lp(int w, int h, int l, int t, int r, int b) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(w, h);
        params.setMargins(dp(l), dp(t), dp(r), dp(b));
        return params;
    }

    private void loading(boolean show) {
        if (show) {
            if (loadingBar == null) {
                loadingBar = new ProgressBar(this);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER);
                root.addView(loadingBar, params);
            }
            loadingBar.setVisibility(View.VISIBLE);
        } else if (loadingBar != null) {
            loadingBar.setVisibility(View.GONE);
        }
    }

    private void updateMiniPlayer() {
        Track track = currentIndex >= 0 && currentIndex < playlist.size() ? playlist.get(currentIndex) : null;
        boolean playing = track != null && mediaPlayer != null;
        // 控制主页底部播放器区域的显示/隐藏
        if (bottomArea != null) bottomArea.setVisibility(playing ? View.VISIBLE : View.GONE);
        if (playerCtrl != null) playerCtrl.setVisibility(playing ? View.VISIBLE : View.GONE);
        if (track != null) {
            // 同步当前详情页追踪的歌曲
            currentFullPlayerTrack = track;
            // 更新左下角专辑图（仅在切换歌曲时加载，放到后台线程）
            String trackKey = track.source + ":" + track.id;
            if (!trackKey.equals(lastAlbumCoverTrack)) {
                lastAlbumCoverTrack = trackKey;
                // 如果详情页正在显示，且播放的歌曲变了，自动刷新整个详情页（封面+标题+歌手+歌词全同步）
                if ("fullPlayer".equals(content.getTag()) && fullPlayerCoverView != null) {
                    if (currentIndex >= 0 && currentIndex < playlist.size()) {
                        final Track refreshTrack = playlist.get(currentIndex);
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                showFullPlayer(refreshTrack);
                            }
                        });
                    }
                }
                worker.execute(new Runnable() {
                    @Override public void run() {
                        try {
                            String cover = resolver.albumCover(track);
                            if (cover != null && cover.length() > 0) {
                                track.coverUrl = cover;
                                loadCoverFromUrl(cover, albumArtView);
                            }
                        } catch (Exception ignored) {}
                    }
                });
            }
        } else {
            lastAlbumCoverTrack = "";
        }
    }

    private void loadCoverFromUrl(final String coverUrl, final ImageView target) {
        if (target == null) return;
        worker.execute(new Runnable() {
            @Override public void run() {
                try {
                    java.net.URL url = new java.net.URL(coverUrl);
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                    conn.setDoInput(true);
                    conn.setConnectTimeout(5000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
                    String referer = guessReferer(coverUrl);
                    if (referer != null && referer.length() > 0) {
                        conn.setRequestProperty("Referer", referer);
                    }
                    conn.connect();
                    final android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(conn.getInputStream());
                    conn.disconnect();
                    if (bmp != null) {
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                target.setImageBitmap(bmp);
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        });
    }

    private void updateHeartVisibility() {
        // 爱心已移至标签栏，不再需要浮动按钮
        if (heartMenuButton != null) heartMenuButton.setVisibility(View.GONE);
        // 底部标签栏始终显示
        if (pageTabViews != null) {
            for (TextView tv : pageTabViews) {
                if (tv != null) tv.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateProgress() {
        try {
            if (mediaPlayer != null && mediaPlayer.getDuration() > 0) {
                if (!userSeeking && seekBar != null) {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition() * 1000 / mediaPlayer.getDuration());
                }
                if (currentTimeView != null) currentTimeView.setText(formatTime(mediaPlayer.getCurrentPosition()));
                if (durationView != null) durationView.setText(formatTime(mediaPlayer.getDuration()));
                updateActiveLyric(mediaPlayer.getCurrentPosition());
                updateLoveProgress();
            }
        } catch (Exception ignored) {}
    }

    private String formatTime(int ms) {
        int total = Math.max(0, ms / 1000);
        return String.format(Locale.CHINA, "%02d:%02d", total / 60, total % 60);
    }

    private void releasePlayer() {
        releaseEqualizer();
        if (mediaPlayer != null) {
            try { mediaPlayer.reset(); } catch (Exception ignored) {}
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    /* ====== 音频焦点管理 ====== */
    private void requestAudioFocus() {
        if (audioManager == null) return;
        audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private void abandonAudioFocus() {
        if (audioManager == null) return;
        audioManager.abandonAudioFocus(audioFocusChangeListener);
    }

    private final AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override public void onAudioFocusChange(int focusChange) {
            if (mediaPlayer == null) return;
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_LOSS:
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        updatePlaybackState();
                        showPlayerNotification();
                    }
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (!mediaPlayer.isPlaying()) {
                        mediaPlayer.start();
                        updatePlaybackState();
                        showPlayerNotification();
                    }
                    break;
            }
        }
    };

    /* ====== WakeLock 管理 ====== */
    private void acquireWakeLock() {
        if (wakeLock != null && !wakeLock.isHeld()) {
            try { wakeLock.acquire(); } catch (Exception ignored) {}
        }
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
    }

    private void copyCurrentUrl() {
        if (currentUrl.length() == 0) {
            toast("当前没有播放链接");
            return;
        }
        ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("播放链接", currentUrl));
        toast("已复制播放链接");
    }

    private void toast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String platformKey(int index) {
        String[] keys = {"wy", "tx", "kw", "kg", "mg", "xm", "bl"};
        return keys[Math.max(0, Math.min(index, keys.length - 1))];
    }

    private String sourceName(String key) {
        if ("tx".equals(key)) return "QQ音乐";
        if ("kw".equals(key)) return "酷我";
        if ("kg".equals(key)) return "酷狗";
        if ("mg".equals(key)) return "咪咕";
        if ("xm".equals(key)) return "喜马拉雅";
        if ("bl".equals(key)) return "哔哩哔哩";
        if ("dy".equals(key)) return "抖音";
        return "网易云";
    }

    private void savePlaylist() {
        JSONArray array = new JSONArray();
        try {
            for (Track track : playlist) array.put(track.toJson());
        } catch (Exception ignored) {}
        prefs.edit().putString(PREF_PLAYLIST, array.toString()).apply();
    }

    private void loadPlaylist() {
        playlist.clear();
        try {
            JSONArray array = new JSONArray(prefs.getString(PREF_PLAYLIST, "[]"));
            for (int i = 0; i < array.length(); i++) playlist.add(Track.fromJson(array.getJSONObject(i)));
        } catch (Exception ignored) {}
    }

    public static class GradientBackground extends View {
        private final Paint paint = new Paint(1);
        private int lastW, lastH; private Shader rectShader, glow1, glow2; public GradientBackground(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w != lastW || h != lastH) {
                lastW = w; lastH = h;
                rectShader = new LinearGradient(0, 0, w, h, Color.BLACK, Color.parseColor("#151515"), Shader.TileMode.CLAMP);
                glow1 = new RadialGradient(w * .22f, 0, w * .7f, Color.argb(40, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP);
                glow2 = new RadialGradient(w * .82f, h * .22f, w * .52f, Color.argb(28, 255, 255, 255), Color.TRANSPARENT, Shader.TileMode.CLAMP);
            }
            paint.setShader(rectShader);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(glow1);
            canvas.drawCircle(w * .22f, 0, w * .7f, paint);
            paint.setShader(glow2);
            canvas.drawCircle(w * .82f, h * .22f, w * .52f, paint);
            paint.setShader(null);
        }
    }

    public static class DiscView extends View {
        private final Paint paint = new Paint(1);
        private boolean playing = false;
        private float angle = 0;
        private RadialGradient discShader;
        private int lastDW, lastDH;
        public DiscView(Context context) { super(context); }
        public void setPlaying(boolean playing) {
            this.playing = playing;
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float r = Math.min(w, h) * .46f;
            float cx = w / 2f;
            float cy = h / 2f;
            if (playing) angle += 1.8f;
            canvas.rotate(angle, cx, cy);
            if (w != lastDW || h != lastDH || discShader == null) {
                lastDW = w; lastDH = h;
                discShader = new RadialGradient(cx, cy, r, new int[]{Color.WHITE, Color.parseColor("#777777"), Color.parseColor("#202020")}, null, Shader.TileMode.CLAMP);
            }
            paint.setShader(discShader);
            canvas.drawCircle(cx, cy, r, paint);
            paint.setShader(null);
            paint.setColor(Color.BLACK);
            canvas.drawCircle(cx, cy, r * .28f, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setColor(Color.argb(110, 255, 255, 255));
            for (int i = 1; i <= 4; i++) canvas.drawCircle(cx, cy, r * (.36f + i * .12f), paint);
            paint.setStyle(Paint.Style.FILL);
            if (playing) postInvalidateDelayed(16);
        }
    }

    public static class LoveMarqueeView extends View {
        private final Paint paint = new Paint(1);
        private float offset = 0;
        private final String text = "周新爱王雨涵    王宇含爱周新    ";
        private LinearGradient textShader;
        private int lastW;

        public LoveMarqueeView(Context context) {
            super(context);
            paint.setAntiAlias(true);
            paint.setTextSize(42f * context.getResources().getDisplayMetrics().scaledDensity);
            paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            setFocusable(true);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawColor(Color.BLACK);
            int w = getWidth();
            if (w != lastW || textShader == null) {
                lastW = w;
                textShader = new LinearGradient(0, 0, w, 0,
                    new int[]{Color.parseColor("#FFD6E8"), Color.parseColor("#FF7BB8"), Color.parseColor("#D81B72")},
                    new float[]{0f, 0.52f, 1f},
                    Shader.TileMode.CLAMP);
            }
            paint.setShader(textShader);
            float width = paint.measureText(text);
            if (width <= 0) width = w;
            offset -= 2.2f;
            if (offset < -width) offset = w;
            float y = getHeight() / 2f - (paint.descent() + paint.ascent()) / 2f;
            for (float x = offset; x < w + width; x += width) {
                canvas.drawText(text, x, y, paint);
            }
            paint.setShader(null);
            postInvalidateDelayed(16);
        }
    }

    public static class LoveMenuButton extends View {
        private final Paint paint = new Paint(1);
        private final Paint textPaint = new Paint(1);
        private float pulse = 0;
        private LinearGradient btnShader;
        private int lastW, lastH;

        public LoveMenuButton(Context context) {
            super(context);
            setFocusable(true);
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(13f * context.getResources().getDisplayMetrics().scaledDensity);
            textPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = getHeight() / 2f;
            pulse += 0.045f;
            int alpha = 190 + (int) (45 * Math.abs(Math.sin(pulse)));
            int w = getWidth(), h = getHeight();
            if (w != lastW || h != lastH || btnShader == null) {
                lastW = w; lastH = h;
                btnShader = new LinearGradient(0, 0, w, h,
                    new int[]{Color.parseColor("#FFD6E8"), Color.parseColor("#FF7BB8"), Color.parseColor("#D81B72")},
                    null,
                    Shader.TileMode.CLAMP);
            }
            paint.setShader(btnShader);
            paint.setAlpha(alpha);
            canvas.drawRoundRect(0, 0, w, h, radius, radius, paint);
            paint.setShader(null);
            paint.setAlpha(255);

            textPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("♥", getHeight() * 0.22f, getHeight() * 0.68f, textPaint);
            canvas.drawText("爱心", getHeight() * 0.68f, getHeight() * 0.64f, textPaint);

            postInvalidateDelayed(32);
        }
    }

    static class LyricLine {
        int timeMs;
        String text;
        LyricLine(int timeMs, String text) {
            this.timeMs = timeMs;
            this.text = text == null ? "" : text;
        }
    }

    static class Track {
        String id;
        String title;
        String artist;
        String album;
        String source;
        String coverUrl;
        String albumId; // 喜马拉雅专辑ID
        String playUrl; // 抖音等直接带播放链接的平台使用
        String type; // 类型：video/audio等
        String authorId; // 作者/UP主ID（B站mid，抖音sec_uid）
        Track(String id, String title, String artist, String album, String source) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.source = source;
            this.coverUrl = "";
            this.type = "";
        }
        Track(String id, String title, String artist, String album, String source, String coverUrl) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.source = source;
            this.coverUrl = coverUrl == null ? "" : coverUrl;
            this.type = "";
        }
        Track(String id, String title, String artist, String album, String source, String coverUrl, String albumId) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.album = album;
            this.source = source;
            this.coverUrl = coverUrl == null ? "" : coverUrl;
            this.albumId = albumId == null ? "" : albumId;
            this.type = "";
        }
        JSONObject toJson() throws Exception {
            JSONObject object = new JSONObject();
            object.put("id", id);
            object.put("title", title);
            object.put("artist", artist);
            object.put("album", album);
            object.put("source", source);
            object.put("coverUrl", coverUrl);
            object.put("albumId", albumId == null ? "" : albumId);
            object.put("playUrl", playUrl == null ? "" : playUrl);
            object.put("type", type == null ? "" : type);
            object.put("authorId", authorId == null ? "" : authorId);
            return object;
        }
        static Track fromJson(JSONObject object) throws Exception {
            Track t = new Track(object.optString("id"), object.optString("title"), object.optString("artist"), object.optString("album"), object.optString("source", "wy"), object.optString("coverUrl"), object.optString("albumId"));
            t.playUrl = object.optString("playUrl", "");
            t.type = object.optString("type", "");
            t.authorId = object.optString("authorId", "");
            return t;
        }
    }

    static class ResolvedPlayback {
        Track track;
        String url;
        ResolvedPlayback(Track track, String url) {
            this.track = track;
            this.url = url;
        }
    }

    static class SearchResult {
        String id;
        String name;
        String artist;
        String album;
        String source;
        String coverUrl;
        String playUrl; // 喜马拉雅等直接带播放链接的平台使用
        String albumId; // 喜马拉雅专辑ID
        String type; // 类型：video/audio/media_bangumi/media_ft等
        String extra; // 额外信息（如sec_uid等）
        String authorId; // 作者/UP主ID（B站mid，抖音sec_uid）
        Track toTrack() {
            Track t = new Track(id, name, artist, album, source, coverUrl, albumId);
            t.playUrl = playUrl;
            t.type = type == null ? "" : type;
            t.authorId = authorId;
            return t;
        }
    }

    static class MusicResolver {
        private static final int REQUEST_TIMEOUT = 5000;
        private static final int OVERALL_TIMEOUT = 15000;
        private static final int FALLBACK_TIMEOUT = 6000;
        private static final String XINGHAI_MAIN_API = "https://music-api.gdstudio.xyz/api.php?use_xbridge3=true&loader_name=forest&need_sec_link=1&sec_link_scene=im&theme=light";
        private static final String[] QUALITY_ORDER = {"flac", "320k", "192k", "128k"};
        private static final String[] METING_APIS = {"https://music.3e0.cn/", "https://api.injahow.cn/meting/", "https://api.moeyao.cn/meting/", "https://meting-api.mcloc.cn/"};
        private static final String[] OFFICIAL_CDN_DOMAINS = {"isure.stream.qqmusic.qq.com", "aqqmusic.tc.qq.com", "dl.stream.qqmusic.qq.com", "streamoc.music.tc.qq.com", "isure2.stream.qqmusic.qq.com", "wx.music.tc.qq.com", "m7.music.126.net", "m8.music.126.net", "m10.music.126.net", "m701.music.126.net", "m702.music.126.net", "fsandroid.tx.kugou.com", "fsweb.tx.kugou.com", "trackercdnbk.kugou.com"};

        private final Map<String, String> xinghaiMap = new HashMap<String, String>();
        private final Map<String, String> metingMap = new HashMap<String, String>();
        private final List<Map<String, String>> haitangTemplates = new ArrayList<Map<String, String>>();
        private final List<Map<String, String>> changqingTemplates = new ArrayList<Map<String, String>>();
        private final List<Map<String, String>> nianxinTemplates = new ArrayList<Map<String, String>>();
        private final List<String> neteaseUnlockApis = new ArrayList<String>();
        private final Map<String, Integer> qualityScore = new HashMap<String, Integer>();
        private final Map<String, String> xmUrlCache = new HashMap<String, String>(); // 喜马拉雅播放链接缓存
        private String biliCookie = ""; // 哔哩哔哩有效 Cookie（含 buvid3）
        private final ExecutorService pool = Executors.newFixedThreadPool(16);

        MusicResolver() {
            xinghaiMap.put("wy", "netease"); xinghaiMap.put("kg", "kugou"); xinghaiMap.put("kw", "kuwo"); xinghaiMap.put("mg", "migu");
            metingMap.put("wy", "netease"); metingMap.put("tx", "tencent"); metingMap.put("kg", "kugou"); metingMap.put("kw", "kuwo"); metingMap.put("mg", "migu");
            // 海棠主站
            Map<String, String> ht1 = new HashMap<String, String>();
            ht1.put("tx", "https://musicapi.haitangw.net/qq/qq.php?id={id}&level={level}");
            ht1.put("kg", "https://musicapi.haitangw.net/kgqq/kg.php?id={id}&level={level}");
            ht1.put("kw", "https://musicapi.haitangw.net/music/kw.php?id={id}&level={level}");
            ht1.put("wy", "https://musicapi.haitangw.net/wy/wy.php?id={id}&level={level}");
            haitangTemplates.add(ht1);
            // 海棠备用
            Map<String, String> ht2 = new HashMap<String, String>();
            ht2.put("tx", "https://music.haitangw.cc/qq/qq.php?id={id}&level={level}");
            ht2.put("kg", "https://music.haitangw.cc/kgqq/kg.php?id={id}&level={level}");
            ht2.put("kw", "https://music.haitangw.cc/music/kw.php?id={id}&level={level}");
            ht2.put("wy", "https://music.haitangw.cc/wy/wy.php?id={id}&level={level}");
            haitangTemplates.add(ht2);
            // 长青SVIP
            Map<String, String> cq1 = new HashMap<String, String>();
            cq1.put("tx", "http://175.27.166.236/kgqq/qq.php?type=mp3&id={id}&level={level}");
            cq1.put("wy", "http://175.27.166.236/wy/wy.php?type=mp3&id={id}&level={level}");
            cq1.put("kw", "https://musicapi.haitangw.net/music/kw.php?type=mp3&id={id}&level={level}");
            cq1.put("kg", "https://music.haitangw.cc/kgqq/kg.php?type=mp3&id={id}&level={level}");
            cq1.put("mg", "https://music.haitangw.cc/musicapi/mg.php?type=mp3&id={id}&level={level}");
            changqingTemplates.add(cq1);
            // 念心SVIP
            Map<String, String> nx1 = new HashMap<String, String>();
            nx1.put("tx", "https://music.nxinxz.com/kgqq/tx.php?id={id}&level={level}&type=mp3");
            nx1.put("wy", "http://music.nxinxz.com/wy.php?id={id}&level={level}&type=mp3");
            nx1.put("kw", "http://music.nxinxz.com/kw.php?id={id}&level={level}&type=mp3");
            nx1.put("kg", "https://music.nxinxz.com/kgqq/kg.php?id={id}&level={level}&type=mp3");
            nx1.put("mg", "http://music.nxinxz.com/mg.php?id={id}&level={level}&type=mp3");
            nianxinTemplates.add(nx1);
            // 网易云解锁API：优先用无损/320k真实链接，避免30秒试听
            neteaseUnlockApis.add("https://music-api.gdstudio.xyz/api.php?types=url&source=netease_unlock&id={id}&br={br}&need_sec_link=1");
            neteaseUnlockApis.add("https://api.paugram.com/netease/?id={id}&type=url");
            neteaseUnlockApis.add("https://api.i-meto.com/meting/api?server=netease&type=url&id={id}&br={br}");
            qualityScore.put("flac", 1000); qualityScore.put("320k", 800); qualityScore.put("192k", 600); qualityScore.put("128k", 400);
        }

        List<SearchResult> search(String platform, String keyword) throws Exception {
            String cleanKeyword = keyword == null ? "" : keyword.trim();
            if (cleanKeyword.length() == 0) throw new Exception("请输入关键词");
            List<String> errors = new ArrayList<String>();
            List<SearchResult> merged = new ArrayList<SearchResult>();
            List<String> variants = keywordVariants(cleanKeyword);
            String[] fallbackPlatforms = {"wy", "tx", "kg", "kw", "mg", "xm", "blv", "dyv"};
            if ("all".equals(platform)) {
                for (String variant : variants) {
                    for (String fallback : fallbackPlatforms) {
                        try {
                            appendUnique(merged, searchSinglePlatform(fallback, variant));
                            if (merged.size() >= 120) break;
                        } catch (Exception error) {
                            if (errors.size() < 5 && error.getMessage() != null) errors.add(sourceNameForResolver(fallback) + ":" + error.getMessage());
                        }
                    }
                    if (merged.size() >= 120) break;
                }
            } else {
                for (String variant : variants) {
                    try {
                        appendUnique(merged, searchSinglePlatform(platform, variant));
                    } catch (Exception error) {
                        if (error.getMessage() != null && errors.size() < 5) errors.add(sourceNameForResolver(platform) + ":" + error.getMessage());
                    }
                    if (merged.size() >= 120) break;
                }
            }
            if (isTiaFarewellQuery(cleanKeyword) && ("all".equals(platform) || "wy".equals(platform))) {
                appendUnique(merged, specialTiaFarewellResults());
            }
            if (!merged.isEmpty()) {
                for (SearchResult result : merged) {
                    if (result.coverUrl == null || result.coverUrl.length() == 0) {
                        result.coverUrl = fallbackAlbumCover(result.source, result.id);
                    }
                }
                return merged;
            }
            throw new Exception(errors.isEmpty() ? "没有搜索到歌曲" : join(errors));
        }

        private boolean isTiaFarewellQuery(String keyword) {
            String text = normalizeName(keyword);
            return text.contains("剩下的告别") || text.contains("盛夏的告别") || text.contains("剩下告别") || text.contains("盛夏告别");
        }

        private List<SearchResult> specialTiaFarewellResults() {
            List<SearchResult> list = new ArrayList<SearchResult>();
            SearchResult result = new SearchResult();
            result.id = "2062885837";
            result.name = "盛夏的告别";
            result.artist = "袁娅维TIA RAY";
            result.album = "我的人间烟火 电视剧原声带";
            result.source = "wy";
            result.coverUrl = "";
            list.add(result);
            return list;
        }

        private List<String> keywordVariants(String keyword) {
            List<String> variants = new ArrayList<String>();
            addVariant(variants, keyword);
            addVariant(variants, keyword.replace(" ", ""));
            addVariant(variants, keyword.replaceAll("[《》()（）\\[\\]【】]", " ").trim());
            if (keyword.contains("剩下")) addVariant(variants, keyword.replace("剩下", "盛夏"));
            if (keyword.contains("盛夏")) addVariant(variants, keyword.replace("盛夏", "剩下"));
            if (keyword.contains("袁娅维") && !keyword.toLowerCase(Locale.ROOT).contains("tia")) addVariant(variants, keyword.replace("袁娅维", "袁娅维TIA RAY"));
            addVariant(variants, keyword.replaceAll("(?i)(feat\\.|ft\\.|with|和|、|&).*$", "").trim());
            addVariant(variants, keyword.replaceAll("(?i)\\b(live|remix|cover|dj|伴奏|现场|版|完整版|正式版|试听版)\\b", " ").trim());
            addVariant(variants, keyword.replaceAll("(?i)\\s*-\\s*(live|remix|cover|dj|伴奏|现场|版|完整版|正式版|试听版).*", "").trim());
            addVariant(variants, keyword.replaceAll("\\s+", " ").trim());
            String[] separators = {" - ", "-", "_", "—", "–", "｜", "|"};
            for (String sep : separators) {
                if (keyword.contains(sep)) {
                    String[] parts = keyword.split(java.util.regex.Pattern.quote(sep));
                    for (String part : parts) addVariant(variants, part.trim());
                }
            }
            return variants;
        }

        private void addVariant(List<String> variants, String value) {
            if (value == null) return;
            String clean = value.trim();
            if (clean.length() == 0) return;
            if (!variants.contains(clean)) variants.add(clean);
        }

        private String fallbackAlbumCover(String platform, String id) {
            return "";
        }

        private void appendUnique(List<SearchResult> target, List<SearchResult> source) {
            for (SearchResult item : source) {
                if (item == null || item.id == null || item.source == null) continue;
                boolean exists = false;
                for (SearchResult old : target) {
                    if (old.id != null && old.source != null && old.id.equals(item.id) && old.source.equals(item.source)) {
                        exists = true;
                        if ((old.coverUrl == null || old.coverUrl.length() == 0) && item.coverUrl != null) old.coverUrl = item.coverUrl;
                        if ((old.album == null || old.album.length() == 0) && item.album != null) old.album = item.album;
                        break;
                    }
                }
                if (!exists) target.add(item);
                if (target.size() >= 180) return;
            }
        }

        private List<SearchResult> searchSinglePlatform(String platform, String cleanKeyword) throws Exception {
            List<String> urls = new ArrayList<String>();
            if ("tx".equals(platform)) {
                for (int page = 1; page <= 5; page++) {
                    urls.add("https://c.y.qq.com/soso/fcgi-bin/client_search_cp?p=" + page + "&n=50&w=" + enc(cleanKeyword) + "&format=json");
                    urls.add("https://c.y.qq.com/soso/fcgi-bin/search_for_qq_cp?p=" + page + "&n=50&w=" + enc(cleanKeyword) + "&format=json&platform=h5");
                }
                urls.add("https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg?key=" + enc(cleanKeyword) + "&format=json");
            } else if ("kw".equals(platform)) {
                for (int page = 0; page <= 4; page++) {
                    urls.add("http://search.kuwo.cn/r.s?all=" + enc(cleanKeyword) + "&ft=music&client=kt&pn=" + page + "&rn=50&rformat=json&encoding=utf8");
                }
                for (int page = 1; page <= 5; page++) {
                    urls.add("https://www.kuwo.cn/api/www/search/searchMusicBykeyWord?key=" + enc(cleanKeyword) + "&pn=" + page + "&rn=50&httpsStatus=1&reqId=tinghanxinyu");
                }
            } else if ("kg".equals(platform)) {
                for (int page = 1; page <= 5; page++) {
                    urls.add("http://mobilecdn.kugou.com/api/v3/search/song?format=json&keyword=" + enc(cleanKeyword) + "&page=" + page + "&pagesize=50");
                    urls.add("https://songsearch.kugou.com/song_search_v2?keyword=" + enc(cleanKeyword) + "&page=" + page + "&pagesize=50&platform=WebFilter&filter=2&iscorrection=1&format=json");
                }
            } else if ("mg".equals(platform)) {
                for (int page = 1; page <= 5; page++) {
                    urls.add("https://c.musicapp.migu.cn/MIGUM2.0/v1.0/content/search_all.do?text=" + enc(cleanKeyword) + "&pageNo=" + page + "&pageSize=50&searchSwitch=%7Bsong%3A1%7D");
                }
            } else if ("xm".equals(platform)) {
                for (int page = 1; page <= 5; page++) {
                    urls.add("https://www.ximalaya.com/revision/search?core=track&kw=" + enc(cleanKeyword) + "&page=" + page + "&spellchecker=true&rows=50");
                }
            } else if ("blv".equals(platform)) {
                for (int page = 1; page <= 3; page++) {
                    urls.add("https://api.bilibili.com/x/web-interface/search/type?keyword=" + enc(cleanKeyword) + "&search_type=video&page=" + page);
                }
            } else if ("bla".equals(platform)) {
                for (int page = 1; page <= 3; page++) {
                    urls.add("https://api.bilibili.com/x/web-interface/search/type?keyword=" + enc(cleanKeyword) + "&search_type=audio&page=" + page);
                }
            } else if ("dyv".equals(platform)) {
                // 抖音视频搜索：使用第三方聚合接口
                for (int page = 1; page <= 3; page++) {
                    urls.add("https://douyin.wang/api/video/search?keyword=" + enc(cleanKeyword) + "&page=" + page);
                }
            } else if ("dya".equals(platform)) {
                // 抖音音频搜索：复用视频搜索，结果中过滤带音频的
                for (int page = 1; page <= 3; page++) {
                    urls.add("https://douyin.wang/api/video/search?keyword=" + enc(cleanKeyword) + "&page=" + page + "&type=audio");
                }
            } else if ("dyu".equals(platform)) {
                // 抖音用户搜索
                urls.add("https://douyin.wang/api/user/search?keyword=" + enc(cleanKeyword));
            } else {
                for (int page = 0; page <= 4; page++) {
                    urls.add("https://music.163.com/api/search/get/web?s=" + enc(cleanKeyword) + "&type=1&offset=" + (page * 50) + "&limit=50");
                }
                urls.add("https://music.163.com/api/search/suggest/web?s=" + enc(cleanKeyword) + "&limit=50");
            }
            // 星海搜索（仅网易云支持）
            if ("wy".equals(platform)) {
                urls.add(XINGHAI_MAIN_API + "&types=search&source=netease&name=" + enc(cleanKeyword));
            }
            // ====== 并发搜索：所有URL同时发出，谁先返回用谁 ======
            List<Callable<Response>> tasks = new ArrayList<Callable<Response>>();
            for (final String url : urls) {
                tasks.add(new Callable<Response>() {
                    @Override public Response call() throws Exception { return httpGet(url); }
                });
            }
            CompletionService<Response> completion = new ExecutorCompletionService<Response>(pool);
            List<Future<Response>> futures = new ArrayList<Future<Response>>();
            for (Callable<Response> task : tasks) futures.add(completion.submit(task));
            List<String> errors = new ArrayList<String>();
            List<SearchResult> merged = new ArrayList<SearchResult>();
            long deadline = System.currentTimeMillis() + OVERALL_TIMEOUT;
            try {
                for (int i = 0; i < tasks.size(); i++) {
                    long waitMs = deadline - System.currentTimeMillis();
                    if (waitMs <= 0) break;
                    Future<Response> future = completion.poll(waitMs, TimeUnit.MILLISECONDS);
                    if (future == null) break;
                    try {
                        appendUnique(merged, parseSearch(future.get().body, platform));
                        if (merged.size() >= 150) break;
                    } catch (Exception error) {
                        if (error.getMessage() != null && errors.size() < 3) errors.add(error.getMessage());
                    }
                }
            } finally {
                for (Future<Response> future : futures) future.cancel(true);
            }
            if (!merged.isEmpty()) return merged;
            throw new Exception(errors.isEmpty() ? "没有搜索到歌曲" : join(errors));
        }

        /** 哔哩哔哩：搜索用户 */
        java.util.List<SearchResult> searchBilibiliUsers(String keyword) throws Exception {
            String url = "https://api.bilibili.com/x/web-interface/search/type?keyword=" + enc(keyword) + "&search_type=bili_user&page=1";
            Response res = httpGet(url);
            Object parsed = parseJsonLike(res.body);
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            if (parsed instanceof org.json.JSONObject) {
                org.json.JSONObject obj = (org.json.JSONObject) parsed;
                if (obj.optInt("code", -1) == 0 && obj.has("data")) {
                    org.json.JSONObject data = obj.getJSONObject("data");
                    if (data.has("result") && data.get("result") instanceof org.json.JSONArray) {
                        org.json.JSONArray array = data.getJSONArray("result");
                        for (int i = 0; i < array.length(); i++) {
                            org.json.JSONObject item = array.getJSONObject(i);
                            SearchResult sr = new SearchResult();
                            sr.id = String.valueOf(item.optLong("mid", 0));
                            if ("0".equals(sr.id)) sr.id = firstString(item, "mid", "id");
                            sr.name = firstString(item, "uname", "title", "nickname");
                            sr.artist = firstString(item, "usign", "sign", "description");
                            if (sr.artist == null) sr.artist = "";
                            sr.album = "哔哩哔哩用户";
                            sr.coverUrl = firstString(item, "upic", "face", "avatar");
                            sr.source = "bl";
                            if (sr.id != null && sr.id.length() > 0 && sr.name != null && sr.name.length() > 0) {
                                results.add(sr);
                            }
                        }
                    }
                }
            }
            return results;
        }

        /** 哔哩哔哩：获取用户所有视频 */
        java.util.List<SearchResult> getBilibiliUserVideos(String mid) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            int page = 1;
            int emptyCount = 0;
            while (page <= 10 && emptyCount < 2 && results.size() < 200) {
                String url = "https://api.bilibili.com/x/space/arc/search?mid=" + enc(mid) + "&ps=30&pn=" + page;
                Response res = httpGet(url);
                Object parsed = parseJsonLike(res.body);
                if (parsed instanceof org.json.JSONObject) {
                    org.json.JSONObject obj = (org.json.JSONObject) parsed;
                    if (obj.optInt("code", -1) == 0 && obj.has("data")) {
                        org.json.JSONObject data = obj.getJSONObject("data");
                        if (data.has("list") && data.get("list") instanceof org.json.JSONObject) {
                            org.json.JSONObject list = data.getJSONObject("list");
                            if (list.has("vlist") && list.get("vlist") instanceof org.json.JSONArray) {
                                org.json.JSONArray vlist = list.getJSONArray("vlist");
                                if (vlist.length() == 0) {
                                    emptyCount++;
                                } else {
                                    emptyCount = 0;
                                    for (int i = 0; i < vlist.length(); i++) {
                                        org.json.JSONObject item = vlist.getJSONObject(i);
                                        SearchResult sr = new SearchResult();
                                        sr.id = firstString(item, "bvid", "aid");
                                        sr.name = firstString(item, "title", "description");
                                        if (sr.name != null) sr.name = sr.name.replaceAll("<[^>]+>", "");
                                        sr.artist = firstString(item, "author", "name", "uname");
                                        if (sr.artist == null) sr.artist = "";
                                        sr.album = "哔哩哔哩视频";
                                        sr.coverUrl = firstString(item, "pic", "cover");
                                        sr.source = "bl";
                                        sr.authorId = mid;
                                        if (sr.id != null && sr.id.length() > 0 && sr.name != null && sr.name.length() > 0) {
                                            results.add(sr);
                                        }
                                    }
                                }
                            } else {
                                emptyCount++;
                            }
                        } else {
                            emptyCount++;
                        }
                    } else {
                        emptyCount++;
                    }
                } else {
                    emptyCount++;
                }
                page++;
            }
            return results;
        }

        /** 哔哩哔哩：获取视频分P列表 */
        java.util.List<SearchResult> getBilibiliVideoPages(String bvid) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            // 1. 获取视频详情和分P信息
            String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + enc(bvid);
            Response res = httpGet(url);
            Object parsed = parseJsonLike(res.body);
            if (!(parsed instanceof org.json.JSONObject)) {
                throw new Exception("视频信息返回格式异常");
            }
            org.json.JSONObject obj = (org.json.JSONObject) parsed;
            if (obj.optInt("code", -1) != 0) {
                throw new Exception(obj.optString("message", "获取视频信息失败"));
            }
            org.json.JSONObject data = obj.optJSONObject("data");
            if (data == null) throw new Exception("视频信息为空");
            String title = data.optString("title", "未知标题");
            String ownerName = "";
            org.json.JSONObject owner = data.optJSONObject("owner");
            if (owner != null) ownerName = owner.optString("name", "");
            String cover = data.optString("pic", "");
            org.json.JSONArray pages = data.optJSONArray("pages");
            if (pages == null || pages.length() == 0) {
                // 没有分P，只有单集
                SearchResult sr = new SearchResult();
                sr.id = bvid;
                sr.name = title;
                sr.artist = ownerName;
                sr.album = "哔哩哔哩视频";
                sr.coverUrl = cover;
                sr.source = "bl";
                results.add(sr);
                return results;
            }
            // 有分P，每个分P作为一个条目
            for (int i = 0; i < pages.length(); i++) {
                org.json.JSONObject page = pages.getJSONObject(i);
                SearchResult sr = new SearchResult();
                long cid = page.optLong("cid", 0);
                sr.id = bvid + "|" + cid;
                String pageName = page.optString("part", "");
                if (pageName.length() == 0) pageName = title + " P" + (i + 1);
                sr.name = pageName;
                sr.artist = ownerName;
                sr.album = title;
                sr.coverUrl = cover;
                sr.source = "bl";
                results.add(sr);
            }
            return results;
        }

        /** 哔哩哔哩：搜索番剧和影视 */
        java.util.List<SearchResult> searchBilibiliBangumi(String keyword) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            // 同时搜索番剧和影视
            String[] searchTypes = {"media_bangumi", "media_ft"};
            for (String searchType : searchTypes) {
                try {
                    String url = "https://api.bilibili.com/x/web-interface/search/type?keyword=" + enc(keyword) + "&search_type=" + searchType + "&page=1";
                    Response res = httpGet(url);
                    Object parsed = parseJsonLike(res.body);
                    if (parsed instanceof org.json.JSONObject) {
                        org.json.JSONObject obj = (org.json.JSONObject) parsed;
                        if (obj.optInt("code", -1) == 0 && obj.has("data")) {
                            org.json.JSONObject data = obj.getJSONObject("data");
                            if (data.has("result") && data.get("result") instanceof org.json.JSONArray) {
                                org.json.JSONArray array = data.getJSONArray("result");
                                for (int i = 0; i < array.length(); i++) {
                                    try {
                                        org.json.JSONObject item = array.getJSONObject(i);
                                        SearchResult sr = new SearchResult();
                                        // media_id 作为番剧ID
                                        sr.id = String.valueOf(item.optInt("media_id", 0));
                                        if ("0".equals(sr.id) || sr.id.length() == 0) continue;
                                        String title = item.optString("title", "").replaceAll("<[^>]+>", "");
                                        if (title.length() == 0) title = item.optString("season_name", "未知标题");
                                        sr.name = title;
                                        sr.artist = item.optString("areas", "") + " " + item.optString("styles", "");
                                        if (sr.artist.trim().length() == 0) sr.artist = "哔哩哔哩";
                                        sr.album = "media_bangumi".equals(searchType) ? "番剧" : "影视";
                                        String cover = item.optString("cover", "");
                                        if (cover.startsWith("//")) cover = "https:" + cover;
                                        sr.coverUrl = cover;
                                        sr.source = "bl";
                                        sr.type = searchType; // 保存类型用于后续获取剧集
                                        results.add(sr);
                                    } catch (Exception ignored) {}
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (results.isEmpty()) throw new Exception("未找到相关番剧/影视");
            return results;
        }

        /** 哔哩哔哩：获取番剧/影视的所有剧集 */
        java.util.List<SearchResult> getBilibiliBangumiEpisodes(String mediaId, String bangumiTitle, String cover, String mediaType) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            // 使用番剧详情API获取剧集列表
            String url = "https://api.bilibili.com/pgc/view/web/season?season_id=" + enc(mediaId);
            Response res = httpGet(url);
            Object parsed = parseJsonLike(res.body);
            if (!(parsed instanceof org.json.JSONObject)) {
                throw new Exception("番剧信息返回格式异常");
            }
            org.json.JSONObject obj = (org.json.JSONObject) parsed;
            if (obj.optInt("code", -1) != 0) {
                throw new Exception(obj.optString("message", "获取番剧信息失败"));
            }
            org.json.JSONObject data = obj.optJSONObject("result");
            if (data == null) throw new Exception("番剧信息为空");
            
            String title = data.optString("title", bangumiTitle);
            if (cover == null || cover.length() == 0) {
                cover = data.optString("cover", "");
                if (cover.startsWith("//")) cover = "https:" + cover;
            }
            
            // 获取正片剧集列表
            org.json.JSONArray episodes = data.optJSONArray("episodes");
            if (episodes != null && episodes.length() > 0) {
                for (int i = 0; i < episodes.length(); i++) {
                    try {
                        org.json.JSONObject ep = episodes.getJSONObject(i);
                        SearchResult sr = new SearchResult();
                        // bvid|cid 格式
                        String bvid = ep.optString("bvid", "");
                        long cid = ep.optLong("cid", 0);
                        if (bvid.length() == 0 || cid == 0) {
                            // 尝试从share_url或其他字段获取
                            String epId = String.valueOf(ep.optLong("id", 0));
                            if (!"0".equals(epId)) {
                                // 用epid构造，后续播放时需要转换
                                sr.id = "ep|" + epId;
                            } else continue;
                        } else {
                            sr.id = bvid + "|" + cid;
                        }
                        String epTitle = ep.optString("long_title", "");
                        String epIndex = ep.optString("title", "");
                        if (epTitle.length() > 0) {
                            sr.name = epIndex + " " + epTitle;
                        } else {
                            sr.name = "第" + epIndex + "集";
                        }
                        // 去掉多余空格
                        sr.name = sr.name.trim();
                        sr.artist = title;
                        sr.album = bangumiTitle != null && bangumiTitle.length() > 0 ? bangumiTitle : title;
                        sr.coverUrl = cover;
                        sr.source = "bl";
                        sr.type = "video";
                        results.add(sr);
                    } catch (Exception ignored) {}
                }
            }
            
            // 如果没有episodes，尝试获取sections（用于一些影视）
            if (results.isEmpty()) {
                org.json.JSONArray sections = data.optJSONArray("sections");
                if (sections != null) {
                    for (int s = 0; s < sections.length(); s++) {
                        try {
                            org.json.JSONObject section = sections.getJSONObject(s);
                            org.json.JSONArray eps = section.optJSONArray("episodes");
                            if (eps != null) {
                                for (int i = 0; i < eps.length(); i++) {
                                    try {
                                        org.json.JSONObject ep = eps.getJSONObject(i);
                                        SearchResult sr = new SearchResult();
                                        String bvid = ep.optString("bvid", "");
                                        long cid = ep.optLong("cid", 0);
                                        if (bvid.length() > 0 && cid != 0) {
                                            sr.id = bvid + "|" + cid;
                                        } else {
                                            String epId = String.valueOf(ep.optLong("id", 0));
                                            if ("0".equals(epId)) continue;
                                            sr.id = "ep|" + epId;
                                        }
                                        String epTitle = ep.optString("long_title", "");
                                        String epIndex = ep.optString("title", "");
                                        if (epTitle.length() > 0) {
                                            sr.name = epIndex + " " + epTitle;
                                        } else {
                                            sr.name = "第" + epIndex + "集";
                                        }
                                        sr.name = sr.name.trim();
                                        sr.artist = title;
                                        sr.album = bangumiTitle != null && bangumiTitle.length() > 0 ? bangumiTitle : title;
                                        sr.coverUrl = cover;
                                        sr.source = "bl";
                                        sr.type = "video";
                                        results.add(sr);
                                    } catch (Exception ignored) {}
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            
            if (results.isEmpty()) throw new Exception("未找到可播放的剧集");
            return results;
        }

        org.json.JSONArray searchXimalayaAlbums(String keyword) throws Exception {
            String url = "https://www.ximalaya.com/revision/search?core=album&kw=" + enc(keyword) + "&page=1&spellchecker=true&rows=50";
            Response res = httpGet(url);
            Object parsed = parseJsonLike(res.body);
            if (parsed instanceof org.json.JSONObject) {
                org.json.JSONObject obj = (org.json.JSONObject) parsed;
                if (obj.has("data")) {
                    org.json.JSONObject data = obj.getJSONObject("data");
                    if (data.has("result")) {
                        org.json.JSONObject result = data.getJSONObject("result");
                        if (result.has("response")) {
                            org.json.JSONObject response = result.getJSONObject("response");
                            if (response.has("docs")) {
                                org.json.JSONArray docs = response.getJSONArray("docs");
                                if (docs.length() > 0) return docs;
                            }
                        }
                    }
                }
            }
            return new org.json.JSONArray();
        }

        java.util.List<SearchResult> getXimalayaAlbumTracks(String albumId) throws Exception {
            return getXimalayaAlbumTracks(albumId, null, null);
        }

        java.util.List<SearchResult> getXimalayaAlbumTracks(String albumId, String albumTitle, String albumArtist) throws Exception {
            java.util.List<SearchResult> tracks = new java.util.ArrayList<SearchResult>();

            // 主方案：使用网页版 getTracksList API（分页获取全部集数）
            try {
                java.util.Map<String, SearchResult> trackMap = new java.util.HashMap<String, SearchResult>();
                int pageNum = 1;
                int emptyPageCount = 0;
                while (pageNum <= 20 && emptyPageCount < 2) {
                    String url = "https://www.ximalaya.com/revision/album/getTracksList?albumId=" + albumId + "&pageNum=" + pageNum + "&pageSize=30";
                    Response res = httpGet(url);
                    org.json.JSONObject obj = (org.json.JSONObject) parseJsonLike(res.body);
                    if (obj.optInt("ret", 0) == 200 && obj.has("data")) {
                        org.json.JSONObject data = obj.getJSONObject("data");
                        if (data.has("tracks")) {
                            org.json.JSONArray arr = data.getJSONArray("tracks");
                            if (arr.length() == 0) {
                                emptyPageCount++;
                            } else {
                                emptyPageCount = 0;
                                for (int i = 0; i < arr.length(); i++) {
                                    org.json.JSONObject item = arr.getJSONObject(i);
                                    SearchResult sr = parseXimalayaTrackItem(item, albumId, albumArtist);
                                    if (sr != null) trackMap.put(sr.id, sr);
                                }
                            }
                        } else {
                            emptyPageCount++;
                        }
                    } else {
                        emptyPageCount++;
                    }
                    pageNum++;
                }
                tracks.addAll(trackMap.values());
                // 按title中的集数排序（正序）
                java.util.Collections.sort(tracks, new java.util.Comparator<SearchResult>() {
                    @Override public int compare(SearchResult a, SearchResult b) {
                        try {
                            int idxA = extractEpisodeNumber(a.name);
                            int idxB = extractEpisodeNumber(b.name);
                            return idxA - idxB;
                        } catch (Exception e) {
                            return 0;
                        }
                    }
                });
                // 补充播放链接
                if (!tracks.isEmpty()) {
                    tracks = fillXimalayaPlayUrls(tracks);
                }
                if (!tracks.isEmpty()) {
                    return tracks;
                }
            } catch (Exception e) {
                // 主方案失败，继续fallback
            }

            // Fallback 1: 使用旧版v1 API（正序+倒序合并）
            if (tracks.isEmpty()) {
                try {
                    java.util.Map<String, SearchResult> trackMap = new java.util.HashMap<String, SearchResult>();
                    String urlAsc = "https://www.ximalaya.com/revision/album/v1/getTracksList?albumId=" + albumId + "&pageNum=1&pageSize=100&sort=0";
                    Response resAsc = httpGet(urlAsc);
                    org.json.JSONObject objAsc = (org.json.JSONObject) parseJsonLike(resAsc.body);
                    if (objAsc.optInt("ret", 0) == 200 && objAsc.has("data")) {
                        org.json.JSONArray arrAsc = objAsc.getJSONObject("data").getJSONArray("tracks");
                        for (int i = 0; i < arrAsc.length(); i++) {
                            SearchResult sr = parseXimalayaTrackItem(arrAsc.getJSONObject(i), albumId, albumArtist);
                            if (sr != null) trackMap.put(sr.id, sr);
                        }
                    }
                    String urlDesc = "https://www.ximalaya.com/revision/album/v1/getTracksList?albumId=" + albumId + "&pageNum=1&pageSize=100&sort=1";
                    Response resDesc = httpGet(urlDesc);
                    org.json.JSONObject objDesc = (org.json.JSONObject) parseJsonLike(resDesc.body);
                    if (objDesc.optInt("ret", 0) == 200 && objDesc.has("data")) {
                        org.json.JSONArray arrDesc = objDesc.getJSONObject("data").getJSONArray("tracks");
                        for (int i = 0; i < arrDesc.length(); i++) {
                            SearchResult sr = parseXimalayaTrackItem(arrDesc.getJSONObject(i), albumId, albumArtist);
                            if (sr != null) trackMap.put(sr.id, sr);
                        }
                    }
                    tracks.addAll(trackMap.values());
                    java.util.Collections.sort(tracks, new java.util.Comparator<SearchResult>() {
                        @Override public int compare(SearchResult a, SearchResult b) {
                            try { return extractEpisodeNumber(a.name) - extractEpisodeNumber(b.name); } catch (Exception e) { return 0; }
                        }
                    });
                    if (!tracks.isEmpty()) {
                        tracks = fillXimalayaPlayUrls(tracks);
                    }
                    if (!tracks.isEmpty()) return tracks;
                } catch (Exception ignored) {}
            }

            // Fallback 2: 使用移动端API
            if (tracks.isEmpty()) {
                try {
                    String url = "https://mobile.ximalaya.com/mobile/v1/album/track?albumId=" + albumId + "&pageId=1&pageSize=100&isAsc=true";
                    Response res = httpGet(url);
                    Object parsed = parseJsonLike(res.body);
                    if (parsed instanceof org.json.JSONObject) {
                        org.json.JSONObject obj = (org.json.JSONObject) parsed;
                        if (obj.has("data")) {
                            org.json.JSONObject data = obj.getJSONObject("data");
                            org.json.JSONArray trackArray = null;
                            if (data.has("list")) trackArray = data.getJSONArray("list");
                            else if (data.has("tracks")) trackArray = data.getJSONArray("tracks");
                            else if (data.has("trackList")) trackArray = data.getJSONArray("trackList");
                            if (trackArray != null && trackArray.length() > 0) {
                                for (int i = 0; i < trackArray.length(); i++) {
                                    if (!(trackArray.get(i) instanceof org.json.JSONObject)) continue;
                                    org.json.JSONObject item = trackArray.getJSONObject(i);
                                    SearchResult result = new SearchResult();
                                    result.id = firstString(item, "trackId", "id");
                                    result.name = firstString(item, "title", "name", "trackName");
                                    result.artist = firstString(item, "nickname", "announcer", "singer");
                                    result.album = firstString(item, "albumTitle", "album");
                                    result.source = "xm";
                                    result.coverUrl = coverForPlatform("xm", item);
                                    result.albumId = albumId;
                                    String playUrl = firstString(item, "play_path_64", "play_path_aacv224", "play_path_aacv164", "play_path_32", "playUrl");
                                    if (playUrl != null && playUrl.length() > 0) result.playUrl = playUrl;
                                    if (result.id != null && result.name != null) tracks.add(result);
                                }
                                if (!tracks.isEmpty()) tracks = fillXimalayaPlayUrls(tracks);
                                if (!tracks.isEmpty()) return tracks;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Fallback 3: 搜索单集
            if (tracks.isEmpty() && albumTitle != null && albumTitle.length() > 0) {
                tracks = searchXimalayaTracksByAlbum(albumId, albumTitle, albumArtist);
            }
            return tracks;
        }

        private SearchResult parseXimalayaTrackItem(org.json.JSONObject item, String albumId, String albumArtist) {
            try {
                SearchResult sr = new SearchResult();
                sr.id = String.valueOf(item.opt("trackId"));
                sr.name = item.optString("title", "");
                sr.artist = (albumArtist != null && albumArtist.length() > 0) ? albumArtist : "黎诺音坊";
                sr.album = item.optString("albumTitle", "");
                sr.source = "xm";
                sr.albumId = albumId;
                String cover = item.optString("cover", "");
                if (cover.startsWith("//")) cover = "https:" + cover;
                sr.coverUrl = cover;
                return sr;
            } catch (Exception e) { return null; }
        }

        private int extractEpisodeNumber(String title) {
            if (title == null) return 0;
            if (title.contains("简介")) return 0;
            // 标准格式：第1集
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("第(\\d+)集");
            java.util.regex.Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                try { return Integer.parseInt(matcher.group(1)); } catch (Exception e) { return 0; }
            }
            // 其他格式：第1章、第1回
            pattern = java.util.regex.Pattern.compile("第(\\d+)[集章回]");
            matcher = pattern.matcher(title);
            if (matcher.find()) {
                try { return Integer.parseInt(matcher.group(1)); } catch (Exception e) { return 0; }
            }
            // 无"集"格式：第39被抛下了
            pattern = java.util.regex.Pattern.compile("第(\\d+)");
            matcher = pattern.matcher(title);
            if (matcher.find()) {
                try { return Integer.parseInt(matcher.group(1)); } catch (Exception e) { return 0; }
            }
            return 0;
        }

        // 并发调用baseInfo API为每个track补充播放链接
        private java.util.List<SearchResult> fillXimalayaPlayUrls(final java.util.List<SearchResult> tracks) {
            java.util.List<java.util.concurrent.Callable<SearchResult>> tasks = new java.util.ArrayList<java.util.concurrent.Callable<SearchResult>>();
            for (final SearchResult sr : tracks) {
                if (sr.playUrl != null && sr.playUrl.length() > 0) {
                    continue; // 已有播放链接
                }
                tasks.add(new java.util.concurrent.Callable<SearchResult>() {
                    @Override public SearchResult call() {
                        try {
                            String url = "https://mobile.ximalaya.com/mobile/v1/track/baseInfo?trackId=" + enc(sr.id);
                            Response res = httpGet(url);
                            org.json.JSONObject obj = (org.json.JSONObject) parseJsonLike(res.body);
                            String playUrl64 = obj.optString("playUrl64", "");
                            String playUrl32 = obj.optString("playUrl32", "");
                            String playUrl16 = obj.optString("playUrl16", "");
                            String downloadUrl = obj.optString("downloadUrl", "");
                            String bestUrl = playUrl64.length() > 0 ? playUrl64 : (playUrl32.length() > 0 ? playUrl32 : (playUrl16.length() > 0 ? playUrl16 : downloadUrl));
                            if (bestUrl.length() > 0) {
                                sr.playUrl = bestUrl;
                            }
                        } catch (Exception ignored) {}
                        return sr;
                    }
                });
            }
            if (!tasks.isEmpty()) {
                try {
                    java.util.List<java.util.concurrent.Future<SearchResult>> futures = pool.invokeAll(tasks, 20, java.util.concurrent.TimeUnit.SECONDS);
                    for (java.util.concurrent.Future<SearchResult> f : futures) {
                        try { f.get(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            }
            // 保留所有tracks（包括付费不可播的），让UI层决定是否显示
            return tracks;
        }

        // 通过搜索单集来获取专辑内的tracks（fallback方案）
        private java.util.List<SearchResult> searchXimalayaTracksByAlbum(String albumId, String albumTitle, String albumArtist) {
            java.util.List<SearchResult> strictMatches = new java.util.ArrayList<SearchResult>();
            java.util.List<SearchResult> fuzzyMatches = new java.util.ArrayList<SearchResult>();
            try {
                String url = "https://www.ximalaya.com/revision/search?core=track&kw=" + enc(albumTitle) + "&page=1&spellchecker=true&rows=100";
                Response res = httpGet(url);
                org.json.JSONObject obj = (org.json.JSONObject) parseJsonLike(res.body);
                if (obj.has("data")) {
                    org.json.JSONObject data = obj.getJSONObject("data");
                    if (data.has("result")) {
                        org.json.JSONObject resultObj = data.getJSONObject("result");
                        if (resultObj.has("response")) {
                            org.json.JSONObject response = resultObj.getJSONObject("response");
                            if (response.has("docs")) {
                                org.json.JSONArray docs = response.getJSONArray("docs");
                                String fuzzyTitle = albumTitle.replaceAll("\\s+", "");
                                for (int i = 0; i < docs.length(); i++) {
                                    org.json.JSONObject item = docs.getJSONObject(i);
                                    String itemAlbumId = String.valueOf(item.opt("album_id"));
                                    String itemAlbumTitle = item.optString("album_title", "");
                                    boolean idMatch = albumId.equals(itemAlbumId);
                                    boolean titleMatch = itemAlbumTitle.replaceAll("\\s+", "").contains(fuzzyTitle) || fuzzyTitle.contains(itemAlbumTitle.replaceAll("\\s+", ""));

                                    if (!idMatch && !titleMatch) continue;

                                    SearchResult sr = new SearchResult();
                                    sr.id = String.valueOf(item.opt("id"));
                                    sr.name = item.optString("title", "");
                                    // fallback时优先使用原始专辑播讲人，避免显示其他版本的主播
                                    sr.artist = (albumArtist != null && albumArtist.length() > 0) ? albumArtist : item.optString("nickname", "未知主播");
                                    sr.album = item.optString("album_title", albumTitle);
                                    sr.source = "xm";
                                    sr.albumId = albumId;
                                    String cover = item.optString("cover_path", item.optString("album_cover_path", ""));
                                    if (cover.startsWith("//")) cover = "https:" + cover;
                                    sr.coverUrl = cover;
                                    String playUrl = firstString(item, "play_path_64", "play_path_aacv224", "play_path_aacv164", "play_path_32", "playUrl");
                                    if (playUrl != null && playUrl.length() > 0) {
                                        sr.playUrl = playUrl;
                                    }
                                    if (sr.id.length() > 0 && sr.name.length() > 0) {
                                        if (idMatch) {
                                            strictMatches.add(sr);
                                        } else {
                                            fuzzyMatches.add(sr);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            // 优先返回严格匹配的结果，如果没有则返回模糊匹配
            return !strictMatches.isEmpty() ? strictMatches : fuzzyMatches;
        }

        // 获取喜马拉雅专辑详情
        org.json.JSONObject getXimalayaAlbumDetail(String albumId) throws Exception {
            String url = "https://mobile.ximalaya.com/mobile/v1/album/detail?albumId=" + albumId;
            Response res = httpGet(url);
            Object parsed = parseJsonLike(res.body);
            if (parsed instanceof org.json.JSONObject) {
                org.json.JSONObject obj = (org.json.JSONObject) parsed;
                if (obj.has("data")) {
                    return obj.getJSONObject("data");
                }
            }
            return new org.json.JSONObject();
        }

        private String sourceNameForResolver(String key) {
            if ("tx".equals(key)) return "QQ";
            if ("kw".equals(key)) return "酷我";
            if ("kg".equals(key)) return "酷狗";
            if ("mg".equals(key)) return "咪咕";
            if ("xm".equals(key)) return "喜马拉雅";
            if ("blv".equals(key) || "bla".equals(key) || "bl".equals(key)) return "哔哩哔哩";
            if ("dyv".equals(key) || "dya".equals(key) || "dyu".equals(key) || "dy".equals(key)) return "抖音";
            return "网易云";
        }

        String lyrics(Track track) {
            if (track == null || track.id == null) return "暂无歌词";
            try {
                if ("wy".equals(track.source)) {
                    String body = httpGet("https://music.163.com/api/song/lyric?id=" + enc(track.id) + "&lv=1&kv=1&tv=-1").body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    if (object.has("lrc")) {
                        JSONObject lrc = object.getJSONObject("lrc");
                        String lyric = lrc.optString("lyric", "");
                        if (lyric.length() > 0) return lyric;
                    }
                } else if ("tx".equals(track.source)) {
                    String body = httpGet("https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=" + enc(track.id) + "&format=json&nobase64=1").body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    String lyric = firstString(object, "lyric");
                    if (lyric != null && lyric.length() > 0) return lyric;
                } else if ("kw".equals(track.source)) {
                    String body = httpGet("https://www.kuwo.cn/api/www/music/musicInfo?mid=" + enc(track.id) + "&httpsStatus=1&reqId=tinghanxinyu").body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONObject data = object.optJSONObject("data");
                    if (data != null) {
                        JSONArray lrcArray = data.optJSONArray("lrclist");
                        if (lrcArray != null && lrcArray.length() > 0) {
                            StringBuilder sb = new StringBuilder();
                            for (int i = 0; i < lrcArray.length(); i++) {
                                JSONObject lrc = lrcArray.getJSONObject(i);
                                String time = firstString(lrc, "time");
                                String lineLyric = firstString(lrc, "lineLyric");
                                if (time != null && lineLyric != null) sb.append("[").append(time).append("]").append(lineLyric).append("\n");
                            }
                            if (sb.length() > 0) return sb.toString();
                        }
                    }
                } else if ("kg".equals(track.source)) {
                    try {
                        String body = httpGet("https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash=" + enc(track.id)).body;
                        JSONObject object = (JSONObject) parseJsonLike(body);
                        JSONObject data = object.optJSONObject("data");
                        if (data != null) {
                            String lyric = firstString(data, "lyrics");
                            if (lyric != null && lyric.length() > 0) return lyric;
                        }
                    } catch (Exception ignored) {}
                } else if ("mg".equals(track.source)) {
                    try {
                        String body = httpGet("https://c.musicapp.migu.cn/MIGUM2.0/v1.0/content/resourceinfo.do?resourceType=2&copyrightId=" + enc(track.id)).body;
                        JSONObject object = (JSONObject) parseJsonLike(body);
                        JSONArray data = object.optJSONArray("resource");
                        if (data != null && data.length() > 0) {
                            JSONObject res = data.getJSONObject(0);
                            String lyric = firstString(res, "lyric", "lrcUrl", "lyricUrl");
                            if (lyric != null && lyric.length() > 0) {
                                if (lyric.startsWith("http")) {
                                    try { lyric = httpGet(lyric).body; } catch (Exception ignored) {}
                                }
                                if (lyric != null && lyric.length() > 0) return lyric;
                            }
                        }
                    } catch (Exception ignored) {}
                }
                // 星海歌词回退
                if (xinghaiMap.containsKey(track.source)) {
                    try {
                        String body = httpGet(XINGHAI_MAIN_API + "&types=lyric&source=" + enc(xinghaiMap.get(track.source)) + "&id=" + enc(track.id)).body;
                        JSONObject object = (JSONObject) parseJsonLike(body);
                        String lyric = firstString(object, "lyric", "lrc");
                        if (lyric != null && lyric.length() > 0) return lyric;
                    } catch (Exception ignored) {}
                }
                // Meting歌词回退
                if (metingMap.containsKey(track.source)) {
                    for (String apiBase : METING_APIS) {
                        try {
                            String body = httpGet(trimQuestion(apiBase) + "?type=lyric&server=" + enc(metingMap.get(track.source)) + "&id=" + enc(track.id)).body;
                            JSONObject object = (JSONObject) parseJsonLike(body);
                            String lyric = firstString(object, "lyric", "lrc");
                            if (lyric == null) {
                                JSONObject lrc = object.optJSONObject("lrc");
                                if (lrc != null) lyric = lrc.optString("lyric", "");
                            }
                            if (lyric != null && lyric.length() > 0) return lyric;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
            return "暂无歌词\n\n当前歌曲暂未获取到歌词。";
        }

        String albumCover(Track track) {
            if (track == null || track.id == null || track.id.length() == 0) return "";
            // 哔哩哔哩：如果搜索结果已带封面，直接返回，避免走音乐平台 fallback
            if ("bl".equals(track.source) && track.coverUrl != null && track.coverUrl.length() > 0) {
                return track.coverUrl;
            }
            // 抖音：如果搜索结果已带封面，直接返回
            if ("dy".equals(track.source) && track.coverUrl != null && track.coverUrl.length() > 0) {
                return track.coverUrl;
            }
            try {
                if ("wy".equals(track.source)) {
                    String body = httpGet("https://music.163.com/api/song/detail?ids=%5B" + enc(track.id) + "%5D").body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONArray songs = object.optJSONArray("songs");
                    if (songs != null && songs.length() > 0) {
                        JSONObject song = songs.getJSONObject(0);
                        String cover = nestedString(song, "al", "picUrl");
                        if (cover == null) cover = nestedString(song, "album", "picUrl", "blurPicUrl");
                        if (cover != null) return cover;
                    }
                } else if ("kw".equals(track.source)) {
                    String body = httpGet("https://www.kuwo.cn/api/www/music/musicInfo?mid=" + enc(track.id) + "&httpsStatus=1&reqId=tinghanxinyu").body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONObject data = object.optJSONObject("data");
                    if (data != null) {
                        String cover = firstString(data, "albumPic", "pic", "albumpic");
                        if (cover != null) return normalizeCoverUrl("kw", cover);
                    }
                } else if ("kg".equals(track.source)) {
                    String body = httpGet("https://wwwapi.kugou.com/yy/index.php?r=play/getdata&hash=" + enc(track.id)).body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONObject data = object.optJSONObject("data");
                    if (data != null) {
                        String cover = firstString(data, "album_img", "img", "album_img_500");
                        if (cover != null) return normalizeCoverUrl("kg", cover.replace("{size}", "400"));
                    }
                } else if ("tx".equals(track.source)) {
                    String data = "{\"comm\":{\"ct\":24,\"cv\":0},\"songinfo\":{\"method\":\"get_song_detail_yqq\",\"param\":{\"song_mid\":\"" + track.id + "\"},\"module\":\"music.pf_song_detail_svr\"}}";
                    String body = httpGet("https://u.y.qq.com/cgi-bin/musicu.fcg?data=" + enc(data)).body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONObject songinfo = object.optJSONObject("songinfo");
                    if (songinfo != null) {
                        JSONObject dataObj = songinfo.optJSONObject("data");
                        if (dataObj != null) {
                            String albummid = nestedString(dataObj, "track_info", "album", "mid");
                            if (albummid == null) {
                                JSONObject trackInfo = dataObj.optJSONObject("track_info");
                                if (trackInfo != null) {
                                    JSONObject album = trackInfo.optJSONObject("album");
                                    if (album != null) albummid = firstString(album, "mid", "pmid");
                                }
                            }
                            if (albummid != null && albummid.length() > 0) return "https://y.gtimg.cn/music/photo_new/T002R500x500M000" + albummid + ".jpg";
                        }
                    }
                } else if ("mg".equals(track.source)) {
                    String body = httpGet("https://c.musicapp.migu.cn/MIGUM2.0/v1.0/content/resourceinfo.do?resourceType=2&copyrightId=" + enc(track.id)).body;
                    JSONObject object = (JSONObject) parseJsonLike(body);
                    JSONArray data = object.optJSONArray("resource");
                    if (data != null && data.length() > 0) {
                        String cover = firstString(data.getJSONObject(0), "albumPicL", "albumPicM", "albumPicS");
                        if (cover != null) return cover;
                    }
                }
            } catch (Exception ignored) {}
            try {
                String searchedCover = searchAlbumCoverFallback(track);
                if (searchedCover != null && searchedCover.length() > 0) return searchedCover;
            } catch (Exception ignored) {}
            try {
                String metingCover = metingAlbumCover(track.source, track.id);
                if (metingCover != null && metingCover.length() > 0) return metingCover;
            } catch (Exception ignored) {}
            try {
                String xinghaiCover = xinghaiAlbumCover(track.source, track.id);
                if (xinghaiCover != null && xinghaiCover.length() > 0) return xinghaiCover;
            } catch (Exception ignored) {}
            return track.coverUrl == null ? "" : track.coverUrl;
        }

        private String metingAlbumCover(String platform, String id) {
            if (id == null || id.length() == 0 || !metingMap.containsKey(platform)) return "";
            for (String apiBase : METING_APIS) {
                try {
                    String url = trimQuestion(apiBase) + "?type=pic&server=" + enc(metingMap.get(platform)) + "&id=" + enc(id);
                    String found = extractUrl(httpGet(url).body);
                    if (found != null && found.length() > 0) return found;
                } catch (Exception ignored) {}
            }
            return "";
        }
        private String xinghaiAlbumCover(String platform, String id) {
            if (id == null || id.length() == 0 || !xinghaiMap.containsKey(platform)) return "";
            try {
                String body = httpGet(XINGHAI_MAIN_API + "&types=pic&source=" + enc(xinghaiMap.get(platform)) + "&id=" + enc(id)).body;
                String found = extractUrl(body);
                if (found != null && found.length() > 0) return found;
            } catch (Exception ignored) {}
            return "";
        }

        private String searchAlbumCoverFallback(Track track) {
            String query = ((track.title == null ? "" : track.title) + " " + (track.artist == null ? "" : track.artist)).trim();
            if (query.length() == 0) return "";
            String[] platforms = fallbackPlatformOrder(track.source);
            for (String platform : platforms) {
                try {
                    List<SearchResult> results = searchSinglePlatform(platform, query);
                    for (SearchResult item : results) {
                        if (looksLikeSameSong(track, item) && item.coverUrl != null && item.coverUrl.length() > 0) {
                            return item.coverUrl;
                        }
                    }
                } catch (Exception ignored) {}
            }
            return "";
        }

        ResolvedPlayback resolvePlayable(Track track) throws Exception {
            return resolvePlayable(track, "320k");
        }

        ResolvedPlayback resolvePlayable(Track track, String quality) throws Exception {
            // 如果track已经有playUrl，直接使用（抖音解析链接、喜马拉雅等场景）
            if (track.playUrl != null && track.playUrl.length() > 0) {
                return new ResolvedPlayback(track, track.playUrl);
            }
            Exception firstError = null;
            if ("wy".equals(track.source) && isTiaFarewellTrack(track)) {
                Track fixed = new Track("2062885837", "盛夏的告别", "袁娅维TIA RAY", "我的人间烟火 电视剧原声带", "wy", "");
                try {
                    String cover = albumCover(fixed);
                    if (cover != null && cover.length() > 0) fixed.coverUrl = cover;
                } catch (Exception ignored) {}
                return new ResolvedPlayback(fixed, resolve("wy", "2062885837", quality));
            }
            try {
                return new ResolvedPlayback(track, resolve(track.source, track.id, quality));
            } catch (Exception error) {
                firstError = error;
            }
            // 哔哩哔哩内容不 fallback 到音乐平台（内容类型不同）
            if ("bl".equals(track.source)) {
                if (firstError != null) throw firstError;
                throw new Exception("哔哩哔哩无播放链接");
            }
            // 抖音内容不 fallback 到音乐平台（内容类型不同）
            if ("dy".equals(track.source)) {
                if (firstError != null) throw firstError;
                throw new Exception("抖音无播放链接");
            }
            // 原音源解析失败，尝试在其他平台搜索同首歌
            String[] fallbackSources = {"wy", "tx", "kg", "kw", "mg", "xm"};
            String keyword = track.title + " " + track.artist;
            for (String fallback : fallbackSources) {
                if (fallback.equals(track.source)) continue;
                try {
                    List<SearchResult> candidates = searchSinglePlatform(fallback, keyword);
                    for (SearchResult candidate : candidates) {
                        if (looksLikeSameSong(track, candidate)) {
                            try {
                                String url = resolve(candidate.source, candidate.id, quality);
                                Track newTrack = candidate.toTrack();
                                newTrack.coverUrl = candidate.coverUrl;
                                return new ResolvedPlayback(newTrack, url);
                            } catch (Exception ignored) { /* try next candidate */ }
                        }
                    }
                } catch (Exception ignored) { /* try next platform */ }
            }
            if (firstError != null) throw firstError;
            throw new Exception("所有平台均未找到可播放链接");
        }

        private boolean looksLikeSameSong(Track track, SearchResult candidate) {
            return songMatchScore(track, candidate) >= 55;
        }

        private boolean isTiaFarewellTrack(Track track) {
            if (track == null) return false;
            String text = normalizeName((track.title == null ? "" : track.title) + " " + (track.artist == null ? "" : track.artist));
            return (text.contains("袁娅维") || text.contains("tiaray") || text.contains("tia"))
                && (text.contains("剩下的告别") || text.contains("盛夏的告别") || text.contains("剩下告别") || text.contains("盛夏告别"));
        }

        private String[] fallbackPlatformOrder(String failedSource) {
            if ("wy".equals(failedSource)) return new String[]{"tx", "kg", "kw", "mg", "wy"};
            return new String[]{"wy", "tx", "kg", "kw", "mg"};
        }

        private int songMatchScore(Track track, SearchResult candidate) {
            String a = normalizeName(track.title);
            String b = normalizeName(candidate.name);
            if (a.length() == 0 || b.length() == 0) return 0;
            int score = 0;
            if (a.equals(b)) score += 70;
            else if (a.contains(b) || b.contains(a)) score += 55;
            else if (commonPrefixLength(a, b) >= Math.min(4, Math.min(a.length(), b.length()))) score += 35;
            else if (commonCharCount(a, b) >= Math.min(4, Math.min(a.length(), b.length()))) score += 25;
            String ar = normalizeName(track.artist);
            String br = normalizeName(candidate.artist);
            if (ar.length() == 0 || br.length() == 0) score += 10;
            else if (ar.equals(br)) score += 30;
            else if (ar.contains(br) || br.contains(ar)) score += 20;
            String al = normalizeName(track.album);
            String bl = normalizeName(candidate.album);
            if (al.length() > 0 && bl.length() > 0 && (al.equals(bl) || al.contains(bl) || bl.contains(al))) score += 10;
            return score;
        }

        private int commonPrefixLength(String a, String b) {
            int len = Math.min(a.length(), b.length());
            int i = 0;
            while (i < len && a.charAt(i) == b.charAt(i)) i++;
            return i;
        }

        private int commonCharCount(String a, String b) {
            int count = 0;
            for (int i = 0; i < a.length(); i++) {
                if (b.indexOf(a.charAt(i)) >= 0) count++;
            }
            return count;
        }

        private String normalizeName(String text) {
            if (text == null) return "";
            return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[《》()（）\\[\\]【】\\-_/|｜·.，,。]", "");
        }

        String resolve(String platform, String songId, String requestedQuality) throws Exception {
            final String cleanSongId = songId.trim();
            // 喜马拉雅：优先使用搜索时缓存的播放链接
            if ("xm".equals(platform)) {
                String cached = xmUrlCache.get(cleanSongId);
                if (cached != null && cached.length() > 0) return cached;
                return resolveXimalaya(cleanSongId);
            }
            // 哔哩哔哩：视频/音频解析
            if ("bl".equals(platform)) {
                return resolveBilibili(cleanSongId);
            }
            // 抖音：视频/音频解析
            if ("dy".equals(platform)) {
                return resolveDouyin(cleanSongId);
            }
            List<String> qualities = getQualitiesToTry(requestedQuality);
            Exception lastError = null;
            for (int i = 0; i < qualities.size(); i++) {
                final String currentQuality = qualities.get(i);
                if ("mg".equals(platform) && "flac".equals(currentQuality)) continue;
                // 检查缓存
                String cached = getCachedUrl(platform, cleanSongId, currentQuality);
                if (cached != null) return cached;
                List<Callable<Result>> tasks = new ArrayList<Callable<Result>>();
                // 网易云优先星海
                if ("wy".equals(platform)) {
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqXinghai(platform, cleanSongId, currentQuality); } });
                }
                // 海棠系列
                for (int j = 0; j < haitangTemplates.size(); j++) {
                    final int idx = j;
                    final Map<String, String> ht = haitangTemplates.get(j);
                    final String apiName = "海棠" + (j + 1);
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqHaitang(ht, platform, cleanSongId, currentQuality, apiName); } });
                }
                // 网易云专属解锁API：可绕过30秒试听限制
                if ("wy".equals(platform)) {
                    for (final String template : neteaseUnlockApis) {
                        tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqNeteaseUnlock(template, cleanSongId, currentQuality); } });
                    }
                }
                // 其他平台星海
                if (!"wy".equals(platform) && xinghaiMap.containsKey(platform)) {
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqXinghai(platform, cleanSongId, currentQuality); } });
                }
                // 长青SVIP
                for (int j = 0; j < changqingTemplates.size(); j++) {
                    final int idx = j;
                    final Map<String, String> cq = changqingTemplates.get(j);
                    final String apiName = "长青" + (j + 1);
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqChangqing(cq, platform, cleanSongId, currentQuality, apiName); } });
                }
                // 念心SVIP
                for (int j = 0; j < nianxinTemplates.size(); j++) {
                    final int idx = j;
                    final Map<String, String> nx = nianxinTemplates.get(j);
                    final String apiName = "念心" + (j + 1);
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqNianxin(nx, platform, cleanSongId, currentQuality, apiName); } });
                }
                // Meting镜像
                for (final String apiBase : METING_APIS) {
                    tasks.add(new Callable<Result>() { @Override public Result call() throws Exception { return reqMeting(apiBase, platform, cleanSongId, currentQuality); } });
                }
                CompletionService<Result> completion = new ExecutorCompletionService<Result>(pool);
                List<Future<Result>> futures = new ArrayList<Future<Result>>();
                for (Callable<Result> task : tasks) futures.add(completion.submit(task));
                long deadline = System.currentTimeMillis() + (i == 0 ? OVERALL_TIMEOUT : FALLBACK_TIMEOUT);
                Result best = null;
                int bestScore = -1;
                try {
                    for (int k = 0; k < tasks.size(); k++) {
                        long waitMs = deadline - System.currentTimeMillis();
                        if (waitMs <= 0) break;
                        Future<Result> future = completion.poll(waitMs, TimeUnit.MILLISECONDS);
                        if (future == null) break;
                        try {
                            Result result = future.get();
                            String validUrl = validateUrl(result.url);
                            // 过滤试听URL，绝不缓存、绝不返回
                            if (isPreviewUrl(validUrl)) continue;
                            boolean isMatch = urlMatchesQuality(validUrl, currentQuality);
                            int score = calcUrlScore(validUrl, currentQuality, isMatch);
                            result.url = validUrl;
                            result.score = score;
                            if (isMatch && score >= 2800) {
                                setCachedUrl(platform, cleanSongId, currentQuality, validUrl);
                                return validUrl;
                            }
                            if (score > bestScore) { bestScore = score; best = result; }
                        } catch (Exception ignored) {}
                    }
                } finally {
                    for (Future<Result> future : futures) future.cancel(true);
                }
                if (best != null && !isPreviewUrl(best.url)) {
                    setCachedUrl(platform, cleanSongId, currentQuality, best.url);
                    return best.url;
                }
                lastError = new Exception("获取" + currentQuality + "失败");
            }
            throw lastError != null ? lastError : new Exception("没有解析到可播放链接");
        }

        private Result reqXinghai(String platform, String songId, String quality) throws Exception {
            String url = XINGHAI_MAIN_API + "&types=url&source=" + enc(xinghaiMap.get(platform)) + "&id=" + enc(songId) + "&br=" + enc(qualityToBr(quality));
            return normalize(httpGet(url), quality, "星海");
        }
        private Result reqMeting(String apiBase, String platform, String songId, String quality) throws Exception {
            String brParam = "flac".equals(quality) ? "999000" : qualityToBr(quality) + "000";
            String url = trimQuestion(apiBase) + "?type=url&server=" + enc(metingMap.get(platform)) + "&id=" + enc(songId) + "&br=" + enc(brParam);
            return normalize(httpGet(url), quality, "Meting");
        }
        private Result reqHaitang(Map<String, String> templates, String platform, String songId, String quality, String apiName) throws Exception {
            String template = templates.get(platform);
            if (template == null) throw new Exception(apiName + "不支持");
            String url = template.replace("{id}", enc(songId)).replace("{level}", enc(qualityToLevel(quality)));
            return normalize(httpGet(url), quality, apiName);
        }
        private Result reqNeteaseUnlock(String template, String songId, String quality) throws Exception {
            String url = template.replace("{id}", enc(songId)).replace("{br}", enc(qualityToBr(quality)));
            return normalize(httpGet(url), quality, "网易云解锁");
        }
        private Result reqChangqing(Map<String, String> templates, String platform, String songId, String quality, String apiName) throws Exception {
            String template = templates.get(platform);
            if (template == null) throw new Exception(apiName + "不支持");
            String url = template.replace("{id}", enc(songId)).replace("{level}", enc(qualityToLevel(quality)));
            return normalize(httpGet(url), quality, apiName);
        }
        private Result reqNianxin(Map<String, String> templates, String platform, String songId, String quality, String apiName) throws Exception {
            String template = templates.get(platform);
            if (template == null) throw new Exception(apiName + "不支持");
            String url = template.replace("{id}", enc(songId)).replace("{level}", enc(qualityToLevel(quality)));
            return normalize(httpGet(url), quality, apiName);
        }
        /** 喜马拉雅：直接通过 track baseInfo API 获取播放链接 */
        private String resolveXimalaya(String trackId) throws Exception {
            String url = "https://mobile.ximalaya.com/mobile/v1/track/baseInfo?trackId=" + enc(trackId);
            String body = httpGet(url).body;
            if (body == null || body.length() == 0) throw new Exception("喜马拉雅API返回空");
            JSONObject json = new JSONObject(body);
            // baseInfo API 返回的JSON没有ret字段，msg="0"表示成功
            String msg = json.optString("msg", "");
            if (!"0".equals(msg) && json.optInt("ret", 0) != 0) throw new Exception("喜马拉雅解析失败");
            // 新API直接返回trackInfo内容，没有包裹层
            String playUrl = json.optString("playUrl64", "");
            if (playUrl.length() == 0) playUrl = json.optString("play_path_64", "");
            if (playUrl.length() == 0) playUrl = json.optString("playUrl32", "");
            if (playUrl.length() == 0) playUrl = json.optString("play_path_32", "");
            if (playUrl.length() == 0) playUrl = json.optString("playUrl", "");
            if (playUrl.length() == 0) playUrl = json.optString("play_path", "");
            if (playUrl.length() == 0) playUrl = json.optString("playPathAacv224", "");
            if (playUrl.length() == 0) playUrl = json.optString("play_path_aacv224", "");
            if (playUrl.length() == 0) throw new Exception("喜马拉雅无播放链接");
            return playUrl;
        }
        /** 哔哩哔哩：视频/音频播放链接解析 */
        private String resolveBilibili(String id) throws Exception {
            // 支持分P格式：bvid|cid
            String bvid = id;
            long explicitCid = 0;
            if (id.contains("|")) {
                int sep = id.indexOf("|");
                bvid = id.substring(0, sep);
                String cidPart = id.substring(sep + 1);
                // 处理ep|epid格式
                if ("ep".equals(bvid)) {
                    // 通过ep_id获取bvid和cid
                    String epId = cidPart;
                    String epUrl = "https://api.bilibili.com/pgc/player/web/playurl?ep_id=" + enc(epId) + "&qn=80&fnver=0&fnval=1&fourk=1&platform=html5";
                    String epBody = httpGet(epUrl).body;
                    if (epBody != null && epBody.length() > 0) {
                        JSONObject epJson = new JSONObject(epBody);
                        if (epJson.optInt("code", -1) == 0) {
                            JSONObject epData = epJson.optJSONObject("result");
                            if (epData != null) {
                                JSONArray durl = epData.optJSONArray("durl");
                                if (durl != null && durl.length() > 0) {
                                    String url = durl.getJSONObject(0).optString("url", "");
                                    if (url.length() > 0) return url;
                                }
                                // 尝试从epid获取bvid
                                bvid = epData.optString("bvid", "");
                                explicitCid = epData.optLong("cid", 0);
                            }
                        }
                    }
                    if (bvid.length() == 0 || explicitCid == 0 || "ep".equals(bvid)) {
                        throw new Exception("无法获取番剧视频信息");
                    }
                } else {
                    try { explicitCid = Long.parseLong(cidPart); } catch (Exception ignored) {}
                }
            }
            // 判断是视频（bvid格式）还是音频（纯数字）
            boolean isVideo = bvid.startsWith("BV") || bvid.startsWith("bv");
            if (isVideo) {
                long cid = explicitCid;
                // 如果没有指定cid，获取默认cid
                if (cid == 0) {
                    String infoUrl = "https://api.bilibili.com/x/web-interface/view?bvid=" + enc(bvid);
                    String infoBody = httpGet(infoUrl).body;
                    if (infoBody == null || infoBody.length() == 0) throw new Exception("哔哩哔哩视频信息返回空");
                    JSONObject infoJson = new JSONObject(infoBody);
                    if (infoJson.optInt("code", -1) != 0) throw new Exception("哔哩哔哩视频信息获取失败");
                    cid = infoJson.getJSONObject("data").optLong("cid", 0);
                }
                if (cid == 0) throw new Exception("无法获取视频CID");
                // 2. 获取播放链接（fnval=1 获取 MP4 单文件流，无分段，MediaPlayer 兼容性最好）
                // 使用qn=64（720P高清）平衡清晰度和流畅度
                String playUrl = "https://api.bilibili.com/x/player/playurl?bvid=" + enc(bvid) + "&cid=" + cid + "&qn=64&fnver=0&fnval=1&fourk=1&platform=html5&high_quality=1";
                String playBody = httpGet(playUrl).body;
                if (playBody == null || playBody.length() == 0) throw new Exception("哔哩哔哩播放链接返回空");
                JSONObject playJson = new JSONObject(playBody);
                if (playJson.optInt("code", -1) != 0) {
                    String msg = playJson.optString("message", "哔哩哔哩播放链接获取失败");
                    throw new Exception(msg);
                }
                JSONObject playData = playJson.getJSONObject("data");
                // MP4 格式返回在 durl 中，且无分段
                JSONArray durl = playData.optJSONArray("durl");
                if (durl != null && durl.length() > 0) {
                    String url = durl.getJSONObject(0).optString("url", "");
                    if (url.length() > 0) return url;
                }
                // 兼容 DASH 格式：优先取视频流，其次音频流
                JSONObject dash = playData.optJSONObject("dash");
                if (dash != null) {
                    JSONArray video = dash.optJSONArray("video");
                    if (video != null && video.length() > 0) {
                        String url = video.getJSONObject(0).optString("baseUrl", "");
                        if (url.length() > 0) return url;
                    }
                    JSONArray audio = dash.optJSONArray("audio");
                    if (audio != null && audio.length() > 0) {
                        String url = audio.getJSONObject(0).optString("baseUrl", "");
                        if (url.length() > 0) return url;
                    }
                }
                throw new Exception("哔哩哔哩无播放链接");
            } else {
                // 音频解析
                String audioUrl = "https://www.bilibili.com/audio/music-service-c/web/url?sid=" + enc(id);
                String audioBody = httpGet(audioUrl).body;
                if (audioBody == null || audioBody.length() == 0) throw new Exception("哔哩哔哩音频链接返回空");
                JSONObject audioJson = new JSONObject(audioBody);
                if (audioJson.optInt("code", -1) != 0) throw new Exception("哔哩哔哩音频链接获取失败");
                JSONObject data = audioJson.optJSONObject("data");
                String url = "";
                if (data != null) {
                    JSONArray cdns = data.optJSONArray("cdns");
                    if (cdns != null && cdns.length() > 0) url = cdns.optString(0, "");
                    if (url.length() == 0) url = data.optString("url", "");
                }
                if (url.length() == 0) throw new Exception("哔哩哔哩无音频链接");
                return url;
            }
        }
        String getBiliCookie() { return biliCookie; }

        /** 抖音：解析视频链接，返回视频信息 */
        SearchResult parseDouyinVideo(String linkText) throws Exception {
            // 1. 从链接中提取视频ID
            String videoId = extractDouyinVideoId(linkText);
            if (videoId == null || videoId.length() == 0) {
                throw new Exception("无法从链接中提取视频ID，请粘贴完整的抖音分享链接");
            }
            
            // 2. 尝试多种方式获取视频信息
            String title = "抖音视频";
            String author = "抖音用户";
            String cover = "";
            String videoUrl = "";
            String musicUrl = "";
            String secUid = "";
            
            // 方式1：优先访问iesdouyin分享页（国内可访问，抖音官方域名）
            try {
                String shareUrl = "https://www.iesdouyin.com/share/video/" + videoId + "/";
                Response res = httpGet(shareUrl);
                if (res.body != null && res.body.length() > 0) {
                    // 从_ROUTER_DATA提取JSON数据
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("window\\._ROUTER_DATA\\s*=\\s*(\\{.*?\\})\\s*</script>", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher m = p.matcher(res.body);
                    if (m.find()) {
                        String jsonStr = m.group(1);
                        // 处理HTML实体编码
                        jsonStr = jsonStr.replace("&quot;", "\"").replace("&amp;", "&").replace("&#x2F;", "/").replace("&lt;", "<").replace("&gt;", ">");
                        try {
                            JSONObject routerData = new JSONObject(jsonStr);
                            JSONObject loaderData = routerData.optJSONObject("loaderData");
                            if (loaderData != null) {
                                // 查找videoInfoRes
                                String[] videoKeys = {"video_(id)/page", "video_layout"};
                                for (String vk : videoKeys) {
                                    JSONObject pageData = loaderData.optJSONObject(vk);
                                    if (pageData != null) {
                                        JSONObject vir = pageData.optJSONObject("videoInfoRes");
                                        if (vir != null) {
                                            JSONArray itemList = vir.optJSONArray("item_list");
                                            if (itemList != null && itemList.length() > 0) {
                                                JSONObject item = itemList.getJSONObject(0);
                                                // 提取信息
                                                String desc = item.optString("desc", "");
                                                if (desc.length() > 0) title = desc;
                                                
                                                JSONObject authorObj = item.optJSONObject("author");
                                                if (authorObj != null) {
                                                    String nickname = authorObj.optString("nickname", "");
                                                    if (nickname.length() > 0) author = nickname;
                                                    JSONObject avatar = authorObj.optJSONObject("avatar_thumb");
                                                    if (avatar != null) {
                                                        JSONArray urlList = avatar.optJSONArray("url_list");
                                                        if (urlList != null && urlList.length() > 0) cover = urlList.optString(0, "");
                                                    }
                                                }
                                                // 提取作者sec_uid
                                                if (authorObj != null) {
                                                    secUid = authorObj.optString("sec_uid", "");
                                                }
                                                if (secUid.length() == 0) {
                                                    secUid = item.optString("sec_uid", "");
                                                }
                                                
                                                JSONObject video = item.optJSONObject("video");
                                                if (video != null) {
                                                    JSONObject playAddr = video.optJSONObject("play_addr");
                                                    if (playAddr != null) {
                                                        JSONArray urlList = playAddr.optJSONArray("url_list");
                                                        if (urlList != null && urlList.length() > 0) videoUrl = urlList.optString(0, "");
                                                    }
                                                    JSONObject coverObj = video.optJSONObject("cover");
                                                    if (coverObj != null && cover.length() == 0) {
                                                        JSONArray urlList = coverObj.optJSONArray("url_list");
                                                        if (urlList != null && urlList.length() > 0) cover = urlList.optString(0, "");
                                                    }
                                                }
                                                
                                                JSONObject music = item.optJSONObject("music");
                                                if (music != null) {
                                                    JSONObject playUrlObj = music.optJSONObject("play_url");
                                                    if (playUrlObj != null) {
                                                        JSONArray urlList = playUrlObj.optJSONArray("url_list");
                                                        if (urlList != null && urlList.length() > 0) musicUrl = urlList.optString(0, "");
                                                    }
                                                    String musicTitle = music.optString("title", "");
                                                    if (musicTitle.length() > 0 && title.equals("抖音视频")) title = musicTitle;
                                                }
                                                
                                                if (videoUrl.length() > 0 || musicUrl.length() > 0) break;
                                            }
                                            // 检查是否有错误提示
                                            JSONArray filterList = vir.optJSONArray("filter_list");
                                            if (filterList != null && filterList.length() > 0) {
                                                JSONObject filter = filterList.getJSONObject(0);
                                                String reason = filter.optString("detail_msg", "");
                                                String notice = filter.optString("notice", "");
                                                if (reason.length() > 0 || notice.length() > 0) {
                                                    // 视频被删除/不可见，但我们仍然返回结果让播放时再尝试
                                                }
                                            }
                                        }
                                    }
                                    if (videoUrl.length() > 0 || musicUrl.length() > 0) break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ignored) {}
            
            // 方式2：尝试国内可访问的第三方解析API
            String[] parseApis = {
                "https://api.douyin.wang/parse?url=" + enc("https://www.douyin.com/video/" + videoId),
            };
            
            for (String apiUrl : parseApis) {
                if (videoUrl.length() > 0 && musicUrl.length() > 0) break;
                try {
                    Response res = httpGet(apiUrl);
                    if (res.body == null || res.body.length() == 0) continue;
                    Object parsed = parseJsonLike(res.body);
                    if (parsed instanceof JSONObject) {
                        JSONObject obj = (JSONObject) parsed;
                        JSONObject data = obj.optJSONObject("data");
                        if (data == null) data = obj;
                        
                        if (videoUrl.length() == 0) {
                            JSONObject video = data.optJSONObject("video");
                            if (video != null) {
                                JSONObject playAddrObj = video.optJSONObject("play_addr");
                                if (playAddrObj != null) {
                                    JSONArray urlList = playAddrObj.optJSONArray("url_list");
                                    if (urlList != null && urlList.length() > 0) videoUrl = urlList.optString(0, "");
                                }
                                if (videoUrl.length() == 0) {
                                    String playUrl = data.optString("playUrl", data.optString("url", ""));
                                    if (playUrl.startsWith("http")) videoUrl = playUrl;
                                }
                                if (cover.length() == 0) {
                                    JSONObject coverObj = video.optJSONObject("cover");
                                    if (coverObj != null) {
                                        JSONArray urlList = coverObj.optJSONArray("url_list");
                                        if (urlList != null && urlList.length() > 0) cover = urlList.optString(0, "");
                                    }
                                }
                            }
                        }
                        
                        if (musicUrl.length() == 0) {
                            JSONObject music = data.optJSONObject("music");
                            if (music != null) {
                                JSONObject playUrlObj = music.optJSONObject("play_url");
                                if (playUrlObj != null) {
                                    JSONArray urlList = playUrlObj.optJSONArray("url_list");
                                    if (urlList != null && urlList.length() > 0) musicUrl = urlList.optString(0, "");
                                }
                            }
                        }
                        
                        String desc = data.optString("desc", data.optString("title", ""));
                        if (desc.length() > 0 && title.equals("抖音视频")) title = desc;
                        
                        JSONObject authorObj = data.optJSONObject("author");
                        if (authorObj != null) {
                            String nickname = authorObj.optString("nickname", authorObj.optString("name", ""));
                            if (nickname.length() > 0 && author.equals("抖音用户")) author = nickname;
                        }
                        // 提取sec_uid
                        if (secUid.length() == 0 && authorObj != null) {
                            secUid = authorObj.optString("sec_uid", "");
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // 即使获取不到playUrl，也返回结果（playUrl留空，播放时会通过resolveDouyin再次尝试）
            SearchResult result = new SearchResult();
            result.id = videoId;
            result.name = title;
            result.artist = author;
            result.album = "抖音";
            result.source = "dy";
            result.coverUrl = cover;
            result.type = "video";
            result.playUrl = videoUrl.length() > 0 ? videoUrl : musicUrl;
            result.authorId = secUid;
            return result;
        }
        
        private String extractDouyinVideoId(String text) {
            if (text == null) return null;
            String input = text.trim();
            
            // 先从文本中提取URL（支持整段分享文字）
            java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("https?://[^\\s，。、！？]+");
            java.util.regex.Matcher urlMatcher = urlPattern.matcher(input);
            
            // 如果找到URL，先尝试从URL中提取
            String foundUrl = null;
            if (urlMatcher.find()) {
                foundUrl = urlMatcher.group();
                // 清理URL末尾的标点
                foundUrl = foundUrl.replaceAll("[，。、！？）】」』.]+$", "");
                input = foundUrl;
            }
            
            // 匹配抖音视频ID：纯数字ID（aweme_id，15-20位数字）
            java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("(?:video/|note/|aweme_id=|item_ids=)(\\d{10,20})");
            java.util.regex.Matcher m1 = p1.matcher(input);
            if (m1.find()) return m1.group(1);
            
            // 匹配路径中的纯数字视频ID (douyin.com/video/123456...)
            java.util.regex.Pattern p1b = java.util.regex.Pattern.compile("(?:douyin\\.com/video/|iesdouyin\\.com/share/video/)(\\d{10,20})");
            java.util.regex.Matcher m1b = p1b.matcher(input);
            if (m1b.find()) return m1b.group(1);
            
            // 如果是短链，跟随重定向获取真实URL
            if (input.contains("v.douyin.com")) {
                try {
                    java.util.regex.Pattern shortPattern = java.util.regex.Pattern.compile("https?://v\\.douyin\\.com/[A-Za-z0-9_-]+/?");
                    java.util.regex.Matcher shortMatcher = shortPattern.matcher(input);
                    if (shortMatcher.find()) {
                        String shortUrl = shortMatcher.group();
                        // 使用HttpURLConnection跟随重定向（多次）
                        String resolved = followDouyinRedirect(shortUrl, 0);
                        if (resolved != null) {
                            return extractDouyinVideoId(resolved);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // 尝试iesdouyin短链接
            if (input.contains("iesdouyin.com")) {
                try {
                    java.util.regex.Pattern shortPattern = java.util.regex.Pattern.compile("https?://www\\.iesdouyin\\.com/share/[^\\s，。]+");
                    java.util.regex.Matcher shortMatcher = shortPattern.matcher(input);
                    if (shortMatcher.find()) {
                        String shortUrl = shortMatcher.group();
                        shortUrl = shortUrl.replaceAll("[，。、！？）】」』.]+$", "");
                        String resolved = followDouyinRedirect(shortUrl, 0);
                        if (resolved != null) {
                            return extractDouyinVideoId(resolved);
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            // 如果是纯数字ID直接返回（15-20位）
            String clean = input.trim().replaceAll("[^0-9]", "");
            if (clean.length() >= 15 && clean.length() <= 20) return clean;
            
            return null;
        }
        
        private String followDouyinRedirect(String url, int depth) {
            if (depth > 5 || url == null) return url;
            try {
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/89.0.4389.105 Mobile Safari/537.36");
                conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
                conn.connect();
                int code = conn.getResponseCode();
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                
                if ((code == 301 || code == 302 || code == 303 || code == 307) && location != null) {
                    if (location.startsWith("/")) {
                        java.net.URL base = new java.net.URL(url);
                        location = base.getProtocol() + "://" + base.getHost() + location;
                    }
                    if (location.startsWith("http")) {
                        return followDouyinRedirect(location, depth + 1);
                    }
                }
                return url;
            } catch (Exception e) {
                return url;
            }
        }

        /** 抖音：关键词搜索视频（快速方案：使用B站搜索相关视频，API稳定速度快） */
        java.util.List<SearchResult> searchDouyinVideos(String keyword) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            
            // 主要方案：B站搜索相关视频（API稳定，响应快，很多抖音热门视频在B站都有）
            try {
                List<SearchResult> blResults = search("blv", keyword);
                if (blResults != null && !blResults.isEmpty()) {
                    results.addAll(blResults);
                }
            } catch (Exception ignored) {}
            
            // 备用方案：也搜索"关键词 抖音"获取更相关的结果
            if (results.size() < 5) {
                try {
                    List<SearchResult> blDyResults = search("blv", keyword + " 抖音");
                    if (blDyResults != null) {
                        for (SearchResult sr : blDyResults) {
                            if (!results.contains(sr)) {
                                results.add(sr);
                                if (results.size() >= 20) break;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
            
            if (results.isEmpty()) {
                throw new Exception("未找到相关视频，请尝试粘贴抖音视频链接直接播放");
            }
            
            return results;
        }

        /** 抖音：搜索音乐/BGM */
        java.util.List<SearchResult> searchDouyinMusic(String keyword) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            java.util.Set<String> addedMusic = new java.util.HashSet<String>();
            
            // 搜索视频，从视频中提取BGM/音乐
            List<SearchResult> videoResults = searchDouyinVideos(keyword);
            if (videoResults != null) {
                for (SearchResult video : videoResults) {
                    // 视频本身的音频就是抖音BGM，把视频作为音频结果
                    String musicKey = video.id;
                    if (!addedMusic.contains(musicKey)) {
                        addedMusic.add(musicKey);
                        SearchResult music = new SearchResult();
                        music.id = video.id;
                        // 使用视频标题作为歌名，或者BGM标题
                        music.name = video.album != null && !"抖音".equals(video.album) ? video.album : video.name;
                        music.artist = video.artist;
                        music.album = "抖音BGM";
                        music.source = "dy";
                        music.coverUrl = video.coverUrl;
                        music.playUrl = video.playUrl;
                        music.type = "audio"; // 标记为音频
                        results.add(music);
                        if (results.size() >= 20) break;
                    }
                }
            }
            
            if (results.isEmpty()) {
                throw new Exception("未找到相关抖音音乐");
            }
            
            return results;
        }

        /** 抖音：搜索用户（抖音官方需要签名验证，免费API不稳定，直接提示用户） */
        java.util.List<SearchResult> searchDouyinUsers(String keyword) throws Exception {
            throw new Exception("抖音用户搜索暂不支持关键词搜索（需官方签名验证）。\n\n建议：\n1. 切换到「哔哩哔哩」音源搜索UP主\n2. 粘贴抖音用户主页链接直接查看\n3. 搜索视频关键词，点击作者查看视频");
        }

        /** 抖音：获取用户视频列表 */
        java.util.List<SearchResult> getDouyinUserVideos(String secUid, String userName) throws Exception {
            java.util.List<SearchResult> results = new java.util.ArrayList<SearchResult>();
            java.util.Set<String> addedIds = new java.util.HashSet<String>();
            String lastError = "";
            
            // 方式1：爬取iesdouyin用户主页（同视频解析一样使用_ROUTER_DATA，最可靠）
            try {
                String userPageUrl = "https://www.iesdouyin.com/share/user/" + secUid + "/";
                Response res = httpGet(userPageUrl);
                if (res.body != null && res.body.length() > 0) {
                    // 从_ROUTER_DATA提取JSON数据
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("window\\._ROUTER_DATA\\s*=\\s*(\\{.*?\\})\\s*</script>", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher m = p.matcher(res.body);
                    if (m.find()) {
                        String jsonStr = m.group(1);
                        jsonStr = jsonStr.replace("&quot;", "\"").replace("&amp;", "&").replace("&#x2F;", "/").replace("&lt;", "<").replace("&gt;", ">");
                        try {
                            JSONObject routerData = new JSONObject(jsonStr);
                            JSONObject loaderData = routerData.optJSONObject("loaderData");
                            if (loaderData != null) {
                                // 查找userInfoRes或类似结构
                                String[] userKeys = {"user_(id)/page", "user_layout", "profile_(id)/page"};
                                for (String uk : userKeys) {
                                    JSONObject pageData = loaderData.optJSONObject(uk);
                                    if (pageData != null) {
                                        // 尝试查找aweme_list或item_list
                                        JSONArray awemeList = null;
                                        JSONObject userInfoRes = pageData.optJSONObject("userInfoRes");
                                        if (userInfoRes != null) {
                                            awemeList = userInfoRes.optJSONArray("aweme_list");
                                            if (awemeList == null) {
                                                JSONObject data = userInfoRes.optJSONObject("data");
                                                if (data != null) {
                                                    awemeList = data.optJSONArray("aweme_list");
                                                }
                                            }
                                        }
                                        // 查找item_list
                                        if (awemeList == null) {
                                            JSONObject postRes = pageData.optJSONObject("postRes");
                                            if (postRes != null) {
                                                awemeList = postRes.optJSONArray("aweme_list");
                                                if (awemeList == null) {
                                                    JSONObject data = postRes.optJSONObject("data");
                                                    if (data != null) awemeList = data.optJSONArray("aweme_list");
                                                }
                                            }
                                        }
                                        if (awemeList != null && awemeList.length() > 0) {
                                            for (int i = 0; i < awemeList.length(); i++) {
                                                results.add(parseDouyinUserVideoItem(awemeList.getJSONObject(i), secUid, userName, addedIds));
                                            }
                                        }
                                    }
                                    if (!results.isEmpty()) break;
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                if (lastError.length() == 0) lastError = e.getMessage();
            }
            if (!results.isEmpty()) return results;
            
            // 方式2：尝试第三方API
            String[] apis = {
                "https://api.yujn.cn/api/dy_user_videos.php?sec_uid=%%SEC_UID%%&count=20",
                "https://www.iesdouyin.com/web/api/v2/aweme/post/?sec_uid=%%SEC_UID%%&count=20&max_cursor=0&aid=1128",
            };
            for (String apiTemplate : apis) {
                try {
                    String url = apiTemplate.replace("%%SEC_UID%%", enc(secUid));
                    Response res = httpGet(url);
                    if (res.body == null || res.body.length() == 0) continue;
                    Object parsed = parseJsonLike(res.body);
                    if (parsed instanceof JSONObject) {
                        JSONObject obj = (JSONObject) parsed;
                        JSONArray awemeList = null;
                        if (obj.has("aweme_list") && obj.get("aweme_list") instanceof JSONArray) {
                            awemeList = obj.getJSONArray("aweme_list");
                        } else if (obj.has("data") && obj.get("data") instanceof JSONArray) {
                            awemeList = obj.getJSONArray("data");
                        } else if (obj.has("videos") && obj.get("videos") instanceof JSONArray) {
                            awemeList = obj.getJSONArray("videos");
                        }
                        if (awemeList != null && awemeList.length() > 0) {
                            for (int i = 0; i < awemeList.length(); i++) {
                                results.add(parseDouyinUserVideoItem(awemeList.getJSONObject(i), secUid, userName, addedIds));
                            }
                            if (!results.isEmpty()) break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (!results.isEmpty()) return results;
            
            // 方式3：兜底 - 用B站搜索该作者名+抖音，至少能出一些相关内容
            try {
                List<SearchResult> blResults = search("blv", userName + " 抖音");
                if (blResults != null && !blResults.isEmpty()) {
                    for (SearchResult sr : blResults) {
                        if (!addedIds.contains(sr.id)) {
                            addedIds.add(sr.id);
                            results.add(sr);
                        }
                    }
                }
            } catch (Exception ignored) {}
            
            if (!results.isEmpty()) return results;
            throw new Exception("无法获取用户视频列表，可能需要登录或用户为私密账号");
        }
        
        /** 解析抖音用户视频列表中的单条视频项 */
        private SearchResult parseDouyinUserVideoItem(JSONObject item, String secUid, String userName, java.util.Set<String> addedIds) {
            try {
                SearchResult sr = new SearchResult();
                String awemeId = String.valueOf(item.optLong("aweme_id", item.optLong("aid", 0)));
                if ("0".equals(awemeId)) awemeId = item.optString("id", "");
                if (awemeId.length() == 0 || "0".equals(awemeId) || addedIds.contains(awemeId)) return null;
                addedIds.add(awemeId);
                sr.id = awemeId;
                String desc = item.optString("desc", "");
                sr.name = desc.length() > 0 ? desc : userName + " 的视频";
                sr.artist = userName;
                sr.album = "抖音";
                sr.source = "dy";
                sr.authorId = secUid;
                JSONObject video = item.optJSONObject("video");
                if (video != null) {
                    JSONObject cover = video.optJSONObject("cover");
                    if (cover != null) {
                        JSONArray urlList = cover.optJSONArray("url_list");
                        if (urlList != null && urlList.length() > 0) {
                            sr.coverUrl = urlList.optString(0, "");
                        }
                    }
                    JSONObject playAddr = video.optJSONObject("play_addr");
                    if (playAddr != null) {
                        JSONArray urlList = playAddr.optJSONArray("url_list");
                        if (urlList != null && urlList.length() > 0) {
                            sr.playUrl = urlList.optString(0, "");
                        }
                    }
                }
                sr.type = "video";
                return sr;
            } catch (Exception ignored) { return null; }
        }

        /** 抖音：视频/音频播放链接解析 */
        private String resolveDouyin(String id) throws Exception {
            String videoPageUrl = "https://www.douyin.com/video/" + id;
            String lastErrorMsg = "";
            
            // 方式1：优先访问iesdouyin分享页（国内可访问）
            try {
                String shareUrl = "https://www.iesdouyin.com/share/video/" + id + "/";
                Response res = httpGet(shareUrl);
                if (res.body != null && res.body.length() > 0) {
                    // 从_ROUTER_DATA提取
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("window\\._ROUTER_DATA\\s*=\\s*(\\{.*?\\})\\s*</script>", java.util.regex.Pattern.DOTALL);
                    java.util.regex.Matcher m = p.matcher(res.body);
                    if (m.find()) {
                        String jsonStr = m.group(1);
                        jsonStr = jsonStr.replace("&quot;", "\"").replace("&amp;", "&").replace("&#x2F;", "/");
                        try {
                            org.json.JSONObject data = new org.json.JSONObject(jsonStr);
                            String url = extractDouyinVideoUrl(data);
                            if (url.length() > 0 && url.startsWith("http")) return url;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                lastErrorMsg = e.getMessage();
            }
            
            // 方式2：尝试国内第三方解析API
            String[] apiUrls = {
                "https://api.douyin.wang/parse?url=" + enc(videoPageUrl),
            };
            for (String apiUrl : apiUrls) {
                try {
                    Response res = httpGet(apiUrl);
                    if (res.body == null || res.body.length() == 0) continue;
                    Object parsed = parseJsonLike(res.body);
                    if (parsed instanceof org.json.JSONObject) {
                        org.json.JSONObject obj = (org.json.JSONObject) parsed;
                        String url = extractDouyinVideoUrl(obj);
                        if (url.length() > 0 && url.startsWith("http")) return url;
                    }
                } catch (Exception e) {
                    // 记录错误但继续尝试下一个
                    if (lastErrorMsg.length() == 0) lastErrorMsg = e.getMessage();
                }
            }
            
            throw new Exception("无法获取视频播放地址，请检查网络连接或视频链接是否有效");
        }
        
        /** 从抖音JSON响应中提取视频URL */
        private String extractDouyinVideoUrl(org.json.JSONObject obj) {
            if (obj == null) return "";
            String url = "";
            
            // 路径0: 处理iesdouyin _ROUTER_DATA格式: loaderData -> video_(id)/page -> videoInfoRes -> item_list[0]
            org.json.JSONObject loaderData = obj.optJSONObject("loaderData");
            if (loaderData != null) {
                String[] pageKeys = {"video_(id)/page", "video_layout"};
                for (String pk : pageKeys) {
                    org.json.JSONObject pageData = loaderData.optJSONObject(pk);
                    if (pageData != null) {
                        org.json.JSONObject vir = pageData.optJSONObject("videoInfoRes");
                        if (vir != null) {
                            org.json.JSONArray itemList = vir.optJSONArray("item_list");
                            if (itemList != null && itemList.length() > 0) {
                                url = extractVideoUrlFromItem(itemList.optJSONObject(0));
                                if (url.length() > 0) return url;
                            }
                        }
                    }
                }
            }
            
            // 路径1: data.video.play_addr.url_list[0]
            org.json.JSONObject data = obj.optJSONObject("data");
            if (data != null) {
                url = extractVideoUrlFromItem(data);
                if (url.length() > 0) return url;
                // 检查data.data嵌套
                org.json.JSONObject data2 = data.optJSONObject("data");
                if (data2 != null) {
                    url = extractVideoUrlFromItem(data2);
                    if (url.length() > 0) return url;
                }
                // 路径: data.items[0]
                org.json.JSONArray items = data.optJSONArray("items");
                if (items != null && items.length() > 0) {
                    url = extractVideoUrlFromItem(items.optJSONObject(0));
                    if (url.length() > 0) return url;
                }
            }
            // 路径2: aweme_details[0].video.play_addr.url_list[0]
            org.json.JSONArray details = obj.optJSONArray("aweme_details");
            if (details != null && details.length() > 0) {
                url = extractVideoUrlFromItem(details.optJSONObject(0));
                if (url.length() > 0) return url;
            }
            // 路径3: item_list[0]
            org.json.JSONArray itemList = obj.optJSONArray("item_list");
            if (itemList != null && itemList.length() > 0) {
                url = extractVideoUrlFromItem(itemList.optJSONObject(0));
                if (url.length() > 0) return url;
            }
            // 路径4: result.video.url
            org.json.JSONObject result = obj.optJSONObject("result");
            if (result != null) {
                url = result.optString("url", result.optString("playUrl", ""));
                if (url.length() > 0) return url;
            }
            // 路径5: 直接取 url / playUrl / video_url
            url = obj.optString("url", obj.optString("playUrl", obj.optString("video_url", "")));
            if (url.length() > 0) return url;
            // 路径6: 根节点本身就是video数据
            url = extractVideoUrlFromItem(obj);
            return url;
        }
        
        private String extractVideoUrlFromItem(org.json.JSONObject item) {
            if (item == null) return "";
            String url = "";
            
            // 检测是否为图文/动图帖子（images数组存在）
            boolean isImagePost = item.has("images") || item.has("image_post_info");
            
            // 如果是图文帖子，优先从music字段提取音频URL
            if (isImagePost) {
                String audioUrl = extractMusicUrlFromItem(item);
                if (audioUrl.length() > 0) return audioUrl;
            }
            
            org.json.JSONObject video = item.optJSONObject("video");
            if (video != null) {
                // play_addr
                org.json.JSONObject playAddr = video.optJSONObject("play_addr");
                if (playAddr != null) {
                    org.json.JSONArray urlList = playAddr.optJSONArray("url_list");
                    if (urlList != null && urlList.length() > 0) {
                        url = urlList.optString(0, "");
                        // 修复：抖音API有时返回的URL中video_id参数值是一个完整URL
                        // 例如：?video_id=https://v3-dy-o.zjcdn.com/... 此时应提取该值作为实际地址
                        url = fixDouyinPlayUrl(url);
                    }
                    // 如果URL无效（video_id值为完整URL或uri本身是完整URL），尝试用play_addr.uri构造标准CDN URL
                    if (url.length() == 0 || url.contains("video_id=https://") || url.contains("video_id=http://")) {
                        String uri = playAddr.optString("uri", "");
                        if (uri.length() > 0) {
                            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                                // uri本身就是完整URL（如图文帖子的CDN地址），直接使用
                                url = uri;
                            } else {
                                url = "https://aweme.snssdk.com/aweme/v1/playwm/?video_id=" + uri;
                            }
                        }
                    }
                }
                // 如果play_addr不行，尝试bit_rate列表
                if (url.length() == 0 || url.contains("video_id=https://") || url.contains("video_id=http://")) {
                    org.json.JSONArray bitRates = video.optJSONArray("bit_rate");
                    if (bitRates != null && bitRates.length() > 0) {
                        org.json.JSONObject firstBit = bitRates.optJSONObject(0);
                        if (firstBit != null) {
                            org.json.JSONObject playAddrObj = firstBit.optJSONObject("play_addr");
                            if (playAddrObj != null) {
                                org.json.JSONArray urlList2 = playAddrObj.optJSONArray("url_list");
                                if (urlList2 != null && urlList2.length() > 0) {
                                    url = urlList2.optString(0, "");
                                    url = fixDouyinPlayUrl(url);
                                }
                                if (url.length() == 0 || url.contains("video_id=https://") || url.contains("video_id=http://")) {
                                    String uri = playAddrObj.optString("uri", "");
                                    if (uri.length() > 0) {
                                        if (uri.startsWith("http://") || uri.startsWith("https://")) {
                                            url = uri;
                                        } else {
                                            url = "https://aweme.snssdk.com/aweme/v1/playwm/?video_id=" + uri;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // 直接找play_url字段
                if (url.length() == 0) {
                    url = video.optString("play_url", video.optString("src", ""));
                }
            }
            
            // 如果仍然没有URL，尝试从music字段提取
            if (url.length() == 0) {
                url = extractMusicUrlFromItem(item);
            }
            
            return url;
        }
        
        /** 从item的music字段提取音频URL（适用于图文/动图帖子） */
        private String extractMusicUrlFromItem(org.json.JSONObject item) {
            if (item == null) return "";
            try {
                org.json.JSONObject music = item.optJSONObject("music");
                if (music != null) {
                    // 尝试 play_url.url_list[0]
                    org.json.JSONObject playUrl = music.optJSONObject("play_url");
                    if (playUrl != null) {
                        org.json.JSONArray urlList = playUrl.optJSONArray("url_list");
                        if (urlList != null && urlList.length() > 0) {
                            String audioUrl = urlList.optString(0, "");
                            if (audioUrl.length() > 0 && audioUrl.startsWith("http")) {
                                // 音频URL也可能有video_id参数问题，修复一下
                                return fixDouyinPlayUrl(audioUrl);
                            }
                        }
                        // 直接取uri
                        String uri = playUrl.optString("uri", "");
                        if (uri.length() > 0) {
                            if (uri.startsWith("http")) return uri;
                            return "https://aweme.snssdk.com/aweme/v1/playwm/?video_id=" + uri;
                        }
                    }
                    // 尝试直接取mid
                    String mid = String.valueOf(music.optLong("id", music.optLong("mid", 0)));
                    if (mid.length() > 0 && !"0".equals(mid)) {
                        return "https://www.douyin.com/aweme/v1/music/stream/?music_id=" + mid;
                    }
                }
            } catch (Exception ignored) {}
            return "";
        }
        
        /** 修复抖音播放URL：当video_id参数值为完整URL时，提取该URL作为实际播放地址 */
        private String fixDouyinPlayUrl(String url) {
            if (url == null || url.length() == 0) return "";
            try {
                // 检查video_id参数值是否以http开头
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("[?&]video_id=(https?://[^&]+)");
                java.util.regex.Matcher m = p.matcher(url);
                if (m.find()) {
                    String videoIdVal = java.net.URLDecoder.decode(m.group(1), "UTF-8");
                    if (videoIdVal.startsWith("http://") || videoIdVal.startsWith("https://")) {
                        // video_id的值本身是一个完整URL，直接使用它
                        // 检查提取的URL是否合理（长度>15，包含域名）
                        if (videoIdVal.length() > 15 && videoIdVal.contains(".")) {
                            return videoIdVal;
                        }
                    }
                }
            } catch (Exception ignored) {}
            return url;
        }

        /** 从QQ音乐排行榜API获取真实歌曲列表 */
        List<Track> fetchToplist(String topId) throws Exception {
            String url = "https://c.y.qq.com/v8/fcg-bin/fcg_v8_toplist_cp.fcg?topid=" + enc(topId) + "&format=json";
            String body = httpGet(url).body;
            Object parsed = parseJsonLike(body);
            List<Track> tracks = new ArrayList<Track>();
            if (parsed instanceof JSONObject) {
                JSONObject obj = (JSONObject) parsed;
                if (obj.has("songlist") && obj.get("songlist") instanceof JSONArray) {
                    JSONArray songlist = obj.getJSONArray("songlist");
                    int max = Math.min(songlist.length(), 50);
                    for (int i = 0; i < max; i++) {
                        try {
                            JSONObject song = songlist.getJSONObject(i);
                            JSONObject data = song.getJSONObject("data");
                            String songmid = data.optString("songmid", "");
                            String songName = data.optString("songname", "");
                            String albumName = data.optString("albumname", "");
                            StringBuilder singerBuilder = new StringBuilder();
                            if (data.has("singer") && data.get("singer") instanceof JSONArray) {
                                JSONArray singers = data.getJSONArray("singer");
                                for (int s = 0; s < singers.length(); s++) {
                                    if (singerBuilder.length() > 0) singerBuilder.append("、");
                                    singerBuilder.append(singers.getJSONObject(s).optString("name", ""));
                                }
                            }
                            String singerName = singerBuilder.length() > 0 ? singerBuilder.toString() : "未知歌手";
                            if (songmid.length() > 0 && songName.length() > 0) {
                                tracks.add(new Track(songmid, songName, singerName, albumName, "tx", ""));
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            return tracks;
        }
        private Response httpGet(String urlText) throws Exception {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(urlText).openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(REQUEST_TIMEOUT);
                conn.setReadTimeout(REQUEST_TIMEOUT);
                boolean isBili = urlText.contains("bilibili.com");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36 TingHanXinYuPlayer/1.3");
                conn.setRequestProperty("Accept", "application/json,text/plain,text/html,*/*");
                conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setRequestProperty("Referer", guessReferer(urlText));
                if (isBili) {
                    conn.setRequestProperty("Origin", "https://www.bilibili.com");
                    // 哔哩哔哩需要有效 buvid3，否则返回 412
                    if (biliCookie == null || biliCookie.length() == 0 || biliCookie.contains("buvid3=;")) {
                        try {
                            // 优先通过spi接口获取buvid3
                            HttpURLConnection spiConn = (HttpURLConnection) new URL("https://api.bilibili.com/x/frontend/finger/spi").openConnection();
                            spiConn.setInstanceFollowRedirects(true);
                            spiConn.setConnectTimeout(5000);
                            spiConn.setReadTimeout(5000);
                            spiConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
                            spiConn.setRequestProperty("Referer", "https://www.bilibili.com/");
                            spiConn.connect();
                            int spiCode = spiConn.getResponseCode();
                            if (spiCode == 200) {
                                String spiBody = readStream(spiConn.getInputStream());
                                JSONObject spiJson = new JSONObject(spiBody);
                                if (spiJson.optInt("code", -1) == 0 && spiJson.has("data")) {
                                    JSONObject data = spiJson.getJSONObject("data");
                                    String b3 = data.optString("b_3", "");
                                    String b4 = data.optString("b_4", "");
                                    if (b3.length() > 0) {
                                        biliCookie = "buvid3=" + b3 + "; buvid4=" + b4 + "; CURRENT_FNVAL=4048";
                                    }
                                }
                            }
                            spiConn.disconnect();
                        } catch (Exception ignored) {}
                        // 如果spi接口失败，从首页获取cookie
                        if (biliCookie == null || biliCookie.length() == 0) {
                            try {
                                HttpURLConnection homeConn = (HttpURLConnection) new URL("https://www.bilibili.com").openConnection();
                                homeConn.setInstanceFollowRedirects(true);
                                homeConn.setConnectTimeout(5000);
                                homeConn.setReadTimeout(5000);
                                homeConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36");
                                homeConn.setRequestProperty("Referer", "https://www.bilibili.com/");
                                homeConn.connect();
                                // 获取所有Set-Cookie头
                                StringBuilder sb = new StringBuilder();
                                for (int i = 0; ; i++) {
                                    String headerName = homeConn.getHeaderFieldKey(i);
                                    String headerValue = homeConn.getHeaderField(i);
                                    if (headerName == null && headerValue == null) break;
                                    if ("Set-Cookie".equalsIgnoreCase(headerName) && headerValue != null) {
                                        // 提取cookie名值对
                                        if (headerValue.contains("buvid3=") || headerValue.contains("b_nut=") || headerValue.contains("buvid4=")) {
                                            int end = headerValue.indexOf(";");
                                            String pair = end > 0 ? headerValue.substring(0, end) : headerValue;
                                            if (sb.length() > 0) sb.append("; ");
                                            sb.append(pair);
                                        }
                                    }
                                }
                                if (sb.length() > 0) {
                                    biliCookie = sb.toString() + "; CURRENT_FNVAL=4048";
                                }
                                homeConn.disconnect();
                            } catch (Exception ignored) {}
                        }
                    }
                    if (biliCookie != null && biliCookie.length() > 0 && !biliCookie.contains("buvid3=;")) {
                        conn.setRequestProperty("Cookie", biliCookie);
                    } else {
                        conn.setRequestProperty("Cookie", "CURRENT_FNVAL=4048");
                    }
                } else {
                    conn.setRequestProperty("Cookie", "uin=0;");
                }
                try {
                    int code = conn.getResponseCode();
                    if (code >= 400) {
                        String errorBody = "";
                        try { errorBody = readStream(conn.getErrorStream()); } catch (Exception ignored) {}
                        throw new Exception("HTTP " + code + ": " + errorBody.substring(0, Math.min(errorBody.length(), 100)));
                    }
                } catch (java.net.UnknownHostException e) {
                    throw new Exception("网络不通: " + e.getMessage());
                } catch (java.net.SocketTimeoutException e) {
                    throw new Exception("连接超时");
                }
                String finalUrl = conn.getURL().toString();
                String contentType = conn.getContentType() == null ? "" : conn.getContentType().toLowerCase(Locale.ROOT);
                if (contentType.contains("audio") || contentType.contains("octet-stream")) return new Response(finalUrl, true);
                String body = readStream(conn.getInputStream());
                return new Response(body, false);
            } finally { if (conn != null) conn.disconnect(); }
        }
        private String readStream(java.io.InputStream stream) throws Exception {
            if (stream == null) return "";
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) != -1) builder.append(buffer, 0, read);
            reader.close();
            return builder.toString().trim();
        }

        private Result normalize(Response response, String quality, String apiName) throws Exception {
            if (response.isAudio) return new Result(response.body, quality, apiName);
            String url = extractUrl(response.body);
            if (url != null) return new Result(url, quality, apiName);
            throw new Exception(apiName + ": 无URL");
        }
        private List<SearchResult> parseSearch(String body, String platform) throws Exception {
            Object parsed = parseJsonLike(body);
            JSONArray array = parsed instanceof JSONArray ? (JSONArray) parsed : null;
            if (array == null && parsed instanceof JSONObject) {
                JSONObject object = (JSONObject) parsed;
                if ("wy".equals(platform) && object.has("result")) {
                    JSONObject result = object.getJSONObject("result");
                    if (result.has("songs")) array = result.getJSONArray("songs");
                } else if ("tx".equals(platform) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("song")) {
                        JSONObject song = data.getJSONObject("song");
                        if (song.has("list")) array = song.getJSONArray("list");
                        if (array == null && song.has("itemlist")) array = song.getJSONArray("itemlist");
                    }
                }
                // u.y.qq.com 统一API响应格式
                if (array == null && "tx".equals(platform) && object.has("search")) {
                    try {
                        JSONObject search = object.getJSONObject("search");
                        JSONObject searchData = search.getJSONObject("data");
                        JSONObject bodyObj = searchData.getJSONObject("body");
                        JSONObject song = bodyObj.getJSONObject("song");
                        if (song.has("list")) array = song.getJSONArray("list");
                    } catch (Exception ignored) {}
                }
                if ("kw".equals(platform) && object.has("abslist")) {
                    array = object.getJSONArray("abslist");
                } else if ("kw".equals(platform) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("list")) array = data.getJSONArray("list");
                } else if ("kg".equals(platform) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("info")) array = data.getJSONArray("info");
                    if (array == null && data.has("lists")) array = data.getJSONArray("lists");
                } else if ("mg".equals(platform) && object.has("songResultData")) {
                    JSONObject data = object.getJSONObject("songResultData");
                    if (data.has("result")) array = data.getJSONArray("result");
                } else if ("xm".equals(platform) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("result")) {
                        JSONObject resultObj = data.getJSONObject("result");
                        if (resultObj.has("response")) {
                            JSONObject response = resultObj.getJSONObject("response");
                            if (response.has("docs")) array = response.getJSONArray("docs");
                        }
                    }
                    if (array == null && data.has("trackList")) array = data.getJSONArray("trackList");
                } else if (("blv".equals(platform) || "bla".equals(platform)) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("result") && data.get("result") instanceof JSONArray) {
                        array = data.getJSONArray("result");
                    }
                } else if (("dyv".equals(platform) || "dya".equals(platform) || "dyu".equals(platform)) && object.has("data")) {
                    JSONObject data = object.getJSONObject("data");
                    if (data.has("list") && data.get("list") instanceof JSONArray) {
                        array = data.getJSONArray("list");
                    } else if (data.has("result") && data.get("result") instanceof JSONArray) {
                        array = data.getJSONArray("result");
                    } else if (data.has("users") && data.get("users") instanceof JSONArray) {
                        array = data.getJSONArray("users");
                    } else if (data.has("videos") && data.get("videos") instanceof JSONArray) {
                        array = data.getJSONArray("videos");
                    }
                }
                if (array == null && object.has("data") && object.get("data") instanceof JSONArray) array = object.getJSONArray("data");
                if (array == null && object.has("result") && object.get("result") instanceof JSONArray) array = object.getJSONArray("result");
                if (array == null) array = findSongArray(object);
            }
            List<SearchResult> results = new ArrayList<SearchResult>();
            if (array == null) return results;
            for (int i = 0; i < array.length(); i++) {
                if (!(array.get(i) instanceof JSONObject)) continue;
                SearchResult result = parseSearchItem(platform, array.getJSONObject(i));
                if (result != null) results.add(result);
            }
            return results;
        }

        private SearchResult parseSearchItem(String platform, JSONObject item) {
            SearchResult result = new SearchResult();
            result.id = searchIdForPlatform(platform, item);
            result.name = firstString(item, "name", "title", "songname", "songName", "SONGNAME", "SongName", "musicName");
            result.artist = firstString(item, "artist", "author", "singer", "artists", "singername", "singerName", "ARTIST", "SINGER", "singers", "SingerName", "singerNameList", "nickname");
            result.album = albumNameForPlatform(platform, item);
            result.coverUrl = coverForPlatform(platform, item);
            if ("kw".equals(platform) && result.id != null && result.id.startsWith("MUSIC_")) {
                result.id = result.id.substring("MUSIC_".length());
            }
            result.source = platform;
            // 哔哩哔哩：处理视频/音频搜索结果
            if ("blv".equals(platform) || "bla".equals(platform)) {
                result.source = "bl";
                // 哔哩哔哩视频：名称可能包含HTML标签
                if ("blv".equals(platform) && result.name != null) {
                    result.name = result.name.replaceAll("<[^>]+>", "");
                }
                // 提取UP主mid（作者ID）
                String mid = firstString(item, "mid", "up_mid", "owner_mid");
                if (mid == null) {
                    // 尝试从owner对象中提取mid
                    if (item.has("owner") && item.opt("owner") instanceof JSONObject) {
                        mid = firstString(item.optJSONObject("owner"), "mid", "id");
                    }
                }
                result.authorId = mid == null ? "" : mid;
            }
            // 抖音：处理视频/音频/用户搜索结果
            if ("dyv".equals(platform) || "dya".equals(platform) || "dyu".equals(platform)) {
                result.source = "dy";
                // 抖音视频：名称可能包含HTML标签
                if ("dyv".equals(platform) && result.name != null) {
                    result.name = result.name.replaceAll("<[^>]+>", "");
                }
                // 提取抖音作者sec_uid
                String secUid = firstString(item, "sec_uid", "author_id", "uid", "id");
                if (secUid == null) {
                    // 尝试从author对象中提取
                    if (item.has("author") && item.opt("author") instanceof JSONObject) {
                        secUid = firstString(item.optJSONObject("author"), "sec_uid", "id");
                    }
                }
                result.authorId = secUid == null ? "" : secUid;
            }
            // 喜马拉雅：直接从搜索结果中提取播放链接和专辑ID
            if ("xm".equals(platform)) {
                String playUrl = firstString(item, "play_path_64", "play_path_aacv224", "play_path_aacv164", "play_path_32", "playUrl");
                if (playUrl != null && playUrl.length() > 0) {
                    result.playUrl = playUrl;
                }
                String albumId = firstString(item, "album_id", "albumId", "albumId", "albumID");
                if (albumId != null && albumId.length() > 0) {
                    result.albumId = albumId;
                }
            }
            if (isSnippetResult(item, result)) return null;
            return result.id != null && result.name != null ? result : null;
        }

        private boolean isSnippetResult(JSONObject item, SearchResult result) {
            String name = normalizeName((result.name == null ? "" : result.name) + " " + firstString(item, "subtitle", "remark", "lyric", "lyric_hilight", "songname_hilight"));
            if (name.contains("片段") || name.contains("试听") || name.contains("铃声") || name.contains("高潮版") || name.contains("秒片段")) return true;
            int duration = firstInt(item, "interval", "duration", "durationMs", "songTimeMinutes", "playTime");
            return duration > 0 && duration < 70;
        }

        private JSONArray findSongArray(Object value) {
            try {
                if (value instanceof JSONArray) {
                    JSONArray array = (JSONArray) value;
                    int songLike = 0;
                    for (int i = 0; i < Math.min(array.length(), 5); i++) {
                        if (array.get(i) instanceof JSONObject) {
                            JSONObject item = array.getJSONObject(i);
                            if ((firstString(item, "name", "title", "songname", "songName", "SONGNAME", "musicName") != null)
                                && (firstString(item, "id", "songid", "songId", "mid", "songmid", "songMid", "hash", "rid", "MUSICRID", "copyrightId", "contentId") != null)) songLike++;
                        }
                    }
                    if (songLike > 0) return array;
                    for (int i = 0; i < array.length(); i++) {
                        JSONArray found = findSongArray(array.get(i));
                        if (found != null) return found;
                    }
                } else if (value instanceof JSONObject) {
                    JSONObject object = (JSONObject) value;
                    JSONArray names = object.names();
                    if (names == null) return null;
                    for (int i = 0; i < names.length(); i++) {
                        JSONArray found = findSongArray(object.get(names.getString(i)));
                        if (found != null) return found;
                    }
                }
            } catch (Exception ignored) {}
            return null;
        }

        private String albumNameForPlatform(String platform, JSONObject item) {
            String album = firstString(item, "album", "albumname", "albumName", "ALBUM", "AlbumName", "album_name", "albumTitle");
            if (album == null) album = nestedString(item, "album", "name", "title");
            if (album == null) album = nestedString(item, "al", "name");
            return album == null ? "" : album;
        }

        private String searchIdForPlatform(String platform, JSONObject item) {
            if ("tx".equals(platform)) {
                return firstString(item, "songmid", "songMid", "media_mid", "mid", "id");
            }
            if ("kg".equals(platform)) {
                return firstString(item, "hash", "FileHash", "HQFileHash", "SQFileHash", "HASH");
            }
            if ("kw".equals(platform)) {
                String rid = firstString(item, "MUSICRID", "musicrid", "rid", "id", "DC_TARGETID");
                if (rid != null && rid.startsWith("MUSIC_")) return rid.substring("MUSIC_".length());
                return rid;
            }
            if ("mg".equals(platform)) {
                return firstString(item, "copyrightId", "contentId", "id", "songId");
            }
            if ("xm".equals(platform)) {
                return firstString(item, "trackId", "id");
            }
            if ("blv".equals(platform)) {
                return firstString(item, "bvid", "id");
            }
            if ("bla".equals(platform)) {
                return firstString(item, "id", "songId");
            }
            if ("dyv".equals(platform)) {
                return firstString(item, "aweme_id", "video_id", "id", "vid");
            }
            if ("dya".equals(platform)) {
                return firstString(item, "aweme_id", "audio_id", "id", "vid");
            }
            if ("dyu".equals(platform)) {
                return firstString(item, "uid", "sec_uid", "user_id", "id");
            }
            return firstString(item, "id", "songid", "songId", "mid", "songmid", "songMid", "hash", "rid", "url_id", "urlId");
        }

        private String coverForPlatform(String platform, JSONObject item) {
            String cover = null;
            if ("wy".equals(platform)) {
                cover = nestedString(item, "album", "picUrl", "blurPicUrl");
                if (cover == null) cover = nestedString(item, "al", "picUrl");
                if (cover == null) cover = firstString(item, "albumPic", "albumImg", "picUrl");
            } else if ("tx".equals(platform)) {
                String albummid = firstString(item, "albummid", "albumMid", "album_mid");
                if (albummid != null && albummid.length() > 0) {
                    return "https://y.gtimg.cn/music/photo_new/T002R300x300M000" + albummid + ".jpg";
                }
                cover = firstString(item, "albumPic", "albumImg");
            } else if ("kg".equals(platform)) {
                cover = firstString(item, "Image", "image", "AlbumPrivilege", "album_img", "albumPic", "AlbumImg");
                if (cover != null) cover = cover.replace("{size}", "400").replace("{SIZE}", "400");
            } else if ("kw".equals(platform)) {
                cover = firstString(item, "albumpic", "albumPic", "web_albumpic_short", "hts_MVPIC");
            } else if ("mg".equals(platform)) {
                cover = firstString(item, "albumPicL", "albumPicM", "albumPicS", "albumImg", "img", "cover");
            } else if ("xm".equals(platform)) {
                cover = firstString(item, "coverUrlLarge", "coverUrlMiddle", "coverUrlSmall", "coverUrl", "cover", "coverLarge", "coverMiddle", "coverSmall", "album_cover_path");
            } else if ("blv".equals(platform) || "bla".equals(platform)) {
                cover = firstString(item, "pic", "cover");
            } else if ("dyv".equals(platform) || "dya".equals(platform)) {
                cover = firstString(item, "cover", "cover_url", "pic", "thumbnail", "avatar");
            } else if ("dyu".equals(platform)) {
                cover = firstString(item, "avatar", "avatar_thumb", "avatar_medium", "avatar_larger", "cover", "pic");
            }
            if (cover == null) cover = firstString(item, "pic");
            if (cover == null) return "";
            cover = normalizeCoverUrl(platform, cover);
            return cover;
        }

        private String normalizeCoverUrl(String platform, String cover) {
            if (cover == null) return "";
            cover = cover.trim();
            if (cover.length() == 0 || "null".equals(cover)) return "";
            if (cover.startsWith("//")) return "https:" + cover;
            if ("kw".equals(platform) && !cover.startsWith("http")) return "https://img1.kuwo.cn/star/albumcover/" + cover;
            return cover;
        }

        private String nestedString(JSONObject object, String childKey, String... keys) {
            try {
                if (!object.has(childKey) || object.isNull(childKey)) return null;
                Object child = object.get(childKey);
                if (child instanceof JSONObject) return firstString((JSONObject) child, keys);
            } catch (Exception ignored) {}
            return null;
        }
        private Object parseJsonLike(String body) throws Exception {
            String text = body == null ? "" : body.trim();
            if (text.length() == 0) throw new Exception("空响应");
            // Strip JSONP callback wrapper: callback(...) or callback({...});
            if (text.startsWith("callback(") || text.startsWith("_callback(") || text.startsWith("jsonp")) {
                int open = text.indexOf('(');
                int close = text.lastIndexOf(')');
                if (open > 0 && close > open) text = text.substring(open + 1, close).trim();
            }
            try {
                return new JSONTokener(text).nextValue();
            } catch (Exception firstError) {
                // Try replacing single quotes with double quotes
                String repaired = text.replace('\'', '"');
                try {
                    return new JSONTokener(repaired).nextValue();
                } catch (Exception secondError) {
                    throw new Exception("JSON解析失败: " + firstError.getMessage());
                }
            }
        }
        private String extractUrl(String body) {
            try {
                String text = body == null ? "" : body.trim();
                text = normalizeUrlText(text);
                if (isHttpUrl(text) && text.length() > 25) return text;
                return extractFromJson(parseJsonLike(text));
            } catch (Exception ignored) { return null; }
        }
        private String extractFromJson(Object data) throws Exception {
            if (data == null || data == JSONObject.NULL) return null;
            if (data instanceof String) {
                String value = normalizeUrlText(((String) data).trim());
                return isHttpUrl(value) ? value : null;
            }
            if (data instanceof JSONArray) {
                JSONArray array = (JSONArray) data;
                for (int i = 0; i < array.length(); i++) {
                    String found = extractFromJson(array.get(i));
                    if (found != null) return found;
                }
            }
            if (data instanceof JSONObject) {
                JSONObject object = (JSONObject) data;
                String[] keys = {"url", "src", "link", "music_url", "musicUrl", "play_url", "playUrl", "data", "result", "song", "info"};
                for (String key : keys) if (object.has(key)) {
                    String found = extractFromJson(object.get(key));
                    if (found != null) return found;
                }
            }
            return null;
        }

        private String normalizeUrlText(String value) {
            if (value == null) return "";
            String text = value.trim()
                .replace("&amp;", "&")
                .replace("\\/", "/")
                .replace("\\u0026", "&");
            while ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
                text = text.substring(1, text.length() - 1).trim();
            }
            return text;
        }
        private String firstString(JSONObject object, String... keys) {
            for (String key : keys) try {
                if (!object.has(key) || object.isNull(key)) continue;
                Object value = object.get(key);
                if (value instanceof JSONArray && ((JSONArray) value).length() > 0) value = ((JSONArray) value).get(0);
                if (value instanceof JSONObject) {
                    String nested = firstString((JSONObject) value, "name", "title");
                    if (nested != null) return nested;
                }
                String text = String.valueOf(value);
                if (text.length() > 0 && !"null".equals(text)) return text;
            } catch (Exception ignored) {}
            return null;
        }
        private int firstInt(JSONObject object, String... keys) {
            for (String key : keys) try {
                if (!object.has(key) || object.isNull(key)) continue;
                Object value = object.get(key);
                if (value instanceof Number) return ((Number) value).intValue();
                String text = String.valueOf(value).replaceAll("[^0-9]", "");
                if (text.length() > 0) return Integer.parseInt(text);
            } catch (Exception ignored) {}
            return 0;
        }
        private List<String> getQualitiesToTry(String requestedQuality) {
            String q = requestedQuality == null || requestedQuality.length() == 0 ? "128k" : requestedQuality;
            int index = Arrays.asList(QUALITY_ORDER).indexOf(q);
            List<String> result = new ArrayList<String>();
            if (index >= 0) for (int i = index; i < QUALITY_ORDER.length; i++) result.add(QUALITY_ORDER[i]);
            else { result.add(q); result.addAll(Arrays.asList(QUALITY_ORDER)); }
            return result;
        }
        private String validateUrl(String url) throws Exception {
            if (url == null) throw new Exception("空URL");
            String trimmed = url.trim();
            if (!isHttpUrl(trimmed)) throw new Exception("非法URL");
            if (trimmed.length() < 20) throw new Exception("URL太短");
            return trimmed;
        }
        private int calcUrlScore(String url, String quality, boolean isExactMatch) {
            int score = qualityScore.containsKey(quality) ? qualityScore.get(quality) : 0;
            if (isExactMatch) score += 2000;
            else if (urlMatchesQuality(url, quality)) score += 1000;
            else score -= 500;
            for (int i = 0; i < OFFICIAL_CDN_DOMAINS.length; i++) if (url.contains(OFFICIAL_CDN_DOMAINS[i])) { score += 500 - i * 10; break; }
            // 网易云真实播放域加权（music.126.net 不是试听，m1-m801是试听）
            if (url.contains("music.126.net") && !url.contains("m8.music.126.net") && !url.contains("/m801/") && !url.contains("/m802/")) {
                score += 350;
            }
            if ("flac".equals(quality)) {
                if (url.contains(".flac?") || url.contains(".flac") || url.contains("F000") || url.contains("/yp/full/")) score += 300;
            } else {
                if (url.contains(".mp3?") || url.contains(".mp3") || url.contains(".m4a")) score += 50;
            }
            // 试听URL大幅降分（30s预览明显特征）
            if (isPreviewUrl(url)) score -= 2000;
            return score;
        }
        /** 检测30秒试听链接：网易云试听URL通常含 wvm=、netease=preview、或 m8.music.126.net/m8xx 短前缀 */
        private boolean isPreviewUrl(String url) {
            if (url == null) return false;
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains("wvm=")) return true;
            if (lower.contains("preview")) return true;
            if (lower.contains("trial")) return true;
            if (lower.contains("30s") || lower.contains("30sec")) return true;
            if (lower.contains("/m8.music.126.net/")) return true;
            if (lower.contains("/m801.music.126.net/") || lower.contains("/m802.music.126.net/")) return true;
            if (lower.contains(".126.net/") && lower.contains("?") && lower.length() < 200) return true;
            return false;
        }
        private boolean urlMatchesQuality(String url, String quality) {
            if (url == null) return false;
            String lower = url.toLowerCase(Locale.ROOT);
            if ("flac".equals(quality)) {
                if (lower.contains("/yp/full/") && !lower.contains(".mp3") && !lower.contains("m800") && !lower.contains("m500")) return true;
                if (lower.contains(".flac") || lower.contains("f000") || lower.contains("flac")) return true;
                if (lower.contains(".mp3") || lower.contains("mp3?") || lower.contains(".m4a") || lower.contains(".aac") || lower.contains("m800") || lower.contains("m500") || lower.contains("m128")) return false;
                if (lower.contains("br=999") || lower.contains("br=999000") || lower.contains("level=lossless")) return true;
                if (lower.contains("music.126.net") && !lower.contains(".mp3")) return true;
            } else {
                if ((lower.contains(".flac") || lower.contains("f000")) && !lower.contains(".mp3")) return false;
                if ("320k".equals(quality)) return lower.contains("320") || lower.contains("m800") || lower.contains("mp3") || lower.contains(".mp3") || lower.contains("/yp/full/") || lower.contains("music.126.net");
            }
            return true;
        }
        private final Map<String, String> urlCache = new HashMap<String, String>();
        private String getCachedUrl(String platform, String songId, String quality) {
            return urlCache.get(platform + "_" + songId + "_" + quality);
        }
        private void setCachedUrl(String platform, String songId, String quality, String url) {
            urlCache.put(platform + "_" + songId + "_" + quality, url);
            if (urlCache.size() > 500) { String firstKey = urlCache.keySet().iterator().next(); urlCache.remove(firstKey); }
        }
        private boolean isHttpUrl(String value) { return value != null && (value.startsWith("http://") || value.startsWith("https://")); }
        private String enc(String value) throws Exception { return URLEncoder.encode(value == null ? "" : value, "UTF-8"); }
        private String trimQuestion(String value) { while (value.endsWith("?")) value = value.substring(0, value.length() - 1); return value; }
        private String qualityToBr(String quality) { if ("flac".equals(quality)) return "999"; if ("320k".equals(quality)) return "320"; if ("192k".equals(quality)) return "192"; return "128"; }
        private String qualityToLevel(String quality) { if ("flac".equals(quality)) return "lossless"; if ("320k".equals(quality)) return "exhigh"; if ("192k".equals(quality)) return "high"; return "standard"; }
        private String join(List<String> values) { StringBuilder b = new StringBuilder(); for (int i = 0; i < values.size(); i++) { if (i > 0) b.append("; "); b.append(values.get(i)); } return b.toString(); }
        static class Response { String body; boolean isAudio; Response(String body, boolean isAudio) { this.body = body; this.isAudio = isAudio; } }
        static class Result { String url; String quality; String apiName; int score; Result(String url, String quality, String apiName) { this.url = url; this.quality = quality; this.apiName = apiName; } }
    }
}
