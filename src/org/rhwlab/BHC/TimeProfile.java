package org.rhwlab.BHC;

import java.io.PrintStream;

/**
 *
 * @author gevirl
 */
public class TimeProfile {
    public TimeProfile(){
        lastTime = System.currentTimeMillis();
    }
    public void report(PrintStream stream,String label){
        long currentTime=System.currentTimeMillis();
        stream.printf("%s: %d\n", label,currentTime - lastTime);
        lastTime = currentTime;
    }
    long lastTime;
}
