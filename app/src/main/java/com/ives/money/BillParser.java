package com.ives.money;

import java.text.Normalizer;
import java.time.*;
import java.util.*;
import java.util.regex.*;

/** Layout-aware extraction. Uncertain fields remain drafts for user review. */
final class BillParser {
    static final class Line {
        final String text; final float left,top,right,bottom;
        Line(String t,float l,float y,float r,float b){text=normalize(t);left=l;top=y;right=r;bottom=b;}
        float mid(){return (top+bottom)/2;} float height(){return bottom-top;}
    }
    static final class Context {
        final int year,month; final String channel; final LocalDate today;
        Context(int y,int m,String c,LocalDate now){year=y;month=m;channel=c;today=now;}
    }
    static final Pattern MONEY=Pattern.compile("(?<![\\d.])([+\\-]?)\\s*([¥￥]?)\\s*((?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?)(\\s*元)?(?![\\d.])");
    static final Pattern FULL_DATE=Pattern.compile("(20\\d{2})[年./-](\\d{1,2})[月./-](\\d{1,2})日?");
    static final Pattern MONTH_DAY=Pattern.compile("(?<!\\d)(\\d{1,2})月(\\d{1,2})日?");
    static final Pattern SHORT_DATE=Pattern.compile("(?<![\\d.-])(\\d{1,2})[-/](\\d{1,2})(?![\\d.-])");
    static final Pattern DAY=Pattern.compile("(?<!\\d)(\\d{1,2})日");
    static final Pattern TIME=Pattern.compile("(?<!\\d)([012]?\\d:[0-5]\\d(?::[0-5]\\d)?)(?!\\d)");
    static final String[] LABELS={"交易时间","支付时间","付款时间","创建时间","收款时间","转账时间","支付方式","付款方式","交易单号","商户单号","订单号","交易号","商品说明","商品名称","交易对方","商家名称","收款方","商品","备注"};
    private static final class Anchor {Line line;String token,sign;long cents;int start,end;Anchor(Line l,Matcher m,long c){line=l;token=m.group();sign=m.group(1);cents=c;start=m.start();end=m.end();}}
    static String normalize(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).replace('−','-').replace('﹣','-').trim();}
    static boolean has(String s,String... words){for(String w:words)if(s.contains(w))return true;return false;}
    static boolean summary(String s){return s.equals("余额")||s.matches("^(余额|结余)[:：]?\\s*[¥￥]?\\s*\\d.*")||s.matches("^(支出|收入)[:：]?\\s*[¥￥+\\-]?\\s*\\d[\\d,. ]*$")||has(s,"月支出","月收入","合计","总计","总支出","总收入","本月支出","本月收入","本月结余","账户余额","可用余额","零钱余额","共支出","共收入","收支统计","支出统计","收入统计","笔交易","笔支出","笔收入");}
    static boolean chrome(String s){return s.isEmpty()||s.matches("[\\d: ]+")||has(s,"全部账单","收支明细","账单详情","交易详情","筛选","微信支付","支付宝账单","收入支出","查看详情","支付成功","交易成功","申请电子回单","对订单有疑惑","交易记录","账单服务");}
    static boolean overlapsDate(String s,int start,int end){
        for(Pattern p:new Pattern[]{FULL_DATE,MONTH_DAY,SHORT_DATE,TIME}){Matcher m=p.matcher(s);while(m.find())if(start<m.end()&&end>m.start())return true;}return false;
    }
    static List<LedgerEntry> parse(List<Line> input,int width,int height,Context ctx){
        List<Line> lines=new ArrayList<>(input);lines.sort(Comparator.comparingDouble((Line l)->l.mid()).thenComparingDouble(l->l.left));
        String full=join(lines);String platform=detectChannel(full,ctx.channel);Matcher header=Pattern.compile("(20\\d{2})年(\\d{1,2})月").matcher(full);if(header.find()){try{ctx=new Context(Integer.parseInt(header.group(1)),Integer.parseInt(header.group(2)),platform,ctx.today);}catch(Exception ignored){}}else ctx=new Context(ctx.year,ctx.month,platform,ctx.today);List<Anchor> anchors=new ArrayList<>();
        for(Line l:lines){
            if(summary(l.text))continue;
            Matcher m=MONEY.matcher(l.text);
            while(m.find()){
                if(overlapsDate(l.text,m.start(),m.end()))continue;
                String token=m.group(),digits=m.group(3);
                boolean standalone=l.text.replace(token,"").trim().isEmpty();
                if(!(token.contains(".")||!m.group(1).isEmpty()||!m.group(2).isEmpty()||m.group(4)!=null))continue;
                if(!(standalone||!m.group(1).isEmpty()||!m.group(2).isEmpty()||m.group(4)!=null))continue;
                if(has(l.text,"订单号","交易单号","交易号","商户单号","联系电话","客服电话"))continue;
                boolean nearSummary=false;
                for(Line other:lines)if(other!=l&&Math.abs(other.mid()-l.mid())<Math.max(l.height(),other.height())*.65&&other.right<=l.left+10&&summary(other.text))nearSummary=true;
                if(nearSummary)continue;
                try{Anchor found=new Anchor(l,m,LedgerEntry.parseCents(digits));boolean repeated=false;for(Anchor old:anchors)if(old.cents==found.cents&&old.sign.equals(found.sign)&&Math.abs(old.line.mid()-found.line.mid())<Math.max(old.line.height(),found.line.height())*.55){repeated=true;break;}if(!repeated)anchors.add(found);}catch(Exception ignored){}
            }
        }
        List<LedgerEntry> out=new ArrayList<>();
        boolean detail=has(full,"账单详情","交易单号","商户单号","微信支付单号","订单编号")||countFields(full)>=2;
        if(detail&&!anchors.isEmpty()){
            Anchor main=Collections.max(anchors,Comparator.comparingDouble(a->a.line.height()*2+(!a.sign.isEmpty()?30:0)+(a.line.top<height*.6?10:0)));
            LedgerEntry e=make(main,lines,ctx);e.raw=full;e.title=detailTitle(lines,main);
            String labelled=labelValue(lines,"交易时间","支付时间","付款时间","创建时间","收款时间","转账时间");
            String d=extractDate(labelled,ctx);if(d.isEmpty())d=extractDate(full,ctx);e.date=d;String timestamp=labelled;if(timestamp.isEmpty())for(Line l:lines)if(!extractDate(l.text,ctx).isEmpty()&&!extractTime(l.text).isEmpty()){timestamp=l.text;break;}e.time=extractTime(timestamp);
            e.txnNo=orderNumber(lines);finish(e,main.sign,full);out.add(e);
        }else{
            for(int i=0;i<anchors.size();i++){
                Anchor a=anchors.get(i);float lo=i==0?0:(anchors.get(i-1).line.mid()+a.line.mid())/2;
                float hi=i+1==anchors.size()?height:(a.line.mid()+anchors.get(i+1).line.mid())/2;
                List<Line> group=new ArrayList<>();for(Line l:lines)if(l.mid()>=lo&&l.mid()<hi)group.add(l);
                LedgerEntry e=make(a,group,ctx);e.title=listTitle(group,a);e.raw=join(group);
                e.date=nearestDate(group,a.line.mid(),ctx);
                if(e.date.isEmpty())for(Line l:lines){if(l.mid()>a.line.mid())break;String d=extractDate(l.text,ctx);if(!d.isEmpty())e.date=d;}
                e.time=nearestTime(group,a.line.mid());finish(e,a.sign,e.raw);out.add(e);
            }
        }
        if(out.isEmpty()){
            LedgerEntry e=new LedgerEntry();e.channel=detectChannel(full,ctx.channel);e.raw=full;e.title="";e.selected=false;e.warn("未提取到金额，请手动补充");out.add(e);
        }
        return out;
    }
    static int countFields(String s){int n=0;for(String k:new String[]{"支付方式","付款方式","创建时间","交易时间","商品说明","收款方"})if(s.contains(k))n++;return n;}
    static LedgerEntry make(Anchor a,List<Line> group,Context ctx){LedgerEntry e=new LedgerEntry();e.cents=a.cents;e.channel=detectChannel(join(group),ctx.channel);e.payment=payment(join(group));return e;}
    static void finish(LedgerEntry e,String sign,String raw){
        e.title=e.title.trim();if(e.title.isEmpty())e.title="待补充商家";e.type=type(e.title+" "+raw,sign);
        e.category=category(e.title+" "+raw);
        if(e.date.isEmpty())e.warn("缺少日期");if(e.title.equals("待补充商家"))e.warn("缺少商家");
        if(sign.isEmpty()&&e.type.equals(LedgerEntry.EXPENSE)&&!has(raw,"支出","付款","支付成功","交易成功","扣款"))e.warn("请确认收支方向");
        e.selected=e.valid()&&e.warning.isEmpty()&&(e.type.equals(LedgerEntry.EXPENSE)||e.type.equals(LedgerEntry.INCOME)||e.type.equals(LedgerEntry.REFUND));
        if(!e.consumption()&&!e.type.equals(LedgerEntry.INCOME))e.warn("不计入消费");
    }
    static String listTitle(List<Line> lines,Anchor a){
        String same=(a.line.text.substring(0,a.start)+a.line.text.substring(a.end)).trim().replaceAll("^[支出收入:： ]+","");
        if(!chrome(same)&&!summary(same)&&!(FULL_DATE.matcher(same).find()||MONTH_DAY.matcher(same).find()||SHORT_DATE.matcher(same).find())&&same.length()>1)return same;
        List<Line> candidates=new ArrayList<>(lines);candidates.sort(Comparator.comparingDouble(l->Math.abs(l.mid()-a.line.mid())+(l.left>a.line.left?500:0)));
        for(Line l:candidates){String t=l.text;if(l==a.line||chrome(t)||summary(t)||isLabel(t)||has(t,"支出","收入")&&t.length()<=4)continue;
            if(FULL_DATE.matcher(t).find()||MONTH_DAY.matcher(t).find()||SHORT_DATE.matcher(t).find()||TIME.matcher(t).find()||t.matches("\\d{1,2}日")||t.matches("(?:20\\d{2}年)?\\d{1,2}月"))continue;
            if(MONEY.matcher(t).matches())continue;
            if(t.length()>1)return t;
        }return "";
    }
    static String detailTitle(List<Line> lines,Anchor main){
        String labelled=labelValue(lines,"商品说明","商品名称","商家名称","交易对方","收款方","商品");
        if(!labelled.isEmpty())return labelled;
        List<Line> nearby=new ArrayList<>();for(Line l:lines)if(l.bottom<main.line.top+10)nearby.add(l);
        String title=listTitle(nearby,main);if(!title.isEmpty())return title;
        return listTitle(lines,main);
    }
    static boolean isLabel(String t){for(String l:LABELS)if(t.startsWith(l))return true;return t.matches("(收支类型|支付状态|交易状态|当前状态|交易分类|金额|服务|更多|说明|账单|退款信息|支付时间|转账说明)[:：]?");}
    static String labelValue(List<Line> lines,String... labels){
        for(String label:labels)for(int i=0;i<lines.size();i++){
            Line l=lines.get(i);if(!l.text.startsWith(label))continue;
            String tail=l.text.substring(label.length()).replaceAll("^[:：\\s]+","");if(!tail.isEmpty())return tail;
            for(int j=i+1;j<Math.min(lines.size(),i+4);j++){Line next=lines.get(j);if(isLabel(next.text))break;if(!chrome(next.text))return next.text;}
        }return "";
    }
    static String orderNumber(List<Line> lines){String v=labelValue(lines,"交易单号","微信支付单号","交易号","订单编号","订单号","商户单号").replace(" ","");Matcher m=Pattern.compile("[A-Za-z0-9_-]{10,80}").matcher(v);return m.find()?m.group():"";}
    static String extractDate(String text,Context ctx){
        if(text==null)return "";String s=normalize(text);Matcher m=FULL_DATE.matcher(s);
        try{if(m.find())return LocalDate.of(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3))).toString();
            m=MONTH_DAY.matcher(s);if(m.find())return LocalDate.of(ctx.year,Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))).toString();
            m=SHORT_DATE.matcher(s);if(m.find())return LocalDate.of(ctx.year,Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2))).toString();
            if(has(s,"前天"))return ctx.today.minusDays(2).toString();if(has(s,"昨天"))return ctx.today.minusDays(1).toString();if(has(s,"今天","今日"))return ctx.today.toString();
            m=DAY.matcher(s);if(m.find())return LocalDate.of(ctx.year,ctx.month,Integer.parseInt(m.group(1))).toString();
        }catch(Exception ignored){}return "";
    }
    static String nearestDate(List<Line> group,float y,Context ctx){String result="";float distance=Float.MAX_VALUE;for(Line l:group){String d=extractDate(l.text,ctx);boolean ownRow=l.mid()<=y||!extractTime(l.text).isEmpty()||l.mid()-y<l.height()*2.1;if(!d.isEmpty()&&ownRow&&Math.abs(l.mid()-y)<distance){result=d;distance=Math.abs(l.mid()-y);}}return result;}
    static String nearestTime(List<Line> group,float y){String result="";float dist=Float.MAX_VALUE;for(Line l:group){String t=extractTime(l.text);if(!t.isEmpty()&&(l.mid()>=y||y-l.mid()<l.height()*2.1)&&Math.abs(l.mid()-y)<dist){result=t;dist=Math.abs(l.mid()-y);}}return result;}
    static String extractTime(String text){Matcher m=TIME.matcher(text);if(m.find()){String s=m.group(1);try{return LocalTime.parse(s.length()==4?"0"+s:s).toString();}catch(Exception ignored){}}return "";}
    static String detectChannel(String full,String preset){if(!preset.equals("自动识别")&&!preset.isEmpty())return preset;if(has(full,"支付宝","花呗","余额宝"))return "支付宝";if(has(full,"微信","零钱","财付通"))return "微信";return "截图";}
    static String payment(String s){for(String p:new String[]{"信用卡","花呗","零钱通","零钱","余额宝","银行卡","余额"})if(s.contains(p))return p;return "";}
    static String type(String s,String sign){
        if(has(s,"转账退回","退回转账","提现退回"))return LedgerEntry.TRANSFER;
        if(has(s,"退款","退货退款","退回")&&!sign.equals("-")&&!has(s,"退款失败","退款中","退款申请"))return LedgerEntry.REFUND;
        if(has(s,"交易关闭","已取消","支付失败","交易失败","已撤销"))return LedgerEntry.IGNORE;
        if(has(s,"信用卡还款","信用卡自动还款","还信用卡","花呗还款","借呗还款")||s.trim().equals("还款"))return LedgerEntry.REPAYMENT;
        if(has(s,"转账","提现","余额宝转入","余额宝转出","零钱通转入","零钱通转出","银行卡转入","银行卡转出"))return LedgerEntry.TRANSFER;
        if(has(s,"充值")&&!has(s,"话费","手机充值","充电","加油"))return LedgerEntry.RECHARGE;
        if(sign.equals("+")||has(s,"收入","工资","奖金","报销到账","收款成功"))return LedgerEntry.INCOME;
        return LedgerEntry.EXPENSE;
    }
    static String category(String s){
        if(has(s,"午饭","晚饭","早饭","早餐","午餐","晚餐","餐饮","餐厅","外卖","美团","饿了么","咖啡","奶茶","水果","面馆","汉堡","食物","饭店"))return "餐饮";
        if(has(s,"车贷"))return "车贷";if(has(s,"房租","租金"))return "房租";
        if(has(s,"充电","加油","洗车","停车","过路费","通行费","高速费"))return "车辆";
        if(has(s,"水电","水费","电费","电网"))return "水电费";
        if(has(s,"话费","宽带","中国联通","中国移动","中国电信"))return "通讯";
        if(has(s,"地铁","公交","打车","滴滴","高铁","火车","机票"))return "交通";
        if(has(s,"超市","便利店","购物","京东","淘宝","拼多多"))return "购物";
        if(has(s,"医院","门诊","药房","药店"))return "医疗";
        if(has(s,"音乐","会员","chatgpt","ChatGPT","视频","订阅"))return "订阅";
        if(has(s,"工资","奖金","收入"))return "工资收入";return "其他";
    }
    static List<LedgerEntry> quick(String input,LocalDate defaultDate){
        String normalized=normalize(input).replaceAll("(?<=\\d),(?=\\d{3}(?:\\D|$))","");
        List<LedgerEntry> out=new ArrayList<>();Context ctx=new Context(defaultDate.getYear(),defaultDate.getMonthValue(),"文字",defaultDate);
        for(String part:normalized.split("[，,;；\\n]+")){
            part=part.trim();if(part.isEmpty())continue;
            LedgerEntry e=new LedgerEntry();e.raw=part;e.channel="文字";e.date=extractDate(part,ctx);if(e.date.isEmpty())e.date=defaultDate.toString();
            String body=FULL_DATE.matcher(part).replaceAll("");body=MONTH_DAY.matcher(body).replaceAll("");body=SHORT_DATE.matcher(body).replaceAll("");body=TIME.matcher(body).replaceAll("");body=body.replaceAll("今天|今日|昨天|前天","").trim();
            Matcher m=MONEY.matcher(body);String digits="",sign="";int start=-1,end=-1,numbers=0;
            while(m.find()){numbers++;digits=m.group(3);sign=m.group(1);start=m.start();end=m.end();}if(numbers>1)e.warn("包含多个数字，请核对金额");
            if(start>=0){try{e.cents=LedgerEntry.parseCents(digits);}catch(Exception ex){e.warn("请检查金额");}e.title=(body.substring(0,start)+body.substring(end)).trim().replaceAll("^[\\s:：]+|[\\s:：元]+$","");}
            else{e.title=body;e.warn("缺少金额");}
            e.type=type(part,sign);e.category=category(e.title);e.time=extractTime(part);if(e.title.isEmpty())e.warn("缺少商家或用途");e.selected=e.valid()&&e.warning.isEmpty();out.add(e);
        }return out;
    }
    static String join(List<Line> lines){StringBuilder s=new StringBuilder();for(Line l:lines)s.append(l.text).append('\n');return s.toString().trim();}
}
