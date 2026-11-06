package com.example.nafa;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Img {
    private Img() {}

    public static final int FULL_PX = 1080, FULL_MAX = 600_000;
    public static final int THUMB_PX = 360, THUMB_MAX = 60_000;
    public static final int AVATAR_PX = 320, AVATAR_MAX = 60_000;

    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) Math.min(Runtime.getRuntime().maxMemory() / 8, 48L * 1024 * 1024)) {
                @Override protected int sizeOf(String k, Bitmap b) { return b.getByteCount(); }
            };

    private static String key(String b64, int px) {
        return b64.length() + ":" + b64.hashCode() + ":" + px;
    }

    public static Bitmap decode(String b64, int reqPx) {
        if (b64 == null || b64.isEmpty()) return null;
        String k = key(b64, reqPx);
        Bitmap cached = CACHE.get(k);
        if (cached != null) return cached;
        try {
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o);
            int s = 1;
            while (reqPx > 0 && Math.min(o.outWidth, o.outHeight) / (s * 2) >= reqPx) s *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = s;
            Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, o2);
            if (b != null) CACHE.put(k, b);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    public static void load(ImageView iv, String b64, int reqPx) {
        if (b64 == null || b64.isEmpty()) return;
        String k = key(b64, reqPx);
        iv.setTag(R.id.nafa_key, k);
        Bitmap cached = CACHE.get(k);
        if (cached != null) { iv.setImageBitmap(cached); return; }
        POOL.execute(() -> {
            Bitmap b = decode(b64, reqPx);
            MAIN.post(() -> {
                if (b != null && k.equals(iv.getTag(R.id.nafa_key))) {
                    iv.setImageBitmap(b);
                }
            });
        });
    }

    public interface Result { void done(String base64); }

    public static void encodeAsync(Activity a, Uri uri, int maxPx, int maxLen, Result r) {
        POOL.execute(() -> {
            String out = null;
            try { out = encode(a, uri, maxPx, maxLen); } catch (Throwable ignored) {}
            final String res = out;
            a.runOnUiThread(() -> r.done(res));
        });
    }

    public static String encode(Context c, Uri uri, int maxPx, int maxLen) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, o);
        }
        if (o.outWidth <= 0) return null;
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= maxPx) s *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = s;
        Bitmap b;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            b = BitmapFactory.decodeStream(in, null, o2);
        }
        if (b == null) return null;

        int rotation = 0;
        try (InputStream in = c.getContentResolver().openInputStream(uri)) {
            if (in != null) {
                int ori = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (ori == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (ori == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (Exception ignored) {}

        float scale = Math.min(1f, (float) maxPx / Math.max(b.getWidth(), b.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix mx = new Matrix();
            mx.postScale(scale, scale);
            mx.postRotate(rotation);
            Bitmap t = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), mx, true);
            if (t != b) b.recycle();
            b = t;
        }
        return compress(b, maxLen);
    }

    public static String compress(Bitmap b, int maxLen) {
        int q = 82;
        String out;
        do {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, q, os);
            out = Base64.encodeToString(os.toByteArray(), Base64.NO_WRAP);
            q -= 8;
        } while (out.length() > maxLen && q >= 30);
        if (out.length() > maxLen) {
            Bitmap half = Bitmap.createScaledBitmap(b, Math.max(1, b.getWidth() * 3 / 4), Math.max(1, b.getHeight() * 3 / 4), true);
            return compress(half, maxLen);
        }
        return out;
    }

    public static String thumb(String fullB64) {
        Bitmap b = decode(fullB64, THUMB_PX);
        if (b == null) return "";
        float scale = Math.min(1f, (float) THUMB_PX / Math.max(b.getWidth(), b.getHeight()));
        Bitmap t = scale < 1f ? Bitmap.createScaledBitmap(b, Math.round(b.getWidth() * scale), Math.round(b.getHeight() * scale), true) : b;
        return compress(t, THUMB_MAX);
    }

    public static void thumbAsync(Activity a, String full, Result r) {
        POOL.execute(() -> {
            String t = null;
            try { t = thumb(full); } catch (Throwable ignored) {}
            final String res = t;
            a.runOnUiThread(() -> r.done(res));
        });
    }

    public interface Pair { void done(String full, String thumb); }

    public static void encodeWithThumb(Activity a, Uri uri, Pair r) {
        POOL.execute(() -> {
            String full = null, th = null;
            try {
                full = encode(a, uri, FULL_PX, FULL_MAX);
                if (full != null) th = thumb(full);
            } catch (Throwable ignored) {}
            final String f = full, t = th;
            a.runOnUiThread(() -> r.done(f, t));
        });
    }
}
