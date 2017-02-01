/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;

/**
 *
 * @author gevirl
 */
public class Testing {
    static public void main(String[] args){
        String[] tokens = "0.011332651026552858 3.41268617023919E-4 0.0018087352262063756 3.4126861702391887E-4 0.013068389158168082 -0.0018712410627799538 0.001808735226206376 -0.0018712410627799538 0.012536567307833844".split(" ");
        
        RealMatrix m = new Array2DRowRealMatrix(3,3);
        for (int r=0 ; r<3 ; ++r){
            for (int c=0 ; c<3 ; ++c){
                m.setEntry(r, c, Double.valueOf(tokens[3*r+c]));
            }
        }
        LUDecomposition lud = new LUDecomposition(m);
        RealMatrix mInv = lud.getSolver().getInverse();
        EigenDecomposition decomp = new EigenDecomposition(lud.getSolver().getInverse());
        double[] e = decomp.getRealEigenvalues();
        int thsdfui=0;
    }
}
