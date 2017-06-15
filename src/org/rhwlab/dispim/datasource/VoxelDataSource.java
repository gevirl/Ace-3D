package org.rhwlab.dispim.datasource;

/**
 *
 * @author gevirl
 */
public interface VoxelDataSource extends DataSource{
    @Override
    public Voxel get(long i);  // return the ith voxel
    
}
