/*    */ package rr;
/*    */ 
/*    */ import java.net.URL;
/*    */ import java.net.URLClassLoader;
/*    */ import java.io.Serializable;
/*    */
/*    */ public final class RRMain$RRMainLoader extends URLClassLoader implements Serializable
/*    */ {
/*    */   private RRMain$RRMainLoader(URL[] urls, ClassLoader parent)
/*    */   {
/* 76 */     super(urls, parent);
/*    */   }
/*    */ 
/*    */   public String toString()
/*    */   {
/* 81 */     return "RRMainLoader";
/*    */   }
/*    */ 
/*    */   public Class<?> findClass(String name) throws ClassNotFoundException
/*    */   {
/* 86 */     return super.findClass(name);
/*    */   }
/*    */ }

/* Location:           /home/dan/fasttrack/fastrack-android/fasttrack/
 * Qualified Name:     rr.RRMain.RRMainLoader
 * JD-Core Version:    0.6.0
 */
