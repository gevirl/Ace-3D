/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class Sphere {
    double[] center;
    double radius;
    
    public Sphere(Nucleus nuc,double r){
        this(r,nuc.getNucleusData().getCenter());
    }
    public Sphere(double r,double[] c){
        this.radius = r;
        this.center = c;
    }
    public boolean isInside(Nucleus nuc){
        double[] nucCenter = nuc.getNucleusData().getCenter();
        double sum = 0.0;
        for (int i=0 ; i< center.length ; ++i){
            double del = center[i] - nucCenter[i];
            sum = sum + del*del;
        }
        return Math.sqrt(sum) <= radius;
    }
    public double getRadius(){
        return this.radius;
    }
    public double[] getCenter(){
        return this.center;
    }
}
