package com.ives.money;

import android.view.*;
import android.widget.*;
import java.time.YearMonth;

/** Month-only navigation for the ledger, with immediate selection. */
final class MonthPicker {
    interface Choice { void selected(YearMonth month); }

    static void show(MainActivity a,YearMonth selected,Choice action){
        UiKit.Sheet sheet=new UiKit.Sheet(a,"选择账单月份","",R.drawable.ic_ui_calendar);
        final int[] year={selected.getYear()};YearMonth current=YearMonth.now();
        int first=current.getYear()-1;if(selected.getYear()<first||selected.getYear()>first+1)first=Math.min(selected.getYear(),9998);final int[] startYear={Math.max(1,first)};
        LinearLayout years=a.row();a.pad(years,6,6,6,6);years.setBackground(a.bg(a.SURFACE,18));sheet.body.addView(years,new LinearLayout.LayoutParams(-1,a.dp(56)));
        TextView previous=UiKit.iconButton(a,R.drawable.ic_ui_back,"查看更早的年份");years.addView(previous,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));
        TextView[] yearButtons=new TextView[2];
        for(int i=0;i<2;i++){TextView y=a.text("",16,a.BLACK,true);y.setGravity(Gravity.CENTER);y.setSingleLine(true);y.setIncludeFontPadding(false);y.setFontFeatureSettings("tnum");y.setAutoSizeTextTypeUniformWithConfiguration(12,16,1,android.util.TypedValue.COMPLEX_UNIT_SP);y.setFocusable(true);UiKit.buttonSemantics(y);LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,-1,1);yp.leftMargin=a.dp(6);years.addView(y,yp);yearButtons[i]=y;}
        TextView next=UiKit.iconButton(a,R.drawable.ic_ui_chevron,"查看更近的年份");LinearLayout.LayoutParams nextp=new LinearLayout.LayoutParams(a.dp(44),a.dp(44));nextp.leftMargin=a.dp(6);years.addView(next,nextp);
        LinearLayout grid=a.column();LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=a.dp(16);sheet.body.addView(grid,gp);
        Runnable refresh=()->{
            for(int i=0;i<2;i++){int value=startYear[0]+i;boolean active=value==year[0];TextView b=yearButtons[i];b.setText(value+"年");b.setTextColor(active?a.BLUE:a.GRAY);b.setBackground(UiKit.surface(a,active?UiKit.TINT:a.WHITE,12,active?UiKit.ACCENT:0));b.setSelected(active);}
            grid.removeAllViews();previous.setEnabled(startYear[0]>1);next.setEnabled(startYear[0]<9998);
            for(int row=0;row<3;row++){
                LinearLayout line=a.row();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);if(row>0)rp.topMargin=a.dp(8);grid.addView(line,rp);
                for(int col=0;col<4;col++){
                    YearMonth month=YearMonth.of(year[0],row*4+col+1);boolean chosen=month.equals(selected),today=month.equals(current);
                    LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER);a.pad(tile,4,8,4,8);
                    tile.setBackground(UiKit.surface(a,chosen?0xc8ffffff:0x35ffffff,16,chosen?0xffcccdc7:0x70ffffff));if(chosen)UiKit.porcelainDepth(a,tile,2);
                    TextView label=a.text(String.valueOf(month.getMonthValue()),23,a.BLACK,true);label.setGravity(Gravity.CENTER);label.setIncludeFontPadding(false);label.setSingleLine(true);label.setFontFeatureSettings("tnum");
                    label.setAutoSizeTextTypeUniformWithConfiguration(17,23,1,android.util.TypedValue.COMPLEX_UNIT_SP);tile.addView(label,new LinearLayout.LayoutParams(-1,a.dp(30)));
                    TextView unit=a.text("月",11,a.GRAY,false);unit.setGravity(Gravity.CENTER);unit.setIncludeFontPadding(false);tile.addView(unit,new LinearLayout.LayoutParams(-1,a.dp(17)));
                    View marker=new View(a);marker.setBackground(a.bg(today?0xff777970:android.graphics.Color.TRANSPARENT,2));LinearLayout.LayoutParams dot=new LinearLayout.LayoutParams(a.dp(4),a.dp(4));dot.topMargin=a.dp(5);tile.addView(marker,dot);
                    LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,a.dp(84),1);if(col>0)tp.leftMargin=a.dp(8);line.addView(tile,tp);
                    tile.setFocusable(true);tile.setSelected(chosen);tile.setContentDescription(month.getYear()+"年"+month.getMonthValue()+"月"+(chosen?"，已选中":"")+(today?"，本月":""));UiKit.buttonSemantics(tile);
                    tile.setOnClickListener(v->{sheet.close();action.selected(month);});
                }
            }
        };
        for(int i=0;i<2;i++){final int index=i;yearButtons[i].setOnClickListener(v->{year[0]=startYear[0]+index;refresh.run();});}
        previous.setOnClickListener(v->{startYear[0]=Math.max(1,startYear[0]-2);year[0]=startYear[0]+1;refresh.run();});next.setOnClickListener(v->{startYear[0]=Math.min(9998,startYear[0]+2);year[0]=startYear[0];refresh.run();});refresh.run();
        TextView now=sheet.action("回到本月",false,()->{sheet.close();action.selected(current);});UiKit.leading(a,now,R.drawable.ic_ui_calendar,UiKit.ACCENT);sheet.show();
    }
    private MonthPicker() {}
}
