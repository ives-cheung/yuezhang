package com.ives.money;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.*;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class ScreenshotImporter {
    static final ExecutorService WORKER=Executors.newSingleThreadExecutor();
    static volatile boolean running;
    static volatile String status="";
    interface Listener {void done(int images,int rows,List<String> failures);}
    static synchronized boolean start(Context context,List<Uri> uris,BillParser.Context options,Listener listener){
        if(running)return false;running=true;status="正在读取截图";
        Context app=context.getApplicationContext();
        WORKER.execute(()->{
            int successful=0,rows=0;List<String> failures=new ArrayList<>();Set<String> seen=new HashSet<>();
            String batch=UUID.randomUUID().toString();LedgerDb db=new LedgerDb(app);TextRecognizer recognizer=null;
            try{
                recognizer=TextRecognition.getClient(new ChineseTextRecognizerOptions.Builder().build());
                for(int i=0;i<uris.size();i++){
                    File file=null;status="识别截图 "+(i+1)+" / "+uris.size();
                    try{
                        file=copy(app,uris.get(i));String hash=hashFile(file);if(!seen.add(hash)){file.delete();continue;}
                        Scan scan=scan(file,recognizer);List<LedgerEntry> entries=BillParser.parse(scan.lines,scan.width,scan.height,options);
                        for(int j=0;j<entries.size();j++){LedgerEntry e=entries.get(j);e.imagePath=file.getAbsolutePath();e.batchId=batch;e.sourceKey=hash+":"+j;}
                        db.addDrafts(entries);successful++;rows+=entries.size();
                    }catch(Exception ex){if(file!=null)file.delete();failures.add("第 "+(i+1)+" 张："+(ex instanceof IOException?"图片无法读取":"识别未完成，请重试"));}
                }
            }catch(Exception ex){failures.add("中文识别初始化失败，请重新导入");}
            finally{if(recognizer!=null)recognizer.close();db.close();running=false;status="";final int count=successful,records=rows;new Handler(Looper.getMainLooper()).post(()->listener.done(count,records,failures));}
        });return true;
    }
    private static File copy(Context c,Uri uri)throws Exception{
        File dir=new File(c.getFilesDir(),"ledger-images");if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException();
        File file=new File(dir,UUID.randomUUID()+".image");boolean success=false;
        try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(file)){
            if(in==null)throw new IOException();byte[] b=new byte[16384];long bytes=0;int n;while((n=in.read(b))!=-1){bytes+=n;if(bytes>64L*1024*1024)throw new IOException("图片过大");out.write(b,0,n);}success=true;return file;
        }finally{if(!success)file.delete();}
    }
    static String hashFile(File file)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)d.update(b,0,n);}StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
    private static final class Scan {int width,height;List<BillParser.Line> lines=new ArrayList<>();}
    private static Scan scan(File file,TextRecognizer recognizer)throws Exception{
        BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(file.getAbsolutePath(),false);
        if(decoder==null)throw new IOException();Scan scan=new Scan();scan.width=decoder.getWidth();scan.height=decoder.getHeight();
        try{
            int sample=1;while(scan.width/sample>1800)sample*=2;
            int tileHeight=2200*sample,overlap=220*sample;
            for(int y=0;y<scan.height;){
                int bottom=Math.min(scan.height,y+tileHeight);BitmapFactory.Options opts=new BitmapFactory.Options();opts.inSampleSize=sample;
                Bitmap bitmap=decoder.decodeRegion(new Rect(0,y,scan.width,bottom),opts);if(bitmap==null)throw new IOException();
                try{
                    Text text=Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap,0)),90,TimeUnit.SECONDS);
                    float sx=(float)scan.width/bitmap.getWidth(),sy=(float)(bottom-y)/bitmap.getHeight();
                    for(Text.TextBlock block:text.getTextBlocks())for(Text.Line line:block.getLines()){
                        Rect box=line.getBoundingBox();if(box==null||line.getText().trim().isEmpty())continue;
                        BillParser.Line item=new BillParser.Line(line.getText(),box.left*sx,y+box.top*sy,box.right*sx,y+box.bottom*sy);
                        boolean duplicate=false;for(int k=scan.lines.size()-1;k>=0;k--){BillParser.Line old=scan.lines.get(k);if(old.bottom<y-overlap)continue;if(old.text.equals(item.text)&&Math.abs(old.mid()-item.mid())<Math.max(18*sample,item.height()*.5)&&Math.abs(old.left-item.left)<25*sample){duplicate=true;break;}}
                        if(!duplicate)scan.lines.add(item);
                    }
                }finally{bitmap.recycle();}
                if(bottom==scan.height)break;y=bottom-overlap;
            }
            return scan;
        }finally{decoder.recycle();}
    }
}
