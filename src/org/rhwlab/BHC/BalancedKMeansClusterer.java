/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.rhwlab.dispim.nucleus.HungarianAlgorithm;

/**
 *
 * @author gevirl
 */
public class BalancedKMeansClusterer extends org.apache.commons.math3.ml.clustering.Clusterer<Clusterable> {
    public BalancedKMeansClusterer(int k)  {
        super(new EuclideanDistance());
        this.k = k;
    }
    @Override
    public List<Cluster<Clusterable>> cluster(Collection<Clusterable> points) throws MathIllegalArgumentException, ConvergenceException {
        s = new int[k];
        for (int i=0 ; i<s.length ; ++i){
            s[i] = 0;
        }
        data = points.toArray(new Clusterable[0]);
        int n = data.length;
        dist = new double[n][];
        for (int i=0 ; i<dist.length ; ++i){
            dist[i] = new double[n];
        }
        HungarianAlgorithm hungar = new HungarianAlgorithm(dist);
        
        slots = new int[n];
        // assign the slots to centroids
        int c = 0;
        for (int i=0 ; i<slots.length ; ++i){
            slots[i] = c;
            ++s[c];
            ++c;
            if (c == k){
                c = 0;
            }
        }
        
        // randomly assign the initial centroids from the data
        Random rnd = new Random();
        centroids = new double[k][];
        HashSet<Clusterable> set = new HashSet<>();  // set used to insure data point is used only once
        for (int i=0 ; i<k ; ++i){
            Clusterable cl = data[rnd.nextInt(n)];
            while (set.contains(cl)){
                cl = data[rnd.nextInt(n)];
            }
            set.add(cl);
            centroids[i] = cl.getPoint();
        }
        assigns = hungar.execute();
        
        // iterate to a solution
        boolean finished = false;
        while(!finished){
            // calculate the distances
            for (int i=0 ; i<n ; ++i){
                for (int j=0 ; j<n ; ++j){
                    dist[i][j] = this.getDistanceMeasure().compute(data[i].getPoint(), centroids[slots[j]]);
                }
            }
        
            // assign data points to clusters
            int[] nextAssigns = hungar.execute();
            // count the changes 
            int changes = 0;
            for (int i=0 ; i>assigns.length ; ++i){
                if (assigns[i] != nextAssigns[i]){
                    ++changes;
                }
            }
            // calculate new centroids
            for (int i=0 ; i<k ; ++i){
                double[] centroid = centroids[i];
                for (int j=0 ; j<centroid.length ; ++j){
                    centroid[j] = 0.0;
                }
            }
            for (int i=0 ; i<n ; ++i){
                double[] p = data[i].getPoint();
                
                for (int j= 0 ; j<p.length ; ++j){
                    centroids[slots[i]][j] = centroids[slots[i]][j] + p[j];
                }
            }
            for (int i=0 ; i<k ; ++i){
                double[] centroid = centroids[i];
                for (int j=0 ; j<centroid.length ; ++j){
                    centroid[j] = centroid[j]/s[j];
                }
            }
            finished = changes==0;
            
        }
        return null;
    }
    Clusterable[] data;
    int k;  // the number of clusters
    int[] s;  // number of points in each cluster
    double[][] centroids;
    int[] slots;  // the data point indexes assigned to each cluster
    double[][] dist;
    int[] assigns;

}
