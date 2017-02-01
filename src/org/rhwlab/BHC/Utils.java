/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

/**
 *
 * @author gevirl
 */
public class Utils {
    static public Double eexp(Double x){
        if (x.isNaN()){
            return 0.0;
        }
        double ret = Math.exp(x);
        if (!Double.isFinite(x)){
            System.exit(2);  // crash for now
        }
        return ret;
    }
    static public Double eln(Double x){
        if (x > 0.0){
            return Math.log(x);
        }
        if (x == 0.0){
            return Double.NaN;
        }
        System.exit(3);  // crash for now
        return Double.NaN;
    }
    // return ln(x+y), given lnX and lnY
    static public Double elnsum(Double lnx,Double lny){
        if (lnx.isNaN()){
            return lny;
        } else if (lny.isNaN()){
            return lnx;
        } else {
            Double del;
            Double ret;
            if (lnx > lny){
                del = lny - lnx;
                ret = lnx + eln(1.0 + eexp(del));
            } else {
                del = lnx - lny;
                ret = lny + eln(1.0 + eexp(del));
            }
            if (!Double.isFinite(ret)){
                System.exit(4);
            }
            return ret;
        }
    }
    static public Double elnMult(Double lnx,Double lny){
        if (lnx.isNaN() || lny.isNaN()){
            return Double.NaN;
        }
        Double sum = lnx + lny;
        if (!Double.isFinite(sum)){
            System.exit(5);
        }
        return sum;
    }
    // return ln (x to the power n)
    static public Double elnPow(Double lnx,double n){
        if (lnx.isNaN()){
            return lnx;
        }
        Double ret = n * lnx;
        if (!Double.isFinite(ret)){
            System.exit(6);
        }
        return ret;
    }
    public static void main(String[] args){
        Double t = Math.exp(-1000.0);
        int uisadfhu=0;
    }
}
