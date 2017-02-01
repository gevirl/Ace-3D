/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.PrintStream;
import java.util.List;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.rhwlab.dispim.datasource.MicroCluster;

/**
 *
 * @author gevirl
 */
public interface Node extends Comparable {
//    public Dfp getPosteriorDfp();
    public double getLogPosterior();
    public Node mergeWith(Node cl);
//    public  void getDataAsFieldVector(List<FieldVector> list);
    public void getDataAsRealVector(List<RealVector> list);
    public Node getLeft();
    public Node getRight();
    public void print(PrintStream stream);
    public void getDataAsMicroCluster(List<MicroCluster> list);
//    public int saveAsXML(Element root,int id);  
    public int saveAsTreeXML(int time,Element root);
//    public  int saveAsXMLByThreshold(Element root,double threshold,int id);
    public int getN();
    public boolean isLeaf();
    public boolean isDescendent(Node other);
    public Node commonAncestor(Node other);
    public boolean isUsed();
}
