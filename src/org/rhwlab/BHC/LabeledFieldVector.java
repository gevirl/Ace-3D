package org.rhwlab.BHC;

import org.apache.commons.math3.FieldElement;
import org.apache.commons.math3.linear.ArrayFieldVector;

/**
 *
 * @author gevirl
 */
public class LabeledFieldVector extends ArrayFieldVector {
    public LabeledFieldVector(FieldElement[] elements,int label){
        super(elements);
        this.label = label;
    }
    public int getLabel(){
        return label;
    }
    int label;
}
