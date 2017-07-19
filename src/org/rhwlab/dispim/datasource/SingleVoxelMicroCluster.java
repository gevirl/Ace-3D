package org.rhwlab.dispim.datasource;

/**
 *
 * @author gevirl
 */
public class SingleVoxelMicroCluster extends MicroCluster {
    public SingleVoxelMicroCluster(Voxel vox){
        super(vox.getPoint(),asShort(vox.getPoint()),asInt(vox),vox.getAdjusted());
        
    }
    static short[][] asShort(double[] v){
        short[][] points = new short[1][];
        points[0] = new short[v.length];
        for (int d=0 ; d<v.length ; ++d){
            points[0][d] = (short)v[d];
        }        
        return points;
    }
    static int[] asInt(Voxel vox){
        int[] intensities = new int[1];
        intensities[0] = vox.getIntensity();        
        return intensities;
    }
}
