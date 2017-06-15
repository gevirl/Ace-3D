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
