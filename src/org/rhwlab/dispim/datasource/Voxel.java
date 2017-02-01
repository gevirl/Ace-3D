/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.ml.clustering.Clusterable;

/**
 *
 * @author gevirl
 */
public class Voxel implements Clusterable {
    public Voxel(long[] v,int i,double adj){
        this(v,i);
        this.adjustedIntensity = adj;
    }
    public Voxel(long[] v,int i){
        this(new ArrayRealVector(toDouble(v)),i,0.0);
    }
    public Voxel(RealVector v,int i,double adj){
        this.coords = v;
        this.intensity = i;
        this.adjustedIntensity = adj;
    }
    @Override
    public double[] getPoint() {
        return coords.toArray();
    }
    
    public int getIntensity(){
        return intensity;
    }
    // convert a long[] to double[]
    static double[] toDouble(long[] v){
        double[] ret = new double[v.length];
        for (int i=0 ;i <v.length ; ++i){
            ret[i] = v[i];
        }
        return ret;
    }
    public void setAdjusted(double v){
        this.adjustedIntensity = v;
        
    }
    public double getAdjusted(){
     return this.adjustedIntensity;
//        return 1.0;
    }
    public RealVector getAsVector(){
        return this.coords;
    }

    public RealVector coords;
    public int intensity; 
    public double adjustedIntensity;
}
