package com.ives.money;
import java.time.*;
import java.util.*;

public final class LedgerCoreChecks {
    static int checks;
    static void check(boolean v,String message){checks++;if(!v)throw new AssertionError(message);}
    static BillParser.Line line(String s,int x,int y,int right,int h){return new BillParser.Line(s,x,y,right,y+h);}
    static LedgerEntry entry(String title,String amount,String type){LedgerEntry e=new LedgerEntry();e.title=title;e.cents=LedgerEntry.parseCents(amount);e.type=type;e.date="2026-10-18";return e;}
    public static void main(String[] args){
        check(LedgerEntry.parseCents("0.10")==10,"decimal cents");check(LedgerEntry.parseCents("¥1,234.56")==123456,"currency and grouping");
        for(String bad:new String[]{"0","-12","NaN","1.234","9999999999999999999999999"}){boolean rejected=false;try{LedgerEntry.parseCents(bad);}catch(Exception e){rejected=true;}check(rejected,"reject "+bad);}
        LedgerEntry purchase=entry("水果","12",LedgerEntry.EXPENSE),repay=entry("信用卡还款","200",LedgerEntry.REPAYMENT),transfer=entry("支付宝转账","500",LedgerEntry.TRANSFER),refund=entry("水果退款","2",LedgerEntry.REFUND);
        check(LedgerRules.netExpense(Arrays.asList(purchase,repay,transfer,refund))==1000,"purchase counts once; repayment and transfer excluded; refund offsets");
        LedgerEntry same=entry("水果","12",LedgerEntry.EXPENSE);check(LedgerRules.possible(purchase,same),"suspect matching day merchant amount");purchase.time="12:30";same.time="15:30";check(!LedgerRules.possible(purchase,same),"distinct known times");
        purchase.sourceKey="imagehash:0";same.sourceKey="imagehash:0";check(LedgerRules.exact(purchase,same),"same source row");same.sourceKey="imagehash:1";same.txnNo=purchase.txnNo="42000000000001";same.channel=purchase.channel="微信";check(LedgerRules.exact(purchase,same),"same provider transaction ID");same.channel="支付宝";check(!LedgerRules.exact(purchase,same),"order numbers scoped by provider");
        List<LedgerEntry> quick=BillParser.quick("午饭28，停车10；收入工资8000",LocalDate.of(2026,10,18));check(quick.size()==3,"quick multi entries");check(quick.get(0).cents==2800&&quick.get(0).category.equals("餐饮"),"meal classification");check(quick.get(1).category.equals("车辆"),"parking classification");check(quick.get(2).type.equals(LedgerEntry.INCOME)&&quick.get(2).cents==800000,"income quick entry");
        quick=BillParser.quick("昨天 午餐28.50\n2025-03-02 房租2000",LocalDate.of(2026,10,18));check(quick.get(0).date.equals("2026-10-17"),"relative quick date");check(quick.get(1).date.equals("2025-03-02"),"explicit quick year");
        quick=BillParser.quick("午饭不知道",LocalDate.of(2026,10,18));check(!quick.get(0).selected&&quick.get(0).cents==0,"unknown quick amount remains draft");
        BillParser.Context ctx=new BillParser.Context(2026,10,"自动识别",LocalDate.of(2026,10,18));
        List<BillParser.Line> list=Arrays.asList(line("微信支付",20,10,250,30),line("本月支出 ¥2000.00",20,70,800,30),line("2026年10月18日",20,120,400,25),line("水果店",40,200,450,30),line("-12.00",790,200,980,30),line("13:05",40,240,220,20),line("停车费",40,340,450,30),line("-10.00",790,340,980,30),line("15:00",40,380,220,20));
        List<LedgerEntry> entries=BillParser.parse(list,1080,500,ctx);check(entries.size()==2,"exclude summary from list");check(entries.get(0).title.equals("水果店")&&entries.get(1).title.equals("停车费"),"row merchant association");check(entries.get(0).date.equals("2026-10-18")&&entries.get(1).date.equals("2026-10-18"),"carry date header");check(entries.get(0).time.equals("13:05"),"time below row");check(entries.get(1).channel.equals("微信"),"global platform inherited");
        List<BillParser.Line> detail=Arrays.asList(line("账单详情",20,0,250,25),line("水果店",200,100,500,30),line("-12.00",250,160,700,60),line("支付成功",220,240,500,25),line("商品说明",20,300,230,25),line("水果",300,300,600,25),line("交易时间 2026-10-18 12:03:04",20,360,950,25),line("支付方式 信用卡",20,420,800,25),line("交易单号 420000000000000001",20,470,980,25));
        entries=BillParser.parse(detail,1080,600,ctx);check(entries.size()==1,"detail only one entry");LedgerEntry e=entries.get(0);check(e.cents==1200&&e.title.equals("水果"),"detail amount and item label");check(e.date.equals("2026-10-18")&&e.time.equals("12:03:04"),"detail timestamp");check(e.type.equals(LedgerEntry.EXPENSE),"credit card purchase is an expense");check(e.txnNo.equals("420000000000000001"),"detail order ID");
        check(BillParser.type("信用卡还款", "-").equals(LedgerEntry.REPAYMENT),"repayment excluded");check(BillParser.type("余额充值", "-").equals(LedgerEntry.RECHARGE),"wallet recharge excluded");check(BillParser.type("手机话费充值", "-").equals(LedgerEntry.EXPENSE),"phone bill still consumption");check(BillParser.type("商户退款成功", "+").equals(LedgerEntry.REFUND),"refund is not income");
        check(BillParser.extractDate("2月29日",new BillParser.Context(2025,2,"",LocalDate.of(2025,3,1))).isEmpty(),"invalid leap date not invented");check(BillParser.extractDate("10月02日",new BillParser.Context(2025,10,"",LocalDate.of(2026,10,18))).equals("2025-10-02"),"import context supplies explicit chosen year");
        entries=BillParser.parse(Arrays.asList(line("水果店",20,50,300,30),line("-12.00",700,50,960,30)),1080,200,ctx);check(entries.get(0).date.isEmpty()&&!entries.get(0).selected,"missing date never silently assigned");
        entries=BillParser.parse(Collections.singletonList(line("无法识别",20,50,300,30)),1080,200,ctx);check(entries.size()==1&&!entries.get(0).valid()&&!entries.get(0).selected,"unparsed source retained for correction");
        check(BillParser.type("支付成功 已全额退款", "-").equals(LedgerEntry.EXPENSE),"original refunded purchase remains expense");
        check(BillParser.type("转账退回", "+").equals(LedgerEntry.TRANSFER),"transfer reversal not consumption refund");
        check(BillParser.type("车贷还款", "-").equals(LedgerEntry.EXPENSE),"car loan budget retains expense treatment");
        LedgerEntry partial=entry("水果退款","4",LedgerEntry.REFUND);partial.refundOf=purchase.id;refund.refundOf=purchase.id;
        check(LedgerRules.validateRefund(partial,Arrays.asList(purchase,refund)).isEmpty(),"valid partial refund");partial.cents=1100;
        check(!LedgerRules.validateRefund(partial,Arrays.asList(purchase,refund)).isEmpty(),"prevent over-refund across records");
        check(LedgerRules.validateRefund(refund,Arrays.asList(purchase,refund)).isEmpty(),"editing refund excludes itself from total");
        List<BillParser.Line> headers=Arrays.asList(line("2026年10月18日",20,70,400,25),line("餐厅",40,150,450,30),line("-28.00",790,150,980,30),line("2026年10月17日",20,240,400,25),line("咖啡",40,350,450,30),line("-15.00",790,350,980,30));
        entries=BillParser.parse(headers,1080,500,ctx);check(entries.get(0).date.equals("2026-10-18")&&entries.get(1).date.equals("2026-10-17"),"later date header cannot change earlier row");
        entries=BillParser.parse(Arrays.asList(line("支付宝",20,10,300,30),line("支出 123.00",20,50,450,30),line("收入 456.00",550,50,980,30),line("水果店",20,150,450,30),line("-12.00",790,150,980,30),line("10月18日 12:30",20,190,450,25)),1080,400,ctx);check(entries.size()==1&&entries.get(0).cents==1200,"exclude plain income and expense header totals");
        entries=BillParser.parse(Arrays.asList(line("余额充值",20,150,450,30),line("-200.00",790,150,980,30),line("10月18日 12:30",20,190,450,25)),1080,400,ctx);check(entries.size()==1&&entries.get(0).type.equals(LedgerEntry.RECHARGE)&&!entries.get(0).selected,"recharge retained as excluded draft, not a balance summary");
        entries=BillParser.parse(Arrays.asList(line("2025年12月",20,20,400,25),line("水果店",20,150,450,30),line("-12.00",790,150,980,30),line("12月18日 12:30",20,190,450,25)),1080,400,ctx);check(entries.get(0).date.equals("2025-12-18"),"visible year overrides import fallback");
        LedgerEntry changedMerchant=entry("水果另一名称","12",LedgerEntry.EXPENSE);changedMerchant.channel=purchase.channel;changedMerchant.txnNo=purchase.txnNo;
        check(!LedgerRules.exact(purchase,changedMerchant)&&LedgerRules.possible(purchase,changedMerchant),"changed order title gets review instead of automatic deletion");
        LedgerEntry refundedOrder=entry("水果","2",LedgerEntry.REFUND);refundedOrder.txnNo=purchase.txnNo;refundedOrder.channel=purchase.channel;
        check(!LedgerRules.exact(purchase,refundedOrder)&&!LedgerRules.possible(purchase,refundedOrder),"purchase and refund may share merchant order number without being duplicates");
        List<BillParser.Line> noTimestamp=Arrays.asList(line("17:05",20,0,200,25),line("账单详情",20,30,300,25),line("商家",300,120,600,30),line("-12.00",300,180,800,60),line("交易时间 2026-10-18",20,350,950,25));
        entries=BillParser.parse(noTimestamp,1080,600,ctx);check(entries.get(0).time.isEmpty(),"status bar clock cannot become transaction time");
        check(CarBrand.isLoan("车贷", "其他"),"loan title displays brand selector");
        check(CarBrand.isLoan("每月还款", "车贷-小米"),"loan category displays brand selector");
        check(CarBrand.isLoan("汽车贷款", "其他"),"loan name alias");
        check(!CarBrand.isLoan("加油", "车辆"),"ordinary car costs keep shared vehicle icon");
        check(CarBrand.resolve("车贷","", "").equals(CarBrand.QIYUAN),"existing loan defaults to Qiyuan");
        check(CarBrand.resolve("车贷-小米","", "").equals(CarBrand.XIAOMI),"infer Xiaomi when unselected");
        check(CarBrand.resolve("车贷 huawei","", "").equals(CarBrand.HUAWEI),"infer Huawei");
        check(CarBrand.resolve("车贷 Tesla","", "").equals(CarBrand.TESLA),"infer Tesla");
        check(CarBrand.resolve("车贷-小米","",CarBrand.TESLA).equals(CarBrand.TESLA),"explicit choice survives renaming");
        check(CarBrand.resolve("车贷","",CarBrand.OTHER).equals(CarBrand.OTHER),"other brand is stable");
        check(CarBrand.resolve("车贷","","未知品牌").equals(CarBrand.OTHER),"unknown stored brands use generic vehicle icon");
        System.out.println("PASS "+checks+" ledger core checks");
    }
}
