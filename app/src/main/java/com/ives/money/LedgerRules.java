package com.ives.money;
import java.util.List;
final class LedgerRules {
    static boolean exact(LedgerEntry a,LedgerEntry b){
        if(a.id.equals(b.id))return false;
        if(!a.sourceKey.isEmpty()&&a.sourceKey.equals(b.sourceKey))return true;
        return sameOrder(a,b)&&a.cents==b.cents&&a.type.equals(b.type)&&LedgerEntry.normalized(a.title).equals(LedgerEntry.normalized(b.title));
    }
    static boolean sameOrder(LedgerEntry a,LedgerEntry b){return !a.txnNo.isEmpty()&&a.txnNo.equals(b.txnNo)&&a.channel.equals(b.channel)&&!a.channel.isEmpty()&&!a.channel.equals("截图")&&!a.channel.equals("其他")&&!a.channel.equals("自动识别");}
    static boolean possible(LedgerEntry a,LedgerEntry b){
        if(!a.id.equals(b.id)&&a.type.equals(b.type)&&sameOrder(a,b))return true;
        if(a.id.equals(b.id)||a.cents!=b.cents||!a.type.equals(b.type)||a.date.isEmpty()||!a.date.equals(b.date))return false;
        String x=LedgerEntry.normalized(a.title),y=LedgerEntry.normalized(b.title);
        if(x.isEmpty()||!x.equals(y))return false;
        return a.time.isEmpty()||b.time.isEmpty()||a.time.substring(0,Math.min(5,a.time.length())).equals(b.time.substring(0,Math.min(5,b.time.length())));
    }
    static int duplicate(LedgerEntry e,List<LedgerEntry> pool){int level=0;for(LedgerEntry p:pool){if(exact(e,p))return 2;if(possible(e,p))level=1;}return level;}
    static long netExpense(List<LedgerEntry> entries){long n=0;for(LedgerEntry e:entries)if(e.type.equals(LedgerEntry.EXPENSE))n+=e.cents;else if(e.type.equals(LedgerEntry.REFUND))n-=e.cents;return n;}
    static String validateRefund(LedgerEntry e,List<LedgerEntry> entries){
        if(!e.type.equals(LedgerEntry.REFUND)||e.refundOf.isEmpty())return "";
        LedgerEntry original=null;long refunded=e.cents;for(LedgerEntry other:entries){if(other.id.equals(e.refundOf))original=other;if(other.type.equals(LedgerEntry.REFUND)&&other.refundOf.equals(e.refundOf)&&!other.id.equals(e.id))refunded+=other.cents;}
        if(original==null||!original.type.equals(LedgerEntry.EXPENSE))return "请选择有效的原支出，或取消关联";
        return refunded>original.cents?"关联退款合计不能超过原支出金额":"";
    }

}
