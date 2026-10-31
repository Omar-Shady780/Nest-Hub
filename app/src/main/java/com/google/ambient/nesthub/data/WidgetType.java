package com.google.ambient.nesthub.data;

public enum WidgetType {
    DIGITAL_CLOCK("Digital Clock", "Modern bedside clock with date & seconds"),
    ANALOG_CLOCK("Analog Clock", "Nest-style minimal analog watchface"),
    WEATHER("Weather & Forecast", "Current weather, temperature and forecast"),
    CALENDAR("Calendar & Agenda", "Upcoming events & schedule sync"),
    ALARM("Alarms Sync", "Next scheduled alarm & system alarm shortcuts"),
    MULTI_TIMER("Multi-Timer", "Concurrent multi-timers with alerts"),
    STOPWATCH("Stopwatch", "Interactive stopwatch with lap tracking"),
    DUAL_BATTERY("Device & Bluetooth Battery", "Tablet charge % and connected Bluetooth accessories"),
    MEDIA_PLAYER("Media Player", "Now playing & playback controls"),
    QUICK_TOGGLES("Quick Controls", "Flashlight, DND, brightness & shortcuts"),
    GEMINI_ASSISTANT("Google Gemini", "One-tap AI voice assistant launcher"),
    AMBIENT_SOUNDS("Sleep Sounds", "White noise, rain, ocean waves & crickets"),
    DAILY_ROUTINE("Bedside Checklist", "Evening and morning routine tracker"),
    STICKY_NOTES("Quick Notes", "Bedside scratchpad for midnight ideas"),
    DAILY_QUOTES("Daily Wisdom", "Inspirational morning and evening quotes"),
    WORLD_CLOCK("World Clocks", "Time in London, New York, Tokyo & more"),
    WATER_TRACKER("Hydration", "Daily water glasses counter & progress"),
    SUN_MOON("Sun & Moon Phase", "Dawn, sunset, golden hour & moon phase"),
    PHOTO_FRAME("Photo Frame", "Calming ambient bedside photography"),
    SYSTEM_APP_WIDGET("Home Screen Widget", "Add any Android home screen widget (Spotify, Google, etc.)"),
    EMPTY("Empty Slot", "Leave this spot blank or tap + to add a widget");

    private final String title;
    private final String description;

    WidgetType(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

