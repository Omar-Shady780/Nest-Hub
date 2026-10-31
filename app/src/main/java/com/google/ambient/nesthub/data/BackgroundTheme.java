package com.google.ambient.nesthub.data;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

import java.util.Calendar;

public class BackgroundTheme {

    public enum Type {
        GRADIENT_1("Aurora Night", new int[]{0xFF0D1B2A, 0xFF1B4965, 0xFF62B6CB}),
        GRADIENT_2("Sunset Horizon", new int[]{0xFF2B0938, 0xFF6C1D45, 0xFFD45D55, 0xFFEB8C00}),
        GRADIENT_3("Emerald Forest", new int[]{0xFF06281E, 0xFF0E4429, 0xFF006D44}),
        GRADIENT_4("Cosmic Nebula", new int[]{0xFF16002C, 0xFF3B185F, 0xFFA12568, 0xFFFEC260}),
        GRADIENT_5("Deep Ocean", new int[]{0xFF001220, 0xFF003B64, 0xFF0077B6}),
        GRADIENT_6("Rose Twilight", new int[]{0xFF2D112C, 0xFF53004B, 0xFF903749, 0xFFE84A5F}),
        GRADIENT_7("Solar Flare", new int[]{0xFF330800, 0xFF701400, 0xFFD9381E, 0xFFFA7921}),
        GRADIENT_8("Nordic Frost", new int[]{0xFF101D28, 0xFF1F384C, 0xFF456990, 0xFF8FB9A8}),
        GRADIENT_9("Cyber Neon", new int[]{0xFF0F051D, 0xFF2C0B4D, 0xFF5D0E6B, 0xFF9E0059}),
        GRADIENT_10("Obsidian Dusk", new int[]{0xFF141419, 0xFF23252E, 0xFF3A3F4D}),

        SOLID_1("OLED Pure Black", 0xFF000000),
        SOLID_2("Material Dark", 0xFF121212),
        SOLID_3("Slate Navy", 0xFF0F172A),
        SOLID_4("Deep Charcoal", 0xFF1E1E24),
        SOLID_5("Espresso Coffee", 0xFF1C140E),
        SOLID_6("Midnight Teal", 0xFF072428),
        SOLID_7("Forest Pine", 0xFF0D2319),
        SOLID_8("Rich Plum", 0xFF240E26),
        SOLID_9("Midnight Indigo", 0xFF141738),
        SOLID_10("Steel Blue", 0xFF14213D),

        DEVICE_WALLPAPER("Device Wallpaper", 0),
        CUSTOM_IMAGE("Imported Photo", 0),
        DYNAMIC_TIME("Dynamic Time/Weather", 0);

        private final String displayName;
        private final int[] colors;
        private final int solidColor;

        Type(String displayName, int[] colors) {
            this.displayName = displayName;
            this.colors = colors;
            this.solidColor = 0;
        }

        Type(String displayName, int solidColor) {
            this.displayName = displayName;
            this.colors = null;
            this.solidColor = solidColor;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isGradient() {
            return colors != null && colors.length > 0;
        }

        public boolean isSolid() {
            return colors == null && solidColor != 0;
        }

        public int[] getColors() {
            return colors;
        }

        public int getSolidColor() {
            return solidColor;
        }
    }

    public static GradientDrawable createGradient(int[] colors) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        gd.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return gd;
    }

    public static GradientDrawable getDynamicTimeGradient() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        int[] colors;
        if (hour >= 5 && hour < 8) {
            // Dawn / Sunrise
            colors = new int[]{0xFF1A1A2E, 0xFF4B2354, 0xFFB33939, 0xFFE58E26};
        } else if (hour >= 8 && hour < 17) {
            // Daytime
            colors = new int[]{0xFF0C2461, 0xFF1E3799, 0xFF4A69BD, 0xFF6A89CC};
        } else if (hour >= 17 && hour < 20) {
            // Sunset / Dusk
            colors = new int[]{0xFF2C0B38, 0xFF6B114D, 0xFFB03A2E, 0xFFD35400};
        } else {
            // Night
            colors = new int[]{0xFF05050A, 0xFF0D1321, 0xFF1D2D44};
        }
        return createGradient(colors);
    }

    public static int getAccentColorForTheme(Type type, android.graphics.drawable.Drawable customDrawable) {
        if (type == null) return 0xFF4D88FF;
        if (type.isGradient()) {
            int[] c = type.getColors();
            if (c != null && c.length > 0) {
                return c[c.length - 1];
            }
        }
        if (type.isSolid()) {
            int col = type.getSolidColor();
            float[] hsv = new float[3];
            Color.colorToHSV(col, hsv);
            hsv[0] = (hsv[0] + 180) % 360;
            hsv[1] = 0.75f;
            hsv[2] = 0.95f;
            return Color.HSVToColor(hsv);
        }
        if (type == Type.DYNAMIC_TIME) {
            int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            if (hour >= 5 && hour < 8) return 0xFFE58E26;
            if (hour >= 8 && hour < 17) return 0xFF4A69BD;
            if (hour >= 17 && hour < 20) return 0xFFD35400;
            return 0xFF62B6CB;
        }
        if (customDrawable instanceof android.graphics.drawable.BitmapDrawable) {
            try {
                android.graphics.Bitmap bmp = ((android.graphics.drawable.BitmapDrawable) customDrawable).getBitmap();
                if (bmp != null && !bmp.isRecycled()) {
                    int pixel = bmp.getPixel(bmp.getWidth() / 2, bmp.getHeight() / 2);
                    float[] hsv = new float[3];
                    Color.colorToHSV(pixel, hsv);
                    hsv[1] = Math.max(0.6f, hsv[1]);
                    hsv[2] = Math.max(0.85f, hsv[2]);
                    return Color.HSVToColor(hsv);
                }
            } catch (Exception ignored) {}
        }
        return 0xFF4D88FF;
    }
}

