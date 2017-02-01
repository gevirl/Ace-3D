/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import org.apache.commons.math3.dfp.Dfp;
import org.apache.commons.math3.dfp.DfpField;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author gevirl
 */
public class GammaTest {
    static public void main(String[] args){
        int n = 1000;
        DfpField field= new DfpField(20); 
        for (int i=0 ; i<n ; ++i){
            Dfp G = field.newDfp(Gamma.logGamma(i)).exp();
            System.out.printf("%d: %e %e %s\n",i,Gamma.gamma(i),Gamma.logGamma(i),G.toString());
        }
    }
}
