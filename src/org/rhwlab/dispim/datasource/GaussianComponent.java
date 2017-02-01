/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.rhwlab.dispim.datasource.VoxelDataSource;

/**
 *
 * @author gevirl
 */
public class GaussianComponent {
    public GaussianComponent(VoxelDataSource src,int id){
        this.id = id;
        this.source = src;
        this.indexes = new HashSet<>();
        this.xBar = new ArrayRealVector(source.getD());
        this.S = new Array2DRowRealMatrix(source.getD(),source.getD());         
    }
    // add a vector point to this componenet, recalulate statistics
    public void addPoint(int index){
        this.addPoint(index,true);
    }
    // add a vector point to this component and recalculate if calc == true
    public void addPoint(int index, boolean calc){
        indexes.add(index);
        if (calc){
            calculateStatistics();
        }
    }
    //remove a point and recalculate stats
    public void removePoint(int index){
        if (indexes.contains(index)) {
            indexes.remove(index);
            calculateStatistics();
        }
    }
    // calculate statistics
    public void calculateStatistics(){

        // compute mean
        xBar.set(0.0);
        for (Integer index : indexes){
            RealVector x = source.get(index).coords;
            xBar = xBar.add(x);
        }
        xBar.mapDivideToSelf(indexes.size());
/*        
        kappa = kappa0 + indexes.size();
        
        nu = nu0 + indexes.size();
        
        m = xBar.mapMultiply(indexes.size()).add(m0.mapMultiply(kappa0)).mapDivide(kappa);
        
        // compute covariance
        S = S0.copy();
        for (Integer index : indexes){
            RealVector x = source.get(index).coords;
            S = S.add(x.outerProduct(x));
        }
        S = S.add(m0.outerProduct(m0).scalarMultiply(kappa0));
        S = S.subtract(m.outerProduct(m).scalarMultiply(kappa));
  */      
        int hsafdiu=0;
    }
    public int getN(){
        return indexes.size();
    }
    public RealVector getMean(){
        return this.xBar;
    }
    public Set<Integer> getIndexes(){
        return indexes;
    }
    public RealVector getM(){
        return m;
    }
    static void setKappa0(double v){
        kappa0 = v;
    }
    static void setNu0(double v){
        nu0 = v;
    }
    static void setM0(RealVector v){
        m0 = v;
    }
    static void setS0(RealMatrix s0){
        S0 = s0;
    }
    // calculate the mean of all the data points in a list of microclusters
    public RealVector mean() {
        // compute mean
        xBar.set(0.0);
        for (Integer index : indexes){
            RealVector x = source.get(index).coords;
            xBar = xBar.add(x);
        }
        xBar.mapDivideToSelf(indexes.size());
        return xBar;
    }
    public RealMatrix precision(RealVector mu){
        RealMatrix ret = new Array2DRowRealMatrix(mu.getDimension(),mu.getDimension());
        ret.scalarMultiply(0.0);  // make sure it is zero
        int n = 0;
        for (Integer index : indexes){
            RealVector x = source.get(index).coords;
            RealVector del = x.subtract(mu);
            ret = ret.add(del.outerProduct(del)); 
            ++n;
        }

        ret = ret.scalarMultiply(1.0/n);
        LUDecomposition lud = new LUDecomposition(ret);
        RealMatrix prec = lud.getSolver().getInverse();
        return prec;
    }    
    int id;
    VoxelDataSource source;

    Set<Integer> indexes;
    RealVector xBar;
    RealMatrix S;
    RealVector m;
    double kappa;
    double nu;
    
    static double kappa0;
    static double nu0;
    static RealVector m0;
    static RealMatrix S0;
}
