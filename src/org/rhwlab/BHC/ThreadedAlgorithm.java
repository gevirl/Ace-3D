/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import org.rhwlab.dispim.datasource.DataSource;
import org.rhwlab.dispim.datasource.MicroCluster;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;

/**
 *
 * @author gevirl
 */
public class ThreadedAlgorithm implements Runnable {
    public ThreadedAlgorithm(){
    }
    public void init() {
        posteriors = new TreeSet(new NodeComparator());
       
        mu = source.getDataMean().toArray();
        NodeBase.maxN = 10000;
        NodeBase.setParameters(nu,beta,mu,s);
//        CellCounts cc = new CellCounts();
        NodeBase.setAlpha(alpha);  // for time point 75
        
        nodeList = new ArrayList<>();
        // build the initial clusters with one data point in each standard cluster
        for (int n=0 ; n<source.getK() ; ++n){
            MicroCluster micro = (MicroCluster)source.get(n);
            Node cluster= new LogNode(micro); 
            nodeList.add(cluster);
        }
        
        // make all possible pairings of initial clusters
        pairs = Collections.synchronizedMap(new HashMap<>());
        for (int i=0 ; i<nodeList.size()-1 ; ++i){
            Map map = Collections.synchronizedMap(new HashMap<>());
            MergeAction merge = new MergeAction(nodeList,i,i+1,map);
            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            },false);
            try {
                pool.invoke(merge);
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(0);
            }
            for (Object obj : map.values()){
                posteriors.add((Node)obj);
            }            
            Node clusterI = nodeList.get(i);
            pairs.put(clusterI,map);
            System.out.printf("Cluster %d paired\n",i);
        }
    }
  
    private Node maximumRCluster(){
        return this.posteriors.last();
    }  
  
    @Override
    public void run() {
        init();
        TimeProfile profile = new TimeProfile();
        while (nodeList.size()>1){
            if (nodeList.size()==2){
                int asdhfuis=0;
            }
            System.out.printf("\n%d Clusters\n",nodeList.size());
            profile.report(System.out, "Before maximumRCluster");
            T = maximumRCluster();
            profile.report(System.out, "After maximumRCluster");
            if (T == null ){
                return;
            }   
            T.print(System.out);

            Node tLeft = T.getLeft();
            Node tRight = T.getRight();
            
            // remove the children of the max r pair
            Map leftMap = (Map)pairs.remove(T.getLeft());
            if (leftMap != null){
                for (Object leftObj : leftMap.values()){
                    this.posteriors.remove(((Node)leftObj));
                }
            }
            
            
            Map rightMap = (Map)pairs.remove(T.getRight());
            if (rightMap != null){
                for (Object rightObj : rightMap.values()){
                    this.posteriors.remove(((Node)rightObj));
                }  
            }
            
            for (Object obj : pairs.values()){
                Map map = (Map)obj;
                Object nodeObj = map.remove(T.getLeft());
                if (nodeObj != null){
                    this.posteriors.remove(((Node)nodeObj));
                }
                nodeObj = map.remove(T.getRight());
                if (nodeObj != null){
                    this.posteriors.remove(((Node)nodeObj));
                }
            }
            System.out.println("Removed");
            
           
            boolean leftRemoved = nodeList.remove(T.getLeft());
            boolean rightRemoved = nodeList.remove(T.getRight());
            
            if (leftRemoved){
 //               T.getLeft().print(System.out);
            } else {
                int iuashdf=0;
            }
            if (rightRemoved){
 //               T.getRight().print(System.out);
            }else {
                int oiashdfio=0;
            }


            profile.report(System.out,"Starting MergeAction");
            // make new pairs with all the clusters
            Map map = Collections.synchronizedMap(new HashMap<>());
            MergeAction merge = new MergeAction(nodeList,T,0,nodeList.size()-1,map);
            ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            },false);
            try {
                pool.invoke(merge);
            } catch(Exception exc){
                exc.printStackTrace();
                System.exit(nu);
            }
            profile.report(System.out,"MergeAction complete");
            pairs.put(T,map);
            for (Object obj : map.values()){
                this.posteriors.add((Node)obj);
            }
            nodeList.add(T);
        }
    }
    public void setSource(DataSource src){
        this.source = (MicroClusterDataSource)src;
    }
    static public void setPrecision(double[] prec){
        s = prec;
    }
    static public void setNu(int v){
        nu = v;
    }
    static public void setAlpha(double a){
        alpha = a;
    }
  
    // saves the result as a BHC tree xml
    public BHCTree saveResultAsXML(String file)throws Exception {
        BHCTree tree = new BHCTree(alpha,s,nu,mu,nodeList);
        tree.saveAsXML(file);
        return tree; 
    } 
    public BHCTree resultAsBHCTree(){
        return new BHCTree(alpha, s, nu, mu, nodeList);
    }
    public Node getFinalCluster(){
        return T;
    }
    static public void setTime(int t){
        time = t;
    }
    
    static public void main(String[] args) throws Exception {
        System.out.println("GaussianGIWPrior");
//        SegmentedTiffDataSource source = new SegmentedTiffDataSource("/nfs/waterston/pete/Segmentation/Cherryimg75.tif",
//                "/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.tiff",1);  // background is seg=1
//        source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/Cherryimg_SimpleSegmentationMulti0350.xml");
        MicroClusterDataSource source = new MicroClusterDataSource("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentation0350Multi.xml");
/*        
        Dfp[] mu = new Dfp[mean.getDimension()];
        for (int i=0 ;  i<mu.length ; ++i){
            mu[i] = field.newDfp(mean.getEntry(i));
        }
*/
//    double alpha = 10000000000.;
//        Cluster.setAlpha(600);  // for time point 350
        ThreadedAlgorithm alg = new ThreadedAlgorithm();
        alg.setSource(source);
        alg.run();
        alg.saveResultAsXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHCTree0350.xml");
//        NodeBase.saveClusterListAsXML("/nfs/waterston/pete/Segmentation/350/Cherryimg_SimpleSegmentationBHC0350.xml", alg.clusters,0.999);
        int hfuis=0;
    }    
    MicroClusterDataSource source;
    Node T;  // top level node with the maximum posterior probability
    List<Node> nodeList;  // current list of top level nodes, becomes one node when algorithm completes
    Map<Node,Map> pairs;  // current pairing of each of the top level nodes with all the other top level nodes (space-time tradeoff for speed)
    TreeSet<Node> posteriors;  // the top level pairings sorted by posterior probability (space-time tradeoff for speed)
    
    static int nu = 20;
    static double[] s;
    double[] mu;
    static double beta = 0.0000001; 
    static double alpha = 10000000;
    
    static public int time;
}
