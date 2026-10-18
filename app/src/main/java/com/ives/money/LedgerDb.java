package com.ives.money;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;

final class LedgerDb extends SQLiteOpenHelper {
    LedgerDb(Context c){super(c.getApplicationContext(),"daily-ledger.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE entries(id TEXT PRIMARY KEY,stage INTEGER NOT NULL,cents INTEGER NOT NULL,title TEXT NOT NULL,category TEXT,date TEXT,time TEXT,channel TEXT,payment TEXT,type TEXT,note TEXT,txnNo TEXT,imagePath TEXT,sourceKey TEXT,raw TEXT,subscriptionId TEXT,refundOf TEXT,warning TEXT,batchId TEXT,createdAt INTEGER,selected INTEGER,carBrand TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE INDEX entry_date ON entries(stage,date)");
        db.execSQL("CREATE INDEX entry_source ON entries(sourceKey)");
        db.execSQL("CREATE INDEX entry_order ON entries(channel,txnNo)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldV,int newV){if(oldV<2)db.execSQL("ALTER TABLE entries ADD COLUMN carBrand TEXT NOT NULL DEFAULT ''");}
    static ContentValues values(LedgerEntry e,boolean draft){
        ContentValues v=new ContentValues();v.put("id",e.id);v.put("carBrand",e.carBrand);v.put("stage",draft?1:0);v.put("cents",e.cents);v.put("title",e.title);v.put("category",e.category);v.put("date",e.date);v.put("time",e.time);v.put("channel",e.channel);v.put("payment",e.payment);v.put("type",e.type);v.put("note",e.note);v.put("txnNo",e.txnNo);v.put("imagePath",e.imagePath);v.put("sourceKey",e.sourceKey);v.put("raw",e.raw);v.put("subscriptionId",e.subscriptionId);v.put("refundOf",e.refundOf);v.put("warning",e.warning);v.put("batchId",e.batchId);v.put("createdAt",e.createdAt);v.put("selected",e.selected?1:0);return v;
    }
    static String s(Cursor c,String n){String value=c.getString(c.getColumnIndexOrThrow(n));return value==null?"":value;}
    static LedgerEntry read(Cursor c){
        LedgerEntry e=new LedgerEntry();e.id=s(c,"id");e.carBrand=s(c,"carBrand");e.cents=c.getLong(c.getColumnIndexOrThrow("cents"));e.title=s(c,"title");e.category=s(c,"category");e.date=s(c,"date");e.time=s(c,"time");e.channel=s(c,"channel");e.payment=s(c,"payment");e.type=s(c,"type");e.note=s(c,"note");e.txnNo=s(c,"txnNo");e.imagePath=s(c,"imagePath");e.sourceKey=s(c,"sourceKey");e.raw=s(c,"raw");e.subscriptionId=s(c,"subscriptionId");e.refundOf=s(c,"refundOf");e.warning=s(c,"warning");e.batchId=s(c,"batchId");e.createdAt=c.getLong(c.getColumnIndexOrThrow("createdAt"));e.selected=c.getInt(c.getColumnIndexOrThrow("selected"))!=0;return e;
    }
    synchronized List<LedgerEntry> list(boolean drafts){return query("stage=?",new String[]{drafts?"1":"0"},drafts?"createdAt ASC,rowid ASC":"date DESC,time DESC,createdAt DESC");}
    synchronized List<LedgerEntry> month(String prefix){return query("stage=0 AND substr(date,1,7)=?",new String[]{prefix},"date DESC,time DESC,createdAt DESC");}
    private List<LedgerEntry> query(String where,String[] args,String order){List<LedgerEntry> result=new ArrayList<>();try(Cursor c=getReadableDatabase().query("entries",null,where,args,null,null,order)){while(c.moveToNext())result.add(read(c));}return result;}
    synchronized void save(LedgerEntry e,boolean draft){if(getWritableDatabase().insertWithOnConflict("entries",null,values(e,draft),SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new SQLiteException("账单保存失败");}
    synchronized void addDrafts(List<LedgerEntry> entries){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{List<LedgerEntry> existing=list(false);existing.addAll(list(true));for(LedgerEntry e:entries){int dup=LedgerRules.duplicate(e,existing);if(dup>0)e.selected=false;save(e,true);existing.add(e);}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    synchronized void select(String id,boolean selected){ContentValues v=new ContentValues();v.put("selected",selected?1:0);getWritableDatabase().update("entries",v,"id=? AND stage=1",new String[]{id});}
    synchronized void delete(String id){getWritableDatabase().delete("entries","id=?",new String[]{id});}
    synchronized void clearDrafts(){getWritableDatabase().delete("entries","stage=1",null);}
    synchronized int[] postSelected(){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();int saved=0,duplicate=0;
        try{List<LedgerEntry> posted=list(false);for(LedgerEntry e:list(true))if(e.selected){
            if(!e.valid())throw new IllegalArgumentException("请补齐已选记录的金额、日期和商家");
            if(LedgerRules.duplicate(e,posted)==2){delete(e.id);duplicate++;continue;}
            String refundError=LedgerRules.validateRefund(e,posted);if(!refundError.isEmpty())throw new IllegalArgumentException(refundError);save(e,false);posted.add(e);saved++;
        }db.setTransactionSuccessful();}finally{db.endTransaction();}return new int[]{saved,duplicate};
    }
}
