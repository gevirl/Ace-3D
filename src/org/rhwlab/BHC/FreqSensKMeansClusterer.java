/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.distance.EuclideanDistance;

/**
 *
 * @author gevirl
 */
public class FreqSensKMeansClusterer extends org.apache.commons.math3.ml.clustering.Clusterer<Clusterable> {
    public FreqSensKMeansClusterer(int k)  {
        super(new EuclideanDistance());
        this.K = k;
        this.kInv = 1.0/K;
    }


    @Override
    public List<? extends Cluster<Clusterable>> cluster(Collection<Clusterable> points) throws MathIllegalArgumentException, ConvergenceException {
        ArrayList<Cluster<Clusterable>> ret = new ArrayList<>();
        
        data = points.toArray(new Clusterable[0]);
        int N = data.length;
        D = data[0].getPoint().length;
        inCluster = new int[N];
        for (int i=0 ; i<N ; ++i){
            inCluster[i] = -1;
        }
        n = new double[K];
        logN = new double[K];
        for (int i=0 ; i<K ; ++i){
            n[i] = (double)N/(double)K;
            logN[i] = Math.log(n[i]);
        }
        
        double f = (double)D*(double)N/(double)K;
        
        // Randomly assign initial means
        Random rnd = new Random();
        centroids = new double[K][];
        HashSet<Clusterable> set = new HashSet<>();  // set used to insure data point is used only once
        for (int i=0 ; i<K ; ++i){
        
            Clusterable cl = data[rnd.nextInt(N)];
            while (set.contains(cl)){
                cl = data[rnd.nextInt(N)];
            }
            set.add(cl);
            centroids[i] = cl.getPoint();
        }
        int changes;
        do {
            changes = 0;
            // make new assignments of each data point
            for (int i=0 ; i<N ; ++i){
                double[] point = data[i].getPoint();
                double hMax = 0.0;
                int index = -1;
                for (int j=0 ; j<K ; ++j){
                    double h = 0.0;
                    for (int d=0 ; d<D ; ++d){
                        h = h + centroids[j][d]*point[d];
                    }
                    h = (h + 1.0 + n[j]*logN[j]/f)/n[j];
                   if (h > hMax){
                       hMax = h;
                       index = j;
                   }
                }
                if (inCluster[i] != index){
                    ++changes;
                }
            }
                
            // update the number of points in each cluster
            int[] sums = new int[K];
            for (int i=0 ; i<N ; ++i){
                ++sums[inCluster[i]];
            }
            for (int j=0 ; j<K ; ++j){
                n[j] = sums[j];
                logN[j] = Math.log(n[j]);
            }
                
            // update the centroids  
            centroids = new double[K][];
            for (int k=0 ; k<K ; ++k){
                centroids[k] = new double[D];
            }
            for (int i=0 ; i<N ; ++i){
                double[] p = data[i].getPoint();
                int c = inCluster[i];
                for (int d=0 ; d<D ; ++d){
                    centroids[c][d] = centroids[c][d] + p[d];
                }
            }
            for (int k=0 ; k<K ; ++k){
                for (int d=0 ; d<D ; ++d){
                    centroids[k][d] = centroids[k][d]/n[k];
                }
            }
            
            
        } while (changes > 0);
        
        return ret;
    }

    int D;  // dimension of the data;
    int K;  // the number of clusters
    double kInv;
    Clusterable[] data;  // the data as an array
    int[] inCluster;  // the cluster assignment for each data point;
    double[][] centroids;   // centroid/means of the clusters
    double[] n;  // number of data points in each cluster
    double[] logN;
}
