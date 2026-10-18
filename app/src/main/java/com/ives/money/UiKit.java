package com.ives.money;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.time.LocalDate;

/** Shared phone controls: vector icons, touch feedback and scrollable action sheets. */
final class UiKit {
    static final int INK=0xff1c1c1e, ACCENT=0xff55554f, TINT=0xffeeeee9;
    static final int DANGER=0xffd93632;
    private UiKit() {}

    static void buttonSemantics(View view){
        view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,android.view.accessibility.AccessibilityNodeInfo info){
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(android.widget.Button.class.getName());
            }
        });
    }
    static Drawable surface(MainActivity a,int color,int radius,int stroke){
        GradientDrawable shape=a.bg(color,radius);
        if(stroke!=0)shape.setStroke(a.dp(1),stroke);
        int feedback=color==INK||color==ACCENT||color==DANGER?0x40ffffff:0x142e2e2b;
        return new RippleDrawable(ColorStateList.valueOf(feedback),shape,a.bg(Color.WHITE,radius));
    }
    // Solid porcelain-white surfaces; depth comes from the outline and a restrained shadow.
    static Drawable porcelain(MainActivity a,int radius,boolean prominent){
        GradientDrawable shell=a.bg(Color.WHITE,radius);
        shell.setStroke(Math.max(1,a.dp(.5f)),prominent?0xffe4e4df:0xffefefeb);
        return new RippleDrawable(ColorStateList.valueOf(0x10585850),shell,a.bg(Color.WHITE,radius));
    }
    static void porcelainDepth(MainActivity a,View view,float elevation){
        view.setElevation(a.dp(Math.min(elevation,3f)));
        if(android.os.Build.VERSION.SDK_INT>=28){view.setOutlineAmbientShadowColor(0x10535349);view.setOutlineSpotShadowColor(0x14535349);}
    }
    static Drawable inputBackground(MainActivity a){
        StateListDrawable states=new StateListDrawable();
        GradientDrawable focused=a.bg(TINT,12);focused.setStroke(a.dp(1.5f),ACCENT);
        GradientDrawable normal=a.bg(a.SURFACE,12);
        states.addState(new int[]{android.R.attr.state_focused},focused);
        states.addState(new int[]{},normal);return states;
    }
    static Drawable drawable(MainActivity a,int resource,int size,int color){
        Drawable d=a.getDrawable(resource).mutate();d.setTint(color);d.setBounds(0,0,a.dp(size),a.dp(size));return d;
    }
    static ImageView icon(MainActivity a,int resource,int size,int color){
        ImageView v=new ImageView(a);v.setImageDrawable(drawable(a,resource,size,color));
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);return v;
    }
    static void leading(MainActivity a,TextView button,int icon,int color){
        button.setCompoundDrawablesRelative(drawable(a,icon,20,color),null,null,null);
        button.setCompoundDrawablePadding(a.dp(10));
    }
    static int actionIcon(String label){
        if(label.contains("编辑")||label.contains("修改"))return R.drawable.ic_ui_edit;
        if(label.contains("删除")||label.contains("清空"))return R.drawable.ic_ui_trash;
        if(label.contains("截图")||label.contains("相册")||label.contains("图片"))return R.drawable.ic_ui_image;
        if(label.contains("粘贴"))return R.drawable.ic_ui_clipboard;
        if(label.contains("日期"))return R.drawable.ic_ui_calendar;
        if(label.contains("生成")||label.contains("核对清单"))return R.drawable.ic_ui_checklist;
        if(label.contains("添加")||label.contains("记一笔"))return R.drawable.ic_ui_plus;
        if(label.contains("保存")||label.contains("确认")||label.contains("确定")||label.contains("标记")||label.contains("收回")||label.contains("更新"))return R.drawable.ic_ui_check;
        if(label.equals("上一页"))return R.drawable.ic_ui_back;
        if(label.equals("下一页"))return R.drawable.ic_ui_chevron;
        return 0;
    }
    static TextView button(MainActivity a,String label,boolean primary){
        String clean=label.replace("＋","").trim();
        TextView t=a.text(clean,15,INK,true);t.setGravity(Gravity.CENTER);
        t.setMinHeight(a.dp(48));a.pad(t,18,8,18,8);t.setMaxLines(2);
        t.setBackground(primary?porcelain(a,24,true):surface(a,Color.TRANSPARENT,24,0));if(primary)porcelainDepth(a,t,2);
        t.setClickable(true);t.setFocusable(true);buttonSemantics(t);
        int icon=actionIcon(clean);if(icon==0&&primary)icon=R.drawable.ic_ui_check;
        if(icon!=0)leading(a,t,icon,ACCENT);
        return t;
    }
    static TextView textAction(MainActivity a,String label,boolean destructive){
        TextView t=a.text(label,13,destructive?DANGER:ACCENT,true);t.setGravity(Gravity.CENTER);
        a.pad(t,12,8,12,8);t.setBackground(surface(a,Color.TRANSPARENT,12,0));t.setFocusable(true);
        buttonSemantics(t);return t;
    }
    static TextView iconButton(MainActivity a,int icon,String description){
        TextView b=a.text("",16,a.BLACK,false);b.setGravity(Gravity.CENTER);
        a.pad(b,11,11,11,11);b.setCompoundDrawablesRelative(drawable(a,icon,22,icon==R.drawable.ic_ui_close?a.GRAY:ACCENT),null,null,null);
        b.setContentDescription(description);b.setBackground(surface(a,Color.TRANSPARENT,22,0));b.setFocusable(true);
        buttonSemantics(b);return b;
    }
    static ImageView iconAction(MainActivity a,int icon,String description,boolean destructive){
        ImageView button=icon(a,icon,21,destructive?DANGER:ACCENT);
        button.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        button.setContentDescription(description);button.setFocusable(true);button.setClickable(true);
        button.setMinimumWidth(a.dp(48));button.setMinimumHeight(a.dp(48));
        button.setBackground(porcelain(a,24,false));
        buttonSemantics(button);return button;
    }
    static void picker(MainActivity a,TextView t,int leading){
        t.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);a.pad(t,15,0,15,0);
        t.setCompoundDrawablesRelative(leading==0?null:drawable(a,leading,20,ACCENT),null,drawable(a,R.drawable.ic_ui_chevron,18,a.GRAY),null);
        t.setCompoundDrawablePadding(a.dp(10));t.setBackground(surface(a,a.SURFACE,12,0));
        t.setFocusable(true);buttonSemantics(t);
    }
    interface Choice { void selected(int index); }
    interface DateChoice { void selected(LocalDate date); }
    static void pickDate(MainActivity a,String title,LocalDate initial,DateChoice action){
        CalendarPicker.show(a,title,initial,action);
    }
    static void choose(MainActivity a,String title,String[] options,int selected,int icon,Choice action){
        Sheet sheet=new Sheet(a,title,"",icon);
        for(int i=0;i<options.length;i++){
            final int index=i;LinearLayout row=a.row();a.pad(row,14,12,14,12);
            row.setMinimumHeight(a.dp(56));row.setBackground(surface(a,i==selected?TINT:a.SURFACE,16,0));
            TextView name=a.text(options[i],15,a.BLACK,i==selected);name.setLineSpacing(a.dp(2),1);row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            row.addView(icon(a,i==selected?R.drawable.ic_ui_check:R.drawable.ic_ui_chevron,20,i==selected?ACCENT:a.GRAY),new LinearLayout.LayoutParams(a.dp(24),a.dp(24)));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=a.dp(8);sheet.body.addView(row,p);
            row.setFocusable(true);row.setOnClickListener(v->{sheet.close();action.selected(index);});
        }
        sheet.cancel("取消");sheet.show();
    }
    static void confirm(MainActivity a,String title,String message,String action,boolean destructive,Runnable confirm){
        Sheet s=new Sheet(a,title,message,destructive?R.drawable.ic_ui_trash:R.drawable.ic_ui_alert);
        s.action(action,destructive,()->{s.close();confirm.run();});s.cancel(destructive?"取消":"返回核对");s.show();
    }
    static final class Sheet {
        final MainActivity a;final Dialog dialog;final LinearLayout page,body,footer;
        Sheet(MainActivity activity,String title,String subtitle,int icon){
            a=activity;dialog=new Dialog(a,R.style.MoneySheetTheme);
            page=new LinearLayout(a){
                @Override protected void onMeasure(int widthSpec,int heightSpec){
                    int limit=(int)(a.getResources().getDisplayMetrics().heightPixels*.84f);
                    int size=View.MeasureSpec.getSize(heightSpec);if(size>0)limit=Math.min(limit,size);
                    super.onMeasure(widthSpec,View.MeasureSpec.makeMeasureSpec(limit,View.MeasureSpec.AT_MOST));
                }
            };page.setOrientation(LinearLayout.VERTICAL);page.setBackground(porcelain(a,30,false));porcelainDepth(a,page,8);
            if(android.os.Build.VERSION.SDK_INT>=29)page.setForceDarkAllowed(false);
            View handle=new View(a);handle.setBackground(a.bg(0xffdce2e4,3));LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(a.dp(32),a.dp(4));hp.gravity=Gravity.CENTER_HORIZONTAL;hp.topMargin=a.dp(10);hp.bottomMargin=a.dp(18);page.addView(handle,hp);
            LinearLayout header=a.row();a.pad(header,20,0,16,14);ImageView badge=icon(a,icon,22,ACCENT);badge.setBackground(a.bg(TINT,12));a.pad(badge,7,7,7,7);header.addView(badge,new LinearLayout.LayoutParams(a.dp(36),a.dp(36)));
            TextView heading=a.text(title,18,a.BLACK,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=a.dp(12);tp.rightMargin=a.dp(8);header.addView(heading,tp);
            TextView close=iconButton(a,R.drawable.ic_ui_close,"关闭");header.addView(close,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));close.setOnClickListener(v->close());page.addView(header);
            ScrollView scroll=new ScrollView(a);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
            body=a.column();a.pad(body,20,0,20,4);scroll.addView(body);
            page.addView(scroll,new LinearLayout.LayoutParams(-1,-2,1));
            if(!subtitle.isEmpty()){TextView description=a.text(subtitle,14,a.GRAY,false);description.setLineSpacing(a.dp(4),1);LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(-1,-2);dp.bottomMargin=a.dp(16);body.addView(description,dp);}
            footer=a.column();a.pad(footer,20,12,20,16);page.addView(footer);
        }
        TextView action(String title,boolean destructive,Runnable run){
            TextView b=button(a,title,true);if(destructive){b.setBackground(surface(a,DANGER,24,0));b.setTextColor(a.WHITE);leading(a,b,R.drawable.ic_ui_trash,a.WHITE);}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,a.dp(52));lp.bottomMargin=a.dp(8);footer.addView(b,lp);b.setOnClickListener(v->run.run());return b;
        }
        void cancel(String label){TextView b=button(a,label,false);footer.addView(b,new LinearLayout.LayoutParams(-1,a.dp(48)));b.setOnClickListener(v->close());}
        void show(){
            dialog.setContentView(page);dialog.setCanceledOnTouchOutside(true);
            Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);w.setDimAmount(.18f);if(android.os.Build.VERSION.SDK_INT>=31){w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);WindowManager.LayoutParams blur=w.getAttributes();blur.setBlurBehindRadius(a.dp(18));w.setAttributes(blur);}w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);w.setNavigationBarColor(a.WHITE);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);w.setWindowAnimations(R.style.MoneySheetAnimation);}
            dialog.show();if(w!=null){w.setLayout(Math.min(a.dp(560),a.getResources().getDisplayMetrics().widthPixels-a.dp(24)),-2);w.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);WindowManager.LayoutParams attributes=w.getAttributes();attributes.y=a.dp(12);w.setAttributes(attributes);}
        }
        void close(){View focused=dialog.getCurrentFocus();if(focused!=null)((InputMethodManager)a.getSystemService(MainActivity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);dialog.dismiss();}
    }
}
