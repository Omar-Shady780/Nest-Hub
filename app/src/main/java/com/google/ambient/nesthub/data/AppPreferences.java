package com.google.ambient.nesthub.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class AppPreferences {
    private static final String PREF_NAME = "nest_hub_prefs";
    private static final String KEY_GRID_SIZE = "grid_size"; // 2 for 2x2, 3 for 3x3, 4 for custom
    private static final String KEY_GRID_MODE = "grid_mode"; // "2x2", "3x3", "CUSTOM"
    private static final String KEY_CUSTOM_ROWS = "custom_rows";
    private static final String KEY_CUSTOM_COLS = "custom_cols";
    private static final String KEY_CUSTOM_SLOT_COUNT = "custom_slot_count";
    private static final String KEY_SPAN_COL_PREFIX = "span_col_";
    private static final String KEY_SPAN_ROW_PREFIX = "span_row_";
    private static final String KEY_APPWIDGET_ID_PREFIX = "appwidget_id_";
    private static final String KEY_TUTORIAL_COMPLETED = "tutorial_completed";
    private static final String KEY_THEME_ACCENT_COLOR = "theme_accent_color";
    private static final String KEY_WIDGET_PREFIX = "widget_slot_";
    private static final String KEY_BG_THEME = "bg_theme";
    private static final String KEY_CUSTOM_BG_URI = "custom_bg_uri";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    private static final String KEY_RED_FILTER = "red_filter";
    private static final String KEY_USE_24_HOUR = "use_24_hour";
    private static final String KEY_STANDBY_PURE_BLACK = "standby_pure_black";
    private static final String KEY_BRIGHTNESS = "screen_brightness";
    private static final String KEY_MUSIC_STOPPED_TIMESTAMP = "music_stopped_timestamp";
    private static final String KEY_USE_CELSIUS = "use_celsius";

    private final SharedPreferences prefs;

    public AppPreferences(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public int getGridSize() {
        return prefs.getInt(KEY_GRID_SIZE, 2); // Default 2x2
    }

    public void setGridSize(int size) {
        prefs.edit().putInt(KEY_GRID_SIZE, size).apply();
    }

    public WidgetType getWidgetForSlot(int slotIndex, WidgetType defaultType) {
        String name = prefs.getString(KEY_WIDGET_PREFIX + slotIndex, defaultType.name());
        try {
            if ("DEVICE_BATTERY".equals(name) || "BLUETOOTH_BATTERY".equals(name)) {
                return WidgetType.DUAL_BATTERY;
            }
            if ("CALCULATOR".equals(name) || "SYSTEM_MONITOR".equals(name)) {
                return defaultType;
            }
            return WidgetType.valueOf(name);
        } catch (Exception e) {
            return defaultType;
        }
    }

    public void setWidgetForSlot(int slotIndex, WidgetType type) {
        prefs.edit().putString(KEY_WIDGET_PREFIX + slotIndex, type.name()).apply();
    }

    public List<WidgetType> getWidgetsForGrid(int size) {
        int totalSlots = (size == 4) ? getCustomSlotCount() : (size * size);
        List<WidgetType> list = new ArrayList<>();
        WidgetType[] defaults;
        if (size == 2) {
            defaults = new WidgetType[]{
                    WidgetType.DIGITAL_CLOCK,
                    WidgetType.WEATHER,
                    WidgetType.CALENDAR,
                    WidgetType.DUAL_BATTERY
            };
        } else if (size == 3) {
            defaults = new WidgetType[]{
                    WidgetType.DIGITAL_CLOCK,
                    WidgetType.WEATHER,
                    WidgetType.CALENDAR,
                    WidgetType.ALARM,
                    WidgetType.MULTI_TIMER,
                    WidgetType.DUAL_BATTERY,
                    WidgetType.STOPWATCH,
                    WidgetType.QUICK_TOGGLES,
                    WidgetType.GEMINI_ASSISTANT
            };
        } else {
            defaults = new WidgetType[]{
                    WidgetType.DIGITAL_CLOCK,
                    WidgetType.WEATHER,
                    WidgetType.CALENDAR,
                    WidgetType.DUAL_BATTERY,
                    WidgetType.STOPWATCH,
                    WidgetType.MULTI_TIMER
            };
        }

        for (int i = 0; i < totalSlots; i++) {
            WidgetType def = (i < defaults.length) ? defaults[i] : WidgetType.EMPTY;
            list.add(getWidgetForSlot(i, def));
        }
        return list;
    }

    public BackgroundTheme.Type getBackgroundTheme() {
        String name = prefs.getString(KEY_BG_THEME, BackgroundTheme.Type.GRADIENT_1.name());
        try {
            return BackgroundTheme.Type.valueOf(name);
        } catch (Exception e) {
            return BackgroundTheme.Type.GRADIENT_1;
        }
    }

    public void setBackgroundTheme(BackgroundTheme.Type type) {
        prefs.edit().putString(KEY_BG_THEME, type.name()).apply();
    }

    public String getCustomBgUri() {
        return prefs.getString(KEY_CUSTOM_BG_URI, null);
    }

    public void setCustomBgUri(String uriString) {
        prefs.edit().putString(KEY_CUSTOM_BG_URI, uriString).apply();
    }

    public boolean isKeepScreenOn() {
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, true);
    }

    public void setKeepScreenOn(boolean keepOn) {
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply();
    }

    public boolean isRedFilterEnabled() {
        return prefs.getBoolean(KEY_RED_FILTER, false);
    }

    public void setRedFilterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_RED_FILTER, enabled).apply();
    }

    public boolean is24HourFormat() {
        return prefs.getBoolean(KEY_USE_24_HOUR, false);
    }

    public void set24HourFormat(boolean is24) {
        prefs.edit().putBoolean(KEY_USE_24_HOUR, is24).apply();
    }

    public boolean isStandbyPureBlack() {
        return prefs.getBoolean(KEY_STANDBY_PURE_BLACK, true);
    }

    public void setStandbyPureBlack(boolean pureBlack) {
        prefs.edit().putBoolean(KEY_STANDBY_PURE_BLACK, pureBlack).apply();
    }

    public boolean isTutorialCompleted() {
        return prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false);
    }

    public void setTutorialCompleted(boolean completed) {
        prefs.edit().putBoolean(KEY_TUTORIAL_COMPLETED, completed).apply();
    }

    public String getGridMode() {
        return prefs.getString(KEY_GRID_MODE, "2x2");
    }

    public void setGridMode(String mode) {
        prefs.edit().putString(KEY_GRID_MODE, mode).apply();
    }

    public int getCustomRows() {
        return prefs.getInt(KEY_CUSTOM_ROWS, 2);
    }

    public void setCustomRows(int rows) {
        prefs.edit().putInt(KEY_CUSTOM_ROWS, Math.max(1, Math.min(4, rows))).apply();
    }

    public int getCustomCols() {
        return prefs.getInt(KEY_CUSTOM_COLS, 3);
    }

    public void setCustomCols(int cols) {
        prefs.edit().putInt(KEY_CUSTOM_COLS, Math.max(1, Math.min(4, cols))).apply();
    }

    public int getCustomSlotCount() {
        return prefs.getInt(KEY_CUSTOM_SLOT_COUNT, 6);
    }

    public void setCustomSlotCount(int count) {
        prefs.edit().putInt(KEY_CUSTOM_SLOT_COUNT, Math.max(1, Math.min(16, count))).apply();
    }

    public int getColSpan(int slotIndex) {
        return prefs.getInt(KEY_SPAN_COL_PREFIX + slotIndex, 1);
    }

    public void setColSpan(int slotIndex, int span) {
        prefs.edit().putInt(KEY_SPAN_COL_PREFIX + slotIndex, Math.max(1, Math.min(3, span))).apply();
    }

    public int getRowSpan(int slotIndex) {
        return prefs.getInt(KEY_SPAN_ROW_PREFIX + slotIndex, 1);
    }

    public void setRowSpan(int slotIndex, int span) {
        prefs.edit().putInt(KEY_SPAN_ROW_PREFIX + slotIndex, Math.max(1, Math.min(3, span))).apply();
    }

    public int getAppWidgetId(int slotIndex) {
        return prefs.getInt(KEY_APPWIDGET_ID_PREFIX + slotIndex, -1);
    }

    public void setAppWidgetId(int slotIndex, int id) {
        prefs.edit().putInt(KEY_APPWIDGET_ID_PREFIX + slotIndex, id).apply();
    }

    public int getThemeAccentColor() {
        return prefs.getInt(KEY_THEME_ACCENT_COLOR, 0xFF4D88FF);
    }

    public void setThemeAccentColor(int color) {
        prefs.edit().putInt(KEY_THEME_ACCENT_COLOR, color).apply();
    }

    public float getScreenBrightness() {
        return prefs.getFloat(KEY_BRIGHTNESS, 0.6f);
    }

    public void setScreenBrightness(float brightness) {
        prefs.edit().putFloat(KEY_BRIGHTNESS, brightness).apply();
    }

    public long getMusicStoppedTimestamp() {
        return prefs.getLong(KEY_MUSIC_STOPPED_TIMESTAMP, 0L);
    }

    public void setMusicStoppedTimestamp(long timestamp) {
        prefs.edit().putLong(KEY_MUSIC_STOPPED_TIMESTAMP, timestamp).apply();
    }

    public boolean isCelsius() {
        return prefs.getBoolean(KEY_USE_CELSIUS, false);
    }

    public void setCelsius(boolean celsius) {
        prefs.edit().putBoolean(KEY_USE_CELSIUS, celsius).apply();
    }
}

