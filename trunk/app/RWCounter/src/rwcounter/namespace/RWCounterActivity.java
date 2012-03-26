package rwcounter.namespace;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class RWCounterActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        runCountTest();
    }
    
    private void runCountTest()
    {
    	Log.v("RWCounter","Starting counter loop...");
    	
    	int[] x = new int[10];
    	
    	// Perform ten writes
    	for(int i = 0; i < 10; i++) {
    		x[i] = i;
    	}
    	
    	// Perform ten reads
    	int total = 0;
    	for(int i = 9; i >=0; i--) {
    		total += x[i];
    	}
    	
    }	
}