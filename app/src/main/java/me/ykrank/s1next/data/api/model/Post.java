package me.ykrank.s1next.data.api.model;

import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.ykrank.s1next.data.SameItem;
import me.ykrank.s1next.data.db.BlackListDbWrapper;
import me.ykrank.s1next.data.db.dbmodel.BlackList;
import me.ykrank.s1next.util.L;

@SuppressWarnings("UnusedDeclaration")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class Post implements Cloneable, SameItem {
    private static final String TAG = Post.class.getSimpleName();

    @JsonProperty("pid")
    private String id;

    @JsonProperty("author")
    private String authorName;

    @JsonProperty("authorid")
    private String authorId;

    @JsonProperty("message")
    private String reply;

    @JsonProperty("number")
    private String count;

    @JsonProperty("dbdateline")
    private long datetime;

    @JsonProperty("attachments")
    private Map<Integer, Attachment> attachmentMap;

    private boolean hide = false;

    public Post() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    /**
     * Replies are null sometimes.
     * <p>
     * See https://github.com/floating-cat/S1-Next/issues/6
     */
    @Nullable
    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        reply = hideBlackListQuote(reply);

        reply = replaceBilibiliTag(reply);

        // Replaces "imgwidth" with "img width",
        // because some img tags in S1 aren't correct.
        // This may be the best way to deal with it though
        // we may replace something wrong by accident.
        // Also maps some colors, see mapColors(String).
        this.reply = mapColors(reply).replaceAll("<imgwidth=\"", "<img width=\"");

        processAttachment();
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        // convert seconds to milliseconds
        this.datetime = TimeUnit.SECONDS.toMillis(datetime);
    }

    public void setAttachmentMap(Map<Integer, Attachment> attachmentMap) {
        this.attachmentMap = attachmentMap;

        processAttachment();
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equal(datetime, post.datetime) &&
                Objects.equal(id, post.id) &&
                Objects.equal(authorName, post.authorName) &&
                Objects.equal(authorId, post.authorId) &&
                Objects.equal(reply, post.reply) &&
                Objects.equal(count, post.count) &&
                Objects.equal(attachmentMap, post.attachmentMap) &&
                Objects.equal(hide, post.hide);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, authorName, authorId, reply, count, datetime, attachmentMap);
    }

    @Override
    public Post clone() {
        Post o = null;
        try {
            o = (Post) super.clone();
        } catch (CloneNotSupportedException e) {
            L.e(TAG, e);
        } catch (ClassCastException e) {
            L.e(TAG, e);
        }
        return o;
    }

    @Override
    public boolean isSameItem(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return Objects.equal(id, post.id) &&
                Objects.equal(authorName, post.authorName) &&
                Objects.equal(authorId, post.authorId);
    }

    /**
     * {@link Color} doesn't support all HTML color names.
     * So {@link android.text.Html#fromHtml(String)} won't
     * map some color names for replies in S1.
     * We need to map these color names to their hex value.
     */
    private static String mapColors(String reply) {
        // example: color="sienna"
        // matcher.group(0): color="sienna"
        // matcher.group(1): sienna
        Matcher matcher = Pattern.compile("color=\"([a-zA-Z]+)\"").matcher(reply);

        StringBuffer stringBuffer = new StringBuffer();
        String color;
        while (matcher.find()) {
            // get color hex value for its color name
            color = COLOR_NAME_MAP.get(matcher.group(1).toLowerCase(Locale.US));
            if (color == null) {
                continue;
            }
            // append part of the string and its color hex value
            matcher.appendReplacement(stringBuffer, "color=\"" + color + "\"");
        }
        matcher.appendTail(stringBuffer);

        return stringBuffer.toString();
    }

    /**
     * 隐藏黑名单用户的引用内容
     *
     * @param reply
     * @return
     */
    private String hideBlackListQuote(String reply) {
        String quoteName = findBlockQuoteName(reply);
        if (quoteName != null) {
            if (BlackListDbWrapper.getInstance().getPostFlag(-1, quoteName) != BlackList.NORMAL) {
                return replaceBlockQuoteContent(reply);
            }
        }
        return reply;
    }

    /**
     * 解析引用对象的用户名
     *
     * @param reply
     * @return
     */
    private String findBlockQuoteName(String reply) {
        String name = null;
        Pattern pattern = Pattern.compile("<blockquote>[\\s\\S]*</blockquote>");
        Matcher matcher = pattern.matcher(reply);
        if (matcher.find()) {
            String quote = matcher.group(0);
            pattern = Pattern.compile("<font color=\"#999999\">.*?发表于");
            matcher = pattern.matcher(quote);
            if (matcher.find()) {
                String rawName = matcher.group(0);
                name = rawName.substring(22, rawName.length() - 4);
            }
        }
        return name;
    }

    /**
     * 替换对已屏蔽对象的引用内容
     *
     * @param reply
     * @return
     */
    private String replaceBlockQuoteContent(String reply) {
        Pattern pattern = Pattern.compile("</font></a>[\\s\\S]*</blockquote>");
        Matcher matcher = pattern.matcher(reply);
        if (matcher.find()) {
            return reply.replaceFirst("</font></a>[\\s\\S]*</blockquote>",
                    "</font></a><br />\r\n[已被抹布]</blockquote>");
        } else {
            pattern = Pattern.compile("</font><br />[\\s\\S]*</blockquote>");
            matcher = pattern.matcher(reply);
            if (matcher.find()) {
                return reply.replaceFirst("</font><br />[\\s\\S]*</blockquote>",
                        "</font><br />\r\n[已被抹布]</blockquote>");
            }
        }
        return reply;
    }

    /**
     * 将B站链接添加自定义Tag
     * like "<bilibili>http://www.bilibili.com/video/av6706141/index_3.html</bilibili>"
     * @param reply
     * @return
     */
    private String replaceBilibiliTag(String reply) {
        Pattern pattern = Pattern.compile("\\[thgame_biliplay.*?\\[/thgame_biliplay\\]");
        Matcher matcher = pattern.matcher(reply);
        while (matcher.find()) {
            try {
                String content = matcher.group(0);
                //find av number
                Pattern avPattern = Pattern.compile("\\{,=av\\}[0-9]+");
                Matcher avMatcher = avPattern.matcher(content);
                if (!avMatcher.find()){
                    continue;
                }
                int avNum = Integer.valueOf(avMatcher.group().substring(6));
                //find page
                int page = 1;
                Pattern pagePattern = Pattern.compile("\\{,=page\\}[0-9]+");
                Matcher pageMatcher = pagePattern.matcher(content);
                if (pageMatcher.find()){
                    page = Integer.valueOf(pageMatcher.group().substring(8));
                }

                //like "<bilibili>http://www.bilibili.com/video/av6706141/index_3.html</bilibili>"
                StringBuilder builder = new StringBuilder("<bilibili>http://www.bilibili.com/video/av");
                builder.append(avNum);
                builder.append("/index_");
                builder.append(page);
                builder.append(".html</bilibili>");

                reply = reply.replace(content, builder.toString());
            }catch (Exception e){
                L.e(e);
            }
        }
        return reply;
    }

    /**
     * Replaces attach tags with HTML img tags
     * in order to display attachment images in TextView.
     * <p>
     * Also concats the missing img tag from attachment.
     * See https://github.com/floating-cat/S1-Next/issues/7
     */
    private void processAttachment() {
        if (reply == null || attachmentMap == null) {
            return;
        }

        for (Map.Entry<Integer, Post.Attachment> entry : attachmentMap.entrySet()) {
            Post.Attachment attachment = entry.getValue();
            String imgTag = "<img src=\"" + attachment.getUrl() + "\" />";
            String replyCopy = reply;
            // get the original string if there is nothing to replace
            reply = reply.replace("[attach]" + entry.getKey() + "[/attach]", imgTag);
            //noinspection StringEquality
            if (reply == replyCopy) {
                // concat the missing img tag
                reply = reply + imgTag;
            }
        }
    }

    private static final SimpleArrayMap<String, String> COLOR_NAME_MAP;

    static {
        COLOR_NAME_MAP = new SimpleArrayMap<>();

        COLOR_NAME_MAP.put("sienna", "#A0522D");
        COLOR_NAME_MAP.put("darkolivegreen", "#556B2F");
        COLOR_NAME_MAP.put("darkgreen", "#006400");
        COLOR_NAME_MAP.put("darkslateblue", "#483D8B");
        COLOR_NAME_MAP.put("indigo", "#4B0082");
        COLOR_NAME_MAP.put("darkslategray", "#2F4F4F");
        COLOR_NAME_MAP.put("darkred", "#8B0000");
        COLOR_NAME_MAP.put("darkorange", "#FF8C00");
        COLOR_NAME_MAP.put("slategray", "#708090");
        COLOR_NAME_MAP.put("dimgray", "#696969");
        COLOR_NAME_MAP.put("sandybrown", "#F4A460");
        COLOR_NAME_MAP.put("yellowgreen", "#9ACD32");
        COLOR_NAME_MAP.put("seagreen", "#2E8B57");
        COLOR_NAME_MAP.put("mediumturquoise", "#48D1CC");
        COLOR_NAME_MAP.put("royalblue", "#4169E1");
        COLOR_NAME_MAP.put("orange", "#FFA500");
        COLOR_NAME_MAP.put("deepskyblue", "#00BFFF");
        COLOR_NAME_MAP.put("darkorchid", "#9932CC");
        COLOR_NAME_MAP.put("pink", "#FFC0CB");
        COLOR_NAME_MAP.put("wheat", "#F5DEB3");
        COLOR_NAME_MAP.put("lemonchiffon", "#FFFACD");
        COLOR_NAME_MAP.put("palegreen", "#98FB98");
        COLOR_NAME_MAP.put("paleturquoise", "#AFEEEE");
        COLOR_NAME_MAP.put("lightblue", "#ADD8E6");

        // https://code.google.com/p/android/issues/detail?id=75953
        COLOR_NAME_MAP.put("white", "#FFFFFF");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Attachment {

        @JsonIgnore
        private final String url;

        @JsonCreator
        public Attachment(@JsonProperty("url") String urlPrefix,
                          @JsonProperty("attachment") String urlSuffix) {
            this.url = urlPrefix + urlSuffix;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Attachment that = (Attachment) o;
            return Objects.equal(url, that.url);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(url);
        }
    }
}
