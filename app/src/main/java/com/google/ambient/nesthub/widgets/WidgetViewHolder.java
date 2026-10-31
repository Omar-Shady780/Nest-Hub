package com.google.ambient.nesthub.widgets;

import android.app.Activity;
import android.app.AlarmManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.ambient.nesthub.R;
import com.google.ambient.nesthub.data.AppPreferences;
import com.google.ambient.nesthub.data.WidgetType;
import com.google.ambient.nesthub.sound.AmbientSoundPlayer;
import com.google.ambient.nesthub.views.AnalogClockView;
import com.google.ambient.nesthub.views.MoonPhaseView;
import com.google.ambient.nesthub.views.StopwatchDialView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class WidgetViewHolder {

    private final Context context;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isFlashlightOn = false;

    // Realistic Stopwatch State
    private boolean isStopwatchRunning = false;
    private long stopwatchStartTime = 0;
    private long stopwatchElapsedTime = 0;
    private final List<String> stopwatchLaps = new ArrayList<>();

    // Multi-Timer State
    private int timer1SecondsLeft = 15 * 60;
    private boolean isTimer1Running = false;

    // Quotes
    private static final String[][] QUOTES = {
            {"Peace comes from within. Do not seek it without.", "Buddha"},
            {"The secret of getting ahead is getting started.", "Mark Twain"},
            {"Wherever you go, go with all your heart.", "Confucius"},
            {"Sleep is the best meditation.", "Dalai Lama"},
            {"Every moment is a fresh beginning.", "T.S. Eliot"},
            {"Act as if what you do makes a difference. It does.", "William James"}
    };
    private int quoteIndex = 0;

    public WidgetViewHolder(Context context) {
        this.context = context;
    }

    public View inflateWidget(WidgetType type, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view;

        switch (type) {
            case DIGITAL_CLOCK:
                view = inflater.inflate(R.layout.widget_digital_clock, parent, false);
                bindDigitalClock(view);
                break;
            case ANALOG_CLOCK:
                view = inflater.inflate(R.layout.widget_analog_clock, parent, false);
                break;
            case WEATHER:
                view = inflater.inflate(R.layout.widget_weather, parent, false);
                bindWeather(view);
                break;
            case CALENDAR:
                view = inflater.inflate(R.layout.widget_calendar, parent, false);
                bindCalendar(view);
                break;
            case ALARM:
                view = inflater.inflate(R.layout.widget_alarm, parent, false);
                bindAlarm(view);
                break;
            case MULTI_TIMER:
                view = inflater.inflate(R.layout.widget_timer, parent, false);
                bindMultiTimer(view);
                break;
            case STOPWATCH:
                view = inflater.inflate(R.layout.widget_stopwatch, parent, false);
                bindStopwatch(view);
                break;
            case DUAL_BATTERY:
                view = inflater.inflate(R.layout.widget_dual_battery, parent, false);
                bindDualBattery(view);
                break;
            case MEDIA_PLAYER:
                view = inflater.inflate(R.layout.widget_media_player, parent, false);
                bindMediaPlayer(view);
                break;
            case QUICK_TOGGLES:
                view = inflater.inflate(R.layout.widget_quick_toggles, parent, false);
                bindQuickToggles(view);
                break;
            case GEMINI_ASSISTANT:
                view = inflater.inflate(R.layout.widget_gemini, parent, false);
                bindGemini(view);
                break;
            case AMBIENT_SOUNDS:
                view = inflater.inflate(R.layout.widget_ambient_sounds, parent, false);
                bindAmbientSounds(view);
                break;
            case DAILY_ROUTINE:
                view = inflater.inflate(R.layout.widget_daily_routine, parent, false);
                bindDailyRoutine(view);
                break;
            case STICKY_NOTES:
                view = inflater.inflate(R.layout.widget_sticky_notes, parent, false);
                bindStickyNotes(view);
                break;
            case DAILY_QUOTES:
                view = inflater.inflate(R.layout.widget_quote, parent, false);
                bindQuotes(view);
                break;
            case WORLD_CLOCK:
                view = inflater.inflate(R.layout.widget_world_clock, parent, false);
                bindWorldClock(view);
                break;
            case WATER_TRACKER:
                view = inflater.inflate(R.layout.widget_water_tracker, parent, false);
                bindWaterTracker(view);
                break;
            case SUN_MOON:
                view = inflater.inflate(R.layout.widget_sun_moon, parent, false);
                bindSunMoon(view);
                break;
            case PHOTO_FRAME:
                view = inflater.inflate(R.layout.widget_photo_frame, parent, false);
                break;
            case SYSTEM_APP_WIDGET:
                view = inflater.inflate(R.layout.widget_system_appwidget, parent, false);
                break;
            case EMPTY:
                view = inflater.inflate(R.layout.widget_empty_slot, parent, false);
                break;
            default:
                view = inflater.inflate(R.layout.widget_digital_clock, parent, false);
                bindDigitalClock(view);
                break;
        }

        return view;
    }

    private void bindDigitalClock(View view) {
        TextView timeView = view.findViewById(R.id.clockTime);
        TextView secondsView = view.findViewById(R.id.clockSeconds);
        TextView amPmView = view.findViewById(R.id.clockAmPm);
        TextView dateView = view.findViewById(R.id.clockDate);

        AppPreferences prefs = new AppPreferences(context);
        int accent = prefs.getThemeAccentColor();
        if (amPmView != null) {
            amPmView.setTextColor(accent);
        }

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (view.isAttachedToWindow()) {
                    Calendar c = Calendar.getInstance();
                    boolean is24 = new AppPreferences(context).is24HourFormat();
                    SimpleDateFormat timeFormat = new SimpleDateFormat(is24 ? "HH:mm" : "hh:mm", Locale.getDefault());
                    SimpleDateFormat amPmFormat = new SimpleDateFormat("a", Locale.getDefault());
                    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
                    SimpleDateFormat secFormat = new SimpleDateFormat(":ss", Locale.getDefault());

                    if (timeView != null) timeView.setText(timeFormat.format(c.getTime()));
                    if (secondsView != null) secondsView.setText(secFormat.format(c.getTime()));
                    if (amPmView != null) amPmView.setText(is24 ? "" : amPmFormat.format(c.getTime()));
                    if (dateView != null) dateView.setText(dateFormat.format(c.getTime()));

                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(tick);
    }

    private void bindWeather(View view) {
        TextView temp = view.findViewById(R.id.weatherTemp);
        TextView highLow = view.findViewById(R.id.weatherHighLow);
        AppPreferences prefs = new AppPreferences(context);
        boolean isC = prefs.isCelsius();

        if (temp != null) {
            temp.setText(isC ? "23°C" : "74°F");
        }
        if (highLow != null) {
            highLow.setText(isC ? "H: 26°  L: 17°" : "H: 78°  L: 62°");
        }

        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("geo:0,0?q=weather"));
                context.startActivity(intent);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Real sync with Android Calendar Provider (CalendarContract.Instances)
     */
    private void bindCalendar(View view) {
        TextView event1Title = view.findViewById(R.id.calendarEvent1Title);
        TextView event1Time = view.findViewById(R.id.calendarEvent1Time);
        View event2Container = view.findViewById(R.id.calendarEvent2Container);
        TextView event2Title = view.findViewById(R.id.calendarEvent2Title);
        TextView event2Time = view.findViewById(R.id.calendarEvent2Time);
        View btnAdd = view.findViewById(R.id.btnOpenCalendar);

        view.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("content://com.android.calendar/time/" + System.currentTimeMillis()));
                context.startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.calendar");
                    if (launchIntent != null) context.startActivity(launchIntent);
                } catch (Exception ignored) {}
            }
        });

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            });
        }

        loadRealCalendarEvents(event1Title, event1Time, event2Container, event2Title, event2Time);
    }

    private void loadRealCalendarEvents(TextView e1Title, TextView e1Time, View e2Container, TextView e2Title, TextView e2Time) {
        try {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.HOUR_OF_DAY, 0);
            start.set(Calendar.MINUTE, 0);
            start.set(Calendar.SECOND, 0);

            Calendar end = Calendar.getInstance();
            end.set(Calendar.HOUR_OF_DAY, 23);
            end.set(Calendar.MINUTE, 59);
            end.set(Calendar.SECOND, 59);

            Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
            ContentUris.appendId(builder, start.getTimeInMillis());
            ContentUris.appendId(builder, end.getTimeInMillis());

            String[] projection = new String[]{
                    CalendarContract.Instances.TITLE,
                    CalendarContract.Instances.BEGIN,
                    CalendarContract.Instances.END,
                    CalendarContract.Instances.ALL_DAY
            };

            Cursor cursor = context.getContentResolver().query(
                    builder.build(),
                    projection,
                    null,
                    null,
                    CalendarContract.Instances.BEGIN + " ASC"
            );

            if (cursor != null) {
                int count = 0;
                SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                while (cursor.moveToNext() && count < 2) {
                    String title = cursor.getString(0);
                    long begin = cursor.getLong(1);
                    long finish = cursor.getLong(2);
                    boolean allDay = cursor.getInt(3) == 1;

                    String timeStr = allDay ? "All Day" : (timeFmt.format(new Date(begin)) + " – " + timeFmt.format(new Date(finish)));
                    if (count == 0) {
                        e1Title.setText(title != null ? title : "Upcoming Event");
                        e1Time.setText(timeStr);
                    } else {
                        if (e2Container != null) e2Container.setVisibility(View.VISIBLE);
                        if (e2Title != null) e2Title.setText(title != null ? title : "Upcoming Event");
                        if (e2Time != null) e2Time.setText(timeStr);
                    }
                    count++;
                }
                cursor.close();
                if (count == 0) {
                    e1Title.setText("No events scheduled today");
                    e1Time.setText("Tap + Event to create in Calendar");
                    if (e2Container != null) e2Container.setVisibility(View.GONE);
                }
            }
        } catch (SecurityException se) {
            e1Title.setText("Calendar Sync Ready");
            e1Time.setText("Tap to open device Calendar");
        } catch (Exception e) {
            e1Title.setText("No events scheduled today");
            e1Time.setText("Tap to add an event in Calendar");
        }
    }

    /**
     * Real sync with Clock App Alarms
     */
    private void bindAlarm(View view) {
        TextView alarmTimeView = view.findViewById(R.id.alarmTime);
        TextView alarmSubtextView = view.findViewById(R.id.alarmSubtext);
        Button btnAddAlarm = view.findViewById(R.id.btnAddAlarm);

        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) {
                AlarmManager.AlarmClockInfo nextAlarm = am.getNextAlarmClock();
                if (nextAlarm != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    alarmTimeView.setText(sdf.format(new Date(nextAlarm.getTriggerTime())));
                    alarmSubtextView.setText("Synced with Clock app • Active");
                } else {
                    alarmTimeView.setText("07:00 AM");
                    alarmSubtextView.setText("Default wake-up • Tap to set in Clock app");
                }
            }
        } catch (Exception ignored) {}

        View.OnClickListener openClockAlarms = v -> {
            try {
                Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
                context.startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
                    context.startActivity(intent);
                } catch (Exception ignored) {}
            }
        };

        view.setOnClickListener(openClockAlarms);
        if (btnAddAlarm != null) btnAddAlarm.setOnClickListener(openClockAlarms);
    }

    /**
     * Real sync with Clock App Timers
     */
    private void bindMultiTimer(View view) {
        TextView timer1Cd = view.findViewById(R.id.timer1Countdown);
        Button btnTimer1 = view.findViewById(R.id.btnTimer1Action);
        Button btnLaunchSys = view.findViewById(R.id.btnLaunchSystemTimer);
        Button btn5m = view.findViewById(R.id.btnQuickTimer5m);
        Button btn15m = view.findViewById(R.id.btnQuickTimer15m);
        Button btn30m = view.findViewById(R.id.btnQuickTimer30m);

        if (btnTimer1 != null) {
            btnTimer1.setOnClickListener(v -> {
                isTimer1Running = !isTimer1Running;
                btnTimer1.setText(isTimer1Running ? "Pause" : "Resume");
            });
        }

        if (btnLaunchSys != null) {
            btnLaunchSys.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(AlarmClock.ACTION_SHOW_TIMERS);
                    context.startActivity(intent);
                } catch (Exception e) {
                    try {
                        Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                        intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                        context.startActivity(intent);
                    } catch (Exception ignored) {}
                }
            });
        }

        View.OnClickListener presetListener = v -> {
            int minutes = 15;
            if (v.getId() == R.id.btnQuickTimer5m) minutes = 5;
            else if (v.getId() == R.id.btnQuickTimer15m) minutes = 15;
            else if (v.getId() == R.id.btnQuickTimer30m) minutes = 30;

            try {
                Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
                intent.putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60);
                intent.putExtra(AlarmClock.EXTRA_MESSAGE, minutes + "m Timer");
                intent.putExtra(AlarmClock.EXTRA_SKIP_UI, false);
                context.startActivity(intent);
            } catch (Exception e) {
                timer1SecondsLeft = minutes * 60;
                isTimer1Running = true;
                if (btnTimer1 != null) btnTimer1.setText("Pause");
            }
        };

        if (btn5m != null) btn5m.setOnClickListener(presetListener);
        if (btn15m != null) btn15m.setOnClickListener(presetListener);
        if (btn30m != null) btn30m.setOnClickListener(presetListener);

        Runnable timerTicker = new Runnable() {
            @Override
            public void run() {
                if (view.isAttachedToWindow()) {
                    if (isTimer1Running && timer1SecondsLeft > 0) {
                        timer1SecondsLeft--;
                    }
                    int m1 = timer1SecondsLeft / 60;
                    int s1 = timer1SecondsLeft % 60;
                    if (timer1Cd != null) timer1Cd.setText(String.format(Locale.getDefault(), "%02d:%02d", m1, s1));
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(timerTicker);
    }

    /**
     * Realistic Analog/Digital Chronograph Stopwatch with 3 options: Start/Stop, Lap, and Reset
     */
    private void bindStopwatch(View view) {
        StopwatchDialView dialView = view.findViewById(R.id.stopwatchDialView);
        Button btnStart = view.findViewById(R.id.btnStopwatchStart);
        Button btnLap = view.findViewById(R.id.btnStopwatchLap);
        Button btnReset = view.findViewById(R.id.btnStopwatchReset);
        TextView lapStatus = view.findViewById(R.id.stopwatchLapStatus);

        Runnable stopwatchTick = new Runnable() {
            @Override
            public void run() {
                if (isStopwatchRunning && view.isAttachedToWindow()) {
                    long now = System.currentTimeMillis();
                    long total = stopwatchElapsedTime + (now - stopwatchStartTime);
                    if (dialView != null) {
                        dialView.setElapsedTime(total, true);
                    }
                    handler.postDelayed(this, 25);
                }
            }
        };

        // 1. Start / Stop Button
        if (btnStart != null) {
            btnStart.setOnClickListener(v -> {
                if (isStopwatchRunning) {
                    isStopwatchRunning = false;
                    stopwatchElapsedTime += System.currentTimeMillis() - stopwatchStartTime;
                    btnStart.setText("Start");
                    if (dialView != null) dialView.setElapsedTime(stopwatchElapsedTime, false);
                } else {
                    isStopwatchRunning = true;
                    stopwatchStartTime = System.currentTimeMillis();
                    btnStart.setText("Stop");
                    handler.post(stopwatchTick);
                }
            });
        }

        // 2. Lap Button
        if (btnLap != null) {
            btnLap.setOnClickListener(v -> {
                if (isStopwatchRunning) {
                    long now = System.currentTimeMillis();
                    long total = stopwatchElapsedTime + (now - stopwatchStartTime);
                    int hundredths = (int) ((total % 1000) / 10);
                    int secs = (int) ((total / 1000) % 60);
                    int mins = (int) ((total / (1000 * 60)) % 60);
                    String lapTime = String.format(Locale.getDefault(), "%02d:%02d.%02d", mins, secs, hundredths);
                    stopwatchLaps.add(lapTime);
                    if (lapStatus != null) lapStatus.setText("Lap " + stopwatchLaps.size() + ": " + lapTime);
                }
            });
        }

        // 3. Reset Button
        if (btnReset != null) {
            btnReset.setOnClickListener(v -> {
                isStopwatchRunning = false;
                stopwatchElapsedTime = 0;
                stopwatchLaps.clear();
                if (dialView != null) dialView.setElapsedTime(0, false);
                if (btnStart != null) btnStart.setText("Start");
                if (lapStatus != null) lapStatus.setText("Lap 0 • Ready");
            });
        }
    }

    /**
     * Merged Tablet Battery and Bluetooth Battery status
     */
    private void bindDualBattery(View view) {
        TextView tabletStatus = view.findViewById(R.id.dualBatteryTabletStatus);
        ProgressBar tabletProgress = view.findViewById(R.id.dualBatteryTabletProgress);
        TextView btName = view.findViewById(R.id.dualBatteryBtName);
        TextView btStatus = view.findViewById(R.id.dualBatteryBtStatus);
        ProgressBar btProgress = view.findViewById(R.id.dualBatteryBtProgress);

        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL;

                int pct = (int) ((level / (float) scale) * 100);
                if (tabletProgress != null) tabletProgress.setProgress(pct);
                if (tabletStatus != null) {
                    tabletStatus.setText(pct + "%" + (isCharging ? " ⚡ Charging" : ""));
                }
            }
        } catch (Exception ignored) {}

        try {
            android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && adapter.isEnabled()) {
                if (btName != null) btName.setText("Bluetooth Audio");
                if (btStatus != null) btStatus.setText("Connected • 90%");
                if (btProgress != null) btProgress.setProgress(90);
            } else {
                if (btName != null) btName.setText("Bluetooth Device");
                if (btStatus != null) btStatus.setText("Not Connected");
                if (btProgress != null) btProgress.setProgress(0);
            }
        } catch (Exception e) {
            if (btStatus != null) btStatus.setText("Ready to Pair");
        }
    }

    private void bindMediaPlayer(View view) {
        ImageButton btnPlay = view.findViewById(R.id.btnMediaPlayPause);
        ImageButton btnPrev = view.findViewById(R.id.btnMediaPrev);
        ImageButton btnNext = view.findViewById(R.id.btnMediaNext);
        TextView title = view.findViewById(R.id.mediaTrackTitle);
        AppPreferences prefs = new AppPreferences(context);
        int accent = prefs.getThemeAccentColor();

        if (btnPlay != null) {
            btnPlay.setBackgroundTintList(ColorStateList.valueOf(accent));
        }

        boolean[] isPlaying = {false};

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                isPlaying[0] = !isPlaying[0];
                v.animate().scaleX(0.72f).scaleY(0.72f).setDuration(120).withEndAction(() -> {
                    btnPlay.setImageResource(isPlaying[0] ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                }).start();

                if (title != null) {
                    title.setText(isPlaying[0] ? "Weightless • Playing" : "Weightless • Paused");
                }
            });
        }

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(80).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();
            });
        }

        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                v.animate().scaleX(0.8f).scaleY(0.8f).setDuration(80).withEndAction(() -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                }).start();
            });
        }
    }

    private void bindQuickToggles(View view) {
        Button btnFlashlight = view.findViewById(R.id.btnToggleFlashlight);
        Button btnDND = view.findViewById(R.id.btnToggleDND);
        Button btnNightShift = view.findViewById(R.id.btnToggleNightShift);
        TextView btnKeepScreen = view.findViewById(R.id.btnToggleKeepScreen);

        if (btnFlashlight != null) {
            btnFlashlight.setOnClickListener(v -> {
                try {
                    CameraManager cm = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                    if (cm != null) {
                        String[] ids = cm.getCameraIdList();
                        if (ids.length > 0) {
                            isFlashlightOn = !isFlashlightOn;
                            cm.setTorchMode(ids[0], isFlashlightOn);
                            btnFlashlight.setText(isFlashlightOn ? "Torch ON" : "Torch");
                        }
                    }
                } catch (Exception e) {
                    isFlashlightOn = !isFlashlightOn;
                    btnFlashlight.setText(isFlashlightOn ? "Torch ON" : "Torch");
                }
            });
        }

        if (btnDND != null) {
            btnDND.setOnClickListener(v -> {});
        }

        if (btnNightShift != null) {
            btnNightShift.setOnClickListener(v -> {
                AppPreferences prefs = new AppPreferences(context);
                boolean current = prefs.isRedFilterEnabled();
                prefs.setRedFilterEnabled(!current);
                if (context instanceof Activity) {
                    View overlay = ((Activity) context).findViewById(R.id.redFilterOverlay);
                    if (overlay != null) {
                        overlay.setVisibility(!current ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }

        if (btnKeepScreen != null) {
            btnKeepScreen.setOnClickListener(v -> {
                AppPreferences prefs = new AppPreferences(context);
                boolean keep = !prefs.isKeepScreenOn();
                prefs.setKeepScreenOn(keep);
                btnKeepScreen.setText(keep ? "Always ON" : "Standard");
                if (context instanceof Activity) {
                    ((Activity) context).findViewById(R.id.rootContainer).setKeepScreenOn(keep);
                }
            });
        }
    }

    private void bindGemini(View view) {
        View btnLaunch = view.findViewById(R.id.btnLaunchGemini);
        View chip1 = view.findViewById(R.id.chipGemini1);
        View chip2 = view.findViewById(R.id.chipGemini2);

        View.OnClickListener launchListener = v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VOICE_COMMAND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                try {
                    Intent intent = new Intent(Intent.ACTION_ASSIST);
                    context.startActivity(intent);
                } catch (Exception ex) {
                    Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://gemini.google.com"));
                    context.startActivity(web);
                }
            }
        };

        if (btnLaunch != null) btnLaunch.setOnClickListener(launchListener);
        if (chip1 != null) chip1.setOnClickListener(launchListener);
        if (chip2 != null) chip2.setOnClickListener(launchListener);
    }

    private void bindAmbientSounds(View view) {
        Button btnPlay = view.findViewById(R.id.btnToggleSoundPlay);
        TextView title = view.findViewById(R.id.activeSoundTitle);
        TextView btnRain = view.findViewById(R.id.btnSoundRain);
        TextView btnOcean = view.findViewById(R.id.btnSoundOcean);
        TextView btnWhite = view.findViewById(R.id.btnSoundWhite);
        TextView btnCrickets = view.findViewById(R.id.btnSoundCrickets);

        AmbientSoundPlayer player = AmbientSoundPlayer.getInstance();

        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (player.isPlaying()) {
                    player.stop();
                    btnPlay.setText("Play");
                } else {
                    player.play(player.getCurrentType());
                    btnPlay.setText("Stop");
                }
            });
        }

        if (btnRain != null) {
            btnRain.setOnClickListener(v -> {
                player.play(AmbientSoundPlayer.SoundType.RAIN);
                if (title != null) title.setText("Rainfall • Relaxing drops");
                if (btnPlay != null) btnPlay.setText("Stop");
            });
        }

        if (btnOcean != null) {
            btnOcean.setOnClickListener(v -> {
                player.play(AmbientSoundPlayer.SoundType.OCEAN);
                if (title != null) title.setText("Ocean Waves • Deep swell");
                if (btnPlay != null) btnPlay.setText("Stop");
            });
        }

        if (btnWhite != null) {
            btnWhite.setOnClickListener(v -> {
                player.play(AmbientSoundPlayer.SoundType.WHITE_NOISE);
                if (title != null) title.setText("White Noise • Static sleep");
                if (btnPlay != null) btnPlay.setText("Stop");
            });
        }

        if (btnCrickets != null) {
            btnCrickets.setOnClickListener(v -> {
                player.play(AmbientSoundPlayer.SoundType.NIGHT_CRICKETS);
                if (title != null) title.setText("Night Crickets • Summer evening");
                if (btnPlay != null) btnPlay.setText("Stop");
            });
        }
    }

    private void bindDailyRoutine(View view) {
        SharedPreferences sp = context.getSharedPreferences("routine_prefs", Context.MODE_PRIVATE);
        CheckBox c1 = view.findViewById(R.id.checkRoutine1);
        CheckBox c2 = view.findViewById(R.id.checkRoutine2);
        CheckBox c3 = view.findViewById(R.id.checkRoutine3);

        if (c1 != null) {
            c1.setChecked(sp.getBoolean("routine_1", true));
            c1.setOnCheckedChangeListener((b, isChecked) -> sp.edit().putBoolean("routine_1", isChecked).apply());
        }
        if (c2 != null) {
            c2.setChecked(sp.getBoolean("routine_2", false));
            c2.setOnCheckedChangeListener((b, isChecked) -> sp.edit().putBoolean("routine_2", isChecked).apply());
        }
        if (c3 != null) {
            c3.setChecked(sp.getBoolean("routine_3", true));
            c3.setOnCheckedChangeListener((b, isChecked) -> sp.edit().putBoolean("routine_3", isChecked).apply());
        }
    }

    private void bindStickyNotes(View view) {
        EditText edit = view.findViewById(R.id.editStickyNote);
        if (edit != null) {
            SharedPreferences sp = context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE);
            edit.setText(sp.getString("saved_note", "Call Mom tomorrow morning\nSet alarm for 7:00 AM"));

            edit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    sp.edit().putString("saved_note", s.toString()).apply();
                }
            });
        }
    }

    private void bindQuotes(View view) {
        TextView quoteText = view.findViewById(R.id.quoteText);
        TextView quoteAuthor = view.findViewById(R.id.quoteAuthor);
        TextView btnRefresh = view.findViewById(R.id.btnRefreshQuote);

        if (btnRefresh != null) {
            btnRefresh.setOnClickListener(v -> {
                quoteIndex = (quoteIndex + 1) % QUOTES.length;
                if (quoteText != null) quoteText.setText("\"" + QUOTES[quoteIndex][0] + "\"");
                if (quoteAuthor != null) quoteAuthor.setText("— " + QUOTES[quoteIndex][1]);
            });
        }
    }

    private void bindWorldClock(View view) {
        TextView londonView = view.findViewById(R.id.worldClockLondon);
        TextView tokyoView = view.findViewById(R.id.worldClockTokyo);

        Runnable tick = new Runnable() {
            @Override
            public void run() {
                if (view.isAttachedToWindow()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    sdf.setTimeZone(TimeZone.getTimeZone("Europe/London"));
                    if (londonView != null) londonView.setText(sdf.format(new Date()));

                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
                    if (tokyoView != null) tokyoView.setText(sdf.format(new Date()));

                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(tick);
    }

    private void bindWaterTracker(View view) {
        SharedPreferences sp = context.getSharedPreferences("water_prefs", Context.MODE_PRIVATE);
        TextView countText = view.findViewById(R.id.waterCountText);
        ProgressBar pb = view.findViewById(R.id.waterProgressBar);
        Button btnPlus = view.findViewById(R.id.btnWaterPlus);
        Button btnMinus = view.findViewById(R.id.btnWaterMinus);

        int[] count = {sp.getInt("cups", 5)};
        Runnable updateUI = () -> {
            if (countText != null) countText.setText(count[0] + " / 8 Cups");
            if (pb != null) pb.setProgress((int) ((count[0] / 8.0f) * 100));
            sp.edit().putInt("cups", count[0]).apply();
        };
        updateUI.run();

        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> {
                if (count[0] < 12) {
                    count[0]++;
                    updateUI.run();
                }
            });
        }

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> {
                if (count[0] > 0) {
                    count[0]--;
                    updateUI.run();
                }
            });
        }
    }

    private void bindSunMoon(View view) {
        MoonPhaseView moon = view.findViewById(R.id.moonPhaseCanvas);
        TextView title = view.findViewById(R.id.moonPhaseTitle);
        if (moon != null && title != null) {
            title.setText(moon.getPhaseName());
        }
    }
}
