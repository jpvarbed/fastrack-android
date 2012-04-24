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

package rr.meta;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.URL;

import rr.RRMain;
import rr.loader.Loader;
import rr.loader.LoaderContext;
import rr.state.update.AbstractFieldUpdater;
import acme.util.Assert;

import rr.state.update.SafeFieldUpdater;
//import updaters.__$rr_test_Test__$rr__Update_y;

public class FieldInfo extends MetaDataInfo {
	protected final ClassInfo rrClass;
	protected final String name;
	protected final String descriptor;
	protected boolean isVolatile;
	protected boolean isStatic; 
	protected boolean isFinal; 
	protected final boolean isSynthetic;

	protected transient AbstractFieldUpdater updater = null;

	public FieldInfo(int id, SourceLocation loc, ClassInfo rrClass, String name, String descriptor, boolean isSynthetic) {
		super(id, loc);
		this.rrClass = rrClass;
		this.name = name;
		this.descriptor = descriptor;
		this.isSynthetic = isSynthetic;
	} 

	public void setFlags(boolean isFinal, boolean isVolatile, boolean isStatic) {
		this.isFinal = isFinal;
		this.isVolatile = isVolatile;
		this.isStatic = isStatic;
	}

	public ClassInfo getOwner() {
		return rrClass;
	}

	public String getName() {
		return name;
	}

	public String getDescriptor() {
		return descriptor;
	}

	public boolean isVolatile() {
		return isVolatile;
	}

	public boolean isStatic() {
		return isStatic;
	}

	public boolean isFinal() {
		return isFinal;
	}


	public boolean isSynthetic() {
		return isSynthetic;
	}


	public AbstractFieldUpdater getUpdater() {

                
        if (updater == null) {
            

                System.out.println("[getUpdater: getting loader for field: " + name + "]");
                /*
                
                if(name.equals("y") && RRMain.runNatively == false) {
                    
                    try {
                        System.out.println("[getUpdater: reading updater object from disk]");

                        FileInputStream fin = new FileInputStream("/home/dan/fasttrack/fastrack-android/fasttrack/test/tmp/obj.ser");
                        ObjectInputStream ois = new ObjectInputStream(fin);
                        //updater = (__$rr_test_Test__$rr__Update_y) ois.readObject();
                        updater = (SafeFieldUpdater) ois.readObject();
                        ois.close();
                
                        System.out.println("[getUpdater: successfully read updater from file!]"); 
                
                    } catch (Exception e) {
                        System.out.println("[ERROR: failed reading updater from file!]");
                        System.out.println("[ERROR: exception message: " + e.getMessage());
                        System.out.println("[ERROR: exception string: " + e.toString());
                        Assert.panic(e);
                    } 
                }
                else {*/
                    try {
                        /* 
                        try {
                            System.out.println("[getUpdater: reading definingLoader from disk]");

                            FileInputStream fin = new FileInputStream("/home/dan/fasttrack/fastrack-android/fasttrack/test/tmp/obj2.ser");
                            ObjectInputStream ois = new ObjectInputStream(fin);
                            ClassLoader cl = (ClassLoader) ois.readObject();
                            ois.close();
                    
                            LoaderContext currentLoader = Loader.get(cl);
                            Loader.classes.put("test/Test", currentLoader);

                            System.out.println("[getUpdater: successfully read updater from file!]"); 

                        } catch (Exception e) {
                            System.out.println("[ERROR: failed reading definingLoader from file!]");
                            System.out.println("[ERROR: exception message: " + e.getMessage());
                            System.out.println("[ERROR: exception string: " + e.toString());
                            Assert.panic(e);
                        }
                        */

                        final LoaderContext loaderForClass = Loader.loaderForClass(rrClass.getName());
    
                        if(loaderForClass == null) {
                            
                            System.out.println("[ERROR: getUpdater: loaderForClass is null! class: " + rrClass.getName() + "]");
                        }				
                            
                        final Class guardStateThunk = loaderForClass.getGuardStateThunk(rrClass.getName(), name, isStatic);
                        setUpdater((AbstractFieldUpdater) guardStateThunk.newInstance());
                    
                    } catch (Throwable e) {
                        Assert.panic(e);
                    }
               // }
        }
        
        
		return updater;
	}


	@Override
	protected String computeKey() {
		return MetaDataInfoKeys.getFieldKey(this.getOwner(), getName(), this.getDescriptor());
	}


	@Override
	public void accept(MetaDataInfoVisitor v) {
		v.visit(this);
	}

	public void setUpdater(AbstractFieldUpdater guardStateThunk) {
  
        //System.out.println("[DEBUG: dumping stack in FieldInfo.java->setUpdater()]");
        //Thread.dumpStack(); 
        
        System.out.println("[setUpdater: parameter of type: " + guardStateThunk.getClass().toString());
        URL u = guardStateThunk.getClass().getClassLoader().getResource(".");
        System.out.println("[URL: location: " + u.toString() + "]");
        System.out.println("[URL: object's class: " + guardStateThunk.getClass().toString());
        
        /*
        try {
            FileOutputStream fout = new FileOutputStream("/home/dan/fasttrack/fastrack-android/fasttrack/test/tmp/obj.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fout);
            oos.writeObject(guardStateThunk);
            oos.close();
            System.out.println("[IO: successfully wrote guardStateThunk to disk.]");

        } catch (IOException e) {
            System.out.println("[ERROR: failed writing guardStateThunk to file!]");
            System.out.println("[ERROR: exception message: " + e.getMessage());
            System.out.println("[ERROR: exception string: " + e.toString());
        }	
        */
        updater = guardStateThunk;

	}
}
