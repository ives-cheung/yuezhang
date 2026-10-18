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
        LinearLayout section=a.column();a.pad(section,20,10,20,20);a.feed.addView(section);
        LinearLayout dateRow=a.row();section.addView(dateRow,lp(-1,48));
        LinearLayout monthControl=a.row();a.pad(monthControl,12,0,10,0);monthControl.setBackground(UiKit.glass(a,24,false));
        LinearLayout.LayoutParams monthp=new LinearLayout.LayoutParams(0,a.dp(48),1);monthp.rightMargin=a.dp(8);dateRow.addView(monthControl,monthp);
        TextView title=a.text(month.format(DateTimeFormatter.ofPattern("yyyy年M月",Locale.CHINA)),17,a.BLACK,true);title.setGravity(Gravity.CENTER_VERTICAL);title.setSingleLine(true);title.setMinWidth(0);title.setAutoSizeTextTypeUniformWithConfiguration(12,17,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        monthControl.addView(title,new LinearLayout.LayoutParams(0,-1,1));ImageView down=UiKit.icon(a,R.drawable.ic_ui_down,14,a.GRAY);LinearLayout.LayoutParams downp=lp(18,18);downp.leftMargin=a.dp(6);monthControl.addView(down,downp);
        monthControl.setFocusable(true);monthControl.setContentDescription(title.getText()+"，选择账单月份");UiKit.buttonSemantics(monthControl);monthControl.setOnClickListener(v->chooseMonth());
        TextView prev=UiKit.iconButton(a,R.drawable.ic_ui_back,"上个月"),next=UiKit.iconButton(a,R.drawable.ic_ui_chevron,"下个月");
        for(TextView b:new TextView[]{prev,next}){b.setBackground(UiKit.surface(a,android.graphics.Color.TRANSPARENT,22,0));dateRow.addView(b,lp(44,48));}
        prev.setOnClickListener(v->{month=month.minusMonths(1);a.render();});next.setOnClickListener(v->{month=month.plusMonths(1);a.render();});
        paragraph(section,"净支出",12,a.GRAY,false,10);
        TextView total=a.text(money(LedgerRules.netExpense(records)),28,a.BLACK,true);total.setMaxLines(1);total.setAutoSizeTextTypeUniformWithConfiguration(18,28,1,android.util.TypedValue.COMPLEX_UNIT_SP);LinearLayout.LayoutParams totalp=lp(-1,48);section.addView(total,totalp);
        long income=0,refund=0;for(LedgerEntry e:records){if(e.type.equals(LedgerEntry.INCOME))income+=e.cents;if(e.type.equals(LedgerEntry.REFUND))refund+=e.cents;}
        TextView stats=a.text("收入 "+money(income)+"   ·   退款 "+money(refund),12,a.GRAY,false);section.addView(stats,lp(-1,-2));
        LinearLayout tools=a.row();tools.setBackground(a.bg(a.WHITE,14));LinearLayout.LayoutParams toolp=lp(-1,44);toolp.topMargin=a.dp(18);section.addView(tools,toolp);
        LinearLayout image=compactShortcut(R.drawable.ic_ui_scan,"截图导入"),text=compactShortcut(R.drawable.ic_ui_quick,"文字快记");tools.addView(image,new LinearLayout.LayoutParams(0,-1,1));View split=new View(a);split.setBackgroundColor(a.LINE);tools.addView(split,lp(1,18));tools.addView(text,new LinearLayout.LayoutParams(0,-1,1));image.setOnClickListener(v->importOptions());text.setOnClickListener(v->quickEntry());
        if(ScreenshotImporter.running){LinearLayout busy=a.row();space(section,12);section.addView(busy,lp(-1,36));ProgressBar progress=new ProgressBar(a);busy.addView(progress,lp(18,18));TextView message=a.text("  "+ScreenshotImporter.status,12,a.GRAY,false);busy.addView(message,new LinearLayout.LayoutParams(0,-2,1));importStatus=message;}
        if(!drafts.isEmpty()){
            LinearLayout pending=a.row();a.pad(pending,0,0,0,0);pending.setBackground(UiKit.surface(a,a.WHITE,12,0));LinearLayout.LayoutParams pp=lp(-1,44);pp.topMargin=a.dp(6);section.addView(pending,pp);pending.addView(UiKit.icon(a,R.drawable.ic_ui_checklist,18,a.BLUE),lp(22,22));TextView label=a.text("待核对 "+drafts.size()+" 笔",13,a.BLUE,true);LinearLayout.LayoutParams labelp=new LinearLayout.LayoutParams(0,-2,1);labelp.leftMargin=a.dp(8);pending.addView(label,labelp);pending.addView(UiKit.icon(a,R.drawable.ic_ui_chevron,16,a.GRAY),lp(22,22));pending.setOnClickListener(v->review());
        }
        space(section,14);
        LinearLayout head=a.row();LinearLayout.LayoutParams headp=lp(-1,52);headp.topMargin=a.dp(6);section.addView(head,headp);head.addView(a.text("日常流水",17,a.BLACK,true));TextView count=a.text(records.size()+" 笔",12,a.GRAY,false);LinearLayout.LayoutParams countp=new LinearLayout.LayoutParams(0,-2,1);countp.leftMargin=a.dp(10);head.addView(count,countp);
        ImageView calendar=UiKit.iconAction(a,R.drawable.ic_ui_calendar,"查看收支日历",false);calendar.setBackground(UiKit.surface(a,a.WHITE,22,0));head.addView(calendar,lp(48,48));calendar.setOnClickListener(v->new FlowCalendar(this).show());
        if(records.isEmpty()){
            TextView empty=a.text("本月暂无流水",14,a.GRAY,false);empty.setGravity(Gravity.CENTER);section.addView(empty,lp(-1,128));
        }else{
            String date="";LinearLayout group=null;
            for(LedgerEntry e:records){if(!e.date.equals(date)){
                date=e.date;LocalDate d=LocalDate.parse(date);String label=d.equals(LocalDate.now())?"今天":d.format(DateTimeFormatter.ofPattern("M月d日 EEEE",Locale.CHINA));
                paragraph(section,label,12,a.GRAY,false,16);group=a.groupedList();space(section,8);section.addView(group,lp(-1,-2));
            }else group.addView(a.insetDivider());addEntryRow(group,e);}
        }
        monitor();
    }
    LinearLayout compactShortcut(int icon,String label){
        LinearLayout button=a.row();button.setGravity(Gravity.CENTER);button.setBackground(UiKit.surface(a,android.graphics.Color.TRANSPARENT,14,0));button.addView(UiKit.icon(a,icon,19,a.BLUE),lp(22,22));TextView title=a.text(label,13,a.BLACK,true);LinearLayout.LayoutParams tp=lp(-2,-2);tp.leftMargin=a.dp(8);button.addView(title,tp);button.setFocusable(true);button.setContentDescription(label);UiKit.buttonSemantics(button);return button;
    }
    void chooseMonth(){MonthPicker.show(a,month,selected->{month=selected;a.render();});}
    void addEntryRow(LinearLayout parent,LedgerEntry e){addEntryRow(parent,e,()->details(e));}
    void addEntryRow(LinearLayout parent,LedgerEntry e,Runnable open){
        LinearLayout r=a.row();a.pad(r,0,12,0,12);parent.addView(r,lp(-1,-2));r.addView(a.sourceIcon(e.title,e.category,loanBrand(e),34),lp(34,34));
        LinearLayout info=a.column();LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.leftMargin=a.dp(12);r.addView(info,ip);TextView name=a.text(e.title,14,a.BLACK,false);name.setMaxLines(2);info.addView(name);
        String meta=e.category+(e.time.isEmpty()?"":" · "+e.time);paragraph(info,meta,11,a.GRAY,false,4);
        LinearLayout amounts=a.column();amounts.setGravity(Gravity.RIGHT);LinearLayout.LayoutParams am=lp(-2,-2);am.leftMargin=a.dp(12);r.addView(amounts,am);
        TextView amount=a.text((e.type.equals(LedgerEntry.EXPENSE)?"−":e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND)?"+":"")+money(e.cents),15,e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND)?0xff21805b:a.BLACK,true);amount.setSingleLine(true);amount.setMaxWidth(a.dp(146));amount.setAutoSizeTextTypeUniformWithConfiguration(10,15,1,android.util.TypedValue.COMPLEX_UNIT_SP);amounts.addView(amount);
        if(!e.type.equals(LedgerEntry.EXPENSE))paragraph(amounts,e.type,11,a.GRAY,false,4);r.setBackground(UiKit.surface(a,a.WHITE,14,0));r.setOnClickListener(v->open.run());
    }
    void label(LinearLayout p,String s){paragraph(p,s,13,a.GRAY,true,18);}
    void input(LinearLayout p,String label,View field,int height){label(p,label);LinearLayout.LayoutParams x=lp(-1,height);x.topMargin=a.dp(8);p.addView(field,x);}
    TextView picker(String label){TextView b=a.text(label,15,a.BLACK,true);UiKit.picker(a,b,0);b.setMinHeight(a.dp(52));return b;}
    final class Page {
        final Dialog dialog;final LinearLayout page,body,footer;final ScrollView scroll;final TextView heading;
        Page(String title){
            dialog=new Dialog(a,R.style.MoneyThemeLight);page=a.column();page.setBackgroundColor(a.WHITE);
            LinearLayout bar=a.row();a.pad(bar,12,6,12,10);TextView back=UiKit.iconButton(a,R.drawable.ic_ui_back,"返回");bar.addView(back,lp(48,48));back.setOnClickListener(v->close());heading=a.text(title,17,a.BLACK,true);heading.setGravity(Gravity.CENTER);bar.addView(heading,new LinearLayout.LayoutParams(0,a.dp(48),1));bar.addView(new View(a),lp(48,48));page.addView(bar);
            scroll=new ScrollView(a);scroll.setFillViewport(false);body=a.column();a.pad(body,20,8,20,24);scroll.addView(body);page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));footer=a.column();a.pad(footer,20,12,20,12);page.addView(footer);
        }
        void button(String label,Runnable action){TextView b=a.primaryButton(label);footer.addView(b,lp(-1,52));b.setOnClickListener(v->action.run());}
        void replaceContent(String title){heading.setText(title);body.removeAllViews();footer.removeAllViews();scroll.scrollTo(0,0);}
        void show(){if(dialog.isShowing())return;dialog.setContentView(page);a.applyInsets(page);Window w=dialog.getWindow();if(w!=null){w.setBackgroundDrawable(new ColorDrawable(a.WHITE));w.setStatusBarColor(a.WHITE);w.setNavigationBarColor(a.WHITE);w.setWindowAnimations(0);w.setLayout(-1,-1);w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE|WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);}dialog.show();if(w!=null)w.setLayout(-1,-1);}
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
        LinearLayout actions=a.row();actions.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);p.footer.addView(actions,lp(-1,52));
        ImageView edit=UiKit.iconAction(a,R.drawable.ic_ui_edit,"编辑账单",false);edit.setTooltipText("编辑账单");actions.addView(edit,lp(52,52));edit.setOnClickListener(v->new EntryEditor(this,e,false,()->a.render(),p).show());
        ImageView del=UiKit.iconAction(a,R.drawable.ic_ui_trash,"删除账单",true);del.setTooltipText("删除账单");LinearLayout.LayoutParams dp=lp(52,52);dp.leftMargin=a.dp(12);actions.addView(del,dp);del.setOnClickListener(v->UiKit.confirm(a,"删除账单","删除这笔账单？","删除账单",true,()->{db.delete(e.id);p.close();a.render();}));p.show();
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
    void edit(LedgerEntry e,boolean draft,Runnable done){new EntryEditor(this,e,draft,done).show();}
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
                String issue=duplicate==2?"已存在同一账单":duplicate==1?"疑似重复，可对照截图后选择":"";if(!e.warning.isEmpty())issue+=(issue.isEmpty()?"":" · ")+e.warning;if(!e.valid()&&issue.isEmpty())issue="请补充金额、日期或商家";
                if(!issue.isEmpty())paragraph(card,issue,11,0xffa46a12,false,8);
                if(!e.subscriptionId.isEmpty())paragraph(card,"关联订阅 · "+subscriptionName(e.subscriptionId),11,a.BLUE,false,6);
                LinearLayout options=a.row();space(card,12);card.addView(options,lp(-1,48));
                if(!e.imagePath.isEmpty()){
                    TextView source=UiKit.textAction(a,"对照截图",false);source.setSingleLine(true);source.setEllipsize(android.text.TextUtils.TruncateAt.END);source.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);a.pad(source,8,0,8,0);options.addView(source,new LinearLayout.LayoutParams(0,-1,1));source.setOnClickListener(v->preview(e.imagePath));
                }else options.addView(new View(a),new LinearLayout.LayoutParams(0,1,1));
                ImageView edit=UiKit.iconAction(a,R.drawable.ic_ui_edit,"修改账单："+e.title,false);LinearLayout.LayoutParams editp=lp(48,48);editp.leftMargin=a.dp(8);options.addView(edit,editp);edit.setOnClickListener(v->edit(e,true,refresh[0]));
                ImageView remove=UiKit.iconAction(a,R.drawable.ic_ui_trash,"移除待核对账单："+e.title,true);LinearLayout.LayoutParams removep=lp(48,48);removep.leftMargin=a.dp(8);options.addView(remove,removep);remove.setOnClickListener(v->{db.delete(e.id);refresh[0].run();});
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
