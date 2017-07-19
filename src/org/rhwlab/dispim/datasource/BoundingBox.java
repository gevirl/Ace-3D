package org.rhwlab.dispim.datasource;

import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class BoundingBox {
    public BoundingBox(Double[] mins,Double[] maxs){
        this.mins = mins;
        this.maxs = maxs;
    }
    public boolean isWithin(Voxel vox){
        double[] p = vox.getPoint();
        for (int d=0 ; d<p.length ; ++d){
            if (mins[d] !=null && p[d]< mins[d] ){
                return false;
            }
            if (maxs[d] !=null && p[d]>maxs[d]){
                return false;
            }
        }
        return true;
    }
    public Double getMin(int d){
        return mins[d];
    }
    public Double getMax(int d){
        return maxs[d];
    }
    public void toXMl(Element ele){
        ele.setAttribute("xmin", Integer.toString(mins[0].intValue()));
        ele.setAttribute("ymin", Integer.toString(mins[1].intValue()));
        ele.setAttribute("zmin", Integer.toString(mins[2].intValue()));
        ele.setAttribute("xmax", Integer.toString(maxs[0].intValue()));
        ele.setAttribute("ymax", Integer.toString(maxs[1].intValue()));
        ele.setAttribute("zmax", Integer.toString(maxs[2].intValue()));        
    }
    Double[] mins;
    Double[] maxs;
}
