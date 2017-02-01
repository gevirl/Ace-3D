/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.rhwlab.command.CommandLine;
import org.rhwlab.dispim.datasource.BoundingBox;

/**
 *
 * @author gevirl
 */
public class Nuclei_IdentificationCLI extends CommandLine {

    @Override
    public void init() {
        mins = new Double[3];
        maxs = new Double[3];
        for (int i=0 ; i<3 ; ++i){
            mins[i] = maxs[i] = null;
        }
    }

    @Override
    public String post() {
        return null;
    }

    @Override
    public String noOption(String s) {
        this.segTiff = s;
        File file = new File(s);
        if (file.exists()){
            return null;
        }
        return String.format("File: %s cannot be found", s);
    }
    
    public String lineageTiff(String s){
        this.lineTiff = s;
        File file = new File(s);
        if (file.exists()){
            return null;
        }
        return String.format("File: %s cannot be found", s);        
    }

    @Override
    public void usage() {
        System.out.println("Nuclei_Identification [Options] segmentedTiff");
        System.out.println("Options:");
        System.out.println("\t-first TIME");
        System.out.println("\t-last TIME");
        System.out.println("\t-force\tforce recalculation for the time point(s)");
        System.out.println("\t-qsub\trun on the whead cluster");
    }
    public String xMin(String s){
        try {
            mins[0] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -xMin %s", s);
        }
        return null;          
    }
    public String xMax(String s){
        try {
            maxs[0] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -xMax %s", s);
        }
        return null;          
    }    
    public String yMin(String s){
        try {
            mins[1] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -yMin %s", s);
        }
        return null;          
    }
    public String yMax(String s){
        try {
            maxs[1] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -yMax %s", s);
        }
        return null;          
    }  
        public String zMin(String s){
        try {
            mins[2] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -zMin %s", s);
        }
        return null;          
    }
    public String zMax(String s){
        try {
            maxs[2] = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -zMax %s", s);
        }
        return null;          
    }  
    public String segTiff(String s){
        return noOption(s);
    }
    public String bhcDir(String s){
        File dir = new File(s);
        if (dir.isDirectory()){
            this.bhcDir = dir.getPath();
            return null;
        }
        return s;
    }
    public String seriesDir(String s){
        File dir = new File(s);
        if (dir.isDirectory()){
            this.seriesDir = dir.getPath();
            return null;
        }
        return s;
    }    
    public String S(String s){
        try {
            S = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -S %s", s);
        }
        return null;   
    }
    public String alpha(String s){
        try {
            alpha = Double.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -alpha %s", s);
        }
        return null;        
    }
    public String segThresh(String s){
        try {
            segThresh = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -segThresh %s", s);
        }
        return null;        
    }    
    public String first(String s){
        try {
            firstTime = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -first %s", s);
        }
        return null;
    }
    public String last(String s){
        try {
            lastTime = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -last %s", s);
        }
        return null;
    }   
    public String nu(String s){
        try {
            nu = Integer.valueOf(s);
        } catch (Exception exc){
            return String.format("Error in option -nu %s", s);
        }
        return null;
    }       
    public String force(String s){
        
        this.force = s;
        return null;
    }
    public void qsub(){
        this.qsub = true;
    }
    public String memory(String s){
        this.memory = s;
        return null;
    }
    public void study(){
        this.study = true;
    }
    
    public Integer getFirstTime(){
        return this.firstTime;
    }
    public Integer getLastTime(){
        return this.lastTime;
    }
    public String getForce(){
        return this.force;
    }
    public boolean getQsub(){
        return this.qsub;
    }
    public boolean getStudy(){
        return this.study;
    }
    public String getBHCDirectory(){
        return this.bhcDir;
    }
    public String getSegmentTiff(){
        return this.segTiff;
    }
    public String getLineTiff(){
        return this.lineTiff;
    }
    public String getMemory(){
        return memory;
    }
    public Double getAlpha(){
        return alpha;
    }
    public Double getS(){
        return this.S;
    }
    public Integer getSegThresh(){
        return segThresh;
    }
    public Integer getNu(){
        return nu;
    }
    public BoundingBox getBoundingBox(){
        return new BoundingBox(mins,maxs);
    }
    static public TreeMap<Integer,String[]> getMVRFiles(String seriesDir,int start,int end){
        File mvrDir = new File(seriesDir,"MVR_STACKS");
        File[] files = mvrDir.listFiles();
        TreeMap<Integer,String[]> ret = new TreeMap<>();
        
        // find a Probabilities file
        File typical = null;
        for (File file : files){
            if (file.getName().contains("Probabilities")){
                typical = file;
                break;
            }
        }
        Pattern segPattern = Pattern.compile("TP(\\d{1,4})(_Ch.+)_Probabilities.+");
        Matcher m = segPattern.matcher(typical.getName());
        m.matches();
        String base = m.group(2);      
        Pattern linePattern = Pattern.compile("TP(\\d{1,4})"+base+".tif");
        for (File file : files){
            m = segPattern.matcher(file.getName());
            if (m.matches()) {
                int t =Integer.valueOf(m.group(1));
                if (start <= t && t <=end){
                    String[] names = ret.get(t);
                    if (names == null){
                        names= new String[2];
                        ret.put(t,names);
                    }
                    names[1] = file.getPath();
                }
            } else {
                m = linePattern.matcher(file.getName());
                if (m.matches()){
                    int t =Integer.valueOf(m.group(1));
                    if (start <= t && t <=end){
                        String[] names = ret.get(t);
                        if (names == null){
                            names= new String[2];
                            ret.put(t,names);
                        } 
                        names[0] = file.getPath();
                    }
                }
            }
        }
        return ret;
    }
    static public TreeMap<Integer,String[]> getTiffs(String segFile,String lineFile,Integer start,Integer end){
        TreeMap<Integer,String> segMap = getFiles(segFile,start,end);
        TreeMap<Integer,String> lineMap = getFiles(lineFile,start,end);
        TreeMap<Integer,String[]> timeMap = new TreeMap<>();
        for (int time : lineMap.keySet()){
            String seg = segMap.get(time);
            if (seg != null){
                String[] names = new String[2];
                names[0] = lineMap.get(time);
                names[1] = segMap.get(time);
                timeMap.put(time,names);
            }
        }
        return timeMap;
    }

    static public TreeMap<Integer,String> getFiles(String typical,Integer start,Integer end){
        TreeMap ret = new TreeMap<>();
        File tiff = new File(typical);
        File dir = tiff.getParentFile(); 

        File[] files = dir.listFiles();
        
        Pattern p = p = Pattern.compile("(TP)(\\d{1,4})(_.+)");      
        Matcher m = p.matcher(tiff.getName());
        if (!m.matches()){
            p = Pattern.compile("(.+)(\\d{3})(.+)");
            m = p.matcher(tiff.getName());
        }
        String prefix = m.group(1);
        String suffix = m.group(3);
        
        
        p = Pattern.compile(prefix+"(\\d{1,4})"+suffix);
        for (File file : files){
            m = p.matcher(file.getName());
            if (m.matches()){
                int time = Integer.valueOf(m.group(1));
                if (start != null && time < start){
                    continue;
                }
                if (end != null && time > end){
                    continue;
                }
                ret.put(time, file.getPath());
            }
        }
       
        return ret;
    }
    Double[] mins;
    Double[] maxs;
    String memory;
    String bhcDir;
    String seriesDir;
    String lineTiff;
    String segTiff;
    Integer firstTime;
    Integer lastTime;
    Double alpha;
    Double S;
    Integer segThresh;
    Integer nu;
    String force=null;
    boolean qsub = false;
    boolean study = false;
}
