package com.ives.money;

import android.content.Context;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;

/** An in-app amount keypad. The regular IME remains available for the description. */
final class AmountKeyboard {
    final MainActivity a;
    final EditText amount;
    final LinearLayout root;
    final View toggle;

    AmountKeyboard(MainActivity activity,EditText field,View keyboardToggle){
        a=activity;amount=field;toggle=keyboardToggle;root=a.column();
        amount.setShowSoftInputOnFocus(false);
        amount.setFilters(new InputFilter[]{(CharSequence source,int start,int end,Spanned dest,int dstart,int dend)->{
            String next=dest.subSequence(0,dstart).toString()+source.subSequence(start,end)+dest.subSequence(dend,dest.length());
            return next.matches("\\d{0,12}(?:\\.\\d{0,2})?")?null:dest.subSequence(dstart,dend);
        }});
        String[][] keys={{"1","2","3"},{"4","5","6"},{"7","8","9"},{".","0","删除"}};
        for(int row=0;row<keys.length;row++){
            LinearLayout line=a.row();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,a.dp(44));rp.bottomMargin=a.dp(4);root.addView(line,rp);
            for(int col=0;col<3;col++){
                String key=keys[row][col];View button;
                if(key.equals("删除")){
                    ImageView delete=UiKit.icon(a,R.drawable.ic_ui_backspace,24,a.BLACK);delete.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
                    delete.setContentDescription("删除一位金额，长按清空");delete.setOnLongClickListener(v->{amount.setText("");return true;});button=delete;
                }else{
                    TextView digit=a.text(key,23,a.BLACK,false);digit.setGravity(Gravity.CENTER);digit.setFontFeatureSettings("tnum");button=digit;button.setContentDescription(key.equals(".")?"小数点":key);
                }
                button.setBackground(UiKit.surface(a,a.SURFACE,10,0));button.setFocusable(true);UiKit.buttonSemantics(button);
                LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-1,1);if(col>0)bp.leftMargin=a.dp(4);line.addView(button,bp);
                button.setOnClickListener(v->press(key));
            }
        }
        amount.setOnFocusChangeListener((view,focused)->{if(focused)show();});
        amount.setOnClickListener(v->show());toggle.setOnClickListener(v->activate());
    }
    void activate(){amount.requestFocus();show();}
    void show(){
        if(amount.getWindowToken()!=null)((InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(amount.getWindowToken(),0);
        root.setVisibility(View.VISIBLE);toggle.setVisibility(View.GONE);
    }
    void hide(){root.setVisibility(View.GONE);toggle.setVisibility(View.VISIBLE);}
    void press(String key){
        int start=Math.max(0,amount.getSelectionStart()),end=Math.max(0,amount.getSelectionEnd());
        int left=Math.min(start,end),right=Math.max(start,end);
        if(key.equals("删除")){
            if(left!=right)amount.getText().delete(left,right);else if(left>0)amount.getText().delete(left-1,left);
            return;
        }
        String insertion=key;
        String remaining=amount.getText().subSequence(0,left).toString()+amount.getText().subSequence(right,amount.length());
        if(key.equals(".")){
            if(remaining.contains("."))return;
            if(left==0)insertion="0.";
        }else if(left==right&&left==1&&amount.getText().toString().equals("0")){
            left=0;
        }
        amount.getText().replace(left,right,insertion);
    }
}
