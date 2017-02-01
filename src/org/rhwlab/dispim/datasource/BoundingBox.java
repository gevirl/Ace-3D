/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

/**
 *
 * @author gevirl
 */
public class BoundingBox {
    public BoundingBox(Double[] mins,Double[] maxs){
        this.mins = mins;
        this.maxs = maxs;
    }
    public boolean isWithin(Voxel vox){
        double[] p = vox.getPoint();
        for (int d=0 ; d<p.length ; ++d){
            if (mins[d] !=null && p[d]< mins[d] ){
                return false;
            }
            if (maxs[d] !=null && p[d]>maxs[d]){
                return false;
            }
        }
        return true;
    }
    public Double getMin(int d){
        return mins[d];
    }
    public Double getMax(int d){
        return maxs[d];
    }
    Double[] mins;
    Double[] maxs;
}
