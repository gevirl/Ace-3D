/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.io.File;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class MicroClusterDataSource extends DataSourceBase {
    // construct this from a ClusteredDataSource file
    public MicroClusterDataSource(String file)throws Exception {
        this.openFromClusteredDataSourceFile(file);
    }    
    
    // construct from an array of microclusters
    public MicroClusterDataSource(MicroCluster[] mc){
        this.micros = mc;
        K = mc.length;
        N = K;
        D = mc[0].asRealVector().getDimension();
    }
    public MicroClusterDataSource(Set<Object> voxelSet){
        this(fromVoxelSet(voxelSet));
    }
    static private MicroCluster[] fromVoxelSet(Set<Object> objs){
        MicroCluster[] ret = new MicroCluster[objs.size()];
        int i=0;
        for (Object obj : objs){
            ret[i] = (SingleVoxelMicroCluster)obj;
            ++i;
        }
        return ret;
    }
   
    public void openFromClusteredDataSourceFile(String xml)throws Exception {
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();
        K = Integer.valueOf(root.getAttributeValue("NumberOfClusters"));
        D = Integer.valueOf(root.getAttributeValue("Dimensions"));
        micros = new MicroCluster[K];
        N = Integer.valueOf(root.getAttributeValue("NumberOfPoints"));
        List<Element> clusterElements = root.getChildren("Cluster");
        int k = 0;
        
        for (Element clusterElement : clusterElements){
            String[] tokens = clusterElement.getAttributeValue("Center").split(" ");
            double[] v = new double[tokens.length];
            for (int i=0 ; i<v.length ; ++i){
                v[i] = Double.valueOf(tokens[i]);
            }
            double prob = Double.valueOf(clusterElement.getAttributeValue("AvgAdjusted"));
            List<Element> pointElements = clusterElement.getChildren("Point");
            short[][] points = new short[pointElements.size()][];
            int[] intensities = new int[pointElements.size()];
            int n = 0;
            for (Element pointElement : pointElements){
                intensities[n] = Integer.valueOf(pointElement.getAttributeValue("Intensity"));
                tokens = pointElement.getTextNormalize().split(" ");
                points[n] = new short[tokens.length];
                for (int i=0 ; i<v.length ; ++i){
                    points[n][i] = Double.valueOf(tokens[i]).shortValue();
                }
                ++n;
            }          
            micros[k] = new MicroCluster(v,points,intensities,prob);
            ++k;
        }
    }


    @Override
    public int getD() {
        return D;
    }

    @Override
    public MicroCluster get(long i) {
        return micros[(int)i];
    }
    public int getK(){
        return K;
    }
    public RealVector getCenter(int cl){
        return micros[cl].asRealVector();
    } 
    public RealVector getDataMean(){
        ArrayRealVector ret = new ArrayRealVector(getD());
        for (int k=0 ; k<micros.length ; ++k){
            ret = ret.add(micros[k].asRealVector());
        }
        return ret.mapDivide(micros.length);
    } 
    public RealMatrix getDataVariance(){
        RealMatrix ret = new Array2DRowRealMatrix(getD(),getD());
        for (int k=0 ; k<micros.length ; ++k){
            RealVector v = micros[k].asRealVector();
            ret = ret.add(v.outerProduct(v));
        } 
        return ret.scalarMultiply(1.0/micros.length);
    }
    public static void main(String[] args)throws Exception {
        MicroClusterDataSource source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentation0075.save");
    }
    int D;
    int K;
    MicroCluster[] micros;
}
