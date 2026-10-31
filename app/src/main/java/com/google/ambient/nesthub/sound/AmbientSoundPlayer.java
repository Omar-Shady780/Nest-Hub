package com.google.ambient.nesthub.sound;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;

import java.util.Random;

public class AmbientSoundPlayer {

    public enum SoundType {
        RAIN("Rainfall", "Gentle relaxing rain"),
        OCEAN("Ocean Waves", "Rhythmic ocean swell"),
        WHITE_NOISE("White Noise", "Calming static for sleep"),
        NIGHT_CRICKETS("Night Crickets", "Soothing summer night"),
        STREAM("Forest Stream", "Babbling brook");

        private final String title;
        private final String desc;

        SoundType(String title, String desc) {
            this.title = title;
            this.desc = desc;
        }

        public String getTitle() {
            return title;
        }

        public String getDesc() {
            return desc;
        }
    }

    private static AmbientSoundPlayer instance;
    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean isPlaying = false;
    private SoundType currentType = SoundType.RAIN;
    private float volume = 0.7f;
    private int sleepTimerMinutes = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable sleepTimerRunnable = new Runnable() {
        @Override
        public void run() {
            stop();
        }
    };

    public static synchronized AmbientSoundPlayer getInstance() {
        if (instance == null) {
            instance = new AmbientSoundPlayer();
        }
        return instance;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public SoundType getCurrentType() {
        return currentType;
    }

    public void setSleepTimer(int minutes) {
        this.sleepTimerMinutes = minutes;
        handler.removeCallbacks(sleepTimerRunnable);
        if (minutes > 0 && isPlaying) {
            handler.postDelayed(sleepTimerRunnable, minutes * 60L * 1000L);
        }
    }

    public int getSleepTimerMinutes() {
        return sleepTimerMinutes;
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0f, Math.min(1f, volume));
        if (audioTrack != null) {
            audioTrack.setVolume(this.volume);
        }
    }

    public float getVolume() {
        return volume;
    }

    public synchronized void play(SoundType type) {
        if (isPlaying && currentType == type) {
            return;
        }
        stop();
        currentType = type;
        isPlaying = true;

        if (sleepTimerMinutes > 0) {
            handler.postDelayed(sleepTimerRunnable, sleepTimerMinutes * 60L * 1000L);
        }

        audioThread = new Thread(() -> generateAudioLoop(type), "AmbientAudioSynth");
        audioThread.setPriority(Thread.MAX_PRIORITY);
        audioThread.start();
    }

    public synchronized void stop() {
        isPlaying = false;
        handler.removeCallbacks(sleepTimerRunnable);
        if (audioThread != null) {
            try {
                audioThread.join(300);
            } catch (InterruptedException ignored) {}
            audioThread = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
                audioTrack.release();
            } catch (Exception ignored) {}
            audioTrack = null;
        }
    }

    private void generateAudioLoop(SoundType type) {
        final int sampleRate = 22050;
        final int bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        audioTrack.setVolume(volume);
        audioTrack.play();

        short[] buffer = new short[bufferSize / 2];
        Random random = new Random();
        float b0 = 0, b1 = 0, b2 = 0; // Filter states
        double phase = 0;

        while (isPlaying) {
            for (int i = 0; i < buffer.length; i++) {
                float white = (random.nextFloat() * 2f - 1f);
                float sample = 0;

                switch (type) {
                    case RAIN:
                        // Pink noise with subtle high-freq drops
                        b0 = 0.99886f * b0 + white * 0.0555179f;
                        b1 = 0.99332f * b1 + white * 0.0750759f;
                        b2 = 0.96900f * b2 + white * 0.1538520f;
                        sample = (b0 + b1 + b2 + white * 0.5362f) * 0.15f;
                        if (random.nextInt(1000) == 0) {
                            sample += 0.4f * (random.nextFloat() - 0.5f);
                        }
                        break;

                    case OCEAN:
                        // Low frequency amplitude modulated noise
                        phase += 0.00015;
                        double waveMod = Math.pow(Math.sin(phase) * 0.5 + 0.5, 2.5);
                        b0 = 0.95f * b0 + white * 0.05f;
                        sample = (float) (b0 * waveMod * 0.8f);
                        break;

                    case WHITE_NOISE:
                        sample = white * 0.18f;
                        break;

                    case NIGHT_CRICKETS:
                        phase += 0.02;
                        double chirpMod = Math.sin(phase * 0.05) > 0.6 ? Math.sin(phase * 15.0) : 0;
                        sample = (float) (chirpMod * 0.12f + white * 0.02f);
                        break;

                    case STREAM:
                        b0 = 0.92f * b0 + white * 0.08f;
                        b1 = 0.85f * b1 + white * 0.15f;
                        sample = (b0 * 0.5f + b1 * 0.3f) * 0.35f;
                        break;
                }

                // Soft clip and scale to 16-bit
                sample = Math.max(-1f, Math.min(1f, sample));
                buffer[i] = (short) (sample * 32767);
            }
            audioTrack.write(buffer, 0, buffer.length);
        }
    }
}

