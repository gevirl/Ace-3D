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
