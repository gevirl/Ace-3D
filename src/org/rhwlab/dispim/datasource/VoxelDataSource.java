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
public interface VoxelDataSource extends DataSource{
    @Override
    public Voxel get(long i);  // return the ith voxel
    
}
