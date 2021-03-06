package me.ykrank.s1next.widget.span;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.ykrank.s1next.util.L;

/**
 * <xmp>
 * 符合intent-filter的<a></a> tag。或者<bilibili></bilibili> tag
 * </xmp>
 * <br>
 * <xmp>
 * eg:<a href="http://www.bilibili.com/video/av6591319/" >http://www.bilibili.com/video/av6591319/</a>
 * </xmp>
 * <br>
 * <xmp>
 * or <bilibili>http://www.bilibili.com/video/av6591319/</bilibili>
 * </xmp>
 * <br>
 * Created by ykrank on 2016/10/16 0016.
 */

public class BilibiliSpan implements CustomMovementMethod.URLSpanClick {
    /**
     * intent-filter.从官方APP AndroidManifest中提取
     */
    private static List<Pair<String, String>> HOST_FILTERS = new ArrayList<>();

    static {
        HOST_FILTERS.add(new Pair<>("space.bilibili.com", ".*"));
        HOST_FILTERS.add(new Pair<>("bilibili.kankanews.com", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.tv", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.cn", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.com", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.tv", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.cn", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.com", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.smgbb.cn", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("m.acg.tv", "/video/av.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.com", "/mobile/video/av.*"));
        HOST_FILTERS.add(new Pair<>("live.bilibili.com", "/live/*.html"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.com", "/bangumi/i/.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.com", "/mobile/bangumi/i/.*"));
        HOST_FILTERS.add(new Pair<>("bangumi.bilibili.com", "/anime/.*"));
        HOST_FILTERS.add(new Pair<>("bangumi.bilibili.com", "/anime/category/.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.com", "/sp/.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.com", "/sp/.*"));
        HOST_FILTERS.add(new Pair<>("bilibili.tv", "/sp/.*"));
        HOST_FILTERS.add(new Pair<>("www.bilibili.tv", "/sp/.*"));
    }

    /**
     * See android.text.HtmlToSpannedConverter#startA(android.text.SpannableStringBuilder, org.xml.sax.Attributes)
     */
    public static void startBilibiliSpan(SpannableStringBuilder text) {
        int len = text.length();
        text.setSpan(new BilibiliHref(), len, len, Spannable.SPAN_MARK_MARK);
    }

    /**
     * See android.text.HtmlToSpannedConverter#endA(android.text.SpannableStringBuilder)
     */
    public static void endBilibiliSpan(@NonNull SpannableStringBuilder text) {
        int len = text.length();
        Object obj = TagHandler.getLastSpan(text, BilibiliHref.class);
        int where = text.getSpanStart(obj);

        text.removeSpan(obj);

        if (where < len && obj != null) {
            char[] hrefChars = new char[len - where];
            text.getChars(where, len, hrefChars, 0);
            String href = String.valueOf(hrefChars);

            text.setSpan(new BilibiliURLSpan(href), where, len,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    @Override
    public boolean isMatch(Uri uri) {
        if ("http".equalsIgnoreCase(uri.getScheme())) {
            String path = uri.getEncodedPath();
            if (path == null) {
                path = "/";
            }
            for (Pair<String, String> filter : HOST_FILTERS) {
                String host = filter.first;
                String pathPattern = filter.second;

                if (host.equalsIgnoreCase(uri.getHost())
                        && path.matches(pathPattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onClick(Uri uri, View view) {
        goBilibili(view.getContext(), uri);
    }

    // 对Bilibili链接进行独立处理，调用Bilibili客户端
    private static void goBilibili(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setClassName("tv.danmaku.bili", "tv.danmaku.bili.ui.IntentHandlerActivity");
        try {
            PackageManager pm = context.getPackageManager();
            ComponentName cn = intent.resolveActivity(pm);
            if (cn == null)
                throw new ActivityNotFoundException();
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            L.e("BilibiliURLSpan", "Bilibili Actvity was not found for intent, " + intent.toString());
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }
    }

    private static class BilibiliHref {

    }

    private static class BilibiliURLSpan extends ClickableSpan {

        private final String mURL;

        BilibiliURLSpan(String url) {
            mURL = url;
        }

        String getURL() {
            return mURL;
        }

        @Override
        public void onClick(View widget) {
            goBilibili(widget.getContext(), Uri.parse(getURL()));
        }
    }

    private static class HostFilter{
        private String host;
        private String filter;
    }
}
