/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import org.apache.commons.math3.linear.ArrayRealVector;

/**
 *
 * @author gevirl
 */
public class LabeledRealVector extends ArrayRealVector {
    public LabeledRealVector(double[] v,int label){
        super(v);
        this.label = label;
    }
    public int getLabel(){
        return label;
    }
    int label;
}
