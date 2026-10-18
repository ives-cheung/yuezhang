package com.ives.money;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Amounts are stored in integer cents, never binary floating point. */
final class LedgerEntry {
    static final String EXPENSE="支出", INCOME="收入", REFUND="退款", TRANSFER="转账", REPAYMENT="还款", RECHARGE="充值", IGNORE="不计收支";
    static final String[] TYPES={EXPENSE,INCOME,REFUND,TRANSFER,REPAYMENT,RECHARGE,IGNORE};
    String id=UUID.randomUUID().toString(), title="", category="其他", date="", time="", channel="", payment="";
    String carBrand="";
    String type=EXPENSE, note="", txnNo="", imagePath="", sourceKey="", raw="", subscriptionId="", refundOf="", warning="", batchId="";
    long cents, createdAt=System.currentTimeMillis();
    boolean selected=true;
    String amount(){return BigDecimal.valueOf(cents,2).toPlainString();}
    static long parseCents(String value){
        String s=value.trim().replace("¥","").replace("￥","").replace("元","").replace(",","").replace(" ","");
        if(!s.matches("\\d+(?:\\.\\d{1,2})?"))throw new IllegalArgumentException("金额最多两位小数");
        long result=new BigDecimal(s).movePointRight(2).longValueExact();
        if(result<=0)throw new IllegalArgumentException("金额需要大于 0");
        return result;
    }
    boolean valid(){
        try {LocalDate.parse(date);if(!time.isEmpty())LocalTime.parse(time);return cents>0&&!title.trim().isEmpty()&&!title.equals("待补充商家");}
        catch(Exception e){return false;}
    }
    boolean consumption(){return EXPENSE.equals(type)||REFUND.equals(type);}
    void warn(String s){if(!s.isEmpty()&&!warning.contains(s))warning+=(warning.isEmpty()?"":" · ")+s;}
    static String normalized(String value){return value==null?"":value.toLowerCase(java.util.Locale.ROOT).replaceAll("[\\s\\p{Punct}，。·]","");}
}
