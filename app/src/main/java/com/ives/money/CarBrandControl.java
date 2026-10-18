package com.ives.money;

import android.app.AlertDialog;
import android.graphics.Typeface;
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
        LinearLayout button=a.row();button.setBackground(a.bg(a.SURFACE,14));a.pad(button,14,8,14,8);root.addView(button,new LinearLayout.LayoutParams(-1,a.dp(60)));
        icon=new ImageView(a);icon.setScaleType(ImageView.ScaleType.FIT_CENTER);button.addView(icon,new LinearLayout.LayoutParams(a.dp(38),a.dp(38)));
        label=a.text("",15,a.BLACK,true);LinearLayout.LayoutParams textp=new LinearLayout.LayoutParams(0,-2,1);textp.leftMargin=a.dp(12);button.addView(label,textp);button.addView(a.text("选择  ›",12,a.BLUE,true));button.setOnClickListener(v->choose());
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
        BaseAdapter adapter=new BaseAdapter(){
            public int getCount(){return CarBrand.OPTIONS.length;}
            public Object getItem(int p){return CarBrand.OPTIONS[p];}
            public long getItemId(int p){return p;}
            public View getView(int p,View convert,android.view.ViewGroup parent){
                String name=CarBrand.OPTIONS[p];LinearLayout row=a.row();a.pad(row,20,10,18,10);ImageView img=new ImageView(a);img.setImageResource(a.carBrandResource(name));img.setScaleType(ImageView.ScaleType.FIT_CENTER);row.addView(img,new LinearLayout.LayoutParams(a.dp(40),a.dp(40)));
                TextView text=a.text(name,16,a.BLACK,name.equals(brand));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,a.dp(44),1);lp.leftMargin=a.dp(16);text.setGravity(Gravity.CENTER_VERTICAL);row.addView(text,lp);
                TextView selected=a.text(name.equals(brand)?"✓":"",20,a.BLUE,true);selected.setGravity(Gravity.CENTER);row.addView(selected,new LinearLayout.LayoutParams(a.dp(32),a.dp(44)));return row;
            }
        };
        new AlertDialog.Builder(a).setTitle("选择车辆品牌").setAdapter(adapter,(d,p)->{brand=CarBrand.OPTIONS[p];chosen=true;refresh();}).setNegativeButton("取消",null).show();
    }
}
