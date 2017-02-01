/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.io.File;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class MicroCluster4DDataSource extends MicroClusterDataSource {
    public MicroCluster4DDataSource(String file)throws Exception {
        super(file);
    }
    final public void openFromClusteredDataSourceFile(String xml)throws Exception {
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
            micros[k] = new MicroCluster4D(v,points,intensities,prob);
            ++k;
        }
    }   
    @Override
    public int getD() {
        return super.getD()+1;
    }    
}
