/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.FileInfoVirtualStack;
import ij.process.ImageProcessor;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.jdom2.Element;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;

/**
 *
 * @author gevirl
 */
public class ImageJHyperstackSource implements ImageSource {
    public ImageJHyperstackSource(File file,int time,String id,ImagedEmbryo emb){
        emb.addSource(this);
        this.id = id;
        this.file = file;
        this.minTime = time;
        ImagePlus ip =FileInfoVirtualStack.openVirtual(file.getPath());
        this.imagePlus = ip;
        dims = imagePlus.getDimensions();
        
        Iterator<DataSetDesc> iter = getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            Ace3D_Frame.setProperties(dataset,new DataSetProperties());
            Ace3D_Frame.getDataSetsDialog().addDataSet(dataset);
        }   
        List<String> props = Ace3D_Frame.datasetsSelected();
        iter = getDataSets().iterator();
        while (iter.hasNext()){
            String dataset = iter.next().getName();
            TimePointImage tpi = TimePointImage.getSingleImage(dataset,getMinTime());
            DataSetProperties ps = Ace3D_Frame.getProperties(dataset);
            ps.max = (float)0.1*tpi.getMax();
            Ace3D_Frame.getDataSetsDialog().setProperties(dataset, ps);
        }
        props = Ace3D_Frame.datasetsSelected();
        int ashdf=0;
        
    }
    public ImageJHyperstackSource(Element xml,ImagedEmbryo emb){
        this(new File(xml.getAttributeValue("file")),Integer.valueOf(xml.getAttributeValue("minTime")),xml.getAttributeValue("id"),emb);
    }
    public ImageJHyperstackSource(){
        
    }

    public Element toXML(){
        Element ret = new Element("ImageJHyperStack");
        ret.setAttribute("file", this.file.getPath());
        ret.setAttribute("minTime",Integer.toString(minTime));
        ret.setAttribute("id", id);
        return ret;
    }
    
    @Override
    public boolean open() {
        imagePlus = IJ.getImage();
        dims = imagePlus.getDimensions();
        return true;
    }

    @Override
    public TimePointImage getImage(String dataset, int time) {
        float[] minmax = new float[2];
        minmax[0] = Float.MAX_VALUE;
        minmax[1] = Float.MIN_VALUE;
        int stackTime = time - minTime + 1;
//        int channel = Integer.valueOf(dataset.substring(7));
        int channel = dims[2]+1;
        ImageStack stack = new ImageStack(dims[0],dims[1],dims[3]);
        for (int slice =1 ; slice<=dims[3] ; ++slice){
            imagePlus.setPosition(channel, slice, stackTime);
            ImageProcessor ip = imagePlus.getProcessor();
            stack.setPixels(ip.getPixels(), slice);
            float ipmin = (float)ip.getMin();
            float ipmax = (float)ip.getMax();
            if (ipmin < minmax[0]){
                minmax[0] = ipmin;
            }
            if (ipmax > minmax[1]){
                minmax[1] = ipmax;
            }    
        }       
        img = ImageJFunctions.wrap(new ImagePlus("",stack));    
        return new TimePointImage(img,minmax,stackTime,dataset);
    }

    @Override
    public int getTimes() {
        int n =imagePlus.getNFrames();
        return n;
    }

    @Override
    public void setFirstTime(int minTime){
        this.minTime = minTime;
        TimePointImage.resetCache();
    }
    @Override
    public int getMinTime() {
        return this.minTime;
    }

    @Override
    public int getMaxTime() {
        return this.minTime + imagePlus.getNFrames() - 1;
    }

    @Override
    public String getFile() {
        return "";
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        ArrayList<DataSetDesc> ret = new ArrayList<>();
        DataSetDescImpl desc = new DataSetDescImpl();
        desc.name = id;
        ret.add(desc);
        return ret;
    }
    String id;
    File file;
    int[] dims;
    long[] longDims;
    ImagePlus imagePlus;
    Img img;
    int minTime=1;
}
