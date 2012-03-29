package rr.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import rr.meta.ClassInfo;
import rr.meta.FieldInfo;
import rr.meta.InstrumentationFilter;
import rr.meta.MetaDataInfoMaps;
import rr.state.agent.DefineClassListener;
import acme.util.Assert;
import acme.util.Util;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;
import acme.util.time.TimedExpr;

public class InstrumentingDefineClassLoader implements DefineClassListener {

	public static CommandLineOption<Boolean> sanityOption  = 
		CommandLine.makeBoolean("sanity", false, CommandLineOption.Kind.EXPERIMENTAL, "Check whether uninstrumented classes contain synchronization operations that will be ignored.");

	public synchronized byte[] define(ClassLoader definingLoader, final String name, final byte[] bytes)   {
	
        System.out.println("CACHE: calling define with name: " + name);

        final LoaderContext currentLoader = Loader.get(definingLoader);
		final String internalName = name.replace('.', '/');
		final ClassInfo rrClass = MetaDataInfoMaps.getClass(internalName);
		if (rrClass.isSynthetic()) {
            System.out.println("CACHE: returning because of synthetic class");
			return bytes;
		} else {
			Loader.classes.put(internalName, currentLoader); 
			if (!InstrumentationFilter.shouldInstrument(rrClass)) {
				System.out.println("CACHE: Skipping " + name + " (Loader=" + Util.objectToIdentityString(definingLoader) + ")");
				MetaDataBuilder.preLoadFully(currentLoader, bytes);
				if (sanityOption.get()) {
					currentLoader.sanityCheck(new ClassReader(bytes));
					Loader.sanityCheckedFiles.add(name);
				}
				Loader.writeToFileCache("classes", rrClass.getName(), bytes);
				Loader.skippedFiles.add(name);
				return bytes;
			} else {		
				Loader.instrumentedFiles.add(name);

				byte[] bytes2 = Loader.readFromFileCache("classes", rrClass.getName());
				if (bytes2 != null) {
					System.out.println("CACHE: Found cached version of " + name);
					MetaDataBuilder.preLoadFully(currentLoader, new ClassReader(bytes2));
					for (FieldInfo f : rrClass.getFields()) {
						if (InstrumentationFilter.shouldInstrument(f)) {
							f.getUpdater();
						}
					}
					return bytes2;
				}
				try {
					return Util.eval(new TimedExpr<byte[]>("WInstrumenting " + name + " (Loader=" + Util.objectToIdentityString(definingLoader) + ")") {
						@Override
						public byte[] run() {
							MetaDataBuilder.preLoadFully(currentLoader, bytes);
							final ClassWriter instrument = currentLoader.instrument(internalName, bytes); 
							byte[] bytes2 = instrument.toByteArray();
							Loader.writeToFileCache("classes", rrClass.getName(), bytes2);
                            
                            // ADDED BY DAN
                            // Probably totally wrong
                            try {
                                if(name.equals("test/Test")) {
                                    System.out.println("DAN: writing file to tmp directory");
                                    String name2 = "/home/dan/fasttrack/stephenfreund-RoadRunner-20b650b/test/tmp/Test.class";
                                    FileOutputStream fos = new FileOutputStream(name2);
                                    fos.write(bytes2);
                                    fos.close();
                                }
                                else System.out.println("DAN: you suck" + name);
                            }
                            catch (Exception e) {
                                System.out.println("DAN: file write failed");
                            }
							//////////////////////////////////////
                            
                            return bytes2;
						}
					});
				} catch (Exception e) {
					Assert.panic(e);
					return null;
				}
			}
		}
	}

}


/*

System.err.println("CLASS " + className + " " + definingLoader);
System.err.println("MOO");
if (className.startsWith("rr/") || className.startsWith("java/") || className.startsWith("acme/") || className.startsWith("org/asm/")) {
	System.err.println("DONE");
	return bytes;
}

System.err.println("MOO1");
final LoaderContext currentLoader = Loader.get(definingLoader);
final String internalName = className.replace('.', '/');
System.err.println("MOO2");
final ClassInfo rrClass = MetaDataInfoMaps.getClass(internalName);
System.err.println("MOO3");
if (rrClass.isSynthetic()) {
	System.err.println("MOO4");
	return bytes;
} else {
	System.err.println("MOO5");
	Loader.classes.put(internalName, currentLoader); 
	if (!InstrumentationFilter.shouldInstrument(rrClass)) {
		//				Util.log("Skipping " + name + " (Loader=" + Util.objectToIdentityString(definingLoader) + ")"); 
		try {
			MetaDataBuilder.preLoadFully(currentLoader, bytes);
		} catch (ClassNotFoundException e) {
			Util.log(e);
		}
		//				if (sanityOption.get()) {
		//					currentLoader.sanityCheck(new ClassReader(bytes));
		//					Loader.sanityCheckedFiles.add(name);
		//				}
		//				Loader.writeToFileCache("classes", rrClass.getName(), bytes);
		//				Loader.skippedFiles.add(name);
		System.err.println("MOO6");
		return bytes;
	} else {		
		System.err.println("MOO7");
		try {
			Loader.instrumentedFiles.add(className);

			byte[] bytes2 = Loader.readFromFileCache("classes", rrClass.getName());
			if (bytes2 != null) {
				Util.logf("Found cached version of %s", className);
				MetaDataBuilder.preLoadFully(currentLoader, new ClassReader(bytes2));
				for (FieldInfo f : rrClass.getFields()) {
					if (InstrumentationFilter.shouldInstrument(f)) {
						f.getUpdater();
					}
				}
				return bytes2;
			}
			return Util.eval(new TimedExpr<byte[]>("Instrumenting " + className + " (Loader=" + Util.objectToIdentityString(definingLoader) + ")") {
				@Override
				public byte[] run() throws ClassNotFoundException {
					try {
						MetaDataBuilder.preLoadFully(currentLoader, bytes);
						final ClassWriter instrument = currentLoader.instrument(internalName, bytes); 
						byte[] bytes2 = instrument.toByteArray();
						Loader.writeToFileCache("classes", rrClass.getName(), bytes2);
						return bytes2;
					} catch (ClassNotFoundException e) {
						Assert.warn(e);
						throw e;
					}
				}
			});
		} catch(ClassNotFoundException e) {
			try {
				throw e;
			} catch (ClassNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} catch(Exception e) {
			Assert.panic(e);
			return null;
		}
	}

}
*/