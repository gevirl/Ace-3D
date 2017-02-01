/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class MicroCluster4D extends MicroCluster {
    public MicroCluster4D(double[] v,short[][] points,int[] intensities,double prob){
        super(v,points,intensities,prob);
    }
    public MicroCluster4D (Element ele){
        super(ele);
    }
    public RealVector asRealVector(){
        double[] vec = new double[v.length +1];
        for (int i=0 ; i<v.length ; ++i){
            vec[i] = v[i];
        }
        vec[v.length-1] = this.getAverageIntensity();
        return new ArrayRealVector(vec);
    }  
    
    
}
