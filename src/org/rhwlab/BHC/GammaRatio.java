/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.HashMap;
import org.apache.commons.math3.special.Gamma;

/**
 *
 * @author gevirl
 */
public class GammaRatio {
    public GammaRatio(int d,int nu){
        this.d =d;
        this.nu = nu;
        ratios = new HashMap<>();
    }
    public double getRatio(int n){

        Double ret = ratios.get(n);
        if (ret != null){
            return ret;
        }
        
        double logr = 0.0;
        double r;
        if (n % 2 == 0){
            
            for (int j=1 ; j<=n/2 ; ++j){
                for (int i=1 ; i<=d ; ++i){
                    logr = logr + Math.log( ((nu-1.0-i)/2.0) + j);
                }
            }
            r = Math.exp(logr);
            ratios.put(n,r);
        } else {
            for (int j=1 ; j<=(n-1)/2 ; ++j){
                for (int i=1 ; i<=d ; ++i){
                    logr = logr + Math.log( ((nu-i)/2.0) +j);
                }
            }
            for (int i=1 ; i<=d ; ++i){
                logr = logr + Gamma.logGamma((nu+2.0-i)/2.0) - Gamma.logGamma((nu+1.0-i)/2.0);
            }
            r = Math.exp(logr);
        }

        if (!Double.isFinite(r)){
            System.out.println("Gamma ratio error");
            System.exit(n);
        }
        return r;
    }
    static public void main(String[] args){
        int d = 3;
        int nu = 4;
        GammaRatio ratio = new GammaRatio(d,nu);
        int n = 1;
        while(true){
            double nuP = nu + n;
            double r = 1.0;
            for (int i=1 ; i<=d ; ++i){
                r = r * Gamma.gamma((nuP+1.0-i)/2.0)/Gamma.gamma((nu+1.0-i)/2.0);
            }
            System.out.printf("%d  Ratio = %e   byGamma = %e\n",n, ratio.getRatio(n),r);
            ++n;
        }
    }
    int d;
    int nu;
    HashMap<Integer,Double> ratios;
}
