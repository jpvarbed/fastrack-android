package rr.loader;

import rr.state.agent.DefineClassListener;

public class NonInstrumentingPreDefineClassLoader implements DefineClassListener {
	public byte[] define(ClassLoader definingLoader, final String name, final byte[] bytes)  {
		return bytes;
	}
}
