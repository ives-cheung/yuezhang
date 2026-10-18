package com.ives.money;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.File;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Phone UI for actual transactions and a persistent import inbox. */
final class LedgerUi {
    static final int PICK_SCREENSHOTS=71;
    final MainActivity a;
    final LedgerDb db;
    YearMonth month=YearMonth.now();
    BillParser.Context importContext;
    final Handler handler=new Handler(Looper.getMainLooper());
    Runnable ticker;
    TextView importStatus;
    String lastImportReport="";
    final String[] categories={"餐饮","购物","交通","车辆","水电费","房租","通讯","订阅","医疗","工资收入","其他"};
    LedgerUi(MainActivity activity){a=activity;db=new LedgerDb(a);}
    String money(long cents){return a.money.format(BigDecimal.valueOf(cents,2));}
    void toast(String s){Toast.makeText(a,s,Toast.LENGTH_LONG).show();}
    LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w<0?w:a.dp(w),h<0?h:a.dp(h));}
    void space(LinearLayout p,int height){p.addView(new View(a),lp(1,height));}
    void paragraph(LinearLayout p,String s,int size,int color,boolean bold,int top){TextView t=a.text(s,size,color,bold);t.setLineSpacing(a.dp(3),1f);LinearLayout.LayoutParams x=lp(-1,-2);x.topMargin=a.dp(top);p.addView(t,x);}
    TextView secondary(String s){return a.secondaryButton(s);}
    void stop(){if(ticker!=null)handler.removeCallbacks(ticker);}
    void monitor(){stop();if(ScreenshotImporter.running){ticker=new Runnable(){public void run(){if(a.isDestroyed()||a.isFinishing())return;if(!ScreenshotImporter.running){if(a.dailyMode)a.render();return;}if(importStatus!=null)importStatus.setText("  "+ScreenshotImporter.status);handler.postDelayed(this,1200);}};handler.postDelayed(ticker,1200);}}
    void renderFeed(){
        a.feed.removeAllViews();importStatus=null;List<LedgerEntry> records=db.month(month.toString());List<LedgerEntry> drafts=db.list(true);
        LinearLayout section=a.column();a.pad(section,20,0,20,20);a.feed.addView(section);
        LinearLayout dateRow=a.row();TextView prev=UiKit.iconButton(a,R.drawable.ic_ui_back,"上个月");dateRow.addView(prev,lp(44,44));prev.setContentDescription("上个月");prev.setOnClickListener(v->{month=month.minusMonths(1);a.render();});
        TextView title=a.text(month.format(DateTimeFormatter.ofPattern("yyyy年M月",Locale.CHINA)),16,a.BLACK,true);title.setGravity(Gravity.CENTER);title.setCompoundDrawablesRelative(null,null,UiKit.drawable(a,R.drawable.ic_ui_down,16,a.GRAY),null);a.pad(title,12,0,12,0);title.setBackground(UiKit.surface(a,a.WHITE,14,0));dateRow.addView(title,new LinearLayout.LayoutParams(0,a.dp(44),1));title.setOnClickListener(v->chooseMonth());
        TextView next=UiKit.iconButton(a,R.drawable.ic_ui_chevron,"下个月");dateRow.addView(next,lp(44,44));next.setContentDescription("下个月");next.setOnClickListener(v->{month=month.plusMonths(1);a.render();});section.addView(dateRow);
        LinearLayout hero=a.column();hero.setBackground(a.bg(a.BLACK,24));a.pad(hero,20,20,20,20);space(section,6);section.addView(hero,lp(-1,-2));
        hero.addView(a.text("已记录净支出",13,0xffc8cdd2,true));paragraph(hero,money(LedgerRules.netExpense(records)),34,android.graphics.Color.WHITE,true,8);
        long income=0,refund=0;for(LedgerEntry e:records){if(e.type.equals(LedgerEntry.INCOME))income+=e.cents;if(e.type.equals(LedgerEntry.REFUND))refund+=e.cents;}
        LinearLayout stats=a.row();space(hero,14);hero.addView(stats);stats.addView(a.text("收入  "+money(income),12,0xffe1e5e8,false),new LinearLayout.LayoutParams(0,-2,1));stats.addView(a.text("退款  "+money(refund),12,0xffe1e5e8,false));
        LinearLayout shortcuts=a.row();LinearLayout.LayoutParams sp=lp(-1,-2);sp.topMargin=a.dp(16);section.addView(shortcuts,sp);
        LinearLayout imageCard=shortcut(R.drawable.ic_ui_scan,"截图导入","多张账单，一起识别",UiKit.ACCENT,UiKit.TINT);LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,-1,1);left.rightMargin=a.dp(6);shortcuts.addView(imageCard,left);imageCard.setOnClickListener(v->importOptions());
        LinearLayout textCard=shortcut(R.drawable.ic_ui_quick,"文字快记","一句话，记好多笔",0xff5269b1,0xffedf0fb);LinearLayout.LayoutParams right=new LinearLayout.LayoutParams(0,-1,1);right.leftMargin=a.dp(6);shortcuts.addView(textCard,right);textCard.setOnClickListener(v->quickEntry());
        if(ScreenshotImporter.running){LinearLayout busy=a.row();a.pad(busy,14,14,14,14);busy.setBackground(a.bg(UiKit.TINT,16));space(section,14);section.addView(busy,lp(-1,-2));ProgressBar progress=new ProgressBar(a);busy.addView(progress,lp(24,24));TextView message=a.text("  "+ScreenshotImporter.status,13,a.BLUE,true);busy.addView(message);importStatus=message;}
        if(!drafts.isEmpty()){
            LinearLayout pending=a.row();a.pad(pending,16,16,16,16);pending.setBackground(UiKit.surface(a,UiKit.TINT,16,0));pending.addView(a.text("待核对 "+drafts.size()+" 笔账单",14,a.BLUE,true),new LinearLayout.LayoutParams(0,-2,1));pending.addView(a.text("查看 ›",13,a.BLUE,true));space(section,14);section.addView(pending,lp(-1,-2));pending.setOnClickListener(v->review());
        }
        if(!lastImportReport.isEmpty()){paragraph(section,lastImportReport,12,a.GRAY,false,12);}
        LinearLayout head=a.row();space(section,24);section.addView(head);head.addView(a.text("日常流水",19,a.BLACK,true));View stretch=new View(a);head.addView(stretch,new LinearLayout.LayoutParams(0,1,1));head.addView(a.text(records.size()+" 笔",12,a.GRAY,false));
        if(records.isEmpty()){
            LinearLayout empty=a.column();empty.setBackground(a.bg(a.SURFACE,20));empty.setGravity(Gravity.CENTER);a.pad(empty,20,26,20,26);space(section,14);section.addView(empty,lp(-1,-2));ImageView emptyIcon=UiKit.icon(a,R.drawable.ic_ui_scan,28,a.BLUE);emptyIcon.setBackground(a.bg(UiKit.TINT,18));a.pad(emptyIcon,14,14,14,14);empty.addView(emptyIcon,lp(56,56));space(empty,14);empty.addView(a.text("从一张账单截图开始",16,a.BLACK,true));paragraph(empty,"批量选择微信或支付宝截图，核对后入账。",13,a.GRAY,false,8);empty.setOnClickListener(v->importOptions());
        }else{
            String date="";LinearLayout group=null;
            for(LedgerEntry e:records){if(!e.date.equals(date)){
                date=e.date;LocalDate d=LocalDate.parse(date);String label=d.equals(LocalDate.now())?"今天":d.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA));
                paragraph(section,label,13,a.GRAY,true,20);group=a.column();space(section,6);section.addView(group,lp(-1,-2));
            }addEntryRow(group,e);group.addView(a.divider());}
        }
        monitor();
    }
    LinearLayout shortcut(int icon,String title,String desc,int accent,int tint){
        LinearLayout c=a.column();c.setBackground(UiKit.surface(a,a.SURFACE,20,a.LINE));a.pad(c,16,16,16,16);
        LinearLayout top=a.row();ImageView mark=UiKit.icon(a,icon,24,accent);mark.setBackground(a.bg(tint,14));a.pad(mark,9,9,9,9);top.addView(mark,lp(42,42));top.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));top.addView(UiKit.icon(a,R.drawable.ic_ui_chevron,17,a.GRAY),lp(20,20));c.addView(top);
        paragraph(c,title,16,a.BLACK,true,14);paragraph(c,desc,12,a.GRAY,false,5);c.setMinimumHeight(a.dp(142));c.setFocusable(true);c.setContentDescription(title+"，"+desc);UiKit.buttonSemantics(c);return c;
    }
    void chooseMonth(){UiKit.pickDate(a,"选择账单月份",month.atDay(1),date->{month=YearMonth.from(date);a.render();});}
    void addEntryRow(LinearLayout parent,LedgerEntry e){
        LinearLayout r=a.row();a.pad(r,0,14,0,14);parent.addView(r,lp(-1,-2));r.addView(a.sourceIcon(e.title,e.category,loanBrand(e),42),lp(42,42));
        LinearLayout info=a.column();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.leftMargin=a.dp(12);r.addView(info,ip);TextView name=a.text(e.title,14,a.BLACK,true);name.setMaxLines(2);info.addView(name);
        String meta=e.category+(e.time.isEmpty()?"":" · "+e.time)+(e.channel.isEmpty()?"":" · "+e.channel);paragraph(info,meta,11,a.GRAY,false,4);
        LinearLayout amounts=a.column();amounts.setGravity(Gravity.RIGHT);LinearLayout.LayoutParams am=lp(-2,-2);am.leftMargin=a.dp(8);r.addView(amounts,am);
        amounts.addView(a.text((e.type.equals(LedgerEntry.EXPENSE)?"−":e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND)?"+":"")+money(e.cents),15,e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND)?0xff21805b:a.BLACK,true));
        if(!e.type.equals(LedgerEntry.EXPENSE))paragraph(amounts,e.type,11,a.GRAY,false,4);r.setBackground(UiKit.surface(a,a.WHITE,14,0));r.setOnClickListener(v->details(e));
    }
    void label(LinearLayout p,String s){paragraph(p,s,13,a.GRAY,true,18);}
    void input(LinearLayout p,String label,View field,int height){label(p,label);LinearLayout.LayoutParams x=lp(-1,height);x.topMargin=a.dp(8);p.addView(field,x);}
    TextView picker(String label){TextView b=a.text(label,15,a.BLACK,true);UiKit.picker(a,b,0);b.setMinHeight(a.dp(52));return b;}
    final class Page {
        final Dialog dialog;final LinearLayout page,body,footer;final ScrollView scroll;
        Page(String title){
            dialog=new Dialog(a,R.style.MoneyThemeLight);page=a.column();page.setBackgroundColor(a.WHITE);
            LinearLayout bar=a.row();a.pad(bar,12,6,12,10);TextView back=UiKit.iconButton(a,R.drawable.ic_ui_back,"返回");bar.addView(back,lp(48,48));back.setOnClickListener(v->dialog.dismiss());TextView heading=a.text(title,18,a.BLACK,true);heading.setGravity(Gravity.CENTER);bar.addView(heading,new LinearLayout.LayoutParams(0,a.dp(48),1));bar.addView(new View(a),lp(48,48));page.addView(bar);page.addView(a.divider());
            scroll=new ScrollView(a);scroll.setFillViewport(false);body=a.column();a.pad(body,20,8,20,24);scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));footer=a.column();a.pad(footer,20,12,20,12);page.addView(a.divider());page.addView(footer);
        }
        void button(String label,Runnable action){TextView b=a.primaryButton(label);footer.addView(b,lp(-1,52));b.setOnClickListener(v->action.run());}
        void show(){dialog.setContentView(page);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(a.WHITE));w.setStatusBarColor(a.WHITE);w.setNavigationBarColor(a.WHITE);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}dialog.show();if(w!=null)w.setLayout(-1,-1);a.applyInsets(page);}
        void close(){View focused=dialog.getCurrentFocus();if(focused!=null)((InputMethodManager)a.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(focused.getWindowToken(),0);dialog.dismiss();}
    }
    void details(LedgerEntry e){
        Page p=new Page("账单详情");space(p.body,18);TextView iconTitle=a.text(e.title,21,a.BLACK,true);p.body.addView(iconTitle);paragraph(p.body,money(e.cents),36,a.BLACK,true,12);
        info(p.body,"收支类型",e.type);info(p.body,"分类",e.category);if(CarBrand.isLoan(e.title,e.category)||!loanBrand(e).isEmpty())info(p.body,"车辆品牌",CarBrand.resolve(e.title,e.category,loanBrand(e)));info(p.body,"时间",e.date+(e.time.isEmpty()?"":"  "+e.time));info(p.body,"来源",e.channel+(e.payment.isEmpty()?"":" · "+e.payment));
        if(!e.note.isEmpty())info(p.body,"备注",e.note);if(!e.txnNo.isEmpty())info(p.body,"交易单号",e.txnNo);
        if(!e.subscriptionId.isEmpty())info(p.body,"关联订阅",subscriptionName(e.subscriptionId));
        if(!e.refundOf.isEmpty()){LedgerEntry original=findPosted(e.refundOf);info(p.body,"原支出",original==null?"原记录已删除":original.title+" · "+money(original.cents));}
        if(!e.imagePath.isEmpty()){space(p.body,16);TextView image=picker("查看原始截图");UiKit.picker(a,image,R.drawable.ic_ui_image);p.body.addView(image,lp(-1,52));image.setOnClickListener(v->preview(e.imagePath));}
        if(!e.raw.isEmpty())rawBlock(p.body,e.raw);
        p.button("编辑账单",()->{p.close();edit(e,false,()->a.render());});TextView del=UiKit.textAction(a,"删除这笔账单",true);p.footer.addView(del,lp(-1,44));del.setOnClickListener(v->UiKit.confirm(a,"删除账单","删除这笔账单？","删除账单",true,()->{db.delete(e.id);p.close();a.render();}));p.show();
    }
    void info(LinearLayout parent,String key,String value){label(parent,key);paragraph(parent,value,15,a.BLACK,false,5);}
    String loanBrand(LedgerEntry e){if(!e.carBrand.isEmpty())return e.carBrand;for(MainActivity.Sub s:a.items)if(s.id.equals(e.subscriptionId))return s.carBrand;return "";}
    String subscriptionName(String id){for(MainActivity.Sub s:a.items)if(s.id.equals(id))return s.name;return "原订阅已删除";}
    LedgerEntry findPosted(String id){for(LedgerEntry e:db.list(false))if(e.id.equals(id))return e;return null;}
    void rawBlock(LinearLayout parent,String raw){
        space(parent,14);TextView toggle=a.text("识别原文  ▾",13,a.BLUE,true);parent.addView(toggle,lp(-1,44));TextView text=a.text(raw,13,a.GRAY,false);text.setTextIsSelectable(true);text.setVisibility(View.GONE);parent.addView(text,lp(-1,-2));toggle.setOnClickListener(v->text.setVisibility(text.getVisibility()==View.GONE?View.VISIBLE:View.GONE));
    }
    void preview(String path){
        File file=new File(path);if(!file.exists()){toast("原始截图已不存在");return;}Page p=new Page("原始截图");p.body.setPadding(0,0,0,0);
        BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeFile(path,bounds);int sample=1;while(bounds.outWidth/sample>1600||(long)(bounds.outWidth/sample)*(bounds.outHeight/sample)>8000000)sample*=2;
        if(bounds.outWidth<=0||bounds.outHeight<=0){toast("截图无法读取");return;}BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=sample;Bitmap bitmap=BitmapFactory.decodeFile(path,options);if(bitmap==null){toast("截图无法读取");return;}
        HorizontalScrollView horizontal=new HorizontalScrollView(a);ImageView image=new ImageView(a);image.setAdjustViewBounds(true);image.setScaleType(ImageView.ScaleType.FIT_CENTER);image.setImageBitmap(bitmap);horizontal.addView(image);p.body.addView(horizontal,lp(-1,-2));
        int base=a.getResources().getDisplayMetrics().widthPixels;final float[] zoom={1};Runnable resize=()->{image.setLayoutParams(new android.widget.FrameLayout.LayoutParams((int)(base*zoom[0]),(int)(base*zoom[0]*bitmap.getHeight()/bitmap.getWidth())));};resize.run();
        TextView scale=secondary("点击放大 / 还原");p.footer.addView(scale,lp(-1,48));scale.setOnClickListener(v->{zoom[0]=zoom[0]==1?2:1;resize.run();});p.dialog.setOnDismissListener(d->{image.setImageDrawable(null);bitmap.recycle();});p.show();
    }
    void edit(LedgerEntry e,boolean draft,Runnable done){
        Page p=new Page(draft?"核对这笔账单":e.title.isEmpty()?"记一笔":"编辑账单");
        if(draft&&!e.imagePath.isEmpty()){TextView compare=picker("对照原始截图");UiKit.picker(a,compare,R.drawable.ic_ui_image);p.body.addView(compare,lp(-1,48));compare.setOnClickListener(v->preview(e.imagePath));}
        final String[] selectedType={e.type};TextView type=picker(e.type);UiKit.picker(a,type,R.drawable.ic_ui_receipt);input(p.body,"收支类型",type,52);
        EditText amount=a.field("0.00",e.cents>0?e.amount():"",InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setTextSize(28);input(p.body,"金额（¥）",amount,64);
        EditText title=a.field("商家或用途",e.title.equals("待补充商家")?"":e.title,InputType.TYPE_CLASS_TEXT);input(p.body,"商家 / 用途",title,52);
        EditText category=a.field("分类",e.category,InputType.TYPE_CLASS_TEXT);input(p.body,"分类",category,52);
        CarBrandControl brandControl=new CarBrandControl(a,title,category,loanBrand(e));p.body.addView(brandControl.root);
        HorizontalScrollView hs=new HorizontalScrollView(a);hs.setHorizontalScrollBarEnabled(false);LinearLayout chips=a.row();space(p.body,10);hs.addView(chips);p.body.addView(hs,lp(-1,40));for(String cat:categories){TextView c=a.text(cat,12,a.GRAY,true);c.setGravity(Gravity.CENTER);c.setBackground(UiKit.surface(a,a.SURFACE,12,0));a.pad(c,12,0,12,0);LinearLayout.LayoutParams cp=lp(-2,38);cp.rightMargin=a.dp(6);chips.addView(c,cp);c.setOnClickListener(v->category.setText(cat));}
        final String[] date={e.date};TextView dateButton=picker(e.date.isEmpty()?"请选择日期":e.date);UiKit.picker(a,dateButton,R.drawable.ic_ui_calendar);input(p.body,"日期",dateButton,52);dateButton.setOnClickListener(v->{LocalDate d;try{d=LocalDate.parse(date[0]);}catch(Exception x){d=LocalDate.now();}UiKit.pickDate(a,"账单日期",d,selected->{date[0]=selected.toString();dateButton.setText(date[0]);});});
        EditText time=a.field("例如 12:30（选填）",e.time,InputType.TYPE_CLASS_DATETIME|InputType.TYPE_DATETIME_VARIATION_TIME);input(p.body,"时间",time,52);
        EditText channel=a.field("微信 / 支付宝 / 现金",e.channel,InputType.TYPE_CLASS_TEXT);input(p.body,"来源 / 支付账户",channel,52);
        EditText payment=a.field("零钱、银行卡、信用卡等（选填）",e.payment,InputType.TYPE_CLASS_TEXT);input(p.body,"支付方式",payment,52);
        EditText note=a.field("备注（选填）",e.note,InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);note.setSingleLine(false);note.setGravity(Gravity.TOP);a.pad(note,14,12,14,12);input(p.body,"备注",note,80);
        LinearLayout subHolder=a.column();p.body.addView(subHolder);TextView subButton=picker(e.subscriptionId.isEmpty()?"不关联":subscriptionName(e.subscriptionId));UiKit.picker(a,subButton,R.drawable.ic_ui_link);input(subHolder,"关联订阅（选填）",subButton,52);subButton.setOnClickListener(v->selectSubscription(e,subButton));
        LinearLayout refundHolder=a.column();p.body.addView(refundHolder);LedgerEntry original=findPosted(e.refundOf);TextView refundButton=picker(original==null?"不关联":original.title+" · "+money(original.cents));UiKit.picker(a,refundButton,R.drawable.ic_ui_link);input(refundHolder,"关联原支出（选填）",refundButton,52);refundButton.setOnClickListener(v->selectOriginal(e,refundButton));
        TextView kindHint=a.text("",12,a.GRAY,false);paragraph(p.body,"",1,a.GRAY,false,4);p.body.addView(kindHint);
        Runnable kindChange=()->{String k=selectedType[0];subHolder.setVisibility(k.equals(LedgerEntry.EXPENSE)?View.VISIBLE:View.GONE);refundHolder.setVisibility(k.equals(LedgerEntry.REFUND)?View.VISIBLE:View.GONE);kindHint.setText(k.equals(LedgerEntry.TRANSFER)||k.equals(LedgerEntry.REPAYMENT)||k.equals(LedgerEntry.RECHARGE)||k.equals(LedgerEntry.IGNORE)?"此类型保留流水，不计入消费。":"");};
        type.setOnClickListener(v->UiKit.choose(a,"收支类型",LedgerEntry.TYPES,Arrays.asList(LedgerEntry.TYPES).indexOf(selectedType[0]),R.drawable.ic_ui_receipt,i->{selectedType[0]=LedgerEntry.TYPES[i];type.setText(selectedType[0]);kindChange.run();}));kindChange.run();
        EditText order=a.field("选填，用于识别重复账单",e.txnNo,InputType.TYPE_CLASS_TEXT);input(p.body,"交易单号",order,52);if(!e.raw.isEmpty())rawBlock(p.body,e.raw);
        p.button(draft?"保存核对结果":"保存账单",()->{
            long cents;try{cents=LedgerEntry.parseCents(amount.getText().toString());}catch(Exception ex){amount.setError("请输入大于 0、最多两位小数的金额");return;}
            String merchant=title.getText().toString().trim();if(merchant.isEmpty()){title.setError("请填写商家或用途");return;}
            try{LocalDate.parse(date[0]);}catch(Exception ex){toast("请选择日期");return;}
            String timeValue=time.getText().toString().trim();try{if(!timeValue.isEmpty()){if(timeValue.length()==4)timeValue="0"+timeValue;timeValue=LocalTime.parse(timeValue).toString();}}catch(Exception ex){time.setError("请使用 12:30 或 12:30:00 格式");return;}
            e.carBrand=brandControl.value();e.cents=cents;e.title=merchant;e.category=category.getText().toString().trim();if(e.category.isEmpty())e.category=BillParser.category(merchant);e.date=date[0];e.time=timeValue;e.type=selectedType[0];e.channel=channel.getText().toString().trim();e.payment=payment.getText().toString().trim();e.note=note.getText().toString().trim();e.txnNo=order.getText().toString().trim();e.warning="";
            if(!e.type.equals(LedgerEntry.EXPENSE))e.subscriptionId="";if(!e.type.equals(LedgerEntry.REFUND))e.refundOf="";
            String refundError=validateRefund(e,db.list(false));if(!refundError.isEmpty()){toast(refundError);return;}
            int duplicate=LedgerRules.duplicate(e,db.list(false));e.selected=duplicate==0;
            Runnable save=()->{try{db.save(e,draft);p.close();done.run();}catch(Exception ex){toast("保存失败，请重试");}};
            if(!draft&&duplicate==2){toast("这笔账单已经入账，请在原记录中修改");return;}
            if(!draft&&duplicate==1)UiKit.confirm(a,"核对重复账单","已存在同日、同金额、同商家的账单，仍保存这笔？","仍然保存",false,save);else save.run();
        });p.show();
    }
    static String validateRefund(LedgerEntry e,List<LedgerEntry> entries){return LedgerRules.validateRefund(e,entries);}
    void selectSubscription(LedgerEntry e,TextView button){String[] labels=new String[a.items.size()+1];labels[0]="不关联";for(int i=0;i<a.items.size();i++)labels[i+1]=a.items.get(i).name+" · "+a.items.get(i).cycle;int selected=0;for(int i=0;i<a.items.size();i++)if(a.items.get(i).id.equals(e.subscriptionId))selected=i+1;UiKit.choose(a,"关联订阅",labels,selected,R.drawable.ic_ui_link,i->{e.subscriptionId=i==0?"":a.items.get(i-1).id;button.setText(labels[i]);});}
    void selectOriginal(LedgerEntry e,TextView button){List<LedgerEntry> originals=new ArrayList<>();for(LedgerEntry x:db.list(false))if(x.type.equals(LedgerEntry.EXPENSE)&&!x.id.equals(e.id))originals.add(x);String[] labels=new String[originals.size()+1];labels[0]="不关联";for(int i=0;i<originals.size();i++){LedgerEntry x=originals.get(i);labels[i+1]=x.date+"  "+x.title+"  "+money(x.cents);}int selected=0;for(int i=0;i<originals.size();i++)if(originals.get(i).id.equals(e.refundOf))selected=i+1;UiKit.choose(a,"选择原支出",labels,selected,R.drawable.ic_ui_link,i->{e.refundOf=i==0?"":originals.get(i-1).id;button.setText(labels[i]);});}
    void suggestSubscriptions(List<LedgerEntry> entries){
        for(LedgerEntry e:entries)if(e.type.equals(LedgerEntry.EXPENSE)&&e.subscriptionId.isEmpty())for(MainActivity.Sub s:a.items){String n=LedgerEntry.normalized(s.name),title=LedgerEntry.normalized(e.title);if(n.length()>=3&&title.contains(n)&&e.cents==Math.round(s.amount*100)){e.subscriptionId=s.id;break;}}
    }
    Dialog reviewDialog;Runnable reviewRefresh;int reviewPage=0;
    void review(){
        if(reviewDialog!=null&&reviewDialog.isShowing()){reviewRefresh.run();return;}
        if(db.list(true).isEmpty()){toast("暂无待核对账单");return;}Page p=new Page("核对账单");reviewPage=0;reviewDialog=p.dialog;
        final Runnable[] refresh={null};refresh[0]=()->{
            int scrollY=p.scroll.getScrollY();p.body.removeAllViews();p.footer.removeAllViews();List<LedgerEntry> drafts=db.list(true);List<LedgerEntry> posted=db.list(false);List<LedgerEntry> pool=new ArrayList<>(posted);int uncertain=0,dups=0,selected=0;long net=0;
            for(LedgerEntry e:drafts){if(!e.valid())uncertain++;int duplicate=LedgerRules.duplicate(e,pool);if(duplicate>0)dups++;if(duplicate==2&&e.selected){e.selected=false;db.select(e.id,false);}pool.add(e);if(e.selected){selected++;if(e.type.equals(LedgerEntry.EXPENSE))net+=e.cents;else if(e.type.equals(LedgerEntry.REFUND))net-=e.cents;}}
            paragraph(p.body,"共 "+drafts.size()+" 笔 · 重复待核对 "+dups+" 笔 · 待补全 "+uncertain+" 笔",13,a.GRAY,false,8);
            LinearLayout controls=a.row();space(p.body,12);p.body.addView(controls,lp(-1,44));String[] actions={"全选可入账","全不选","补日期"};for(int i=0;i<3;i++){final int act=i;TextView b=secondary(actions[i]);LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-1,1);if(i>0)bp.leftMargin=a.dp(6);controls.addView(b,bp);b.setTextSize(12);b.setCompoundDrawablesRelative(null,null,null,null);a.pad(b,6,0,6,0);b.setOnClickListener(v->{if(act==2){UiKit.pickDate(a,"补全缺失日期",LocalDate.now(),selectedDate->{String date=selectedDate.toString();for(LedgerEntry e:db.list(true))if(e.date.isEmpty()){e.date=date;e.warning=e.warning.replace("缺少日期","").replaceAll("^ · | · $","");db.save(e,true);}refresh[0].run();});}else{List<LedgerEntry> seen=new ArrayList<>(db.list(false));for(LedgerEntry e:db.list(true)){e.selected=act==0&&e.valid()&&LedgerRules.duplicate(e,seen)==0&&(e.type.equals(LedgerEntry.EXPENSE)||e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND));db.select(e.id,e.selected);seen.add(e);}refresh[0].run();}});}
            int pages=Math.max(1,(drafts.size()+39)/40);reviewPage=Math.max(0,Math.min(reviewPage,pages-1));
            if(pages>1){LinearLayout pager=a.row();space(p.body,10);p.body.addView(pager,lp(-1,44));TextView previous=secondary("上一页");pager.addView(previous,new LinearLayout.LayoutParams(0,-1,1));TextView position=a.text((reviewPage+1)+" / "+pages+" 页",12,a.GRAY,true);position.setGravity(Gravity.CENTER);pager.addView(position,lp(90,44));TextView next=secondary("下一页");pager.addView(next,new LinearLayout.LayoutParams(0,-1,1));previous.setOnClickListener(v->{if(reviewPage>0){reviewPage--;p.scroll.scrollTo(0,0);refresh[0].run();}});next.setOnClickListener(v->{if(reviewPage+1<pages){reviewPage++;p.scroll.scrollTo(0,0);refresh[0].run();}});}
            pool=new ArrayList<>(posted);
            for(int index=0;index<drafts.size();index++){
                LedgerEntry e=drafts.get(index);int duplicate=LedgerRules.duplicate(e,pool);pool.add(e);if(index<reviewPage*40||index>=(reviewPage+1)*40)continue;LinearLayout card=a.column();card.setBackground(a.bg(a.SURFACE,18));a.pad(card,12,12,12,12);space(p.body,12);p.body.addView(card,lp(-1,-2));
                LinearLayout line=a.row();card.addView(line);CheckBox check=new CheckBox(a);check.setButtonTintList(android.content.res.ColorStateList.valueOf(a.BLUE));check.setChecked(e.selected);check.setContentDescription("选择账单 "+e.title);check.setEnabled(e.valid()&&duplicate!=2);line.addView(check,lp(44,48));
                LinearLayout content=a.column();line.addView(content,new LinearLayout.LayoutParams(0,-2,1));content.addView(a.text(e.title.isEmpty()?"待补充商家":e.title,14,a.BLACK,true));paragraph(content,(e.cents>0?money(e.cents):"待补充金额")+" · "+e.type,15,a.BLACK,true,3);paragraph(content,(e.date.isEmpty()?"日期待补充":e.date)+(e.time.isEmpty()?"":" "+e.time)+" · "+e.category,11,a.GRAY,false,4);content.setOnClickListener(v->edit(e,true,refresh[0]));
                TextView edit=UiKit.textAction(a,"修改",false);line.addView(edit,lp(52,48));edit.setGravity(Gravity.CENTER);edit.setOnClickListener(v->edit(e,true,refresh[0]));
                String issue=duplicate==2?"已存在同一账单":duplicate==1?"疑似重复，可对照截图后选择":"";if(!e.warning.isEmpty())issue+=(issue.isEmpty()?"":" · ")+e.warning;if(!e.valid()&&issue.isEmpty())issue="请补充金额、日期或商家";
                if(!issue.isEmpty())paragraph(card,issue,11,0xffa46a12,false,8);
                if(!e.subscriptionId.isEmpty())paragraph(card,"关联订阅 · "+subscriptionName(e.subscriptionId),11,a.BLUE,false,6);
                LinearLayout options=a.row();space(card,6);card.addView(options);if(!e.imagePath.isEmpty()){TextView source=UiKit.textAction(a,"对照截图",false);options.addView(source,lp(90,38));source.setGravity(Gravity.CENTER_VERTICAL);source.setOnClickListener(v->preview(e.imagePath));}
                View fill=new View(a);options.addView(fill,new LinearLayout.LayoutParams(0,1,1));TextView remove=UiKit.textAction(a,"移除",true);remove.setGravity(Gravity.CENTER);options.addView(remove,lp(48,38));remove.setOnClickListener(v->{db.delete(e.id);refresh[0].run();});
                check.setOnCheckedChangeListener((b,value)->{db.select(e.id,value);refresh[0].run();});
            }
            paragraph(p.footer,"已选 "+selected+" 笔 · 净支出 "+money(net),12,a.GRAY,false,0);space(p.footer,8);p.button("确认入账（"+selected+"）",()->confirmPost(p,refresh[0]));
            TextView discard=UiKit.textAction(a,"清空待核对",true);p.footer.addView(discard,lp(-1,38));discard.setOnClickListener(v->{if(ScreenshotImporter.running){toast("识别结束后可清空");return;}UiKit.confirm(a,"清空待核对","清空全部待核对账单？已入账记录会保留。","清空待核对",true,()->{db.clearDrafts();p.close();a.render();});});
            p.scroll.post(()->p.scroll.scrollTo(0,scrollY));
        };reviewRefresh=refresh[0];p.dialog.setOnDismissListener(d->{reviewDialog=null;reviewRefresh=null;a.render();});refresh[0].run();p.show();
    }
    void confirmPost(Page p,Runnable refresh){
        List<LedgerEntry> drafts=db.list(true),pool=new ArrayList<>(db.list(false));int selected=0,possible=0;LedgerEntry first=null;
        for(LedgerEntry e:drafts)if(e.selected){selected++;if(first==null)first=e;if(!e.valid()){toast("请先补齐已选账单");return;}String refundError=validateRefund(e,pool);if(!refundError.isEmpty()){toast(refundError);return;}if(LedgerRules.duplicate(e,pool)==1)possible++;pool.add(e);}
        if(selected==0){toast("请选择要入账的记录");return;}final LedgerEntry chosen=first;
        Runnable post=()->{try{int[] result=db.postSelected();month=YearMonth.from(LocalDate.parse(chosen.date));a.dailyMode=true;a.walletMode=false;p.close();a.render();toast("已入账 "+result[0]+" 笔"+(result[1]>0?"，跳过重复 "+result[1]+" 笔":""));}catch(Exception ex){toast(ex.getMessage()==null?"保存失败，请重试":ex.getMessage());refresh.run();}};
        if(possible>0)UiKit.confirm(a,"核对重复账单","已选记录中有 "+possible+" 笔疑似重复，仍然入账？","仍然入账",false,post);else post.run();
    }
    void importOptions(){
        if(ScreenshotImporter.running){toast("正在识别，请稍候");return;}
        UiKit.Sheet sheet=new UiKit.Sheet(a,"导入账单截图","选择来源和日期，再从相册批量选择账单截图。",R.drawable.ic_ui_scan);
        LinearLayout content=sheet.body;android.content.SharedPreferences prefs=a.getSharedPreferences("ledger_options",Context.MODE_PRIVATE);LocalDate now=LocalDate.now();
        String[] channels={"自动识别","微信","支付宝","其他"};final String[] channel={prefs.getString("channel","自动识别")};
        label(content,"账单来源");LinearLayout sourceGrid=a.column();space(content,8);content.addView(sourceGrid);
        TextView[] channelButtons=new TextView[4];Runnable sourceStyle=()->{for(int i=0;i<4;i++){boolean active=channels[i].equals(channel[0]);channelButtons[i].setTextColor(active?UiKit.ACCENT:a.BLACK);channelButtons[i].setBackground(UiKit.surface(a,active?UiKit.TINT:a.SURFACE,14,active?UiKit.ACCENT:a.LINE));channelButtons[i].setSelected(active);}};
        for(int row=0;row<2;row++){
            LinearLayout line=a.row();LinearLayout.LayoutParams linep=lp(-1,48);linep.bottomMargin=a.dp(8);sourceGrid.addView(line,linep);
            for(int col=0;col<2;col++){int i=row*2+col;TextView b=a.text(channels[i],14,a.BLACK,true);b.setGravity(Gravity.CENTER);b.setFocusable(true);UiKit.buttonSemantics(b);channelButtons[i]=b;LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-1,1);if(col==0)bp.rightMargin=a.dp(5);else bp.leftMargin=a.dp(5);line.addView(b,bp);b.setOnClickListener(v->{channel[0]=channels[i];sourceStyle.run();});}
        }sourceStyle.run();
        LinearLayout dates=a.row();content.addView(dates);LinearLayout yearCol=a.column(),monthCol=a.column();LinearLayout.LayoutParams yp=new LinearLayout.LayoutParams(0,-2,1);yp.rightMargin=a.dp(6);dates.addView(yearCol,yp);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(0,-2,1);mp.leftMargin=a.dp(6);dates.addView(monthCol,mp);
        EditText year=a.field("年份",String.valueOf(prefs.getInt("year",now.getYear())),InputType.TYPE_CLASS_NUMBER);input(yearCol,"截图年份",year,52);
        EditText monthField=a.field("月份",String.valueOf(prefs.getInt("month",now.getMonthValue())),InputType.TYPE_CLASS_NUMBER);input(monthCol,"截图月份",monthField,52);space(content,12);
        sheet.action("从相册选择",false,()->{
            int y,m;try{y=Integer.parseInt(year.getText().toString());if(y<2000||y>now.getYear()+1)throw new IllegalArgumentException();}catch(Exception ex){year.setError("请填写有效年份");year.requestFocus();return;}
            try{m=Integer.parseInt(monthField.getText().toString());if(m<1||m>12)throw new IllegalArgumentException();}catch(Exception ex){monthField.setError("请输入 1–12 月");monthField.requestFocus();return;}
            prefs.edit().putInt("year",y).putInt("month",m).putString("channel",channel[0]).apply();importContext=new BillParser.Context(y,m,channel[0],now);sheet.close();pickImages();
        });sheet.cancel("取消");sheet.show();
    }
    void pickImages(){
        Intent intent;if(Build.VERSION.SDK_INT>=33){intent=new Intent(MediaStore.ACTION_PICK_IMAGES);intent.setType("image/*");intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX,Math.min(30,MediaStore.getPickImagesMaxLimit()));}else{intent=new Intent(Intent.ACTION_GET_CONTENT);intent.setType("image/*");intent.addCategory(Intent.CATEGORY_OPENABLE);intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);}
        try{a.startActivityForResult(intent,PICK_SCREENSHOTS);}catch(ActivityNotFoundException ex){intent=new Intent(Intent.ACTION_GET_CONTENT);intent.setType("image/*");intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);try{a.startActivityForResult(intent,PICK_SCREENSHOTS);}catch(ActivityNotFoundException x){toast("未找到可用的相册应用");}}
    }
    void activityResult(int resultCode,Intent data){
        if(resultCode!=Activity.RESULT_OK||data==null)return;List<Uri> uris=new ArrayList<>();if(data.getClipData()!=null){for(int i=0;i<data.getClipData().getItemCount()&&uris.size()<30;i++){Uri u=data.getClipData().getItemAt(i).getUri();if(!uris.contains(u))uris.add(u);}}else if(data.getData()!=null)uris.add(data.getData());if(uris.isEmpty())return;
        if(importContext==null){android.content.SharedPreferences p=a.getSharedPreferences("ledger_options",Context.MODE_PRIVATE);LocalDate now=LocalDate.now();importContext=new BillParser.Context(p.getInt("year",now.getYear()),p.getInt("month",now.getMonthValue()),p.getString("channel","自动识别"),now);}
        boolean started=ScreenshotImporter.start(a,uris,importContext,(images,rows,failures)->{
            if(a.isDestroyed()||a.isFinishing())return;List<LedgerEntry> drafts=db.list(true);suggestSubscriptions(drafts);for(LedgerEntry e:drafts)db.save(e,true);
            lastImportReport="已识别 "+images+" 张截图，生成 "+rows+" 条待核对记录"+(failures.isEmpty()?"":"；失败 "+failures.size()+" 张");
            a.render();if(!failures.isEmpty()){UiKit.Sheet message=new UiKit.Sheet(a,"部分截图未识别",android.text.TextUtils.join("\n",failures),R.drawable.ic_ui_alert);message.action("查看核对清单",false,()->{message.close();review();});message.show();}else review();
        });if(!started)toast("正在识别，请稍后再导入");a.render();
    }
    void quickEntry(){
        Page p=new Page("文字快记");space(p.body,12);LinearLayout intro=a.row();a.pad(intro,16,16,16,16);intro.setBackground(a.bg(UiKit.TINT,20));intro.addView(UiKit.icon(a,R.drawable.ic_ui_quick,28,a.BLUE),lp(32,32));LinearLayout words=a.column();LinearLayout.LayoutParams wp=new LinearLayout.LayoutParams(0,-2,1);wp.leftMargin=a.dp(12);intro.addView(words,wp);words.addView(a.text("一句话记多笔",18,a.BLACK,true));paragraph(words,"午饭28，停车10；收入工资8000",12,a.GRAY,false,6);p.body.addView(intro,lp(-1,-2));
        EditText text=a.field("输入或粘贴内容，每笔用逗号或换行分隔", "",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);text.setSingleLine(false);text.setGravity(Gravity.TOP);a.pad(text,15,15,15,15);input(p.body,"记账内容",text,170);
        TextView paste=secondary("粘贴文字");space(p.body,10);p.body.addView(paste,lp(-1,48));paste.setOnClickListener(v->{android.content.ClipboardManager manager=(android.content.ClipboardManager)a.getSystemService(Context.CLIPBOARD_SERVICE);ClipData clip=manager.getPrimaryClip();if(clip!=null&&clip.getItemCount()>0)text.setText(clip.getItemAt(0).coerceToText(a));else toast("剪贴板里没有文字");});
        final LocalDate[] date={LocalDate.now()};TextView dateButton=picker(date[0].toString());UiKit.picker(a,dateButton,R.drawable.ic_ui_calendar);input(p.body,"默认日期",dateButton,52);dateButton.setOnClickListener(v->UiKit.pickDate(a,"默认记账日期",date[0],selected->{date[0]=selected;dateButton.setText(selected.toString());}));
        p.button("生成核对清单",()->{List<LedgerEntry> entries=BillParser.quick(text.getText().toString(),date[0]);if(entries.isEmpty()){text.setError("请填写记账内容");return;}String batch=UUID.randomUUID().toString();for(LedgerEntry e:entries)e.batchId=batch;suggestSubscriptions(entries);db.addDrafts(entries);p.close();review();});p.show();
    }
}
