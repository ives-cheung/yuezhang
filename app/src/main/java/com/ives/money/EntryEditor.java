package com.ives.money;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.*;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** A short daily entry flow: amount, tiled types and one optional description. */
final class EntryEditor {
    final LedgerUi ui;
    final MainActivity a;
    final LedgerEntry entry;
    final boolean draft,fresh;
    final Runnable done;
    final LedgerUi.Page page;
    final EditText amount,description,category;
    final TextView amountLabel,categoryButton,dateButton;
    final CarBrandControl brand;
    final AmountKeyboard keyboard;
    final LinearLayout extras,subscription,refund;
    final LinearLayout[] typeTiles=new LinearLayout[LedgerEntry.TYPES.length];
    final ImageView[] typeIcons=new ImageView[LedgerEntry.TYPES.length];
    final TextView[] typeLabels=new TextView[LedgerEntry.TYPES.length];
    final int[] icons={R.drawable.ic_ui_expense,R.drawable.ic_ui_income,R.drawable.ic_ui_refund,R.drawable.ic_ui_transfer,R.drawable.ic_ui_card,R.drawable.ic_ui_recharge,R.drawable.ic_ui_ignore};
    final String[] categories={"餐饮","购物","交通","车辆","水电费","房租","通讯","订阅","医疗","工资收入","车贷","其他"};
    boolean categoryChosen;

    EntryEditor(LedgerUi ledger,LedgerEntry original,boolean isDraft,Runnable callback){
        this(ledger,original,isDraft,callback,null);
    }
    EntryEditor(LedgerUi ledger,LedgerEntry original,boolean isDraft,Runnable callback,LedgerUi.Page existing){
        ui=ledger;a=ui.a;entry=original.copy();draft=isDraft;done=callback;
        fresh=!draft&&entry.cents==0&&entry.title.isEmpty();
        if(fresh&&entry.time.isEmpty())entry.time=LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title=draft?"核对这笔账单":fresh?"记一笔":"编辑账单";
        page=existing==null?ui.new Page(title):existing;if(existing!=null)page.replaceContent(title);
        page.scroll.setBackgroundColor(a.SURFACE);a.pad(page.body,16,16,16,20);a.pad(page.footer,16,10,16,10);
        page.body.setFocusableInTouchMode(true);page.body.requestFocus();

        LinearLayout hero=a.column();hero.setBackground(a.bg(a.WHITE,24));a.pad(hero,18,14,18,10);page.body.addView(hero,ui.lp(-1,-2));
        amountLabel=a.text("金额",12,a.GRAY,true);hero.addView(amountLabel);
        LinearLayout amountRow=a.row();LinearLayout.LayoutParams ap=ui.lp(-1,66);ap.topMargin=a.dp(4);hero.addView(amountRow,ap);
        amountRow.addView(a.text("¥",26,a.BLACK,true),ui.lp(30,-2));
        amount=a.field("0.00",entry.cents>0?entry.amount():"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setShowSoftInputOnFocus(false);amount.setTextSize(36);amount.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));amount.setBackgroundColor(Color.TRANSPARENT);amount.setSelectAllOnFocus(true);amount.setFontFeatureSettings("tnum");a.pad(amount,8,0,0,0);
        amountRow.addView(amount,new LinearLayout.LayoutParams(0,-1,1));amount.setContentDescription("记账金额");

        ui.space(page.body,14);addTypes();
        LinearLayout form=a.column();form.setBackground(a.bg(a.WHITE,22));a.pad(form,14,12,14,12);LinearLayout.LayoutParams fp=ui.lp(-1,-2);fp.topMargin=a.dp(14);page.body.addView(form,fp);
        TextView noteLabel=a.text("用途 / 备注",12,a.GRAY,true);form.addView(noteLabel);
        description=a.field("用途 / 备注（选填）",combinedDescription(entry),InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        description.setSingleLine(false);description.setMaxLines(4);description.setMinHeight(a.dp(64));description.setGravity(Gravity.CENTER_VERTICAL);a.pad(description,14,10,14,10);
        description.setHint("写点什么（选填）");description.setTextSize(15);LinearLayout.LayoutParams np=ui.lp(-1,-2);np.topMargin=a.dp(8);form.addView(description,np);

        category=a.field("分类",entry.category,InputType.TYPE_CLASS_TEXT);
        LinearLayout meta=a.row();LinearLayout.LayoutParams mp=ui.lp(-1,48);mp.topMargin=a.dp(10);form.addView(meta,mp);
        categoryButton=ui.picker(entry.category);UiKit.picker(a,categoryButton,R.drawable.ic_ui_category);categoryButton.setTextSize(13);
        categoryButton.setMaxLines(1);categoryButton.setEllipsize(TextUtils.TruncateAt.END);compactMeta(categoryButton,R.drawable.ic_ui_category);
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,-1,1);cp.rightMargin=a.dp(5);meta.addView(categoryButton,cp);categoryButton.setOnClickListener(v->{preparePicker();chooseCategory();});
        dateButton=ui.picker(dateLabel());UiKit.picker(a,dateButton,R.drawable.ic_ui_calendar);dateButton.setTextSize(13);
        dateButton.setMaxLines(1);dateButton.setEllipsize(TextUtils.TruncateAt.END);compactMeta(dateButton,R.drawable.ic_ui_calendar);
        LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(0,-1,1);dp.leftMargin=a.dp(5);meta.addView(dateButton,dp);
        dateButton.setOnClickListener(v->{preparePicker();LocalDate date;try{date=LocalDate.parse(entry.date);}catch(Exception ex){date=LocalDate.now();}UiKit.pickDate(a,"账单日期",date,selected->{entry.date=selected.toString();dateButton.setText(dateLabel());});});
        brand=new CarBrandControl(a,description,category,ui.loanBrand(entry));form.addView(brand.root);

        TextView more=UiKit.textAction(a,"更多信息",false);more.setGravity(Gravity.CENTER_VERTICAL);more.setCompoundDrawablesRelative(null,null,UiKit.drawable(a,R.drawable.ic_ui_down,18,a.GRAY),null);page.body.addView(more,ui.lp(-1,42));
        extras=a.column();extras.setBackground(a.bg(a.WHITE,22));a.pad(extras,14,0,14,16);extras.setVisibility(View.GONE);page.body.addView(extras);
        more.setOnClickListener(v->{boolean open=extras.getVisibility()!=View.VISIBLE;extras.setVisibility(open?View.VISIBLE:View.GONE);more.setText(open?"收起信息":"更多信息");});
        if(!entry.imagePath.isEmpty()){TextView compare=ui.picker("对照原始截图");UiKit.picker(a,compare,R.drawable.ic_ui_image);extras.addView(compare,ui.lp(-1,48));compare.setOnClickListener(v->ui.preview(entry.imagePath));}
        EditText time=a.field("例如 12:30（选填）",entry.time,InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_TIME);ui.input(extras,"时间",time,50);
        subscription=a.column();extras.addView(subscription);TextView subButton=ui.picker(entry.subscriptionId.isEmpty()?"不关联":ui.subscriptionName(entry.subscriptionId));UiKit.picker(a,subButton,R.drawable.ic_ui_link);ui.input(subscription,"关联订阅（选填）",subButton,50);subButton.setOnClickListener(v->ui.selectSubscription(entry,subButton));
        refund=a.column();extras.addView(refund);LedgerEntry originalExpense=ui.findPosted(entry.refundOf);TextView refundButton=ui.picker(originalExpense==null?"不关联":originalExpense.title+" · "+ui.money(originalExpense.cents));UiKit.picker(a,refundButton,R.drawable.ic_ui_link);ui.input(refund,"关联原支出（选填）",refundButton,50);refundButton.setOnClickListener(v->ui.selectOriginal(entry,refundButton));
        EditText order=a.field("选填",entry.txnNo,InputType.TYPE_CLASS_TEXT);ui.input(extras,"交易单号",order,50);if(!entry.raw.isEmpty())ui.rawBlock(extras,entry.raw);

        LinearLayout actions=a.row();TextView toggle=UiKit.iconButton(a,R.drawable.ic_ui_keypad,"打开金额数字键盘");LinearLayout.LayoutParams tp=ui.lp(48,52);tp.rightMargin=a.dp(8);actions.addView(toggle,tp);
        TextView save=a.primaryButton(draft?"保存核对结果":"保存账单");actions.addView(save,new LinearLayout.LayoutParams(0,a.dp(52),1));
        keyboard=new AmountKeyboard(a,amount,toggle);page.footer.addView(keyboard.root,ui.lp(-1,-2));ui.space(page.footer,4);page.footer.addView(actions,ui.lp(-1,52));
        description.setOnFocusChangeListener((v,focus)->{if(focus)keyboard.hide();});
        time.setOnFocusChangeListener((v,focus)->{if(focus)keyboard.hide();});order.setOnFocusChangeListener((v,focus)->{if(focus)keyboard.hide();});
        save.setOnClickListener(v->save(time,order));refreshTypes();
    }

    void compactMeta(TextView button,int icon){
        a.pad(button,10,0,10,0);button.setCompoundDrawablesRelative(UiKit.drawable(a,icon,18,a.BLUE),null,null,null);button.setCompoundDrawablePadding(a.dp(8));
    }
    static String combinedDescription(LedgerEntry e){
        String title=e.title.equals("待补充商家")?"":e.title;
        if(e.note.isEmpty()||e.note.equals(title))return title;
        return title.isEmpty()?e.note:title+"\n"+e.note;
    }
    String dateLabel(){
        try{LocalDate d=LocalDate.parse(entry.date);if(d.equals(LocalDate.now()))return "今天";return d.format(DateTimeFormatter.ofPattern(d.getYear()==LocalDate.now().getYear()?"M月d日":"yy/M/d"));}
        catch(Exception ex){return "选择日期";}
    }
    void addTypes(){
        LinearLayout grid=a.column();grid.setBackground(a.bg(a.WHITE,22));a.pad(grid,6,10,6,10);page.body.addView(grid,ui.lp(-1,-2));
        for(int row=0;row<2;row++){
            LinearLayout line=a.row();LinearLayout.LayoutParams rp=ui.lp(-1,64);if(row>0)rp.topMargin=a.dp(6);grid.addView(line,rp);
            for(int col=0;col<4;col++){
                int index=row*4+col;LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);if(col>0)p.leftMargin=a.dp(6);line.addView(tile,p);
                if(index>=LedgerEntry.TYPES.length){tile.setVisibility(View.INVISIBLE);continue;}
                typeTiles[index]=tile;typeIcons[index]=UiKit.icon(a,icons[index],20,a.GRAY);a.pad(typeIcons[index],7,7,7,7);tile.addView(typeIcons[index],ui.lp(34,34));
                typeLabels[index]=a.text(LedgerEntry.TYPES[index],12,a.GRAY,true);LinearLayout.LayoutParams lp=ui.lp(-2,-2);lp.topMargin=a.dp(4);tile.addView(typeLabels[index],lp);
                tile.setContentDescription(LedgerEntry.TYPES[index]);tile.setFocusable(true);UiKit.buttonSemantics(tile);tile.setOnClickListener(v->{entry.type=LedgerEntry.TYPES[index];refreshTypes();});
            }
        }
    }
    void refreshTypes(){
        for(int i=0;i<typeTiles.length;i++){boolean active=LedgerEntry.TYPES[i].equals(entry.type);typeTiles[i].setSelected(active);typeTiles[i].setBackground(UiKit.surface(a,Color.TRANSPARENT,16,0));typeIcons[i].setBackground(a.bg(active?UiKit.INK:a.SURFACE,17));typeIcons[i].setImageDrawable(UiKit.drawable(a,icons[i],20,active?a.WHITE:a.GRAY));typeLabels[i].setTextColor(active?a.BLACK:a.GRAY);}
        amountLabel.setText(entry.type+"金额");subscription.setVisibility(entry.type.equals(LedgerEntry.EXPENSE)?View.VISIBLE:View.GONE);refund.setVisibility(entry.type.equals(LedgerEntry.REFUND)?View.VISIBLE:View.GONE);
    }
    int categoryIcon(String name){
        switch(name){case "购物":return R.drawable.ic_ui_bag;case "交通":return R.drawable.ic_ui_bus;case "车辆":case "车贷":return R.drawable.ic_ui_car;case "通讯":return R.drawable.ic_ui_phone;case "订阅":return R.drawable.ic_ui_repeat;case "医疗":return R.drawable.ic_ui_medical;case "工资收入":return R.drawable.ic_ui_income;default:return R.drawable.ic_ui_category;}
    }
    void chooseCategory(){
        UiKit.Sheet sheet=new UiKit.Sheet(a,"选择分类","",R.drawable.ic_ui_category);
        for(int row=0;row<3;row++){
            LinearLayout line=a.row();LinearLayout.LayoutParams rp=ui.lp(-1,74);rp.bottomMargin=a.dp(8);sheet.body.addView(line,rp);
            for(int col=0;col<4;col++){
                String name=categories[row*4+col];boolean selected=name.equals(category.getText().toString());LinearLayout tile=a.column();tile.setGravity(Gravity.CENTER);tile.setBackground(UiKit.surface(a,selected?UiKit.TINT:a.SURFACE,14,selected?a.BLUE:0));
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-1,1);if(col>0)tp.leftMargin=a.dp(6);line.addView(tile,tp);
                View icon=a.brandFor(name)>0?a.sourceIcon(name,name,30):UiKit.icon(a,categoryIcon(name),26,selected?a.BLUE:a.BLACK);tile.addView(icon,ui.lp(30,30));TextView title=a.text(name,12,selected?a.BLUE:a.BLACK,true);LinearLayout.LayoutParams lp=ui.lp(-2,-2);lp.topMargin=a.dp(6);tile.addView(title,lp);
                tile.setFocusable(true);tile.setSelected(selected);UiKit.buttonSemantics(tile);tile.setOnClickListener(v->{selectCategory(name);sheet.close();});
            }
        }
        EditText custom=a.field("自定义分类（选填）",Arrays.asList(categories).contains(category.getText().toString())?"":category.getText().toString(),InputType.TYPE_CLASS_TEXT);sheet.body.addView(custom,ui.lp(-1,52));
        sheet.action("使用自定义分类",false,()->{String value=custom.getText().toString().trim();if(value.isEmpty()){custom.setError("请填写分类");return;}selectCategory(value);sheet.close();});sheet.show();
    }
    void selectCategory(String value){categoryChosen=true;category.setText(value);categoryButton.setText(value);}
    void save(EditText time,EditText order){
        LedgerEntry candidate=entry.copy();
        String value=amount.getText().toString();if(value.startsWith("."))value="0"+value;if(value.endsWith("."))value=value.substring(0,value.length()-1);
        try{candidate.cents=LedgerEntry.parseCents(value);}catch(Exception ex){amount.setError("请输入大于 0 的金额");keyboard.activate();return;}
        try{LocalDate.parse(candidate.date);}catch(Exception ex){ui.toast("请选择日期");return;}
        String content=description.getText().toString().trim();String[] parts=content.split("\\R",2);
        candidate.category=category.getText().toString().trim();if(candidate.category.isEmpty())candidate.category="其他";
        if(!categoryChosen&&candidate.category.equals("其他")&&!content.isEmpty())candidate.category=BillParser.category(content);
        if(!content.isEmpty()&&content.equals(combinedDescription(entry))&&!entry.title.trim().isEmpty()&&!entry.title.equals("待补充商家")){
            candidate.title=entry.title;candidate.note=entry.note;
        }else{
            candidate.title=content.isEmpty()?(candidate.category.equals("其他")?candidate.type:candidate.category):parts[0].trim();candidate.note=parts.length>1?parts[1].trim():"";
        }
        candidate.carBrand=brand.value();candidate.time=time.getText().toString().trim();
        try{if(!candidate.time.isEmpty()){if(candidate.time.length()==4)candidate.time="0"+candidate.time;candidate.time=LocalTime.parse(candidate.time).toString();}}catch(Exception ex){extras.setVisibility(View.VISIBLE);time.setError("请使用 12:30 或 12:30:00 格式");time.requestFocus();return;}
        candidate.txnNo=order.getText().toString().trim();candidate.warning="";
        if(!candidate.type.equals(LedgerEntry.EXPENSE))candidate.subscriptionId="";if(!candidate.type.equals(LedgerEntry.REFUND))candidate.refundOf="";
        String refundError=LedgerRules.validateRefund(candidate,ui.db.list(false));if(!refundError.isEmpty()){ui.toast(refundError);return;}
        int duplicate=LedgerRules.duplicate(candidate,ui.db.list(false));candidate.selected=duplicate==0;
        Runnable persist=()->{try{ui.db.save(candidate,draft);page.close();done.run();}catch(Exception ex){ui.toast("保存失败，请重试");}};
        if(!draft&&duplicate==2){ui.toast("这笔账单已经入账，请在原记录中修改");return;}
        if(!draft&&duplicate==1)UiKit.confirm(a,"核对重复账单","已存在同日、同金额、同商家的账单，仍保存这笔？","仍然保存",false,persist);else persist.run();
    }
    void preparePicker(){
        View focused=page.dialog.getCurrentFocus();
        if(focused!=null)((android.view.inputmethod.InputMethodManager)a.getSystemService(MainActivity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);
        page.body.requestFocus();
    }
    void show(){
        if(fresh){amount.requestFocus();keyboard.show();if(amount.length()>0)amount.selectAll();}
        else{page.body.requestFocus();keyboard.hide();}
        page.show();
    }
}
