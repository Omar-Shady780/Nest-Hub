package com.google.ambient.nesthub;

import android.app.AlarmManager;
import android.app.Dialog;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ambient.nesthub.data.AppPreferences;
import com.google.ambient.nesthub.data.BackgroundTheme;
import com.google.ambient.nesthub.data.WidgetType;
import com.google.ambient.nesthub.receivers.BatteryStatusReceiver;
import com.google.ambient.nesthub.sound.AmbientSoundPlayer;
import com.google.ambient.nesthub.views.PinchWidgetCard;
import com.google.ambient.nesthub.widgets.WidgetViewHolder;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements BatteryStatusReceiver.BatteryListener {

    private AppPreferences preferences;
    private WidgetViewHolder widgetViewHolder;
    private BatteryStatusReceiver batteryReceiver;

    // AppWidget Host support
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private static final int APPWIDGET_HOST_ID = 2048;
    private static final int REQUEST_PICK_APPWIDGET = 2049;
    private static final int REQUEST_CREATE_APPWIDGET = 2050;
    private int pendingSlotForAppWidget = -1;

    private ViewFlipper mainViewFlipper;
    private GridLayout widgetGridLayout;
    private ImageView bgImageView;
    private View standbyBlackOverlay;
    private View redFilterOverlay;

    // Header views (Only Date, Battery, and Settings)
    private TextView headerDate;
    private TextView headerBatteryPercent;
    private ImageView headerBatteryIcon;

    // StandBy Digital views
    private TextView standbyDigitalTime;
    private TextView standbyDigitalSeconds;
    private TextView standbyDigitalDate;
    private TextView standbyDigitalAlarm;
    private TextView standbyDigitalBattery;
    private TextView standbyDigitalWeather;

    // StandBy Analog views
    private TextView standbyAnalogDate;
    private TextView standbyAnalogAlarm;
    private TextView standbyAnalogBattery;

    // Audio & Brightness volume key management
    private AudioManager audioManager;
    private AudioManager.AudioPlaybackCallback audioPlaybackCallback;
    private boolean isMusicActivelyPlaying = false;
    private long musicStoppedTimestamp = 0;
    private static final long MUSIC_STOP_COOLDOWN_MS = 15 * 1000L; // 15 seconds

    // Sleek Brightness HUD
    private LinearLayout brightnessHudContainer;
    private ProgressBar brightnessHudProgress;
    private TextView brightnessHudText;
    private final Handler brightnessHudHandler = new Handler(Looper.getMainLooper());

    // Battery & Alarm Cache
    private int currentBatteryPercent = 100;
    private boolean currentIsCharging = false;
    private String currentChargeType = "Battery";
    private String nextAlarmString = "No Alarms Set";

    private GestureDetector verticalSwipeDetector;
    private final Handler timeUpdateHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<String> imagePickerLauncher;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "NestBedsideHub:StayAwakeWakeLock"
            );
            try {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
            } catch (Exception ignored) {}
        }

        setContentView(R.layout.activity_main);

        preferences = new AppPreferences(this);
        widgetViewHolder = new WidgetViewHolder(this);

        // Initialize AppWidget Host
        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);

        initViews();
        setupGestureDetector();
        setupImagePicker();
        applyPreferences();
        setupHeaderButtons();
        rebuildWidgetGrid();
        updateNextAlarm();
        updateWeatherDisplays();
        startTimeTicker();
        initBrightnessAndAudio();

        batteryReceiver = new BatteryStatusReceiver(this);

        // First-time tutorial onboarding check
        if (!preferences.isTutorialCompleted()) {
            new Handler(Looper.getMainLooper()).postDelayed(this::showTutorialDialog, 500);
        }
    }

    private void initViews() {
        mainViewFlipper = findViewById(R.id.mainViewFlipper);
        widgetGridLayout = findViewById(R.id.widgetGridLayout);
        bgImageView = findViewById(R.id.bgImageView);
        standbyBlackOverlay = findViewById(R.id.standbyBlackOverlay);
        redFilterOverlay = findViewById(R.id.redFilterOverlay);

        // Brightness HUD
        brightnessHudContainer = findViewById(R.id.brightnessHudContainer);
        brightnessHudProgress = findViewById(R.id.brightnessHudProgress);
        brightnessHudText = findViewById(R.id.brightnessHudText);

        // Header: ONLY Date, Battery, and Settings
        headerDate = findViewById(R.id.headerDate);
        headerBatteryPercent = findViewById(R.id.headerBatteryPercent);
        headerBatteryIcon = findViewById(R.id.headerBatteryIcon);

        // StandBy Digital
        standbyDigitalTime = findViewById(R.id.standbyDigitalTime);
        standbyDigitalSeconds = findViewById(R.id.standbyDigitalSeconds);
        standbyDigitalDate = findViewById(R.id.standbyDigitalDate);
        standbyDigitalAlarm = findViewById(R.id.standbyDigitalAlarm);
        standbyDigitalBattery = findViewById(R.id.standbyDigitalBattery);
        standbyDigitalWeather = findViewById(R.id.standbyDigitalWeather);

        // StandBy Analog
        standbyAnalogDate = findViewById(R.id.standbyAnalogDate);
        standbyAnalogAlarm = findViewById(R.id.standbyAnalogAlarm);
        standbyAnalogBattery = findViewById(R.id.standbyAnalogBattery);
    }

    private void setupGestureDetector() {
        verticalSwipeDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 60;
            private static final int SWIPE_VELOCITY_THRESHOLD = 60;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();

                if (Math.abs(diffY) > Math.abs(diffX)) {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY < 0) {
                            showNextStandByScreen();
                        } else {
                            showPreviousStandByScreen();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (verticalSwipeDetector != null && verticalSwipeDetector.onTouchEvent(ev)) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private void showNextStandByScreen() {
        mainViewFlipper.setInAnimation(this, R.anim.slide_in_bottom);
        mainViewFlipper.setOutAnimation(this, R.anim.slide_out_top);
        mainViewFlipper.showNext();
        checkStandbyBackground();
    }

    private void showPreviousStandByScreen() {
        mainViewFlipper.setInAnimation(this, R.anim.slide_in_top);
        mainViewFlipper.setOutAnimation(this, R.anim.slide_out_bottom);
        mainViewFlipper.showPrevious();
        checkStandbyBackground();
    }

    private void checkStandbyBackground() {
        int currentChild = mainViewFlipper.getDisplayedChild();
        boolean isStandby = (currentChild != 0);
        boolean wantPureBlack = isStandby && preferences.isStandbyPureBlack();

        if (standbyBlackOverlay != null) {
            float targetAlpha = wantPureBlack ? 1.0f : 0.0f;
            standbyBlackOverlay.animate()
                    .alpha(targetAlpha)
                    .setDuration(320)
                    .start();
        }
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        preferences.setBackgroundTheme(BackgroundTheme.Type.CUSTOM_IMAGE);
                        preferences.setCustomBgUri(uri.toString());
                        applyBackgroundTheme();
                    }
                }
        );
    }

    private void applyPreferences() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        findViewById(R.id.rootContainer).setKeepScreenOn(true);
        redFilterOverlay.setVisibility(preferences.isRedFilterEnabled() ? View.VISIBLE : View.GONE);
        applyBackgroundTheme();
        checkStandbyBackground();
    }

    public void applyBackgroundTheme() {
        BackgroundTheme.Type type = preferences.getBackgroundTheme();
        Drawable activeDrawable = null;

        if (type == BackgroundTheme.Type.DEVICE_WALLPAPER) {
            try {
                WallpaperManager wm = WallpaperManager.getInstance(this);
                activeDrawable = wm.getDrawable();
                if (activeDrawable == null) {
                    activeDrawable = wm.peekDrawable();
                }
            } catch (Exception ignored) {}
            if (activeDrawable == null) {
                activeDrawable = BackgroundTheme.createGradient(BackgroundTheme.Type.GRADIENT_7.getColors());
            }
            bgImageView.setImageDrawable(activeDrawable);
        } else if (type == BackgroundTheme.Type.CUSTOM_IMAGE) {
            String uriStr = preferences.getCustomBgUri();
            if (uriStr != null) {
                try {
                    Uri uri = Uri.parse(uriStr);
                    InputStream is = getContentResolver().openInputStream(uri);
                    activeDrawable = Drawable.createFromStream(is, uri.toString());
                } catch (Exception ignored) {}
            }
            if (activeDrawable == null) {
                activeDrawable = BackgroundTheme.createGradient(BackgroundTheme.Type.GRADIENT_1.getColors());
            }
            bgImageView.setImageDrawable(activeDrawable);
        } else if (type == BackgroundTheme.Type.DYNAMIC_TIME) {
            activeDrawable = BackgroundTheme.getDynamicTimeGradient();
            bgImageView.setImageDrawable(activeDrawable);
        } else if (type.isGradient()) {
            activeDrawable = BackgroundTheme.createGradient(type.getColors());
            bgImageView.setImageDrawable(activeDrawable);
        } else if (type.isSolid()) {
            GradientDrawable solid = new GradientDrawable();
            solid.setColor(type.getSolidColor());
            activeDrawable = solid;
            bgImageView.setImageDrawable(solid);
        } else {
            activeDrawable = BackgroundTheme.createGradient(BackgroundTheme.Type.GRADIENT_1.getColors());
            bgImageView.setImageDrawable(activeDrawable);
        }

        // Dynamically extract vibrant accent color from wallpaper/theme and apply across app
        int dynamicAccent = BackgroundTheme.getAccentColorForTheme(type, activeDrawable);
        preferences.setThemeAccentColor(dynamicAccent);
        applyAccentColorToViews(dynamicAccent);
    }

    private void applyAccentColorToViews(int accentColor) {
        if (standbyDigitalDate != null) standbyDigitalDate.setTextColor(accentColor);
        if (standbyAnalogDate != null) standbyAnalogDate.setTextColor(accentColor);
        if (headerBatteryPercent != null && currentIsCharging) {
            headerBatteryPercent.setTextColor(accentColor);
        }
    }

    private void setupHeaderButtons() {
        findViewById(R.id.btnSettings).setOnClickListener(v -> showSettingsDialog());
    }

    public void rebuildWidgetGrid() {
        int gridSize = preferences.getGridSize();
        widgetGridLayout.removeAllViews();

        int rows;
        int cols;
        int totalSlots;

        if (gridSize == 4) {
            // Custom Grid mode
            rows = preferences.getCustomRows();
            cols = preferences.getCustomCols();
            totalSlots = preferences.getCustomSlotCount();
        } else {
            rows = gridSize;
            cols = gridSize;
            totalSlots = gridSize * gridSize;
        }

        widgetGridLayout.setRowCount(rows);
        widgetGridLayout.setColumnCount(cols);

        List<WidgetType> widgets = preferences.getWidgetsForGrid(gridSize);

        for (int i = 0; i < totalSlots; i++) {
            final int slotIndex = i;
            WidgetType type = (i < widgets.size()) ? widgets.get(i) : WidgetType.EMPTY;

            PinchWidgetCard card = new PinchWidgetCard(this);
            card.setSlotIndex(slotIndex);
            card.setOnWidgetEditListener(new PinchWidgetCard.OnWidgetEditListener() {
                @Override
                public void onWidgetPinch(int slot) {
                    showWidgetSpecificConfigDialog(slot, type);
                }

                @Override
                public void onWidgetLongPress(int slot) {
                    showWidgetSpecificConfigDialog(slot, type);
                }
            });

            View widgetContent = widgetViewHolder.inflateWidget(type, card);

            // Handle AppWidget hosting if SYSTEM_APP_WIDGET
            if (type == WidgetType.SYSTEM_APP_WIDGET) {
                int appWidgetId = preferences.getAppWidgetId(slotIndex);
                if (appWidgetId != -1) {
                    AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
                    if (appWidgetInfo != null) {
                        try {
                            AppWidgetHostView hostView = appWidgetHost.createView(this, appWidgetId, appWidgetInfo);
                            hostView.setAppWidget(appWidgetId, appWidgetInfo);
                            FrameLayout container = widgetContent.findViewById(R.id.systemAppWidgetContainer);
                            View placeholder = widgetContent.findViewById(R.id.systemAppWidgetEmptyPlaceholder);
                            if (placeholder != null) placeholder.setVisibility(View.GONE);
                            if (container != null) {
                                container.removeAllViews();
                                container.addView(hostView);
                            }
                        } catch (Exception ignored) {}
                    }
                }
                widgetContent.setOnClickListener(v -> pickAppWidget(slotIndex));
            } else if (type == WidgetType.EMPTY) {
                widgetContent.setOnClickListener(v -> showWidgetPickerDialog(slotIndex));
            }

            card.addView(widgetContent);

            int colSpan = Math.min(cols, preferences.getColSpan(slotIndex));
            int rowSpan = Math.min(rows, preferences.getRowSpan(slotIndex));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = 0;
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, rowSpan, 1f);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, colSpan, 1f);
            params.setMargins(10, 10, 10, 10);
            card.setLayoutParams(params);

            widgetGridLayout.addView(card);
        }
    }

    /**
     * Launch System AppWidget Picker
     */
    private void pickAppWidget(int slotIndex) {
        pendingSlotForAppWidget = slotIndex;
        int appWidgetId = appWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_APPWIDGET) {
            if (resultCode == RESULT_OK && data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    AppWidgetProviderInfo appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
                    if (appWidgetInfo != null && appWidgetInfo.configure != null) {
                        Intent configIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                        configIntent.setComponent(appWidgetInfo.configure);
                        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                        startActivityForResult(configIntent, REQUEST_CREATE_APPWIDGET);
                    } else {
                        completeAppWidgetBinding(appWidgetId);
                    }
                }
            } else if (resultCode == RESULT_CANCELED && data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    appWidgetHost.deleteAppWidgetId(appWidgetId);
                }
            }
        } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
            if (resultCode == RESULT_OK && data != null) {
                int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
                if (appWidgetId != -1) {
                    completeAppWidgetBinding(appWidgetId);
                }
            }
        }
    }

    private void completeAppWidgetBinding(int appWidgetId) {
        if (pendingSlotForAppWidget >= 0) {
            preferences.setAppWidgetId(pendingSlotForAppWidget, appWidgetId);
            preferences.setWidgetForSlot(pendingSlotForAppWidget, WidgetType.SYSTEM_APP_WIDGET);
            rebuildWidgetGrid();
        }
    }

    /**
     * When user pinches on a widget, first show settings specifically for that widget.
     * At the very bottom, give the option to change the widget.
     */
    private void showWidgetSpecificConfigDialog(int slotIndex, WidgetType type) {
        Dialog dialog = new Dialog(this, R.style.Theme_NestHubStandby);
        dialog.setContentView(R.layout.dialog_widget_specific_config);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.Animation_NestHub_DialogZoom);
        }

        TextView title = dialog.findViewById(R.id.specificConfigTitle);
        TextView subtitle = dialog.findViewById(R.id.specificConfigSubtitle);
        LinearLayout optionsContainer = dialog.findViewById(R.id.specificOptionsContainer);

        title.setText(type.getTitle() + " Options");
        subtitle.setText("Slot #" + (slotIndex + 1) + " • " + type.getDescription());

        // Widget Sizing & Span buttons
        Button btnSpanCol1 = dialog.findViewById(R.id.btnSpanCol1);
        Button btnSpanCol2 = dialog.findViewById(R.id.btnSpanCol2);
        Button btnSpanRow2 = dialog.findViewById(R.id.btnSpanRow2);
        Button btnRemoveWidget = dialog.findViewById(R.id.btnRemoveWidget);

        btnSpanCol1.setOnClickListener(v -> {
            preferences.setColSpan(slotIndex, 1);
            rebuildWidgetGrid();
            dialog.dismiss();
        });

        btnSpanCol2.setOnClickListener(v -> {
            preferences.setColSpan(slotIndex, 2);
            rebuildWidgetGrid();
            dialog.dismiss();
        });

        btnSpanRow2.setOnClickListener(v -> {
            int cur = preferences.getRowSpan(slotIndex);
            preferences.setRowSpan(slotIndex, cur == 1 ? 2 : 1);
            rebuildWidgetGrid();
            dialog.dismiss();
        });

        btnRemoveWidget.setOnClickListener(v -> {
            preferences.setWidgetForSlot(slotIndex, WidgetType.EMPTY);
            rebuildWidgetGrid();
            dialog.dismiss();
        });

        // Add dynamic specific controls based on widget type
        optionsContainer.removeAllViews();
        switch (type) {
            case DIGITAL_CLOCK:
                CheckBox cb24 = new CheckBox(this);
                cb24.setText("Use 24-Hour Military Format");
                cb24.setTextColor(Color.WHITE);
                cb24.setChecked(preferences.is24HourFormat());
                cb24.setOnCheckedChangeListener((b, isChecked) -> {
                    preferences.set24HourFormat(isChecked);
                    rebuildWidgetGrid();
                });
                optionsContainer.addView(cb24);
                break;

            case WEATHER:
                CheckBox cbCelsius = new CheckBox(this);
                cbCelsius.setText("Display Temperature in Celsius (°C)");
                cbCelsius.setTextColor(Color.WHITE);
                cbCelsius.setChecked(preferences.isCelsius());
                cbCelsius.setOnCheckedChangeListener((b, isChecked) -> {
                    preferences.setCelsius(isChecked);
                    rebuildWidgetGrid();
                    updateWeatherDisplays();
                });
                optionsContainer.addView(cbCelsius);
                break;

            case ALARM:
                Button btnAlarm = new Button(this);
                btnAlarm.setText("Open Android System Alarms");
                btnAlarm.setOnClickListener(v -> {
                    try {
                        startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS));
                    } catch (Exception ignored) {}
                });
                optionsContainer.addView(btnAlarm);
                break;

            case MULTI_TIMER:
                TextView tvTimer = new TextView(this);
                tvTimer.setText("Quick Add Preset Timer:");
                tvTimer.setTextColor(Color.WHITE);
                tvTimer.setPadding(0, 0, 0, 12);
                optionsContainer.addView(tvTimer);

                LinearLayout timerRow = new LinearLayout(this);
                timerRow.setOrientation(LinearLayout.HORIZONTAL);
                int[] presets = {5, 15, 30, 45};
                for (int p : presets) {
                    Button b = new Button(this);
                    b.setText(p + "m");
                    b.setOnClickListener(v -> {
                        try {
                            Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                            intent.putExtra(AlarmClock.EXTRA_LENGTH, p * 60);
                            intent.putExtra(AlarmClock.EXTRA_MESSAGE, p + "m Bedside Timer");
                            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                            startActivity(intent);
                        } catch (Exception ignored) {}
                    });
                    timerRow.addView(b);
                }
                optionsContainer.addView(timerRow);
                break;

            case SYSTEM_APP_WIDGET:
                Button btnPickAppWidget = new Button(this);
                btnPickAppWidget.setText("Select Different Home Screen Widget");
                btnPickAppWidget.setOnClickListener(v -> {
                    dialog.dismiss();
                    pickAppWidget(slotIndex);
                });
                optionsContainer.addView(btnPickAppWidget);
                break;

            default:
                TextView tvGeneral = new TextView(this);
                tvGeneral.setText("Active widget: " + type.getTitle() + "\nReal-time updates & bedside integration active.");
                tvGeneral.setTextColor(Color.WHITE);
                tvGeneral.setTextSize(14);
                optionsContainer.addView(tvGeneral);
                break;
        }

        // Color Accent Dots
        View.OnClickListener colorClick = v -> {
            int color = 0xFF4D88FF;
            int id = v.getId();
            if (id == R.id.colorDot1) color = 0xFF4D88FF;
            else if (id == R.id.colorDot2) color = 0xFFFF5252;
            else if (id == R.id.colorDot3) color = 0xFF00E676;
            else if (id == R.id.colorDot4) color = 0xFFFFB300;
            else if (id == R.id.colorDot5) color = 0xFF00E5FF;
            preferences.setThemeAccentColor(color);
            applyAccentColorToViews(color);
            rebuildWidgetGrid();
        };

        dialog.findViewById(R.id.colorDot1).setOnClickListener(colorClick);
        dialog.findViewById(R.id.colorDot2).setOnClickListener(colorClick);
        dialog.findViewById(R.id.colorDot3).setOnClickListener(colorClick);
        dialog.findViewById(R.id.colorDot4).setOnClickListener(colorClick);
        dialog.findViewById(R.id.colorDot5).setOnClickListener(colorClick);

        // At the very bottom: button to change widget type
        dialog.findViewById(R.id.btnChangeWidgetType).setOnClickListener(v -> {
            dialog.dismiss();
            showWidgetPickerDialog(slotIndex);
        });

        dialog.findViewById(R.id.btnDoneWidgetConfig).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showWidgetPickerDialog(int slotIndex) {
        Dialog dialog = new Dialog(this, R.style.Theme_NestHubStandby);
        dialog.setContentView(R.layout.dialog_widget_picker);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.Animation_NestHub_DialogZoom);
        }

        TextView subtitle = dialog.findViewById(R.id.pickerSubtitle);
        subtitle.setText("Select from 20+ smart bedside widgets to place in Slot #" + (slotIndex + 1));

        RecyclerView rv = dialog.findViewById(R.id.widgetRecyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));

        WidgetType[] allTypes = WidgetType.values();
        rv.setAdapter(new RecyclerView.Adapter<WidgetOptionVH>() {
            @NonNull
            @Override
            public WidgetOptionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = getLayoutInflater().inflate(R.layout.item_widget_option, parent, false);
                return new WidgetOptionVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull WidgetOptionVH holder, int position) {
                WidgetType wt = allTypes[position];
                holder.title.setText(wt.getTitle());
                holder.desc.setText(wt.getDescription());
                holder.previewEmoji.setText(getWidgetEmoji(wt));

                holder.itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (wt == WidgetType.SYSTEM_APP_WIDGET) {
                        pickAppWidget(slotIndex);
                    } else {
                        preferences.setWidgetForSlot(slotIndex, wt);
                        rebuildWidgetGrid();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return allTypes.length;
            }
        });

        dialog.findViewById(R.id.btnClosePicker).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private String getWidgetEmoji(WidgetType wt) {
        switch (wt) {
            case DIGITAL_CLOCK: return "10:45";
            case ANALOG_CLOCK: return "🕒";
            case WEATHER: return "24°☀️";
            case CALENDAR: return "📅";
            case ALARM: return "⏰";
            case MULTI_TIMER: return "⏳";
            case STOPWATCH: return "⏱️";
            case DUAL_BATTERY: return "🔋";
            case MEDIA_PLAYER: return "🎵";
            case QUICK_TOGGLES: return "⚡";
            case GEMINI_ASSISTANT: return "✨";
            case AMBIENT_SOUNDS: return "🌊";
            case DAILY_ROUTINE: return "☑️";
            case STICKY_NOTES: return "📝";
            case DAILY_QUOTES: return "💬";
            case WORLD_CLOCK: return "🌍";
            case WATER_TRACKER: return "💧";
            case SUN_MOON: return "🌗";
            case PHOTO_FRAME: return "🖼️";
            case SYSTEM_APP_WIDGET: return "⊞";
            case EMPTY: return "➕";
            default: return "📱";
        }
    }

    static class WidgetOptionVH extends RecyclerView.ViewHolder {
        TextView title;
        TextView desc;
        TextView previewEmoji;

        WidgetOptionVH(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.widgetOptionTitle);
            desc = itemView.findViewById(R.id.widgetOptionDesc);
            previewEmoji = itemView.findViewById(R.id.widgetOptionPreviewEmoji);
        }
    }

    private void showSettingsDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_NestHubStandby);
        dialog.setContentView(R.layout.dialog_settings);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.Animation_NestHub_DialogZoom);
        }

        RadioGroup rg = dialog.findViewById(R.id.radioGroupGrid);
        RadioButton r2 = dialog.findViewById(R.id.radioGrid2x2);
        RadioButton r3 = dialog.findViewById(R.id.radioGrid3x3);
        RadioButton rCustom = dialog.findViewById(R.id.radioGridCustom);

        LinearLayout layoutCustomGrid = dialog.findViewById(R.id.layoutCustomGridSettings);
        TextView tvCustomColInfo = dialog.findViewById(R.id.tvCustomColInfo);
        TextView tvCustomRowInfo = dialog.findViewById(R.id.tvCustomRowInfo);
        TextView tvCustomSlotsCount = dialog.findViewById(R.id.tvCustomSlotsCount);
        Button btnDecCols = dialog.findViewById(R.id.btnDecCols);
        Button btnIncCols = dialog.findViewById(R.id.btnIncCols);
        Button btnDecRows = dialog.findViewById(R.id.btnDecRows);
        Button btnIncRows = dialog.findViewById(R.id.btnIncRows);
        Button btnAddSlot = dialog.findViewById(R.id.btnAddSlot);
        Button btnRemoveSlot = dialog.findViewById(R.id.btnRemoveSlot);

        int currentGridSize = preferences.getGridSize();
        if (currentGridSize == 2) {
            r2.setChecked(true);
            layoutCustomGrid.setVisibility(View.GONE);
        } else if (currentGridSize == 3) {
            r3.setChecked(true);
            layoutCustomGrid.setVisibility(View.GONE);
        } else {
            rCustom.setChecked(true);
            layoutCustomGrid.setVisibility(View.VISIBLE);
        }

        Runnable updateCustomLabels = () -> {
            tvCustomColInfo.setText("Columns: " + preferences.getCustomCols());
            tvCustomRowInfo.setText("Rows: " + preferences.getCustomRows());
            tvCustomSlotsCount.setText("Total Slots: " + preferences.getCustomSlotCount());
        };
        updateCustomLabels.run();

        btnDecCols.setOnClickListener(v -> {
            preferences.setCustomCols(preferences.getCustomCols() - 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });
        btnIncCols.setOnClickListener(v -> {
            preferences.setCustomCols(preferences.getCustomCols() + 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });
        btnDecRows.setOnClickListener(v -> {
            preferences.setCustomRows(preferences.getCustomRows() - 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });
        btnIncRows.setOnClickListener(v -> {
            preferences.setCustomRows(preferences.getCustomRows() + 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });
        btnAddSlot.setOnClickListener(v -> {
            preferences.setCustomSlotCount(preferences.getCustomSlotCount() + 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });
        btnRemoveSlot.setOnClickListener(v -> {
            preferences.setCustomSlotCount(preferences.getCustomSlotCount() - 1);
            updateCustomLabels.run();
            rebuildWidgetGrid();
        });

        rg.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioGrid2x2) {
                preferences.setGridSize(2);
                layoutCustomGrid.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioGrid3x3) {
                preferences.setGridSize(3);
                layoutCustomGrid.setVisibility(View.GONE);
            } else {
                preferences.setGridSize(4);
                layoutCustomGrid.setVisibility(View.VISIBLE);
            }
            rebuildWidgetGrid();
        });

        // StandBy Pure Black toggle
        CheckBox cbStandbyBlack = dialog.findViewById(R.id.checkStandbyPureBlack);
        cbStandbyBlack.setChecked(preferences.isStandbyPureBlack());
        cbStandbyBlack.setOnCheckedChangeListener((b, isChecked) -> {
            preferences.setStandbyPureBlack(isChecked);
            checkStandbyBackground();
        });

        // LIVE WALLPAPER PREVIEW CARD
        View wallpaperPreviewBg = dialog.findViewById(R.id.wallpaperPreviewBg);
        TextView tvWallpaperPreviewName = dialog.findViewById(R.id.tvWallpaperPreviewName);
        TextView tvWallpaperStatus = dialog.findViewById(R.id.tvWallpaperStatus);
        Button btnApplyWallpaper = dialog.findViewById(R.id.btnApplyWallpaper);

        final BackgroundTheme.Type[] pendingTheme = {preferences.getBackgroundTheme()};
        final Drawable[] pendingDrawable = {bgImageView.getDrawable()};

        if (pendingDrawable[0] != null) {
            wallpaperPreviewBg.setBackground(pendingDrawable[0]);
        }
        tvWallpaperPreviewName.setText(pendingTheme[0].getDisplayName());

        Runnable updatePreview = () -> {
            if (pendingDrawable[0] != null) {
                wallpaperPreviewBg.setBackground(pendingDrawable[0]);
            }
            tvWallpaperPreviewName.setText(pendingTheme[0].getDisplayName());
            tvWallpaperStatus.setText("Preview ready • Tap 'Apply' to confirm");
            btnApplyWallpaper.setEnabled(true);
        };

        btnApplyWallpaper.setOnClickListener(v -> {
            preferences.setBackgroundTheme(pendingTheme[0]);
            applyBackgroundTheme();
            tvWallpaperStatus.setText("✓ Wallpaper Applied!");
            btnApplyWallpaper.setEnabled(false);
        });

        // Copy Device Wallpaper
        dialog.findViewById(R.id.btnCopyDeviceWallpaper).setOnClickListener(v -> {
            pendingTheme[0] = BackgroundTheme.Type.DEVICE_WALLPAPER;
            try {
                WallpaperManager wm = WallpaperManager.getInstance(this);
                pendingDrawable[0] = wm.getDrawable();
                if (pendingDrawable[0] == null) pendingDrawable[0] = wm.peekDrawable();
            } catch (Exception ignored) {}
            if (pendingDrawable[0] == null) {
                pendingDrawable[0] = BackgroundTheme.createGradient(BackgroundTheme.Type.GRADIENT_7.getColors());
            }
            updatePreview.run();
        });

        // Import custom photo
        dialog.findViewById(R.id.btnImportCustomBg).setOnClickListener(v -> {
            imagePickerLauncher.launch("image/*");
            dialog.dismiss();
        });

        // Dynamic Time/Weather button
        dialog.findViewById(R.id.btnDynamicTheme).setOnClickListener(v -> {
            pendingTheme[0] = BackgroundTheme.Type.DYNAMIC_TIME;
            pendingDrawable[0] = BackgroundTheme.getDynamicTimeGradient();
            updatePreview.run();
        });

        // Populate 10 Gradients
        LinearLayout gradContainer = dialog.findViewById(R.id.gradientButtonsContainer);
        BackgroundTheme.Type[] allThemes = BackgroundTheme.Type.values();
        for (BackgroundTheme.Type bt : allThemes) {
            if (bt.isGradient()) {
                Button btn = new Button(this);
                btn.setText(bt.getDisplayName());
                btn.setTextSize(12);
                btn.setTextColor(Color.WHITE);
                btn.setBackground(BackgroundTheme.createGradient(bt.getColors()));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (int) (40 * getResources().getDisplayMetrics().density)
                );
                lp.setMargins(8, 0, 8, 0);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> {
                    pendingTheme[0] = bt;
                    pendingDrawable[0] = BackgroundTheme.createGradient(bt.getColors());
                    updatePreview.run();
                });
                gradContainer.addView(btn);
            }
        }

        // Populate 10 Solids
        LinearLayout solidContainer = dialog.findViewById(R.id.solidButtonsContainer);
        for (BackgroundTheme.Type bt : allThemes) {
            if (bt.isSolid()) {
                Button btn = new Button(this);
                btn.setText(bt.getDisplayName());
                btn.setTextSize(12);
                btn.setTextColor(Color.WHITE);
                GradientDrawable gd = new GradientDrawable();
                gd.setColor(bt.getSolidColor());
                gd.setCornerRadius(16);
                gd.setStroke(2, 0x44FFFFFF);
                btn.setBackground(gd);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        (int) (40 * getResources().getDisplayMetrics().density)
                );
                lp.setMargins(8, 0, 8, 0);
                btn.setLayoutParams(lp);
                btn.setOnClickListener(v -> {
                    pendingTheme[0] = bt;
                    GradientDrawable solid = new GradientDrawable();
                    solid.setColor(bt.getSolidColor());
                    pendingDrawable[0] = solid;
                    updatePreview.run();
                });
                solidContainer.addView(btn);
            }
        }

        // Bedside Checkboxes
        CheckBox cbKeepScreen = dialog.findViewById(R.id.checkKeepScreenOn);
        CheckBox cbRedFilter = dialog.findViewById(R.id.checkRedFilter);
        CheckBox cb24Hour = dialog.findViewById(R.id.check24Hour);

        cbKeepScreen.setChecked(preferences.isKeepScreenOn());
        cbRedFilter.setChecked(preferences.isRedFilterEnabled());
        cb24Hour.setChecked(preferences.is24HourFormat());

        cbKeepScreen.setOnCheckedChangeListener((b, isChecked) -> {
            preferences.setKeepScreenOn(isChecked);
            findViewById(R.id.rootContainer).setKeepScreenOn(isChecked);
        });

        cbRedFilter.setOnCheckedChangeListener((b, isChecked) -> {
            preferences.setRedFilterEnabled(isChecked);
            redFilterOverlay.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        cb24Hour.setOnCheckedChangeListener((b, isChecked) -> {
            preferences.set24HourFormat(isChecked);
            rebuildWidgetGrid();
        });

        CheckBox cbCelsius = dialog.findViewById(R.id.checkUseCelsius);
        if (cbCelsius != null) {
            cbCelsius.setChecked(preferences.isCelsius());
            cbCelsius.setOnCheckedChangeListener((b, isChecked) -> {
                preferences.setCelsius(isChecked);
                rebuildWidgetGrid();
                updateWeatherDisplays();
            });
        }

        // Restart Tutorial Button
        Button btnRestartTutorial = dialog.findViewById(R.id.btnRestartTutorial);
        if (btnRestartTutorial != null) {
            btnRestartTutorial.setOnClickListener(v -> {
                dialog.dismiss();
                showTutorialDialog();
            });
        }

        dialog.findViewById(R.id.btnDoneSettings).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * First-time onboarding walkthrough tutorial
     */
    private void showTutorialDialog() {
        Dialog dialog = new Dialog(this, R.style.Theme_NestHubStandby);
        dialog.setContentView(R.layout.dialog_tutorial);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setWindowAnimations(R.style.Animation_NestHub_DialogZoom);
        }

        ViewFlipper flipper = dialog.findViewById(R.id.tutorialFlipper);
        TextView dots = dialog.findViewById(R.id.tutorialPageDots);
        Button btnSkip = dialog.findViewById(R.id.btnTutorialSkip);
        Button btnNext = dialog.findViewById(R.id.btnTutorialNext);

        final int totalSteps = 5;
        final String[] dotPatterns = {
                "●  ○  ○  ○  ○",
                "○  ●  ○  ○  ○",
                "○  ○  ●  ○  ○",
                "○  ○  ○  ●  ○",
                "○  ○  ○  ○  ●"
        };

        btnSkip.setOnClickListener(v -> {
            preferences.setTutorialCompleted(true);
            dialog.dismiss();
        });

        btnNext.setOnClickListener(v -> {
            int current = flipper.getDisplayedChild();
            if (current < totalSteps - 1) {
                flipper.setInAnimation(this, R.anim.slide_in_bottom);
                flipper.setOutAnimation(this, R.anim.slide_out_top);
                flipper.showNext();
                int next = flipper.getDisplayedChild();
                dots.setText(dotPatterns[next]);
                if (next == totalSteps - 1) {
                    btnNext.setText("Get Started ✓");
                }
            } else {
                preferences.setTutorialCompleted(true);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updateNextAlarm() {
        try {
            AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                AlarmManager.AlarmClockInfo nextAlarm = am.getNextAlarmClock();
                if (nextAlarm != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    nextAlarmString = "Alarm: " + sdf.format(new Date(nextAlarm.getTriggerTime()));
                } else {
                    nextAlarmString = "Alarm: 07:00 AM • Tomorrow";
                }
            }
        } catch (Exception e) {
            nextAlarmString = "Alarm: 07:00 AM • Tomorrow";
        }

        if (standbyDigitalAlarm != null) standbyDigitalAlarm.setText(nextAlarmString);
        if (standbyAnalogAlarm != null) standbyAnalogAlarm.setText(nextAlarmString);
    }

    private void updateWeatherDisplays() {
        boolean isC = preferences.isCelsius();
        String weatherStr = isC ? "23°C  •  Mostly Sunny" : "74°F  •  Mostly Sunny";
        if (standbyDigitalWeather != null) {
            standbyDigitalWeather.setText(weatherStr);
        }
    }

    private void startTimeTicker() {
        Runnable ticker = new Runnable() {
            @Override
            public void run() {
                Calendar c = Calendar.getInstance();
                boolean is24 = preferences.is24HourFormat();
                SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
                SimpleDateFormat tf = new SimpleDateFormat(is24 ? "HH:mm" : "hh:mm", Locale.getDefault());
                SimpleDateFormat sec = new SimpleDateFormat(":ss", Locale.getDefault());

                String dateStr = df.format(c.getTime());
                String timeStr = tf.format(c.getTime());
                String secStr = sec.format(c.getTime());

                if (headerDate != null) headerDate.setText(dateStr);

                if (standbyDigitalTime != null) standbyDigitalTime.setText(timeStr);
                if (standbyDigitalSeconds != null) standbyDigitalSeconds.setText(secStr);
                if (standbyDigitalDate != null) standbyDigitalDate.setText(dateStr.toUpperCase(Locale.getDefault()));

                if (standbyAnalogDate != null) standbyAnalogDate.setText(dateStr.toUpperCase(Locale.getDefault()));

                timeUpdateHandler.postDelayed(this, 1000);
            }
        };
        timeUpdateHandler.post(ticker);
    }

    @Override
    public void onDeviceBatteryChanged(int percent, boolean isCharging, String chargeType) {
        currentBatteryPercent = percent;
        currentIsCharging = isCharging;
        currentChargeType = chargeType;

        String batText = percent + "%" + (isCharging ? " ⚡" : "");
        String fullStatus = "Battery: " + percent + "% • " + (isCharging ? chargeType : "Discharging");

        if (headerBatteryPercent != null) {
            headerBatteryPercent.setText(batText);
        }

        if (standbyDigitalBattery != null) {
            standbyDigitalBattery.setText(fullStatus);
        }

        if (standbyAnalogBattery != null) {
            standbyAnalogBattery.setText(fullStatus);
        }
    }

    @Override
    public void onBluetoothBatteriesChanged(List<BatteryStatusReceiver.BluetoothDeviceInfo> devices) {
        // Handled inside dual battery widget
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (appWidgetHost != null) {
            try {
                appWidgetHost.startListening();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (appWidgetHost != null) {
            try {
                appWidgetHost.stopListening();
            } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (wakeLock != null) {
            try {
                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();
                }
            } catch (Exception ignored) {}
        }
        if (batteryReceiver != null) {
            batteryReceiver.register(this);
        }
        updateNextAlarm();

        if (audioManager != null && audioManager.isMusicActive()) {
            isMusicActivelyPlaying = true;
            musicStoppedTimestamp = 0;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            } catch (Exception ignored) {}
        }
        if (batteryReceiver != null) {
            batteryReceiver.unregister(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null) {
            try {
                if (wakeLock.isHeld()) {
                    wakeLock.release();
                }
            } catch (Exception ignored) {}
        }
        timeUpdateHandler.removeCallbacksAndMessages(null);
        brightnessHudHandler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioManager != null && audioPlaybackCallback != null) {
            try {
                audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback);
            } catch (Exception ignored) {}
        }
        AmbientSoundPlayer.getInstance().stop();
    }

    private void initBrightnessAndAudio() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        musicStoppedTimestamp = preferences.getMusicStoppedTimestamp();

        float savedBrightness = preferences.getScreenBrightness();
        if (savedBrightness >= 0.02f && savedBrightness <= 1.0f) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.screenBrightness = savedBrightness;
            getWindow().setAttributes(lp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioManager != null) {
            audioPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
                @Override
                public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                    super.onPlaybackConfigChanged(configs);
                    evaluatePlaybackConfigurations(configs);
                }
            };
            try {
                audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, new Handler(Looper.getMainLooper()));
            } catch (Exception ignored) {}
        }
    }

    private void evaluatePlaybackConfigurations(List<AudioPlaybackConfiguration> configs) {
        boolean mediaActive = false;
        if (configs != null && !configs.isEmpty()) {
            for (AudioPlaybackConfiguration config : configs) {
                AudioAttributes attrs = config.getAudioAttributes();
                if (attrs != null) {
                    int usage = attrs.getUsage();
                    if (usage == AudioAttributes.USAGE_MEDIA || usage == AudioAttributes.USAGE_GAME || usage == AudioAttributes.USAGE_UNKNOWN) {
                        mediaActive = true;
                        break;
                    }
                }
            }
        }
        if (audioManager != null && audioManager.isMusicActive()) {
            mediaActive = true;
        }

        if (mediaActive) {
            isMusicActivelyPlaying = true;
            musicStoppedTimestamp = 0;
            preferences.setMusicStoppedTimestamp(0);
        } else {
            if (isMusicActivelyPlaying) {
                isMusicActivelyPlaying = false;
                musicStoppedTimestamp = System.currentTimeMillis();
                preferences.setMusicStoppedTimestamp(musicStoppedTimestamp);
            }
        }
    }

    private boolean shouldVolumeControlBrightness() {
        if (audioManager == null) return true;

        boolean activeNow = audioManager.isMusicActive() || isMusicActivelyPlaying;
        if (activeNow) {
            isMusicActivelyPlaying = true;
            musicStoppedTimestamp = 0;
            return false;
        }

        if (musicStoppedTimestamp > 0) {
            long elapsed = System.currentTimeMillis() - musicStoppedTimestamp;
            if (elapsed < MUSIC_STOP_COOLDOWN_MS) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (shouldVolumeControlBrightness()) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    adjustScreenBrightness(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void adjustScreenBrightness(boolean increase) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float current = lp.screenBrightness;
        if (current < 0) {
            current = preferences.getScreenBrightness();
        }

        float step = 0.05f;
        float target = increase ? (current + step) : (current - step);
        if (target > 1.0f) target = 1.0f;
        if (target < 0.02f) target = 0.02f;

        lp.screenBrightness = target;
        getWindow().setAttributes(lp);
        preferences.setScreenBrightness(target);

        showBrightnessHud(target);
    }

    private final Runnable hideBrightnessHudRunnable = () -> {
        if (brightnessHudContainer != null) {
            brightnessHudContainer.animate()
                    .alpha(0f)
                    .setDuration(250)
                    .withEndAction(() -> brightnessHudContainer.setVisibility(View.GONE))
                    .start();
        }
    };

    private void showBrightnessHud(float brightness) {
        if (brightnessHudContainer == null) return;

        brightnessHudHandler.removeCallbacks(hideBrightnessHudRunnable);
        int percent = Math.round(brightness * 100);
        if (brightnessHudProgress != null) {
            brightnessHudProgress.setProgress(percent);
        }
        if (brightnessHudText != null) {
            brightnessHudText.setText(percent + "%");
        }

        brightnessHudContainer.animate().cancel();
        brightnessHudContainer.setAlpha(1f);
        brightnessHudContainer.setVisibility(View.VISIBLE);

        brightnessHudHandler.postDelayed(hideBrightnessHudRunnable, 1500);
    }
}
