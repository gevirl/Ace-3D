/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.datasource;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.io.Opener;
import net.imglib2.RandomAccess;
import net.imglib2.img.ImagePlusAdapter;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

/**
 *
 * @author gevirl
 */
public class TiffDataSource extends DataSourceBase implements VoxelDataSource{
    public TiffDataSource(String file){
        imagePlus = new Opener().openImage( file);
        image = ImagePlusAdapter.wrap( imagePlus);
        sampler = image.randomAccess();
        dims = new long[image.numDimensions()];
        image.dimensions(dims);
        N = 1;
        for (int d=0 ; d<dims.length ; ++d){
            N = N * dims[d];
        }
    }
    public TiffDataSource (TiffDataSource s){
        this.image = s.image;
        this.N = s.N;
        this.dims = s.dims;
        this.sampler = s.sampler;
        this.imagePlus = s.imagePlus;
    }



    @Override
    public Voxel get(long i) {
        long[] pos = getCoords(i);
        sampler.setPosition(pos);
        AbstractIntegerType obj = (AbstractIntegerType)sampler.get();
        int intensity = obj.getInteger();
        return new Voxel(pos,intensity);
    }

    public void setIntensity(long i,int intensity){
        long[] pos = getCoords(i);
        sampler.setPosition(pos);
        AbstractIntegerType obj = (AbstractIntegerType)sampler.get();
        obj.setInteger(intensity);
    }

    public RealVector getMidpoint(){
        double[] d = new double[dims.length];
        for (int i=0 ; i< dims.length ; ++i){
            d[i] = dims[i]/2.0;
        }
        ArrayRealVector v = new ArrayRealVector(d);
        return v;
    }
    public void saveAsTiff(String file){
        FileSaver saver = new FileSaver(imagePlus);
        saver.saveAsTiff(file);
    }
    ImagePlus imagePlus;
    final Img image;
    RandomAccess sampler;

    
    
    static public void main(String[] args) throws Exception {
        
        
 /*       
        TiffDataSource source = new TiffDataSource("/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.tiff");
        List  cl = source.cluster(source.voxels, 50);
        OutputStream stream = new FileOutputStream("/nfs/waterston/pete/Segmentation/Cherryimg75_SimpleSegmentation.xml");
        source.saveClusters(cl, stream);
        
        int K = 50;
        source.setK(K);
        long[] dims = source.getDims();
        double[] values= new double[dims.length];
        for (int i=0 ; i<dims.length ; ++i){
            values[i] = dims[i]/2.0;
        }
        RealVector m0 = new ArrayRealVector(values);
        GaussianMixture gm = new GaussianMixture();
        gm.setSource(source);
        gm.setAlpha0(0.001);
        gm.setBeta0(.00000001);
        gm.setNu0(3.0);
        gm.setMu0(m0);
        
        RealMatrix W0 = MatrixUtils.createRealIdentityMatrix(source.getD());
        W0 = W0.scalarMultiply(0.00000001);  
        gm.setW0(W0);        
        gm.init(K);
        gm.run();
*/
    }




}