/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.List;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class MicroCluster {
    public MicroCluster(double[] v,short[][] points,int[] intensities,double prob){
        this.v = v;
        this.points = points;
        this.intensities = intensities;
        this.avgProb = prob;
    }
    // construct from an xml element
    public MicroCluster (Element ele){
        int D = 3;
        String centerStr = ele.getAttributeValue("center").trim();
        String[] centerTokens = centerStr.split(" ");
        v = new double[D];
        for (int d=0 ; d<D ; ++d){
            v[d] = Double.valueOf(centerTokens[d]);
        }
        this.avgProb = Double.valueOf(ele.getAttributeValue("avgProb"));
        int nPts = Integer.valueOf(ele.getAttributeValue("points"));
        points = new short[nPts][];
        intensities = new int[nPts];
        String cont = ele.getTextNormalize();
        String[] tokens = cont.substring(1, cont.length()-1).split("\\)\\(");
        for (int i=0 ; i<nPts ; ++i){
            short[] p = new short[D];
            String[] valStrs = tokens[i].split(",");
            for (int d=0 ; d<D ; ++d){
                p[d] = Short.valueOf(valStrs[d]);
            }
            points[i] = p;
            intensities[i] = Integer.valueOf(valStrs[valStrs.length-1]);
        }
        
    }
    public RealVector asRealVector(){
        return new ArrayRealVector(v);
    }
/*
    public FieldVector asDfpVector(){
        Dfp[] dfp = new Dfp[v.length];
        for (int i=0 ; i<dfp.length ; ++i){
            dfp[i] = field.newDfp(v[i]);
        }
        return new ArrayFieldVector(dfp);
    }
*/
    // calculate the mean of all the data points in a list of microclusters
    public static RealVector mean(List<MicroCluster> data) {
        if (data.isEmpty()){
            return null;
        }
        RealVector first = data.get(0).asRealVector();
        long n = 0;
        long[] mu = new long[first.getDimension()];
        for (MicroCluster micro : data){
            for (int p=0 ; p<micro.points.length ; ++p){
                for (int d=0 ; d<mu.length ; ++d){
                    mu[d] = mu[d] + micro.points[p][d];
                }
                ++n;
            }
        }
        RealVector ret = new ArrayRealVector(first.getDimension());
        for (int d=0 ; d<mu.length ; ++d){
            ret.setEntry(d,(double)mu[d]/(double)n);
        }        
        return ret;
    }
    public static RealMatrix precision(List<MicroCluster> data,RealVector mu){
        RealMatrix ret = new Array2DRowRealMatrix(mu.getDimension(),mu.getDimension());
        RealVector v = new ArrayRealVector(mu.getDimension());
        long n = 0;
        for (MicroCluster micro : data){
            for (int p =0 ; p<micro.points.length ; ++p){
                for (int d=0 ; d<mu.getDimension() ; ++d){
                    v.setEntry(d, micro.points[p][d]);
                }
                RealVector del = v.subtract(mu);
                ret = ret.add(del.outerProduct(del));
                ++n;
            }
            
        }
        ret = ret.scalarMultiply(1.0/n);
        LUDecomposition lud = new LUDecomposition(ret);
        RealMatrix prec = null;
        if (lud.getSolver().isNonSingular()){
            prec = lud.getSolver().getInverse();
        }
        return prec;
    }
    // add content to an xml node
    public int addContent(Element node){
        StringBuilder builder = new StringBuilder();
        for (int j=0 ; j<v.length ; ++j){
            if (j > 0){
                builder.append(" ");
            }
            builder.append(v[j]);

        }
        node.setAttribute("center", builder.toString());
        node.setAttribute("avgProb", String.format("%.2f",this.avgProb));
        node.addContent(pointsAsString());
        return points.length;
    }
/*    
    static public void setField(DfpField fld){
        field = fld;
    }
*/
    // record the point as 4D (x,y,z,intensity)
    public String pointsAsString(){
        StringBuilder builder = new StringBuilder();
        for (int p=0 ; p<points.length ; ++p)    {
            builder.append("(");
            short[] pnt = points[p];
            for (int i=0 ; i<pnt.length ; ++i){
                builder.append(pnt[i]);
                builder.append(",");
            }
            builder.append(intensities[p]);
            builder.append(")");
        }
        return builder.toString();
    }
    public int getPointCount(){
        return points.length;
    }
    public int[] getIntensities(){
        return this.intensities;
    }
    public long getTotalIntensity(){
        if (totalIntensity == null){
            long ret = 0;
            for (int i : this.intensities){
                ret = ret + i;
            }
            totalIntensity = ret;
        }
        return totalIntensity;
    }
    public double getAverageIntensity(){
        if (avgIntensity == null){
            avgIntensity = (double)this.getTotalIntensity()/(double)this.getPointCount();
        }
        return avgIntensity;
    }
    public RealVector getPoint(int i){
        short[] vals = points[i];
        double[] d = new double[vals.length];
        for (int j=0 ; j<d.length ; ++j){
            d[j] = vals[j];
        }
        return new ArrayRealVector(d);
    }
    public double getAvgProb(){
        return this.avgProb;
    }

    double[] v;  // center
    short[][] points;
    int[] intensities;
    double avgProb;
    Long totalIntensity;
    Double avgIntensity;
}
