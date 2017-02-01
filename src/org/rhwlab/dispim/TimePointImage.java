/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.HashMap;
import java.util.LinkedList;
import net.imglib2.RandomAccessibleInterval;
import org.rhwlab.ace3d.Ace3D_Frame;

/**
 *
 * @author gevirl
 */
public class TimePointImage{


    public TimePointImage(RandomAccessibleInterval img,float[] mm,int time,String dataset){
        this.image = img;
        this.minmax = mm;
        this.time = time;
        this.dataset = dataset;
    }

    public int getTime(){
        return time;
    }

    public RandomAccessibleInterval getImage(){
        return this.image;
    }
    public float getMin(){
        return minmax[0];
    }
    public float getMax(){
        return minmax[1];
    }
    public long[] getDims(){
        long[] ret = new long[image.numDimensions()];
        image.dimensions(ret);
        return ret;
    }


    @Override
    public boolean equals(Object obj){
        if (obj instanceof TimePointImage){
            TimePointImage other = (TimePointImage)obj;
            if (this.time == other.time){
                if (this.dataset.equals(other.dataset)){
                    return true;
                }
            }
        }
        return false;
    }
    public String getDataset(){
        return this.dataset;
    }
    public double[] getMinPosition(){
        double[] ret = new double[image.numDimensions()];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = image.realMin(i);
        }
        return ret;
    }
    public double[] getMaxPosition(){
        double[] ret = new double[image.numDimensions()];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = image.realMax(i);
        }
        return ret;
    }
    static public double[] getMinPosition2D(int excludeDim){
        double[] ret = new double[2];
        initCache();
        RandomAccessibleInterval img = timePointCache.get(0).getImage();
        switch (excludeDim) {
            case 0:
                ret[0] = img.realMin(1);
                ret[1] = img.realMin(2);
                break;
            case 1:
                ret[0] = img.realMin(0);
                ret[1] = img.realMin(2);                
                break;
            default:
                ret[0] = img.realMin(0);
                ret[1] = img.realMin(1);                
                break;
        }
        return ret;
    }
    static public double[] getMaxPosition2D(int excludeDim){
        double[] ret = new double[2];
        initCache();
        RandomAccessibleInterval img = timePointCache.get(0).getImage();
        switch (excludeDim) {
            case 0:
                ret[0] = img.realMax(1);
                ret[1] = img.realMax(2);
                break;
            case 1:
                ret[0] = img.realMax(0);
                ret[1] = img.realMax(2);                
                break;
            default:
                ret[0] = img.realMax(0);
                ret[1] = img.realMax(1);                
                break;
        }
        return ret;
    }    
    public double minPosition(int d){
       
        return image.realMin(d);
    }
    public double maxPosition(int d){
        return image.realMax(d);
    } 
    static public double getMinPosition(int d){
        initCache();
        RandomAccessibleInterval img = timePointCache.get(0).getImage(); 
        return img.realMin(d);
    }
    static public double getMaxPosition(int d){
        initCache();
        RandomAccessibleInterval img = timePointCache.get(0).getImage(); 
        return img.realMax(d);
    }    
    static public TimePointImage getSingleImage(String dataset,int time){
        HashMap<Integer,TimePointImage> map = imageIndex.get(dataset);
        if (map != null){
            TimePointImage ret = map.get(time);
            if (ret != null){
                return ret;
            }   
        } else {
            map = new HashMap<>();
            imageIndex.put(dataset,map);
        }
        TimePointImage ret = addToCache(dataset,time);
        map.put(time, ret);
        return ret;

    }
    static private TimePointImage addToCache(String dataset,int time){
        ImageSource img = embryo.sourceForDataset(dataset);
        TimePointImage tpi = img.getImage(dataset,time);
        if (timePointCache.size()==cacheSize){
            
            // cache is full, remove the last
            TimePointImage last = timePointCache.removeLast();
            HashMap<Integer,TimePointImage> map  = imageIndex.get(dataset);
            map.remove(last.time);  // remove it from the index too
        }  
        timePointCache.addFirst(tpi);
        return tpi;
    }


    static public int[] getIntDims(){
        initCache();
        TimePointImage tpi = timePointCache.get(0);
        int[] ret = new int[tpi.getImage().numDimensions()];
        long[] r = tpi.getDims();
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = (int)r[i];
        }
        return ret;
    }
    static public void setEmbryo(ImagedEmbryo emb){
        TimePointImage.embryo = emb;
    }
    static void initCache(){
        if (timePointCache.isEmpty()){
            String dataset = Ace3D_Frame.getAllDatsets().get(0);
            ImageSource src = embryo.sourceForDataset(dataset);
            int minTime = src.getMinTime();
            addToCache(dataset,minTime);
        }
    }
    static void resetCache(){
        timePointCache.clear();
        initCache();
    }
    String dataset;  // the dataset of this TimePointImage
    int time;  // the time of this TimePointImage
    private RandomAccessibleInterval image;
    float[] minmax;
//    long[] dims;
    
    
    static ImagedEmbryo embryo;
    static int cacheSize = 20;
//    static ImageSource source;
    static LinkedList<TimePointImage> timePointCache = new LinkedList<>();   // cach of TimePointImages - any dataset any time    
    static HashMap<String,HashMap<Integer,TimePointImage>> imageIndex = new HashMap<>();  // TimePointImages indexed by dataset,time
}
