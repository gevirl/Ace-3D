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
public class InfiniteBoundingBox extends BoundingBox{
    public InfiniteBoundingBox(){
        super(null,null);
    }
    public boolean isWithin(Voxel vox){
        return true;
    }
}
