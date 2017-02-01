/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.command;

import java.io.*;
import java.util.*;
/**
 *
 * @author gevirtzm
 */
/* class to encapsulate program logging to a PrintStream
 */
public class Logging {
    public Logging() {
        stream = System.out;
        date = new Date();
        level = 0;
    }
    public void useStream(PrintStream aStream) {
        if (aStream != null){
            stream = aStream;
        }
    }
    // set the logging level
    // will print messages that have a level higher than the set level
    // setting the level to zero turns off logging
    // any negaive value turns off logging too
    public void setLevel(int lev) {
        level = lev;
        if (level < 0)
            level = 0;
    }
    public void setLevel(String strLev){
        try {
            setLevel(Integer.parseInt(strLev));
        }
        catch (Exception ex){
            setLevel(0);
        }
    }
    // log the msg to the log file
    public void logIt(Object source,int lev,String msg){
        if (lev <= 0 ) return;  // no logging for messages with negative or zero level values
        if (lev <= level) {    // log the message if the set level is high enough
            date.setTime(System.currentTimeMillis());
            Class cl = source.getClass();
            String clName = cl.getName();
            stream.printf("%s %s: %s\n",date.toString(),clName,msg);
        }
    }
    public int getLevel(){
        return level;
    }
    PrintStream stream;
    int level;
    Date date;
}
