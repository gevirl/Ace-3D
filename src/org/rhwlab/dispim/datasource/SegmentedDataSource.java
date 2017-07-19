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
