/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;


/**
 *
 * @author gevirl
 */
public interface SegmentedDataSource extends DataSource{
    public ClusteredDataSource kMeansCluster(int nClusters,int nPartitions)throws Exception;
    public int getSegmentN();
    public Voxel getSegmentVoxel(int i);
}
