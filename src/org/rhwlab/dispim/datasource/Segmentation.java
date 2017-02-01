/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class Segmentation {
    public Segmentation(VoxelDataSource source,double thresh,BoundingBox box){
        this.source = source;
        this.thresh = thresh;
        segmentIndex = new ArrayList<>();
        mins = new double[source.getD()];
        maxs = new double[source.getD()];
        for (int d=0 ; d<source.getD() ; ++d ){
            mins[d] = Double.MAX_VALUE;
            maxs[d] = 0.0;
        }
        
        for (long i=0 ; i<source.getN() ; ++i){  // process each voxel in the segmented tiff
            Voxel segVox = source.get(i);
            if (segVox.getIntensity() >= thresh && box.isWithin(segVox)){  // do not include the voxel in the segmentation if less than the threshold or outside the bounding box
                for (int d=0 ; d<mins.length ; ++d){
                    RealVector v = segVox.coords;
                    double e = v.getEntry(d);
                    if (e < mins[d]){
                        mins[d] = e;
                    }
                    if (e > maxs[d]) {
                        maxs[d] = e;
                    }
                }                  
                segmentIndex.add(i); 
            }
        }        
    }

    
    public long getVoxelIndex(int segIndex){
        return segmentIndex.get(segIndex);
    }
    public int getSegmentN(){
        return segmentIndex.size();
    }
    public double[] getMins(){
        return mins;
    }
    public double[] getMaxs(){
        return maxs;
    }
    public double getThreshold(){
        return thresh;
    }
    public double getSegmentationProbability(int segIndex){
        return ((double)source.get(segmentIndex.get(segIndex)).getIntensity())/100.0;
    }
    VoxelDataSource source;
    double thresh;
    double[] mins;  // min coordinates of non-background voxels
    double[] maxs;  // max coordinates of non-background voxels  
    List<Long> segmentIndex;  // list of voxels in the segment    
}
