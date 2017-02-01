/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import org.rhwlab.dispim.datasource.Voxel;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.ml.clustering.Clusterer;

/**
 *
 * @author gevirl
 */
public class VoxelClusterer extends Thread {
    public VoxelClusterer(ArrayList<Voxel> voxels,Clusterer clusterer){
        this.clusterer = clusterer;
        this.voxels = voxels;
    }

    @Override
    public void run(){
        result = clusterer.cluster(voxels);
    }
    public List getResult(){
        return this.result;
    }
    ArrayList<Voxel> voxels;
    Clusterer clusterer;
    private List result;  // the result of the clustering
}
