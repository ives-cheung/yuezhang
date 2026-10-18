package com.ives.money;

import java.util.Locale;

/** Shared car-loan brand rules for subscriptions, actual entries and wallet icons. */
final class CarBrand {
    static final String QIYUAN="长安启源", XIAOMI="小米", HUAWEI="华为", TESLA="特斯拉", OTHER="其他";
    static final String[] OPTIONS={QIYUAN,XIAOMI,HUAWEI,TESLA,OTHER};
    static boolean loanPrefix(String value){String s=value==null?"":value.trim();return s.startsWith("车贷")||s.startsWith("汽车贷款")||s.startsWith("汽车分期");}
    static boolean isLoan(String title,String category){return loanPrefix(title)||loanPrefix(category);}
    static String resolve(String title,String category,String saved){
        if(saved!=null&&!saved.trim().isEmpty()){
            for(String option:OPTIONS)if(option.equals(saved.trim()))return option;
            return OTHER;
        }
        String s=((title==null?"":title)+" "+(category==null?"":category)).toLowerCase(Locale.ROOT);
        if(s.contains("小米")||s.contains("xiaomi"))return XIAOMI;
        if(s.contains("华为")||s.contains("huawei"))return HUAWEI;
        if(s.contains("特斯拉")||s.contains("tesla"))return TESLA;
        if(s.contains("其他"))return OTHER;
        return QIYUAN;
    }
}
