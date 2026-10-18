package com.ives.money;

import android.view.*;
import android.widget.*;
import java.time.*;

/** A tap-to-select calendar for entries that need an actual transaction date. */
final class CalendarPicker {
    static void show(MainActivity a,String title,LocalDate selected,UiKit.DateChoice action){
        UiKit.Sheet sheet=new UiKit.Sheet(a,title,"",R.drawable.ic_ui_calendar);
        YearMonth[] month={YearMonth.from(selected)};LocalDate today=LocalDate.now();
        LinearLayout navigation=a.row();sheet.body.addView(navigation,new LinearLayout.LayoutParams(-1,a.dp(48)));
        TextView previous=UiKit.iconButton(a,R.drawable.ic_ui_back,"上个月");navigation.addView(previous,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));
        TextView heading=a.text("",18,a.BLACK,true);heading.setGravity(Gravity.CENTER);heading.setBackground(UiKit.surface(a,a.SURFACE,14,0));heading.setFocusable(true);UiKit.buttonSemantics(heading);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(0,a.dp(44),1);hp.leftMargin=a.dp(8);hp.rightMargin=a.dp(8);navigation.addView(heading,hp);
        TextView next=UiKit.iconButton(a,R.drawable.ic_ui_chevron,"下个月");navigation.addView(next,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));
        LinearLayout weekdays=a.row();LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(-1,a.dp(36));wp.topMargin=a.dp(10);sheet.body.addView(weekdays,wp);
        for(String day:new String[]{"一","二","三","四","五","六","日"}){TextView label=a.text(day,12,a.GRAY,false);label.setGravity(Gravity.CENTER);weekdays.addView(label,new LinearLayout.LayoutParams(0,-1,1));}
        LinearLayout grid=a.column();sheet.body.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        Runnable refresh=()->{
            heading.setText(month[0].getYear()+"年"+month[0].getMonthValue()+"月  ▾");grid.removeAllViews();
            int offset=month[0].atDay(1).getDayOfWeek().getValue()-1;
            int count=(offset+month[0].lengthOfMonth()+6)/7;
            for(int row=0;row<count;row++){
                LinearLayout line=a.row();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=a.dp(4);grid.addView(line,rp);
                for(int col=0;col<7;col++){
                    int day=row*7+col-offset+1;TextView cell=a.text("",16,a.BLACK,false);cell.setGravity(Gravity.CENTER);cell.setMinHeight(a.dp(46));a.pad(cell,0,6,0,6);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);if(col>0)cp.leftMargin=a.dp(2);line.addView(cell,cp);
                    if(day<1||day>month[0].lengthOfMonth())continue;
                    LocalDate date=month[0].atDay(day);boolean chosen=date.equals(selected),current=date.equals(today);
                    cell.setText(String.valueOf(day));cell.setTextColor(chosen?a.WHITE:current?a.BLUE:a.BLACK);cell.setBackground(UiKit.surface(a,chosen?UiKit.INK:a.SURFACE,12,!chosen&&current?a.BLUE:0));
                    cell.setSelected(chosen);cell.setFocusable(true);cell.setContentDescription(date.getYear()+"年"+date.getMonthValue()+"月"+day+"日"+(chosen?"，已选中":"")+(current?"，今天":""));UiKit.buttonSemantics(cell);
                    cell.setOnClickListener(v->{sheet.close();action.selected(date);});
                }
            }
        };
        previous.setOnClickListener(v->{month[0]=month[0].minusMonths(1);refresh.run();});next.setOnClickListener(v->{month[0]=month[0].plusMonths(1);refresh.run();});
        heading.setOnClickListener(v->MonthPicker.show(a,month[0],value->{month[0]=value;refresh.run();}));refresh.run();
        TextView now=sheet.action("选择今天",false,()->{sheet.close();action.selected(today);});UiKit.leading(a,now,R.drawable.ic_ui_calendar,a.WHITE);sheet.show();
    }
    private CalendarPicker() {}
}
