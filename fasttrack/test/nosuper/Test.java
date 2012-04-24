/*    */ package test;
/*    */ 
/*    */ import java.io.PrintStream;
/*    */ import rr.instrument.classes.InterruptFixer;
/*    */ import rr.simple.LastTool;
/*    */ import rr.state.ShadowThread;
/*    */ import rr.state.ShadowVar;
/*    */ import rr.tool.RREventGenerator;
/*    */ import tools.fasttrack.FastTrackTool;
/*    */ 
/*    */ public class Test extends Thread
/*    */ {
/*    */   public static final int ITERS = 100;
/*    */   public static int y;
/*    */   public static transient ShadowVar __$rr_y;
/*    */ 
/*    */   public static void __$rr_put_y(int paramInt1, int paramInt2, ShadowThread paramShadowThread)
/*    */   {
/*    */     ShadowVar localShadowVar = Test.__$rr_y;
/*    */     if ((localShadowVar == null) || ((!FastTrackTool.writeFastPath(localShadowVar, paramShadowThread)) && (!LastTool.writeFastPath(localShadowVar, paramShadowThread))))
/*    */       RREventGenerator.writeAccess(null, localShadowVar, paramInt2, paramShadowThread);
/*    */     Test.y = paramInt1;
/*    */   }
/*    */ 
/*    */   public static int __$rr_get_y(int paramInt, ShadowThread paramShadowThread)
/*    */   {
/*    */     ShadowVar localShadowVar = Test.__$rr_y;
/*    */     if ((localShadowVar == null) || ((!FastTrackTool.readFastPath(localShadowVar, paramShadowThread)) && (!LastTool.readFastPath(localShadowVar, paramShadowThread))))
/*    */       RREventGenerator.readAccess(null, localShadowVar, paramInt, paramShadowThread);
/*    */     return Test.y;
/*    */   }
/*    */ 
/*    */   public void __$rr_inc__$rr__Original_()
/*    */   {
/*    */     __$rr___$rr_inc__$rr__Original___$rr__with_ThreadState_(ShadowThread.getCurrentShadowThread());
/*    */   }
/*    */ 
/*    */   public void __$rr___$rr_inc__$rr__Original___$rr__with_ThreadState_(ShadowThread paramShadowThread)
/*    */   {
/* 52 */     ShadowThread localShadowThread = paramShadowThread; __$rr_put_y(__$rr_get_y(0, localShadowThread) + 1, 1, localShadowThread);
/*    */   }
/*    */ 
/*    */   public void inc()
/*    */   {
/*    */     __$rr_inc__$rr__with_ThreadState_(ShadowThread.getCurrentShadowThread());
/*    */   }
/*    */ 
/*    */   public void __$rr_inc__$rr__with_ThreadState_(ShadowThread paramShadowThread)
/*    */   {
/* 51 */     ShadowThread localShadowThread = paramShadowThread; __$rr___$rr_inc__$rr__Original___$rr__with_ThreadState_(localShadowThread);
/*    */   }
/*    */   public void __$rr_run__$rr__Original_() {
/*    */     __$rr___$rr_run__$rr__Original___$rr__with_ThreadState_(ShadowThread.getCurrentShadowThread());
/*    */   }
/* 57 */   public void __$rr___$rr_run__$rr__Original___$rr__with_ThreadState_(ShadowThread paramShadowThread) { ShadowThread localShadowThread = paramShadowThread; for (int i = 0; i < 100; i++)
/* 58 */       __$rr_inc__$rr__with_ThreadState_(localShadowThread);
/*    */   }
/*    */ 
/*    */   public void run()
/*    */   {
/* 56 */     ShadowThread localShadowThread = ShadowThread.getCurrentShadowThread(); __$rr___$rr_run__$rr__Original___$rr__with_ThreadState_(localShadowThread);
/*    */   }
/*    */   public void __$rr_startTest__$rr__Original_() throws Exception {
/*    */     __$rr___$rr_startTest__$rr__Original___$rr__with_ThreadState_(ShadowThread.getCurrentShadowThread());
/*    */   }
/*    */ 
/*    */   public void __$rr___$rr_startTest__$rr__Original___$rr__with_ThreadState_(ShadowThread paramShadowThread) throws Exception {
/* 64 */     ShadowThread localShadowThread = paramShadowThread; Test localTest1 = new Test();
/* 65 */     Test localTest2 = new Test();
/* 66 */     RREventGenerator.start(localTest1, 0);
/* 67 */     RREventGenerator.start(localTest2, 1);
/* 68 */     RREventGenerator.join(localTest1, 0);
/* 69 */     RREventGenerator.join(localTest2, 1);
/*    */   }
/*    */ 
/*    */   public void startTest()
/*    */     throws Exception
/*    */   {
/*    */     __$rr_startTest__$rr__with_ThreadState_(ShadowThread.getCurrentShadowThread());
/*    */   }
/*    */ 
/*    */   public void __$rr_startTest__$rr__with_ThreadState_(ShadowThread paramShadowThread)
/*    */     throws Exception
/*    */   {
/* 63 */     ShadowThread localShadowThread = paramShadowThread; __$rr___$rr_startTest__$rr__Original___$rr__with_ThreadState_(localShadowThread);
/*    */   }
/*    */ 
/*    */   public static void __$rr_main__$rr__Original_(String[] paramArrayOfString)
/*    */   {
/*    */     __$rr___$rr_main__$rr__Original___$rr__with_ThreadState_(paramArrayOfString, ShadowThread.getCurrentShadowThread());
/*    */   }
/*    */ 
/*    */   public static void __$rr___$rr_main__$rr__Original___$rr__with_ThreadState_(String[] paramArrayOfString, ShadowThread paramShadowThread)
/*    */   {
/* 76 */     ShadowThread localShadowThread = paramShadowThread; Test localTest = new Test();
/*    */     try {
/* 78 */       localTest.__$rr_startTest__$rr__with_ThreadState_(localShadowThread);
/*    */     }
/*    */     catch (Exception localException2)
/*    */     {
/*    */       Exception tmp20_20 = 
/* 81 */         localException2;
/*    */ 
/* 79 */       InterruptFixer.__$rr_handleException(tmp20_20); Exception localException1 = tmp20_20;
/* 80 */       System.out.println("Failure!");
/*    */     }
/*    */   }
/*    */ 
/*    */   public static void main(String[] paramArrayOfString)
/*    */   {
/* 75 */     ShadowThread localShadowThread = ShadowThread.getCurrentShadowThread(); __$rr___$rr_main__$rr__Original___$rr__with_ThreadState_(paramArrayOfString, localShadowThread);
/*    */   }
/*    */ }

/* Location:           /home/dan/fasttrack/fastrack-android/backup/classes/
 * Qualified Name:     test.Test
 * JD-Core Version:    0.6.0
 */
