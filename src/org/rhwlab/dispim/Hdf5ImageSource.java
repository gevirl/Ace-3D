/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mpicbg.spim.data.generic.sequence.ImgLoaderHints;
import mpicbg.spim.data.registration.ViewRegistration;
import mpicbg.spim.data.registration.ViewRegistrations;
import mpicbg.spim.data.registration.ViewTransform;
import mpicbg.spim.data.sequence.Angle;
import mpicbg.spim.data.sequence.Channel;
import mpicbg.spim.data.sequence.Illumination;
import mpicbg.spim.data.sequence.ImgLoader;
import mpicbg.spim.data.sequence.SequenceDescription;
import mpicbg.spim.data.sequence.SetupImgLoader;
import mpicbg.spim.data.sequence.TimePoint;
import mpicbg.spim.data.sequence.TimePoints;
import mpicbg.spim.data.sequence.ViewDescription;
import mpicbg.spim.data.sequence.ViewId;
import mpicbg.spim.data.sequence.ViewSetup;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.realtransform.RealViews;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import org.jdom2.Element;
import spim.fiji.plugin.queryXML.LoadParseQueryXML;
import spim.fiji.spimdata.SpimData2;
import spim.process.fusion.FusionHelper;

/**
 *
 * @author gevirl
 */
public class  Hdf5ImageSource implements ImageSource {
    public Hdf5ImageSource(){

    }
    @Override
    public boolean open(){
        result = new LoadParseQueryXML();
        if (result.queryXML( "editing", false, false, false, false )){
            ArrayList<ViewSetup> setups = result.getViewSetupsToProcess();
            for (ViewSetup setup : setups){
                Hdf5DataSetDesc desc = new Hdf5DataSetDesc(setup);
                this.dataSetMap.put(desc.getName(), desc);
            }
            timepoints = result.getData().getSequenceDescription().getTimePoints();
            angles = result.getData().getSequenceDescription().getAllAngles();
            channels = result.getData().getSequenceDescription().getAllChannels();
            illuminations = result.getData().getSequenceDescription().getAllIlluminations();
            spimData = result.getData();  
            return true;
        }
        return false;        
    }
    @Override
    public TimePointImage getImage(String dataset,int time) {
        Hdf5DataSetDesc desc = (Hdf5DataSetDesc)this.dataSetMap.get(dataset);
        return getImage(dataset,time,desc.getAngleId(),desc.getChannelId(),desc.getIllumninationId());
    }
    
    public  TimePointImage getImage(String dataset,int time,int angle,int channel,int illum) {

        TimePoint timepoint = timepoints.getTimePoints().get(time);
        Channel ch = channels.get(channel);
        Angle an = angles.get(angle);
        Illumination il = illuminations.get(illum);
        final ViewId viewId = SpimData2.getViewId( 
                spimData.getSequenceDescription(),timepoint,channels.get(channel),angles.get(angle),illuminations.get(illum) );
        ViewRegistrations viewRegs = spimData.getViewRegistrations();
        ViewRegistration viewReg = viewRegs.getViewRegistration(viewId);
        
        List<ViewTransform> xformList = viewReg.getTransformList();
        AffineTransform3D affine = new AffineTransform3D();
        affine.identity();
        AffineTransform3D completeXform = new AffineTransform3D();
        completeXform.identity();
        for (ViewTransform xform : xformList){
            if (xform.getName().equals("calibration")){
//                affine.set(xform.asAffine3D().get(2,2), 2, 2);
                affine = affine.concatenate(xform.asAffine3D());
                
            }
            if (xform.getName().contains("Rotation")){
                affine = affine.concatenate(xform.asAffine3D());
            }
            
            completeXform = completeXform.preConcatenate(xform.asAffine3D());
        }
       
        SequenceDescription seqDesc = spimData.getSequenceDescription();
        ViewDescription viewDesc = seqDesc.getViewDescription(viewId);

        ImgLoader imgLoader = seqDesc.getImgLoader();
        SetupImgLoader setupImgLoader = imgLoader.getSetupImgLoader(viewId.getViewSetupId());
        
        RandomAccessibleInterval img = setupImgLoader.getImage(viewId.getTimePointId(),ImgLoaderHints.LOAD_COMPLETELY  );
        RandomAccessible input = Views.extendZero(img);
        RealRandomAccessible<UnsignedShortType> interpolated = Views.interpolate( input, new NLinearInterpolatorFactory<UnsignedShortType>() );

        
        int nDim = img.numDimensions();
        double[] dims = new double[nDim];
        double[] xformDims = new double[nDim];
        for (int d=0 ; d<nDim ; ++d){
            dims[d] = img.dimension(d);
        }
        affine.apply(dims, xformDims);
        

        
        long[] min = new long[nDim];
        long[] max = new long[nDim];
        for (int d=0 ; d<nDim ; ++d){
            min[d] = 0;
            max[d] = (long)xformDims[d];
        }
        
        double[] minPoint = new double[nDim];
        double[] maxPoint = new double[nDim];
        double[] minTarget = new double[nDim];
        double[] maxTarget = new double[nDim];
        for (int i=0 ; i<nDim ; ++i){
            minPoint[i] = img.min(i);
            maxPoint[i] = img.max(i);
        }
        viewReg.getModel().apply(maxPoint,maxTarget);
        viewReg.getModel().apply(minPoint,minTarget);
         for (int d=0 ; d<nDim ; ++d){
            min[d] = Math.min((long)minTarget[d],(long)maxTarget[d]);
            max[d] = Math.max((long)minTarget[d],(long)maxTarget[d]);
        }       
        
        
        
        float[] minmax = FusionHelper.minMax(img);  
//	RealRandomAccessible< UnsignedShortType > realview = RealViews.affineReal( interpolated, affine );
        RealRandomAccessible< UnsignedShortType > realview = RealViews.affineReal( interpolated, viewReg.getModel() );

        RandomAccessibleInterval< UnsignedShortType > view = Views.interval( Views.raster( realview ),min,max);
        long[] viewDims = new long[nDim];
        view.dimensions(viewDims);
//        return new TimePointImage(view,minmax,time,xformDims,dataset);
        return new TimePointImage(view,minmax,time,dataset);
    }
    public void setAngle(int a){
        this.angle = a;
    }
    public void setChannel(int ch){
        this.channel = ch;
    }
    public void setIllumination(int il){
        this.illum = il;
    }
    @Override
    public int getTimes() {
        return timepoints.size();
    } 
    public String getFile(){
        return result.getXMLFileName();
    }

   
    LoadParseQueryXML result;
    TreeMap<String,DataSetDesc> dataSetMap = new TreeMap<>();
    int angle;
    int channel;
    int illum;
    TimePoints timepoints;
    Map<Integer, Angle > angles;
    Map<Integer, Channel > channels;
    Map<Integer, Illumination > illuminations;
    SpimData2 spimData;

    @Override
    public Collection<DataSetDesc> getDataSets() {
        return this.dataSetMap.values();
    }

    @Override
    public int getMinTime() {
       return timepoints.getTimePointsOrdered().get(0).getId();
        
    }

    @Override
    public int getMaxTime() {
        return timepoints.getTimePointsOrdered().get(timepoints.size()-1).getId();
    }

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element toXML() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }





}
