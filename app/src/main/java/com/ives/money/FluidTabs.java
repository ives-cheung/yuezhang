package com.ives.money;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One continuous porcelain selection pill, stretched gently along its direction of travel. */
final class FluidTabs extends LinearLayout {
    private final MainActivity a;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill=new RectF();
    private ValueAnimator motion;
    private int selected;
    private float position,stretch;

    FluidTabs(MainActivity activity,int initial){
        super(activity);a=activity;selected=initial;position=initial;
        setOrientation(HORIZONTAL);setPadding(a.dp(3),a.dp(3),a.dp(3),a.dp(3));
        setBackground(UiKit.porcelain(a,25,false));UiKit.porcelainDepth(a,this,2);
    }
    void select(int index){
        if(index==selected)return;
        if(motion!=null)motion.cancel();
        selected=index;updateLabels();
        if(!ValueAnimator.areAnimatorsEnabled()){position=index;stretch=0;invalidate();return;}
        final float from=position,initialStretch=stretch,distance=index-from;
        motion=ValueAnimator.ofFloat(0,1);motion.setDuration(380);
        motion.setInterpolator(new PathInterpolator(.22f,.0f,.18f,1f));
        motion.addUpdateListener(anim->{float t=(float)anim.getAnimatedValue();position=from+distance*t;
            stretch=initialStretch*(1-t)+(float)Math.sin(Math.PI*t)*Math.min(.19f,Math.abs(distance)*.13f);
            invalidate();});motion.start();
    }
    private void updateLabels(){
        for(int i=0;i<getChildCount();i++){TextView t=(TextView)getChildAt(i);boolean active=i==selected;
            t.setSelected(active);t.setTextColor(active?a.BLACK:a.GRAY);
            t.setTypeface(android.graphics.Typeface.create(active?"sans-serif-medium":"sans-serif",android.graphics.Typeface.NORMAL));}
    }
    @Override protected void dispatchDraw(Canvas canvas){
        if(getChildCount()>0){View first=getChildAt(0);float step=first.getWidth()+a.dp(2);
            float left=first.getLeft()+position*step,extra=first.getWidth()*stretch;
            pill.set(Math.max(getPaddingLeft(),left-extra/2),first.getTop()+a.dp(stretch*5),
                Math.min(getWidth()-getPaddingRight(),left+first.getWidth()+extra/2),first.getBottom()-a.dp(stretch*5));
            paint.setColor(0xffeeeee8);canvas.drawRoundRect(pill,a.dp(22),a.dp(22),paint);
            paint.setColor(0xffffffff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(a.dp(1));
            canvas.drawRoundRect(pill,a.dp(22),a.dp(22),paint);paint.setStyle(Paint.Style.FILL);
        }
        super.dispatchDraw(canvas);
    }
    @Override protected void onDetachedFromWindow(){if(motion!=null)motion.cancel();super.onDetachedFromWindow();}
}
