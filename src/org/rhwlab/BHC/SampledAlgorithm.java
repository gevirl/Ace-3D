/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.RealVector;
import org.rhwlab.dispim.datasource.BoundingBox;
import org.rhwlab.dispim.datasource.FloatHDF5DataSource;
import org.rhwlab.dispim.datasource.MicroCluster;
import org.rhwlab.dispim.datasource.MicroClusterDataSource;
import org.rhwlab.dispim.datasource.Segmentation;
import org.rhwlab.dispim.datasource.SegmentedTiffDataSource;

/**
 *
 * @author gevirl
 */
public class SampledAlgorithm extends ThreadedAlgorithm {
    @Override
    public void run() {
        
        // does it need to be run sampled?
        if (source.getN() > 10*nSamples){
            MicroClusterDataSource save = this.source;
            Set<Object> sampledSet = this.source.sample(nSamples);
            this.source =  new MicroClusterDataSource(sampledSet);

            super.run();  // running the algorithm on the sampled data set
            NodeBase top = (NodeBase)nodeList.get(0);
            // split the data source in two  
            Gaussian leftGaussian =  new Gaussian(top.getLeft());
            Set<Object> leftSet = new HashSet<>();
            double leftPrior = ((LogNode)top.getLeft()).lnPi;
            
            Gaussian rightGaussian =  new Gaussian(top.getRight());
            Set<Object> rightSet = new HashSet<>();
            double rightPrior = ((LogNode)top.getRight()).lnPi;
            
            for (long i=0 ; i<save.getN() ; ++i){
                MicroCluster cl = save.get(i);
                RealVector v = cl.asRealVector();
                double pLeft = leftGaussian.logProbability(v);
                double pRight = rightGaussian.logProbability(v);
                if (pLeft > pRight){
                    leftSet.add(cl);
                }else {
                    rightSet.add(cl);
                }
            }
            MicroClusterDataSource leftSource = new MicroClusterDataSource(leftSet);
            MicroClusterDataSource rightSource = new MicroClusterDataSource(rightSet);
            
            // run algorithm on each split in a spearate thread
            SampledAlgorithm leftAlg = new SampledAlgorithm();
            leftAlg.setSource(leftSource);
            SampledAlgorithm rightAlg = new SampledAlgorithm();
            rightAlg.setSource(rightSource);
            
            leftAlg.run();
            rightAlg.run();
            top.left = leftAlg.nodeList.get(0);
            top.right = rightAlg.nodeList.get(0);
/*            
            Thread leftThread = new Thread(leftAlg);
            
            Thread rightThread = new Thread(rightAlg);
            try {
                leftThread.start();
                leftThread.join();
                rightThread.start();
                rightThread.join();
            } catch (Exception exc){
                exc.printStackTrace();
                System.exit(0);
            }
 */           
            
        } else {
            super.run(); // run it as is, not sampled
        }
    }

    public static void setNSamples(int n){
        nSamples = n;
    }
    static public void main(String[] args)throws Exception {
        FloatHDF5DataSource hdf5 = new FloatHDF5DataSource(new File("/net/waterston/vol9/diSPIM/vab-15Copy/MVR_STACKS/TP198_Ch2_Ill0_Ang0,90_Probabilities.h5"),"exported_data",100.0,1);
        long[] dims = hdf5.getDims();
        Double[] mins = new Double[dims.length];
        Double[] maxs = new Double[dims.length];
        for (int d=0 ; d <dims.length ; ++d){
            mins[d] =.05*dims[d];
            maxs[d] = .95*dims[d];
        }
        Segmentation segmentation = new Segmentation(hdf5,70,new BoundingBox(mins,maxs));
        SegmentedTiffDataSource src = new SegmentedTiffDataSource("/net/waterston/vol9/diSPIM/vab-15Copy/MVR_STACKS/TP198_Ch2_Ill0_Ang0,90.tif",segmentation);
        MicroClusterDataSource mcSrc = src.asMicroClusterDataSource();
//  mcSrc = new MicroClusterDataSource(mcSrc.sample(3500));
        SampledAlgorithm alg = new SampledAlgorithm();
        alg.setTime(time);
        alg.setSource(mcSrc);
        double[] precision = new double[src.getD()];
        precision[0] = precision[1] = precision[2] = .2;  //20
        alg.setPrecision(precision);
        alg.setNu(40); //10
        alg.setAlpha(10000);
        alg.run();
        alg.saveResultAsXML("/net/waterston/vol9/diSPIM/vab-15Copy/test.xml");
int iuhdfidush=0;        
    }
    public class Gaussian {
        public Gaussian(Node node){
            ArrayList<MicroCluster> list = new ArrayList<>();
            node.getDataAsMicroCluster(list);
            
            RealVector mean = MicroCluster.mean(list);
//            RealMatrix variance = MicroCluster.variance(list, mean);
            
 //           dist = new MultivariateNormalDistribution(mean.toArray(),variance.getData());
        }
        public double logProbability(RealVector v){
            return Math.log(dist.density(v.toArray()));
        }
        MultivariateNormalDistribution dist;
    }
    static int nSamples=500;  // the number of samples to use     
}