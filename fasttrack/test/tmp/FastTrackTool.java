/*     */ package tools.fasttrack;
/*     */ 
/*     */ import acme.util.Assert;
/*     */ import acme.util.Util;
/*     */ import acme.util.decorations.Decoration;
/*     */ import acme.util.decorations.DecorationFactory.Type;
/*     */ import acme.util.decorations.NullDefault;
/*     */ import acme.util.identityhash.ConcurrentIdentityHashMap;
/*     */ import acme.util.io.XMLWriter;
/*     */ import acme.util.option.CommandLine;
/*     */ import java.io.PrintStream;
/*     */ import org.objectweb.asm.Opcodes;
/*     */ import rr.annotations.Abbrev;
/*     */ import rr.barrier.BarrierEvent;
/*     */ import rr.barrier.BarrierListener;
/*     */ import rr.barrier.BarrierMonitor;
/*     */ import rr.error.ErrorMessage;
/*     */ import rr.error.ErrorMessages;
/*     */ import rr.event.AccessEvent;
/*     */ import rr.event.AccessEvent.Kind;
/*     */ import rr.event.AcquireEvent;
/*     */ import rr.event.ArrayAccessEvent;
/*     */ import rr.event.ClassInitializedEvent;
/*     */ import rr.event.Event;
/*     */ import rr.event.FieldAccessEvent;
/*     */ import rr.event.JoinEvent;
/*     */ import rr.event.NewThreadEvent;
/*     */ import rr.event.NotifyEvent;
/*     */ import rr.event.ReleaseEvent;
/*     */ import rr.event.StartEvent;
/*     */ import rr.event.VolatileAccessEvent;
/*     */ import rr.event.WaitEvent;
/*     */ import rr.instrument.classes.ArrayAllocSiteTracker;
/*     */ import rr.meta.ArrayAccessInfo;
/*     */ import rr.meta.ClassInfo;
/*     */ import rr.meta.FieldAccessInfo;
/*     */ import rr.meta.FieldInfo;
/*     */ import rr.meta.MetaDataAllocator;
/*     */ import rr.meta.MetaDataInfoMaps;
/*     */ import rr.state.AbstractArrayState;
/*     */ import rr.state.ShadowLock;
/*     */ import rr.state.ShadowThread;
/*     */ import rr.state.ShadowVar;
/*     */ import rr.state.ShadowVolatile;
/*     */ import rr.tool.Tool;
/*     */ import tools.util.CV;
/*     */ 
/*     */ @Abbrev("FT")
/*     */ public class FastTrackTool extends Tool
/*     */   implements BarrierListener<FastTrackBarrierState>, Opcodes
/*     */ {
/*     */   static final int INIT_CV_SIZE = 4;
/*  99 */   public final ErrorMessage<FieldInfo> fieldErrors = ErrorMessages.makeFieldErrorMessage("FastTrack");
/* 100 */   public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("FastTrack");
/*     */ 
/* 102 */   public static final Decoration<ClassInfo, CV> classInitTime = MetaDataInfoMaps.getClasses().makeDecoration("FastTrack:InitTime", DecorationFactory.Type.MULTIPLE, new FastTrackTool.1());
/*     */ 
/* 125 */   static final Decoration<ShadowLock, FastTrackLockData> ftLockData = ShadowLock.makeDecoration("FastTrack:ShadowLock", DecorationFactory.Type.MULTIPLE, new FastTrackTool.3());
/*     */ 
/* 132 */   static final Decoration<ShadowVolatile, FastTrackVolatileData> ftVolatileData = ShadowVolatile.makeDecoration("FastTrack:shadowVolatile", DecorationFactory.Type.MULTIPLE, new FastTrackTool.4());
/*     */ 
/* 451 */   private final Decoration<ShadowThread, CV> cvForExit = ShadowThread.makeDecoration("FT:barrier", DecorationFactory.Type.MULTIPLE, new NullDefault());
/*     */ 
/*     */   public FastTrackTool(String name, Tool next, CommandLine commandLine)
/*     */   {
/* 110 */     super(name, next, commandLine);
/* 111 */     new BarrierMonitor(this, new FastTrackTool.2(this));
/*     */   }
/*     */ 
/*     */   static int ts_get_epoch(ShadowThread ts)
/*     */   {
/* 118 */     Assert.panic("Bad"); return -1; } 
/* 119 */   static void ts_set_epoch(ShadowThread ts, int v) { Assert.panic("Bad"); } 
/*     */   static CV ts_get_cv(ShadowThread ts) {
/* 121 */     Assert.panic("Bad"); return null; } 
/* 122 */   static void ts_set_cv(ShadowThread ts, CV cv) { Assert.panic("Bad");
/*     */   }
/*     */ 
/*     */   static final FastTrackLockData get(ShadowLock ld)
/*     */   {
/* 129 */     return (FastTrackLockData)ftLockData.get(ld);
/*     */   }
/*     */ 
/*     */   static final FastTrackVolatileData get(ShadowVolatile ld)
/*     */   {
/* 136 */     return (FastTrackVolatileData)ftVolatileData.get(ld);
/*     */   }
/*     */ 
/*     */   protected FastTrackGuardState createHelper(AccessEvent e)
/*     */   {
/* 141 */     return new FastTrackGuardState(e.isWrite(), e.getThread().epoch_tools_fasttrack_FastTrackTool);
/*     */   }
/*     */ 
/*     */   public final ShadowVar makeShadowVar(AccessEvent fae)
/*     */   {
/* 146 */     if (fae.getKind() == AccessEvent.Kind.VOLATILE) {
/* 147 */       FastTrackVolatileData vd = get(((VolatileAccessEvent)fae).getShadowVolatile());
/* 148 */       ShadowThread currentThread = fae.getThread();
/* 149 */       vd.cv.max(currentThread.cv_tools_fasttrack_FastTrackTool);
/* 150 */       return super.makeShadowVar(fae);
/*     */     }
/* 152 */     return createHelper(fae);
/*     */   }
/*     */ 
/*     */   protected void maxAndIncEpochAndCV(ShadowThread currentThread, CV other, Event e)
/*     */   {
/* 158 */     CV cv = currentThread.cv_tools_fasttrack_FastTrackTool;
/* 159 */     cv.max(other);
/* 160 */     cv.inc(currentThread.getTid());
/* 161 */     currentThread.epoch_tools_fasttrack_FastTrackTool = cv.get(currentThread.getTid());
/*     */   }
/*     */ 
/*     */   protected void maxEpochAndCV(ShadowThread currentThread, CV other, Event e)
/*     */   {
/* 166 */     CV cv = currentThread.cv_tools_fasttrack_FastTrackTool;
/* 167 */     cv.max(other);
/* 168 */     currentThread.epoch_tools_fasttrack_FastTrackTool = cv.get(currentThread.getTid());
/*     */   }
/*     */ 
/*     */   protected void incEpochAndCV(ShadowThread currentThread, Event e)
/*     */   {
/* 173 */     CV cv = currentThread.cv_tools_fasttrack_FastTrackTool;
/* 174 */     cv.inc(currentThread.getTid());
/* 175 */     currentThread.epoch_tools_fasttrack_FastTrackTool = cv.get(currentThread.getTid());
/*     */   }
/*     */ 
/*     */   public void create(NewThreadEvent e)
/*     */   {
/* 181 */     ShadowThread currentThread = e.getThread();
/* 182 */     System.out.println("FT: about to call ts_get_cv in FastTrackTool.java->create()");
/*     */ 
/* 184 */     CV cv = currentThread.cv_tools_fasttrack_FastTrackTool;
/*     */ 
/* 186 */     if (cv == null) {
/* 187 */       cv = new CV(4);
/* 188 */       currentThread.cv_tools_fasttrack_FastTrackTool = cv;
/* 189 */       cv.set(currentThread.getTid(), Epoch.make(currentThread.getTid(), 0));
/* 190 */       incEpochAndCV(currentThread, null);
/*     */     }
/*     */ 
/* 193 */     super.create(e);
/*     */   }
/*     */ 
/*     */   public void stop(ShadowThread td)
/*     */   {
/* 200 */     super.stop(td);
/*     */   }
/*     */ 
/*     */   public void acquire(AcquireEvent ae)
/*     */   {
/* 205 */     ShadowThread td = ae.getThread();
/* 206 */     ShadowLock shadowLock = ae.getLock();
/* 207 */     FastTrackLockData fhbLockData = get(shadowLock);
/*     */ 
/* 209 */     maxEpochAndCV(td, fhbLockData.cv, ae);
/*     */ 
/* 211 */     super.acquire(ae);
/*     */   }
/*     */ 
/*     */   public void release(ReleaseEvent re)
/*     */   {
/* 218 */     ShadowThread td = re.getThread();
/* 219 */     ShadowLock shadowLock = re.getLock();
/* 220 */     FastTrackLockData fhbLockData = get(shadowLock);
/*     */ 
/* 222 */     CV cv = td.cv_tools_fasttrack_FastTrackTool;
/* 223 */     fhbLockData.cv.max(cv);
/* 224 */     incEpochAndCV(td, re);
/*     */ 
/* 226 */     super.release(re);
/*     */   }
/*     */ 
/*     */   public void access(AccessEvent fae)
/*     */   {
/* 234 */     ShadowVar orig = fae.getOriginalShadow();
/* 235 */     ShadowThread td = fae.getThread();
/*     */ 
/* 237 */     if ((orig instanceof FastTrackGuardState)) {
/* 238 */       FastTrackGuardState x = (FastTrackGuardState)orig;
/*     */ 
/* 240 */       int tdEpoch = td.epoch_tools_fasttrack_FastTrackTool;
/* 241 */       CV tdCV = td.cv_tools_fasttrack_FastTrackTool;
/*     */ 
/* 243 */       Object target = fae.getTarget();
/* 244 */       if (target == null) {
/* 245 */         CV initTime = (CV)classInitTime.get(((FieldAccessEvent)fae).getInfo().getField().getOwner());
/* 246 */         tdCV.max(initTime);
/*     */       }
/*     */ 
/* 249 */       synchronized (x)
/*     */       {
/* 251 */         if (fae.isWrite()) {
/*     */ 
/* 254 */           int lastWriteEpoch = x.lastWrite;
/* 255 */           if (lastWriteEpoch == tdEpoch) {
/* 257 */             return;
/*     */           }
/*     */ 
/* 260 */           int lastWriter = Epoch.tid(lastWriteEpoch);
/* 261 */           if (lastWriteEpoch > tdCV.get(lastWriter)) {
/* 262 */             error(fae, 1, "write-by-thread-", lastWriter, "write-by-thread-", td.getTid());
/*     */           }
/*     */ 
/* 265 */           int lastReadEpoch = x.lastRead;
/* 266 */           if (lastReadEpoch != -1) {
/* 267 */             int lastReader = Epoch.tid(lastReadEpoch);
/* 268 */             if ((lastReader != td.getTid()) && (lastReadEpoch > tdCV.get(lastReader))) {
/* 269 */               error(fae, 2, "read-by-thread-", lastReader, "write-by-thread-", td.getTid());
/*     */             }
/*     */           }
/* 272 */           else if (x.anyGt(tdCV)) {
/* 273 */             for (int prevReader = x.nextGt(tdCV, 0); prevReader > -1; prevReader = x.nextGt(tdCV, prevReader + 1)) {
/* 274 */               if (prevReader != td.getTid()) {
/* 275 */                 error(fae, 3, "read-by-thread-", prevReader, "write-by-thread-", td.getTid());
/*     */               }
/*     */             }
/*     */           }
/*     */ 
/* 280 */           x.lastWrite = tdEpoch;
/* 281 */           x.lastRead = tdEpoch;
/*     */         }
/*     */         else {
/*     */ 
/* 286 */           int lastReadEpoch = x.lastRead;
/*     */ 
/* 288 */           if (lastReadEpoch == tdEpoch)
/* 289 */             return;
/* 290 */           if ((lastReadEpoch == -1) && 
/* 291 */             (x.get(td.getTid()) == tdEpoch)) {
/* 292 */             return;
/*     */           }
/*     */ 
/* 296 */           int lastWriteEpoch = x.lastWrite;
/* 297 */           int lastWriter = Epoch.tid(lastWriteEpoch);
/* 298 */           if (lastWriteEpoch > tdCV.get(lastWriter)) {
/* 299 */             error(fae, 4, "write-by-thread-", lastWriter, "read-by-thread-", td.getTid());
/*     */           }
/*     */ 
/* 302 */           if (lastReadEpoch != -1) {
/* 303 */             int lastReader = Epoch.tid(lastReadEpoch);
/* 304 */             if (lastReadEpoch <= tdCV.get(lastReader)) {
/* 305 */               x.lastRead = tdEpoch;
/*     */             } else {
/* 307 */               x.makeCV(4);
/* 308 */               x.set(lastReader, lastReadEpoch);
/* 309 */               x.set(td.getTid(), tdEpoch);
/* 310 */               x.lastRead = -1;
/*     */             }
/*     */           } else {
/* 313 */             x.set(td.getTid(), tdEpoch);
/*     */           }
/*     */         }
/*     */       }
/*     */     } else {
/* 318 */       super.access(fae);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void volatileAccess(VolatileAccessEvent fae)
/*     */   {
/* 324 */     ShadowVar orig = fae.getOriginalShadow();
/* 325 */     ShadowThread td = fae.getThread();
/*     */ 
/* 327 */     FastTrackVolatileData vd = get(fae.getShadowVolatile());
/* 328 */     CV cv = td.cv_tools_fasttrack_FastTrackTool;
/* 329 */     if (fae.isWrite()) {
/* 330 */       vd.cv.max(cv);
/* 331 */       incEpochAndCV(td, fae);
/*     */     } else {
/* 333 */       cv.max(vd.cv);
/*     */     }
/* 335 */     super.volatileAccess(fae);
/*     */   }
/*     */ 
/*     */   private void error(AccessEvent ae, int errorCase, String prevOp, int prevTid, String curOp, int curTid)
/*     */   {
/*     */     try
/*     */     {
/* 342 */       if ((ae instanceof FieldAccessEvent)) {
/* 343 */         FieldAccessEvent fae = (FieldAccessEvent)ae;
/* 344 */         FieldInfo fd = fae.getInfo().getField();
/* 345 */         ShadowThread currentThread = fae.getThread();
/* 346 */         Object target = fae.getTarget();
/*     */ 
/* 348 */         this.fieldErrors.error(currentThread, fd, new Object[] { "Guard State", fae.getOriginalShadow(), "Current Thread", toString(currentThread), "Class", target == null ? fd.getOwner() : target.getClass(), "Field", Util.objectToIdentityString(target) + "." + fd, "Prev Op", prevOp + prevTid, "Cur Op", curOp + curTid, "Case", "#" + errorCase, "Stack", ShadowThread.stackDumpForErrorMessage(currentThread) });
/*     */ 
/* 359 */         if (!this.fieldErrors.stillLooking(fd)) {
/* 360 */           advance(ae);
/* 361 */           return;
/*     */         }
/*     */       } else {
/* 364 */         ArrayAccessEvent aae = (ArrayAccessEvent)ae;
/* 365 */         ShadowThread currentThread = aae.getThread();
/* 366 */         Object target = aae.getTarget();
/*     */ 
/* 368 */         this.arrayErrors.error(currentThread, aae.getInfo(), new Object[] { "Alloc Site", ArrayAllocSiteTracker.allocSites.get(aae.getTarget()), "Guard State", aae.getOriginalShadow(), "Current Thread", toString(currentThread), "Array", Util.objectToIdentityString(target) + "[" + aae.getIndex() + "]", "Prev Op", prevOp + prevTid + "name = " + ShadowThread.get(prevTid).getThread().getName(), "Cur Op", curOp + curTid + "name = " + ShadowThread.get(curTid).getThread().getName(), "Case", "#" + errorCase, "Stack", ShadowThread.stackDumpForErrorMessage(currentThread) });
/*     */ 
/* 380 */         aae.getArrayState().specialize();
/*     */ 
/* 382 */         if (!this.arrayErrors.stillLooking(aae.getInfo())) {
/* 383 */           advance(aae);
/* 384 */           return;
/*     */         }
/*     */       }
/*     */     } catch (Throwable e) {
/* 388 */       Assert.panic(e);
/*     */     }
/*     */   }
/*     */ 
/*     */   public void preStart(StartEvent se)
/*     */   {
/* 395 */     ShadowThread td = se.getThread();
/* 396 */     ShadowThread forked = se.getNewThread();
/*     */ 
/* 398 */     CV curCV = td.cv_tools_fasttrack_FastTrackTool;
/*     */ 
/* 400 */     CV forkedCV = forked.cv_tools_fasttrack_FastTrackTool;
/* 401 */     maxAndIncEpochAndCV(forked, curCV, se);
/*     */ 
/* 403 */     incEpochAndCV(td, se);
/*     */ 
/* 406 */     super.preStart(se);
/*     */   }
/*     */ 
/*     */   public void postJoin(JoinEvent je)
/*     */   {
/* 412 */     ShadowThread td = je.getThread();
/* 413 */     ShadowThread joining = je.getJoiningThread();
/*     */ 
/* 415 */     if (joining.getTid() != -1) {
/* 416 */       incEpochAndCV(joining, je);
/* 417 */       maxEpochAndCV(td, joining.cv_tools_fasttrack_FastTrackTool, je);
/*     */     }
/*     */ 
/* 420 */     super.postJoin(je);
/*     */   }
/*     */ 
/*     */   public void preNotify(NotifyEvent we)
/*     */   {
/* 426 */     super.preNotify(we);
/*     */   }
/*     */ 
/*     */   public void preWait(WaitEvent we)
/*     */   {
/* 431 */     FastTrackLockData lockData = get(we.getLock());
/* 432 */     incEpochAndCV(we.getThread(), we);
/* 433 */     synchronized (lockData) {
/* 434 */       lockData.cv.max(we.getThread().cv_tools_fasttrack_FastTrackTool);
/*     */     }
/* 436 */     super.preWait(we);
/*     */   }
/*     */ 
/*     */   public void postWait(WaitEvent we)
/*     */   {
/* 441 */     FastTrackLockData lockData = get(we.getLock());
/* 442 */     maxEpochAndCV(we.getThread(), lockData.cv, we);
/* 443 */     super.postWait(we);
/*     */   }
/*     */ 
/*     */   public static String toString(ShadowThread td) {
/* 447 */     return String.format("[tid=%-2d   cv=%s   epoch=%s]", new Object[] { Integer.valueOf(td.getTid()), td.cv_tools_fasttrack_FastTrackTool, Epoch.toString(td.epoch_tools_fasttrack_FastTrackTool) });
/*     */   }
/*     */ 
/*     */   public void preDoBarrier(BarrierEvent<FastTrackBarrierState> be)
/*     */   {
/* 455 */     FastTrackBarrierState ftbe = (FastTrackBarrierState)be.getBarrier();
/* 456 */     ShadowThread currentThread = be.getThread();
/* 457 */     CV entering = ftbe.getEntering();
/* 458 */     entering.max(currentThread.cv_tools_fasttrack_FastTrackTool);
/* 459 */     this.cvForExit.set(currentThread, entering);
/*     */   }
/*     */ 
/*     */   public void postDoBarrier(BarrierEvent<FastTrackBarrierState> be) {
/* 463 */     FastTrackBarrierState ftbe = (FastTrackBarrierState)be.getBarrier();
/* 464 */     ShadowThread currentThread = be.getThread();
/* 465 */     CV old = (CV)this.cvForExit.get(currentThread);
/* 466 */     ftbe.reset(old);
/* 467 */     maxAndIncEpochAndCV(currentThread, old, be);
/*     */   }
/*     */ 
/*     */   public void classInitialized(ClassInitializedEvent e)
/*     */   {
/* 473 */     ShadowThread currentThread = e.getThread();
/* 474 */     CV cv = currentThread.cv_tools_fasttrack_FastTrackTool;
/* 475 */     Util.log("Class Init for " + e + " -- " + cv);
/* 476 */     ((CV)classInitTime.get(e.getRRClass())).max(cv);
/* 477 */     incEpochAndCV(currentThread, e);
/* 478 */     super.classInitialized(e);
/*     */   }
/*     */ 
/*     */   public void printXML(XMLWriter xml)
/*     */   {
/* 484 */     for (ShadowThread td : ShadowThread.getThreads())
/* 485 */       Util.log(toString(td));
/*     */   }
/*     */ 
/*     */   public static boolean readFastPath(ShadowVar gs, ShadowThread td)
/*     */   {
/* 493 */     if ((gs instanceof FastTrackGuardState)) {
/* 494 */       FastTrackGuardState x = (FastTrackGuardState)gs;
/* 495 */       int tdEpoch = td.epoch_tools_fasttrack_FastTrackTool;
/* 496 */       int lastReadEpoch = x.lastRead;
/*     */ 
/* 498 */       if (lastReadEpoch == tdEpoch) {
/* 499 */         return true;
/*     */       }
/*     */ 
/* 502 */       int tid = td.getTid();
/*     */ 
/* 504 */       int lastWriteEpoch = x.lastWrite;
/*     */ 
/* 507 */       CV fhbCV = td.cv_tools_fasttrack_FastTrackTool;
/*     */ 
/* 509 */       int lastWriter = Epoch.tid(lastWriteEpoch);
/*     */ 
/* 511 */       if ((lastWriter != tid) && (lastWriteEpoch > fhbCV.get(lastWriter))) {
/* 512 */         return false;
/*     */       }
/*     */ 
/* 515 */       if (lastReadEpoch == -1) {
/* 516 */         if (x.get(tid) != tdEpoch) {
/* 517 */           synchronized (x) {
/* 518 */             x.set(tid, tdEpoch);
/*     */           }
/*     */         }
/* 521 */         return true;
/*     */       }
/* 523 */       int lastReader = Epoch.tid(lastReadEpoch);
/* 524 */       if (lastReader == tid) {
/* 525 */         synchronized (x) {
/* 526 */           if (x.lastRead != lastReadEpoch) return false;
/* 527 */           x.lastRead = tdEpoch;
/* 528 */           return true;
/*     */         }
/*     */       }
/* 531 */       if (lastReadEpoch <= fhbCV.get(lastReader)) {
/* 532 */         synchronized (x) {
/* 533 */           if (x.lastRead != lastReadEpoch) return false;
/* 534 */           x.lastRead = tdEpoch;
/* 535 */           return true;
/*     */         }
/*     */       }
/* 538 */       synchronized (x) {
/* 539 */         if (x.lastRead != lastReadEpoch) return false;
/* 540 */         x.makeCV(4);
/* 541 */         x.set(lastReader, lastReadEpoch);
/* 542 */         x.set(td.getTid(), tdEpoch);
/* 543 */         x.lastRead = -1;
/* 544 */         return true;
/*     */       }
/*     */ 
/*     */     }
/*     */ 
/* 549 */     return false;
/*     */   }
/*     */ 
/*     */   public static boolean writeFastPath(ShadowVar gs, ShadowThread td) {
/* 553 */     if ((gs instanceof FastTrackGuardState)) {
/* 554 */       FastTrackGuardState x = (FastTrackGuardState)gs;
/*     */ 
/* 556 */       int lastWriteEpoch = x.lastWrite;
/* 557 */       int tdEpoch = td.epoch_tools_fasttrack_FastTrackTool;
/* 558 */       if (lastWriteEpoch == tdEpoch) {
/* 559 */         return true;
/*     */       }
/*     */ 
/* 562 */       int lastWriter = Epoch.tid(lastWriteEpoch);
/* 563 */       int tid = td.getTid();
/*     */ 
/* 565 */       CV tdCV = td.cv_tools_fasttrack_FastTrackTool;
/* 566 */       if ((lastWriter != tid) && (lastWriteEpoch > tdCV.get(lastWriter))) {
/* 567 */         return false;
/*     */       }
/*     */ 
/* 570 */       int lastReadEpoch = x.lastRead;
/*     */ 
/* 572 */       if (lastReadEpoch == tdEpoch) {
/* 573 */         synchronized (x) {
/* 574 */           if (x.lastWrite != lastWriteEpoch) return false;
/* 575 */           if (x.lastRead != lastReadEpoch) return false;
/* 576 */           x.lastWrite = tdEpoch;
/* 577 */           return true;
/*     */         }
/*     */       }
/*     */ 
/* 581 */       if (lastReadEpoch != -1) {
/* 582 */         int lastReader = Epoch.tid(lastReadEpoch);
/* 583 */         if ((lastReader != tid) && (lastReadEpoch > tdCV.get(lastReader))) {
/* 584 */           return false;
/*     */         }
/*     */       }
/* 587 */       else if (x.anyGt(tdCV)) {
/* 588 */         return false;
/*     */       }
/*     */ 
/* 591 */       synchronized (x) {
/* 592 */         if (x.lastWrite != lastWriteEpoch) return false;
/* 593 */         if (x.lastRead != lastReadEpoch) return false;
/* 594 */         x.lastWrite = tdEpoch;
/* 595 */         x.lastRead = tdEpoch;
/* 596 */         return true;
/*     */       }
/*     */     }
/* 599 */     return false;
/*     */   }
/*     */ }

/* Location:           /home/dan/fasttrack/fastrack-android/backup/test/tmp/
 * Qualified Name:     tools.fasttrack.FastTrackTool
 * JD-Core Version:    0.6.0
 */
