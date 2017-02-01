/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import ch.systemsx.cisd.base.mdarray.MDFloatArray;
import ch.systemsx.cisd.hdf5.HDF5DataSetInformation;
import ch.systemsx.cisd.hdf5.HDF5Factory;
import ch.systemsx.cisd.hdf5.IHDF5Reader;
import java.io.File;
import java.util.List;

/**
 *
 * @author gevirl
 */
public class FloatHDF5DataSource extends DataSourceBase implements VoxelDataSource {
    public FloatHDF5DataSource(File hdFile,String hdDataSet,double scale,int segment){
        this.scale = scale;
        this.segment = segment;
        this.hd = hdFile;
        this.hdDataSet = hdDataSet;       
        IHDF5Reader reader = HDF5Factory.openForReading(hd);
        
        List<String> groups = reader.object().getGroupMembers("/");
        HDF5DataSetInformation dsInfo = reader.object().getDataSetInformation("/"+hdDataSet); 
        long[] hdDims  = dsInfo.getDimensions();
        dims = new long[3];
        dims[0] = hdDims[2];
        dims[1] = hdDims[1];
        dims[2] = hdDims[0];
        
        N = dims[0];
        N = N * dims[1];
        N = N * dims[2];
        
 //       HDF5DataTypeInformation dtInfo = dsInfo.getTypeInformation();
 //       int elSize = dtInfo.getElementSize();        
 //       HDF5DataClass dc = dtInfo.getDataClass();
        mdArray = reader.float32().readMDArray("/"+hdDataSet);

    }

    @Override
    public Voxel get(long i) {
        long[] coords = this.getCoords(i);
        
        int v = (int)(scale*mdArray.get((int)coords[2],(int)coords[1],(int)coords[0],segment));
        return new Voxel(coords,v,0.0);
    }

    
    static public void main(String[] args){
        FloatHDF5DataSource source = new FloatHDF5DataSource(
                new File("/net/waterston/vol9/diSPIM/20161207_tbx-9_OP636/MVR_STACKS","TP200_Ch2_Ill0_Ang0,90_Probabilities.h5"),
                "exported_data",100.0,0);  // seg=0 is background, seg=1 is foreground
        Voxel vox = source.get(0);
        int iuasdfuis=0;
    }
    int segment;
    double scale;
    File hd;
    String hdDataSet;
    MDFloatArray mdArray;
}
