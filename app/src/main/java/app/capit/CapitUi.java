package app.capit;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.*;

/** Small native views and vector artwork. No image downloads or animation engine. */
final class CapitUi {
    static final int INK = Color.rgb(25, 57, 47);
    static final int PAPER = Color.rgb(248, 248, 242);
    static final int MINT = Color.rgb(228, 239, 218);
    static final int MUTED = Color.rgb(90, 108, 97);
    static final int LILAC = Color.rgb(234, 230, 247);
    static final int LINE = Color.rgb(224, 230, 218);
    static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    static GradientDrawable shape(Context c, int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d;
    }
    static TextView text(Context c, String value, int size, int color, boolean bold) {
        TextView v = new TextView(c); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("kern"); v.setIncludeFontPadding(false);
        v.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        return v;
    }
    static void clickable(View view, int color, int radius, Runnable action) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22386F4C), shape(view.getContext(), color, radius), null));
        view.setFocusable(true); view.setClickable(true);
        view.setOnClickListener(v -> action.run());
    }
    static void enter(View view, boolean reduce) {
        view.animate().cancel(); view.setAlpha(1); view.setTranslationY(0);
        if (reduce || !ValueAnimator.areAnimatorsEnabled()) return;
        view.setAlpha(0); view.setTranslationY(dp(view.getContext(), 10));
        view.animate().alpha(1).translationY(0).setDuration(220)
                .setInterpolator(new DecelerateInterpolator()).start();
    }
    static LinearLayout column(Context c) { LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l; }
    static void space(LinearLayout parent, int height) { parent.addView(new View(parent.getContext()), new LinearLayout.LayoutParams(1, dp(parent.getContext(), height))); }
    static void label(LinearLayout parent, String text, int size, int color, boolean bold) { parent.addView(text(parent.getContext(), text, size, color, bold)); }

    static View home(Context c, Runnable messages, Runnable stories, Runnable following, Runnable about) {
        boolean compact = c.getResources().getConfiguration().screenWidthDp <= 360;
        ScrollView scroll = new ScrollView(c); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout page = column(c); page.setPadding(dp(c,24), dp(c,16), dp(c,24), dp(c,24)); scroll.addView(page);
        TextView eyebrow = text(c, "YOUR SPACE TO CONNECT", 10, MUTED, true); eyebrow.setLetterSpacing(.16f); page.addView(eyebrow);
        space(page, 14);
        TextView headline = text(c, "Keep the people.\nSkip the scroll.", compact ? 30 : 36, INK, true);
        headline.setLetterSpacing(-.045f); headline.setLineSpacing(dp(c,2), 1f); page.addView(headline);
        space(page,10);
        TextView subtitle = text(c, "A little less noise. More of what matters.", 14, MUTED, false); subtitle.setLineSpacing(dp(c,4),1); page.addView(subtitle);
        page.addView(new ConnectionArt(c), new LinearLayout.LayoutParams(-1,dp(c,compact ? 88 : 138)));

        LinearLayout messageCard = column(c); messageCard.setPadding(dp(c,20),dp(c,19),dp(c,20),dp(c,19));
        clickable(messageCard, INK, 24, messages);
        LinearLayout heading = new LinearLayout(c); heading.setGravity(Gravity.CENTER_VERTICAL);
        Icon chat = new Icon(c, "messages", MINT); heading.addView(chat,new LinearLayout.LayoutParams(dp(c,28),dp(c,28)));
        TextView msg = text(c,"Messages",21,Color.WHITE,true); LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(0,-2,1); ml.setMarginStart(dp(c,12)); heading.addView(msg,ml);
        heading.addView(new Icon(c,"arrow",MINT),new LinearLayout.LayoutParams(dp(c,24),dp(c,24)));
        messageCard.addView(heading); space(messageCard,10); label(messageCard,"Pick up a conversation.",13,0xFFD9E9DA,false);
        messageCard.setContentDescription("Messages. Pick up a conversation."); page.addView(messageCard);
        space(page,12);
        LinearLayout cards = new LinearLayout(c);
        LinearLayout.LayoutParams left = new LinearLayout.LayoutParams(0,-1,1); left.setMarginEnd(dp(c,6));
        LinearLayout.LayoutParams right = new LinearLayout.LayoutParams(0,-1,1); right.setMarginStart(dp(c,6));
        cards.addView(smallCard(c,"Stories","Little life updates.","stories",MINT,stories),left);
        cards.addView(smallCard(c,"Following","Your people’s posts.","following",LILAC,following),right);
        page.addView(cards,new LinearLayout.LayoutParams(-1,-2));
        space(page,20);
        LinearLayout note = new LinearLayout(c); note.setGravity(Gravity.CENTER_VERTICAL);
        note.addView(new Icon(c,"leaf",INK),new LinearLayout.LayoutParams(dp(c,24),dp(c,24)));
        LinearLayout words=column(c); LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.setMarginStart(dp(c,10));note.addView(words,wp);
        label(words,"Less noise, by design",13,INK,true); space(words,4); label(words,"Reels & Explore filters · Inside Capit",11,MUTED,false);
        page.addView(note); space(page,20);
        TextView footer=text(c,"Free to use. Made for a calmer feed.",12,MUTED,false);
        footer.setGravity(Gravity.CENTER); footer.setMinHeight(dp(c,48)); clickable(footer,Color.TRANSPARENT,12,about);page.addView(footer);
        return scroll;
    }
    private static View smallCard(Context c,String title,String subtitle,String icon,int color,Runnable action) {
        LinearLayout card=column(c); card.setPadding(dp(c,18),dp(c,18),dp(c,14),dp(c,18));
        clickable(card,color,24,action);card.setMinimumHeight(dp(c,139));
        card.addView(new Icon(c,icon,INK),new LinearLayout.LayoutParams(dp(c,27),dp(c,27)));
        space(card,14);label(card,title,18,INK,true);space(card,6);label(card,subtitle,12,MUTED,false);
        card.setContentDescription(title+". "+subtitle);return card;
    }
    static final class Icon extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path=new Path();
        private final RectF rect=new RectF();
        private final String kind;
        private int tint;
        Icon(Context c,String kind,int tint) { super(c);this.kind=kind;this.tint=tint;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        void tint(int color) { tint=color;invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);canvas.save();canvas.translate(getPaddingLeft(),getPaddingTop());canvas.scale((getWidth()-getPaddingLeft()-getPaddingRight())/24f,(getHeight()-getPaddingTop()-getPaddingBottom())/24f);
            paint.setColor(tint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);path.reset();
            switch(kind) {
                case "messages": rect.set(3,4,21,18);canvas.drawRoundRect(rect,5,5,paint);path.moveTo(7,18);path.lineTo(5,22);path.lineTo(13,18);canvas.drawPath(path,paint);canvas.drawLine(8,9,16,9,paint);canvas.drawLine(8,13,13,13,paint);break;
                case "stories": rect.set(3,3,21,21);canvas.drawArc(rect,35,285,false,paint);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(20,7,2,paint);paint.setStyle(Paint.Style.STROKE);canvas.drawCircle(12,12,4.5f,paint);break;
                case "following": rect.set(3,4,21,21);canvas.drawRoundRect(rect,3,3,paint);canvas.drawLine(8,2,8,6,paint);canvas.drawLine(16,2,16,6,paint);canvas.drawLine(3,10,21,10,paint);canvas.drawLine(8,14,16,14,paint);canvas.drawLine(8,17,13,17,paint);break;
                case "home": path.moveTo(3,10);path.lineTo(12,3);path.lineTo(21,10);path.lineTo(21,21);path.lineTo(15,21);path.lineTo(15,14);path.lineTo(9,14);path.lineTo(9,21);path.lineTo(3,21);path.close();canvas.drawPath(path,paint);break;
                case "arrow": canvas.drawLine(4,12,20,12,paint);path.moveTo(14,6);path.lineTo(20,12);path.lineTo(14,18);canvas.drawPath(path,paint);break;
                case "more": paint.setStyle(Paint.Style.FILL);for(int x=5;x<=19;x+=7)canvas.drawCircle(x,12,1.5f,paint);break;
                case "leaf": path.moveTo(5,19);path.cubicTo(0,8,10,3,21,3);path.cubicTo(21,15,16,22,5,19);canvas.drawPath(path,paint);canvas.drawLine(4,21,15,10,paint);break;
            }
            canvas.restore();
        }
    }
    /** Decorative, static illustration; drawn only when invalidated. */
    private static final class ConnectionArt extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r=new RectF();
        private final Path path=new Path();
        private final android.graphics.drawable.Drawable logo;
        ConnectionArt(Context c) { super(c);logo=c.getDrawable(R.drawable.ic_capit);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas c) {
            c.save();float scale=Math.min(getWidth()/330f,getHeight()/138f);c.translate((getWidth()-330*scale)/2,0);c.scale(scale,scale);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(LINE);r.set(43,14,291,126);c.drawOval(r,p);r.set(75,1,259,137);c.drawOval(r,p);
            p.setStyle(Paint.Style.FILL);p.setColor(MINT);c.drawCircle(85,74,29,p);p.setColor(LILAC);c.drawCircle(259,50,24,p);
            p.setColor(INK);r.set(66,61,104,83);c.drawRoundRect(r,8,8,p);path.reset();path.moveTo(76,81);path.lineTo(71,90);path.lineTo(88,81);c.drawPath(path,p);
            p.setColor(PAPER);for(int x=77;x<=93;x+=8)c.drawCircle(x,72,1.7f,p);
            p.setColor(INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);path.reset();path.moveTo(249,48);path.cubicTo(249,40,259,40,259,46);path.cubicTo(261,40,270,41,269,48);path.cubicTo(268,53,259,59,259,59);path.cubicTo(259,59,250,53,249,48);c.drawPath(path,p);
            logo.setBounds(128,31,206,109);logo.draw(c);
            p.setStyle(Paint.Style.FILL);p.setColor(0xFFB4CBA4);c.drawCircle(230,108,4,p);c.drawCircle(113,26,3,p);p.setColor(0xFFA79CC5);c.drawCircle(39,40,3,p);
            c.restore();
        }
    }
}
