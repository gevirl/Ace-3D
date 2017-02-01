/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import ij.ImagePlus;
import ij.io.Opener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import org.jdom2.Element;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;
//import org.scijava.Context;

/**
 *
 * @author gevirl
 */
public class TifDirectoryImageSource implements ImageSource {
    public TifDirectoryImageSource(String typical,String dataset,String dir,ImagedEmbryo emb,boolean sel){
        emb.addSource(this);
        this.datasetname = dataset;
        this.typical = typical;
        this.directory = dir;
        this.select = sel;
        open();        
    }
    public TifDirectoryImageSource(String typicalFile,String id,ImagedEmbryo emb,boolean sel){
        this(new File(typicalFile).getName(),id,new File(typicalFile).getParent(),emb,sel);
    }
    public TifDirectoryImageSource(Element e,ImagedEmbryo emb){
        this(e.getAttributeValue("typicalFile"),e.getAttributeValue("dataset"),e.getAttributeValue("directory"),emb,Boolean.valueOf(e.getAttributeValue("selected")));
    }
    @Override
    public boolean open(){
        Pattern p1 = Pattern.compile("(TP)(\\d{1,4})(_.+tif)");
        Pattern p2 = Pattern.compile("(\\D+)(\\d{1,4})(\\D+)");
        Matcher matcher = p1.matcher(typical);
        if (!matcher.find()){
            matcher = p2.matcher(typical);
            matcher.find();
        }
        String prefix = matcher.group(1);
        String suffix = matcher.group(3);         
        File dir = new File(this.directory);     
        File[] files = dir.listFiles();
        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;
        Pattern p = Pattern.compile(prefix+"(\\d{1,4})"+suffix);
        for (File file : files){
            String fileName = file.getName();
            matcher = p.matcher(fileName);

            if (matcher.matches()){
                String timeStr = matcher.group(1);
                Integer time = new Integer(timeStr);

                if (time < minTime){
                    minTime = time;
                }
                if (time> maxTime){
                    maxTime = time;
                }
                fileNames.put(time,file.getPath());
            }
            
        }
//        opener = new ImgOpener(new Context());
        
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
            ps.max = 2000;
            ps.min = 0;
            ps.selected = select;
            Ace3D_Frame.getDataSetsDialog().setProperties(dataset, ps);
        } 
        props = Ace3D_Frame.datasetsSelected();
    return true;        
    }

    @Override
    public TimePointImage getImage(String dataset,int time) {
        Opener opn = new Opener();
        ImagePlus ip = opn.openImage(fileNames.get(time));
        float[] mm = new float[2];
        mm[0] = (float)ip.getProcessor().getMin();
        mm[1] = (float)ip.getProcessor().getMax();
        Img img = ImageJFunctions.wrap(ip);
        return new TimePointImage(img,mm,time,dataset);
/*        
        try {
 
            List<SCIFIOImgPlus<?>> list = opener.openImgs(fileNames.get(time));
            SCIFIOImgPlus img = list.get(0);
            
            long[] dims = new long[img.numDimensions()];
            for (int d=0 ; d<dims.length ; ++d) {
                dims[d] = img.dimension(d);
            }
            float[] mm = new float[2];           
            float[] minmax = FusionHelper.minMax(img);
            
            return new TimePointImage(img.getImg(),mm,time,dataset);
        } catch (ImgIOException ex) {
            Logger.getLogger(TifDirectoryImageSource.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
*/
    }

    @Override
    public int getTimes() {
        return fileNames.size();
    }
    
    public String getFile(){
        return directory;
    }
    @Override
    public Collection<DataSetDesc> getDataSets() {
        HashSet<DataSetDesc> ret = new HashSet<>();
        DataSetDescImpl ds = new DataSetDescImpl();
        ds.name = datasetname;
        ret.add(ds);
        return ret;
    }

    @Override
    public Element toXML() {
        Element ret = new Element("TifDirectorySource");
        ret.setAttribute("directory", directory);
        ret.setAttribute("typicalFile", typical);
        ret.setAttribute("dataset", datasetname);
        ret.setAttribute("selected",Boolean.toString(select));
        return ret;
    }    


    @Override
    public int getMinTime() {
        return minTime;
    }

    @Override
    public int getMaxTime() {
        return maxTime;
    }
    int minTime;
    int maxTime;

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    boolean select;
    String typical;
 //   ImgOpener opener;
    String directory;
    String datasetname;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();



}
