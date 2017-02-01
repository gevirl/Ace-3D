/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RecursiveAction;

/**
 *
 * @author gevirl
 */
// merge the base cluster with a sequential list of clusters
// the start and end of the list are specified
// it not end is given use all clusters greater than the start
public class MergeAction extends RecursiveAction {
    public MergeAction(List<Node> clusters,int base,int start,Map pairs){
        this(clusters,base,start,clusters.size()-1,pairs);
    }
    public MergeAction(List<Node> clusters,int base,int start,int end,Map pairs){
        this(clusters,clusters.get(base),start,end,pairs);
    }
    public MergeAction(List<Node> clusters,Node B,int start,int end,Map pairs){
        this.clusters = clusters;
        this.B = B;
        this.start = start;
        this.end = end;
        this.pairs = pairs;
    }

    @Override
    protected void compute() {
        if (start ==0 && end == 1){
            int jiashdfuis=0;
        }
        if (end - start < mergeSize){
//            Map pairsa = Collections.synchronizedMap(new HashMap<>());
            for (int i=start ; i<=end ; ++i){
                Node T = clusters.get(i);
                Node M = B.mergeWith(T);
                pairs.put(T, M);
            } 
//            pairs.putAll(pairsa);
        } else {
            // split the job up 
            int mid = (start + end)/2;
            Map pairsa = Collections.synchronizedMap(new HashMap<>());
            MergeAction a = new MergeAction(clusters,B,start,mid,pairsa);
            Map pairsb = Collections.synchronizedMap(new HashMap<>());
            MergeAction b = new MergeAction(clusters,B,mid+1,end,pairsb);
            invokeAll(a,b);
            pairs.putAll(pairsa);
            pairs.putAll(pairsb);
        }
    }
    static public void setMergeSize(int size){
        mergeSize = size;
    }
    List<Node> clusters;
    Node B;
    int start;
    int end;
    Map pairs;

    static int mergeSize = 2000;
}
