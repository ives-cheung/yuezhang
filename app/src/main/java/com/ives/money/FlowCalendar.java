package com.ives.money;

import android.util.TypedValue;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.*;
import android.widget.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A monthly income/expense heat map, using posted transactions only. */
final class FlowCalendar {
    static final int POSITIVE=0xffc44242, NEGATIVE=0xff20815d;
    final LedgerUi ui;
    final MainActivity a;
    final UiKit.Sheet sheet;
    final LinearLayout grid;
    final TextView heading,netValue;
    YearMonth month;

    static final class Totals {
        long income,expense,refund;
        final List<LedgerEntry> entries=new ArrayList<>();
        void add(LedgerEntry entry){
            entries.add(entry);
            switch(entry.type){
                case LedgerEntry.INCOME:income+=entry.cents;break;
                case LedgerEntry.EXPENSE:expense+=entry.cents;break;
                case LedgerEntry.REFUND:refund+=entry.cents;break;
                default:break;
            }
        }
        long net(){return income+refund-expense;}
    }

    FlowCalendar(LedgerUi ledger){
        ui=ledger;a=ui.a;month=ui.month;
        sheet=new UiKit.Sheet(a,"收支日历","",R.drawable.ic_ui_calendar);a.pad(sheet.body,12,0,12,4);
        LinearLayout navigation=a.row();sheet.body.addView(navigation,ui.lp(-1,44));
        TextView previous=UiKit.iconButton(a,R.drawable.ic_ui_back,"上个月");previous.setBackground(UiKit.surface(a,a.SURFACE,22,0));navigation.addView(previous,ui.lp(44,44));previous.setOnClickListener(v->{month=month.minusMonths(1);refresh();});
        heading=a.text("",18,a.BLACK,true);heading.setGravity(Gravity.CENTER);heading.setBackground(UiKit.surface(a,Color.TRANSPARENT,20,0));heading.setFocusable(true);UiKit.buttonSemantics(heading);
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,-1,1);hp.leftMargin=a.dp(6);hp.rightMargin=a.dp(6);navigation.addView(heading,hp);heading.setOnClickListener(v->MonthPicker.show(a,month,selected->{month=selected;refresh();}));
        TextView next=UiKit.iconButton(a,R.drawable.ic_ui_chevron,"下个月");next.setBackground(UiKit.surface(a,a.SURFACE,22,0));navigation.addView(next,ui.lp(44,44));next.setOnClickListener(v->{month=month.plusMonths(1);refresh();});

        LinearLayout summary=a.row();a.pad(summary,4,10,4,10);LinearLayout.LayoutParams sp=ui.lp(-1,-2);sp.topMargin=a.dp(4);sheet.body.addView(summary,sp);
        LinearLayout caption=a.column();summary.addView(caption,new LinearLayout.LayoutParams(0,-2,1));caption.addView(a.text("本月结余",13,a.BLACK,true));ui.paragraph(caption,"收入 + 退款 − 支出",10,a.GRAY,false,3);
        netValue=a.text("",21,a.BLACK,true);netValue.setMaxLines(1);netValue.setAutoSizeTextTypeUniformWithConfiguration(12,21,1,TypedValue.COMPLEX_UNIT_SP);summary.addView(netValue,new LinearLayout.LayoutParams(0,a.dp(42),1.2f));netValue.setGravity(Gravity.CENTER_VERTICAL|Gravity.END);
        LinearLayout weekdays=a.row();LinearLayout.LayoutParams wp=ui.lp(-1,28);wp.topMargin=a.dp(6);sheet.body.addView(weekdays,wp);
        for(String day:new String[]{"一","二","三","四","五","六","日"}){TextView label=a.text(day,11,a.GRAY,false);label.setGravity(Gravity.CENTER);weekdays.addView(label,new LinearLayout.LayoutParams(0,-1,1));}
        grid=a.column();sheet.body.addView(grid,ui.lp(-1,-2));

        a.pad(sheet.footer,20,12,20,14);LinearLayout footer=a.row();sheet.footer.addView(footer,ui.lp(-1,44));
        LinearLayout legend=a.column();legend.setGravity(Gravity.CENTER_VERTICAL);legend.addView(a.text("＋ 结余增加",11,POSITIVE,false));legend.addView(a.text("− 结余减少",11,NEGATIVE,false));footer.addView(legend,new LinearLayout.LayoutParams(0,-1,1));
        TextView list=a.secondaryButton("返回列表");a.pad(list,12,0,12,0);UiKit.leading(a,list,R.drawable.ic_ui_checklist,a.BLUE);footer.addView(list,ui.lp(108,44));list.setOnClickListener(v->sheet.close());
        sheet.dialog.setOnDismissListener(d->{ui.month=month;a.render();});refresh();
    }

    static int color(long cents,int neutral){return cents>0?POSITIVE:cents<0?NEGATIVE:neutral;}
    String signed(long cents){return (cents>0?"+":cents<0?"−":"")+ui.money(Math.abs(cents));}
    static String compact(long cents){
        BigDecimal value=BigDecimal.valueOf(Math.abs(cents),2);String unit="";
        if(value.compareTo(BigDecimal.valueOf(100000000))>=0){value=value.divide(BigDecimal.valueOf(100000000),2,RoundingMode.HALF_UP);unit="亿";}
        else if(value.compareTo(BigDecimal.valueOf(10000))>=0){value=value.divide(BigDecimal.valueOf(10000),2,RoundingMode.HALF_UP);unit="万";}
        return (cents>0?"+":cents<0?"−":"")+value.setScale(2,RoundingMode.HALF_UP).toPlainString()+unit;
    }
    static CharSequence amountLabel(long cents){
        String label=compact(cents);SpannableString styled=new SpannableString(label);int decimal=label.indexOf('.');
        if(decimal>=0)styled.setSpan(new RelativeSizeSpan(.8f),decimal,decimal+3,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return styled;
    }
    void refresh(){
        heading.setText(month.format(DateTimeFormatter.ofPattern("yyyy年M月",Locale.CHINA)));
        List<LedgerEntry> records=ui.db.month(month.toString());Map<String,Totals> days=new HashMap<>();Totals total=new Totals();
        for(LedgerEntry entry:records){Totals day=days.get(entry.date);if(day==null){day=new Totals();days.put(entry.date,day);}day.add(entry);total.add(entry);}
        netValue.setText(signed(total.net()));netValue.setTextColor(color(total.net(),a.BLACK));
        grid.removeAllViews();int offset=month.atDay(1).getDayOfWeek().getValue()-1;int weeks=(offset+month.lengthOfMonth()+6)/7;
        for(int row=0;row<weeks;row++){
            LinearLayout line=a.row();LinearLayout.LayoutParams rp=ui.lp(-1,-2);if(row>0)rp.topMargin=a.dp(6);grid.addView(line,rp);
            for(int col=0;col<7;col++){
                int number=row*7+col-offset+1;LinearLayout cell=a.column();cell.setGravity(Gravity.CENTER);cell.setMinimumHeight(a.dp(60));a.pad(cell,0,3,0,3);
                LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(col>0)cp.leftMargin=a.dp(2);line.addView(cell,cp);
                if(number<1||number>month.lengthOfMonth())continue;
                LocalDate date=month.atDay(number);Totals day=days.get(date.toString());long net=day==null?0:day.net();boolean today=date.equals(LocalDate.now());
                cell.setBackground(UiKit.surface(a,Color.TRANSPARENT,18,0));
                TextView label=a.text(String.valueOf(number),15,today?a.WHITE:a.BLACK,false);label.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));label.setGravity(Gravity.CENTER);label.setIncludeFontPadding(false);
                if(today)label.setBackground(a.bg(a.BLUE,15));cell.addView(label,ui.lp(30,30));
                TextView value=a.text("",11,day==null?0xffb6bfc4:color(net,a.GRAY),false);value.setText(day==null?"·":amountLabel(net));value.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));value.setMaxLines(1);value.setHorizontallyScrolling(false);value.setGravity(Gravity.CENTER);value.setIncludeFontPadding(false);value.setFontFeatureSettings("tnum");
                value.setAutoSizeTextTypeUniformWithConfiguration(8,11,1,TypedValue.COMPLEX_UNIT_SP);a.pad(value,1,0,1,0);if(day!=null)value.setBackground(a.bg(net>0?0xffffeeee:net<0?0xffeaf6ef:a.SURFACE,9));
                LinearLayout.LayoutParams vp=ui.lp(-1,20);vp.topMargin=a.dp(3);cell.addView(value,vp);
                cell.setContentDescription(date.getMonthValue()+"月"+number+"日，"+(day==null?"暂无流水":"结余"+signed(net)+"，共"+day.entries.size()+"笔流水")+(today?"，今天":""));cell.setFocusable(true);UiKit.buttonSemantics(cell);cell.setOnClickListener(v->dayDetails(date));
            }
        }
    }
    void dayDetails(LocalDate date){
        Totals day=new Totals();for(LedgerEntry e:ui.db.month(YearMonth.from(date).toString()))if(e.date.equals(date.toString()))day.add(e);
        UiKit.Sheet detail=new UiKit.Sheet(a,date.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA)),"",R.drawable.ic_ui_receipt);
        detail.body.addView(a.text("当天结余",12,a.GRAY,true));ui.paragraph(detail.body,signed(day.net()),29,color(day.net(),a.BLACK),true,6);
        ui.paragraph(detail.body,"收入 "+ui.money(day.income)+"  ·  支出 "+ui.money(day.expense)+"  ·  退款 "+ui.money(day.refund),12,a.GRAY,false,8);
        ui.space(detail.body,14);
        if(day.entries.isEmpty()){TextView empty=a.text("当天暂无流水",14,a.GRAY,false);empty.setGravity(Gravity.CENTER);detail.body.addView(empty,ui.lp(-1,72));}
        else for(LedgerEntry entry:day.entries){ui.addEntryRow(detail.body,entry,()->{detail.close();sheet.close();ui.details(entry);});detail.body.addView(a.divider());}
        detail.cancel("返回日历");detail.show();
    }
    void show(){sheet.show();}
}
