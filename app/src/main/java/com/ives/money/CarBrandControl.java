package com.ives.money;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;

/** A compact picker that appears only for a car-loan name or category. */
final class CarBrandControl {
    final MainActivity a;
    final LinearLayout root;
    final EditText title,category;
    final ImageView icon;
    final TextView label;
    String brand;
    boolean chosen;
    CarBrandControl(MainActivity activity,EditText titleField,EditText categoryField,String saved){
        a=activity;title=titleField;category=categoryField;chosen=saved!=null&&!saved.isEmpty();brand=CarBrand.resolve(title.getText().toString(),category.getText().toString(),saved);
        root=a.column();TextView heading=a.text("车辆品牌",13,a.GRAY,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=a.dp(18);hp.bottomMargin=a.dp(8);root.addView(heading,hp);
        LinearLayout button=a.row();button.setBackground(UiKit.surface(a,a.SURFACE,14,a.LINE));a.pad(button,14,8,14,8);root.addView(button,new LinearLayout.LayoutParams(-1,a.dp(52)));
        icon=new ImageView(a);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);button.addView(icon,new LinearLayout.LayoutParams(a.dp(30),a.dp(30)));
        label=a.text("",15,a.BLACK,true);LinearLayout.LayoutParams textp=new LinearLayout.LayoutParams(0,-2,1);textp.leftMargin=a.dp(12);button.addView(label,textp);button.addView(UiKit.icon(a,R.drawable.ic_ui_chevron,20,a.GRAY),new LinearLayout.LayoutParams(a.dp(24),a.dp(24)));button.setOnClickListener(v->choose());
        TextWatcher watcher=new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){}public void onTextChanged(CharSequence s,int start,int before,int count){}public void afterTextChanged(Editable e){refresh();}};
        title.addTextChangedListener(watcher);category.addTextChangedListener(watcher);refresh();
    }
    void refresh(){
        if(!chosen)brand=CarBrand.resolve(title.getText().toString(),category.getText().toString(),"");
        icon.setImageResource(a.carBrandResource(brand));icon.setContentDescription(brand);label.setText(brand);
        root.setVisibility(CarBrand.isLoan(title.getText().toString(),category.getText().toString())?View.VISIBLE:View.GONE);
    }
    String value(){return CarBrand.isLoan(title.getText().toString(),category.getText().toString())?brand:"";}
    void choose(){
        UiKit.Sheet sheet=new UiKit.Sheet(a,"选择车辆品牌","",R.drawable.ic_ui_car);
        for(String name:CarBrand.OPTIONS){
            boolean selected=name.equals(brand);LinearLayout row=a.row();a.pad(row,14,10,14,10);row.setMinimumHeight(a.dp(64));row.setBackground(UiKit.surface(a,selected?UiKit.TINT:a.SURFACE,16,0));
            ImageView img=new ImageView(a);img.setImageResource(a.carBrandResource(name));img.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(img,new LinearLayout.LayoutParams(a.dp(30),a.dp(30)));
            TextView text=a.text(name,15,a.BLACK,selected);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=a.dp(14);row.addView(text,lp);
            row.addView(UiKit.icon(a,selected?R.drawable.ic_ui_check:R.drawable.ic_ui_chevron,20,selected?a.BLUE:a.GRAY),new LinearLayout.LayoutParams(a.dp(24),a.dp(24)));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.bottomMargin=a.dp(8);sheet.body.addView(row,rp);row.setFocusable(true);row.setOnClickListener(v->{brand=name;chosen=true;refresh();sheet.close();});
        }
        sheet.cancel("取消");sheet.show();
    }
}
