/******************************************************************************

Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

All rights reserved.  

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

 * Neither the names of the University of California, Santa Cruz
      and Williams College nor the names of its contributors may be
      used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 ******************************************************************************/

package rr;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.io.Serializable;
import java.io.InputStream;
import java.io.ObjectInputStream;

import rr.instrument.Instrumentor;
import rr.instrument.classes.CloneFixer;
import rr.instrument.classes.ThreadDataThunkInserter;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.replay.RRReplay;
import rr.state.AbstractArrayStateCache;
import rr.state.ArrayStateFactory;
import rr.state.agent.ThreadStateExtensionAgent;
import rr.state.agent.ThreadStateExtensionAgent.InstrumentationMode;
import rr.state.update.Updaters;
import rr.tool.RR;
import rr.tool.ToolLoader;
import acme.util.Assert;
import acme.util.Util;
import acme.util.io.URLUtils;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.TimedStmt;
import java.io.Serializable;

public class RRMain {

    static boolean hasProcessedArgs = false;

    // Set to true if RR is started normall (ie. rrrun ...)
    // false if target application invoked RR
    public static boolean runNatively = false;

	public static final class RRMainLoader extends URLClassLoader {

        public RRMainLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);

            if(parent == null) {
                System.out.println("[RRMAIN: RRMainLoader constructor parameter 'parent' is null]");
            }
            else {
                System.out.println("[RRMAIN: RRMainLoader constructor parameter 'parent' is type: " + parent.getClass().toString() + "]");
            }

            System.out.println("[RRMAIN: printing urls array with length: " + urls.length + "]");
            for(int i = 0; i < urls.length; i++) {
                System.out.println("\t" + urls[i]);
            }   
            System.out.println("[RRMAIN: class location: " + this.getClass().getResource(".") + "]");
            System.out.println("[RRMAIN: constructed object with hash: " + this.hashCode() + "]");            
            /*
            URL[] urls2 = null;
            try {
                urls2 = new URL[] {new URL("file:/home/dan/fasttrack/fastrack-android/fasttrack/./")};
            } catch (Exception e) {
                Assert.panic(e);
            }
                
            if(urls2[0].equals(urls[0])) {
                System.out.println("[TEMP: URLs seemt o match]");
            }
            */
		}
		
        @Override
		public String toString() {
			return "RRMainLoader";
		}

		@Override
		public Class<?> findClass(String name) throws ClassNotFoundException {
            System.out.println("[TEMP: is this the problem?]");
			return super.findClass(name);
		}
	}

	public static final CommandLineOption<Boolean> noInstrumentOption = 
		CommandLine.makeBoolean("noinst", false, CommandLineOption.Kind.DEPRECATED, "Do not instrument any class files.", new Runnable() { public void run() { instrumentOption.set(InstrumentationMode.NOINST); } });

	public static final CommandLineOption<InstrumentationMode> instrumentOption = 
		CommandLine.makeEnumChoice("inst", InstrumentationMode.INST, CommandLineOption.Kind.STABLE, "Instrument mode: ISNT for instrument; NOINST for no instrument; REP for build repository", InstrumentationMode.class, 
					new Runnable() { public void run() { ThreadStateExtensionAgent.addInstrumenter(instrumentOption.get());} });

	public static final CommandLineOption<Integer> infinitelyRunningThreadsOption = 
		CommandLine.makeInteger("infThreads", 0, CommandLineOption.Kind.EXPERIMENTAL, "Number of threads that loop forever.");

	public static RRMainLoader loader;

	private static volatile int runningThreads;

    // Function to allow Android app to initalize FT with proper property files
    public static void initFTSession(InputStream simplePropFH, InputStream ftPropFH, ObjectInputStream metaFH) {
        
        ToolLoader.simplePropertiesStream = simplePropFH;
        ToolLoader.ftPropertiesStream = ftPropFH;
        MetaDataInfoMaps.in = metaFH;

        MetaDataInfoMaps.readMetaData();
    }


	private static void runTargetMain(final String className,
			final String[] argv) throws InterruptedException {
		Thread appMainThread = new Thread("main") {
			@Override
			public void run() {
				try {
					Util.message("----- ----- ----- -----       Meep Meep.      ----- ----- ----- -----");
					Util.message("");

					RR.startTimer();
                
                    Class<?> cl = loader.findClass(className);
					
					Method method = method = cl.getMethod("main", new Class[] { argv.getClass() });

					/*
					 * Method main is sane ?
					 */
					int m = method.getModifiers();
					Class r = method.getReturnType();

					if (!(Modifier.isPublic(m) && Modifier.isStatic(m))
							|| Modifier.isAbstract(m) || (r != Void.TYPE)) {
						Assert.fail("In class "
								+ className
								+ ": public static void main(String[] argv) is not defined");

					}
                    // Call to main() in input test file
				    Util.message("DAN: invoking testcase main()");
                    method.invoke(null, new Object[] { argv });
                    Util.message("DAN: returned from invoking testcase main()");
					RR.endTimer();

					Util.message("");
					Util.message("----- ----- ----- -----      Thpthpthpth.     ----- ----- ----- -----");
				} catch (Exception e) {
					e.printStackTrace();
					Assert.panic(e);
				}
			}
		};
        Util.message("DAN: Thread name: " + appMainThread.getName());
		appMainThread.start();
		appMainThread.join();
	}


	public static int processArgs(String argv[]) {

        if(hasProcessedArgs) {
            return 0;
        }
    
        hasProcessedArgs = true;

		final CommandLine cl = new CommandLine("rrrun", "MainClass/EventLog");
		cl.add(new CommandLineOption<Boolean>("help", false, false, CommandLineOption.Kind.STABLE, "Print this message.") {
			@Override
			protected void apply(String arg) {
				Util.error("\n\nEnvironment Variables");
				Util.error("---------------------");
				Util.error("  RR_MODE        either FAST or SLOW.  All asserts, logging, and debugging statements\n" +
				"                 should be nested inside a test ensuring that RR_MODE is SLOW.");
				Util.error("  RR_META_DATA   The directory created on previous run by -dump from which to reload\n" +
				"                 cached metadata and instrumented class files.\n");
				cl.usage();
				Util.exit(0);
			}
		});

		cl.addGroup("General");

		cl.add(rr.tool.RR.classPathOption); 
		cl.add(rr.tool.RR.toolPathOption); 
		cl.add(rr.tool.RR.toolOption);
		cl.add(rr.tool.RR.printToolsOption); 

		//cl.add(rr.loader.LoaderContext.repositoryPathOption);

		cl.addGroup("Instrumentor");
		cl.add(noInstrumentOption); 
		cl.add(instrumentOption); 
		cl.add(rr.tool.RR.nofastPathOption);
		cl.add(InstrumentationFilter.classesToWatch);
		cl.add(InstrumentationFilter.fieldsToWatch);
		cl.add(InstrumentationFilter.methodsToWatch);
		cl.add(InstrumentationFilter.linesToWatch);
		cl.add(InstrumentationFilter.methodsSupportThreadStateParam);
		cl.add(InstrumentationFilter.noOpsOption);
		cl.add(rr.tool.RR.valuesOption);
		cl.add(ThreadDataThunkInserter.noConstructorOption);
		cl.add(CloneFixer.noCloneOption);
		cl.add(rr.tool.RR.noEnterOption);
		cl.add(Instrumentor.dumpClassOption);
//		cl.add(InstrumentingDefineClassLoader.sanityOption);
		cl.add(Instrumentor.fancyOption);
		cl.add(Instrumentor.trackArraySitesOption);
		//cl.add(ThreadStateExtensionAgent.noDecorationInline);
		//cl.addOrderConstraint(ThreadStateExtensionAgent.noDecorationInline, rr.tool.RR.toolOption);


		cl.addGroup("Monitor");
		cl.add(rr.tool.RR.xmlFileOption);
		cl.add(rr.tool.RR.noxmlOption);
		cl.add(rr.tool.RR.stackOption); 
		cl.add(rr.tool.RR.pulseOption);
		cl.add(rr.tool.RR.noTidGCOption);
		cl.add(rr.tool.RREventGenerator.noJoinOption);  
		cl.add(rr.tool.RREventGenerator.indicesToWatch);  
		cl.add(rr.tool.RREventGenerator.multiClassLoaderOption);  
		cl.add(rr.tool.RR.forceGCOption);
		cl.add(Updaters.updateOptions);
		cl.add(ArrayStateFactory.arrayOption);
		cl.add(Instrumentor.fieldOption);
		cl.add(rr.barrier.BarrierMonitor.noBarrier);
		cl.add(RR.noEventReuseOption);
		cl.add(AbstractArrayStateCache.noOptimizedArrayLookupOption);
		cl.add(infinitelyRunningThreadsOption);
		//cl.add(rr.instrument.methods.ThreadDataInstructionAdapter.callSitesOption);

		cl.addGroup("Limits");
		cl.add(rr.tool.RR.timeOutOption);
		cl.add(rr.tool.RR.memMaxOption);
		cl.add(rr.tool.RR.maxTidOption);
		cl.add(rr.error.ErrorMessage.maxWarnOption); 


		cl.addOrderConstraint(rr.tool.RR.classPathOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.tool.RR.toolPathOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.tool.RR.toolOption, rr.tool.RR.toolOption);
		cl.addOrderConstraint(rr.barrier.BarrierMonitor.noBarrier, rr.tool.RR.toolOption);

		int n = cl.apply(argv);

		RR.createDefaultToolIfNecessary();

		if (n >= argv.length) {
			Assert.fail("Missing class name. Use -help for summary of options.");
		}
		return n;
	}

	private static void waitForAllThreads() {
//		while (runningThreads < infinitelyRunningThreadsOption.get()) {
//			acme.util.Util.logf("Waiting for Thread Count to go over %d.  Current Count: %d", infinitelyRunningThreadsOption.get(), runningThreads);
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				acme.util.Assert.warn("Interrupted...");
//			}
//		}


		while (runningThreads > infinitelyRunningThreadsOption.get()) {
			acme.util.Util.logf("Waiting for Thread Count to reach %d.  Current Count: %d", infinitelyRunningThreadsOption.get(), runningThreads);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				acme.util.Assert.warn("Interrupted...");
			}
		}
	}

	/**
	 * Default main method used as wrapper, expects the fully qualified class
	 * name of the real class as the first argument.
	 */
	public static void main(final String[] argv) {

		Thread.currentThread().setName("RR");
        Util.log("DAN: hellooooo");
        Util.log("DAN: " + argv.length);
		/*
		 * Expects class name as first argument, other arguments are by-passed.
		 */
		final int n = processArgs(argv);
        Util.log("Size of argv: " + argv.length);
//        Util.log("ARGV: " + argv);

		ThreadStateExtensionAgent.addInstrumenter(instrumentOption.get());

		String urls = RR.classPathOption.get();

		Util.log("System Class Path = " + Arrays.toString(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), urls)));
		loader = new RRMainLoader(URLUtils.getURLArrayFromString(System.getProperty("user.dir"), urls), RRMain.class.getClassLoader());

		RR.startUp();

		try {
			final String fileName = argv[n].replace("/", ".");
			final String[] newArgv = new String[argv.length - (n + 1)];

			System.arraycopy(argv, n + 1, newArgv, 0, newArgv.length);
			if (!fileName.endsWith(".rrlog")) {
				runNormally(fileName, newArgv);
                Util.log("DAN: returned from runNormally()");
			} else {
				replay(fileName, newArgv);
			}
		} catch (RuntimeException e) {
			Util.log("Cleaning up after RuntimeException " + e + "...");
		} catch (Exception e) {
			Assert.panic(e);
		}
        Util.log("DAN: about to shut down RR");
		RR.shutDown();		
        Util.log("DAN: finished shutting down RR");
		Util.exit(0);
	}


	private static void runNormally(final String className, final String newArgv[]) throws Exception {

		Util.log(new TimedStmt("Running target") {
			@Override
			public void run() throws Exception {
				runTargetMain(className, newArgv);
				Util.log("DAN: finished call to runTargetMain()");
                waitForAllThreads();
                Util.log("DAN: done waiting for all threads");
			}
		});
	}

	private static void replay(final String className, final String newArgv[]) throws Exception {

		Util.log(new TimedStmt("Running Replay of " + className) {
			@Override
			public void run() throws Exception {
				RRReplay replay = new RRReplay(className);
				replay.go();
			}
		});
	}


	/***********/

	public static final int MODE_SLOW = 0;
	public static final int MODE_FAST = 1;
	private static final String modeNames[] = { "SLOW", "FAST" };

	public static final int mode;

	static {
		String m = Util.getenv("RR_MODE", "SLOW");
		if (m.equals("FAST")) {
			mode = MODE_FAST;
			Util.log("Running in FAST Mode");
		} else if (m.equals("SLOW")) {
			mode = MODE_SLOW;
			Util.log("Running in SLOW Mode");
		} else {
			Assert.fail("RR_MODE environment variable is '" + m + "'.  It must be SLOW or FAST.");
			mode = MODE_SLOW; // bogus
		}
	}

	public static final boolean fastMode() {
		return mode == MODE_FAST;
	}

	public static final boolean slowMode() {
		return mode == MODE_SLOW;
	}

	public static String modeName() {
		return modeNames[mode];
	}


	public static synchronized void incThreads() {
		runningThreads++;
	}

	public static synchronized void decThreads() {
		runningThreads--;
	}

}