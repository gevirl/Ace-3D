/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;

/**
 *
 * @author gevirl
 */
public class HDF5DirectoryImageSource implements ImageSource{
    public HDF5DirectoryImageSource(File dir,String hddataset,String aceDataSet,ImagedEmbryo emb,boolean sel){
        emb.addSource(this);
        this.directory = dir;
        this.HDF5dataset = hddataset;
        this.aceDataset = aceDataSet;
        this.select = sel;

    }
    public HDF5DirectoryImageSource(Element e,ImagedEmbryo emb){
        this(new File(e.getAttributeValue("directory")),"exported_data",e.getAttributeValue("dataset"),emb,Boolean.valueOf(e.getAttributeValue("selected")));
        open();
    }    
    @Override
    public boolean open() {
        if (!directory.isDirectory()){
            return false;
        }
        Pattern p = Pattern.compile("TP(\\d{1,4})_Ch(\\d)_");
        minTime = Integer.MAX_VALUE;
        maxTime = Integer.MIN_VALUE;
        File[] files = directory.listFiles();
        for (File file : files){
            if (file.getName().endsWith("Probabilities.h5")){
                Matcher matcher = p.matcher(file.getName());
                if (matcher.find() ){
                    int time = Integer.valueOf(matcher.group(1));
                    channel = Integer.valueOf(matcher.group(2));
                    this.fileNames.put(time,file.getPath());
                    if (time < minTime){
                        minTime = time;
                    }
                    if (time > maxTime){
                        maxTime = time;
                    }
                }
            }
        }
        if (fileNames.isEmpty()){
            return false;
        }
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
            
            DataSetProperties ps = Ace3D_Frame.getProperties(dataset);
            ps.max = 2;
            ps.min = 1;
            ps.selected = select;
            Ace3D_Frame.getDataSetsDialog().setProperties(dataset, ps);
            TimePointImage tpi = TimePointImage.getSingleImage(dataset,getMinTime());
        } 
        props = Ace3D_Frame.datasetsSelected();        
        return true;
    }

    @Override
    public TimePointImage getImage(String acedatatset, int time) {
        String fileName = fileNames.get(time);
        if (fileName ==  null){
            return null;
        }
        File file = new File(fileName);
        HDF5Image image = new HDF5Image(file,this.HDF5dataset);
        TimePointImage ret = new TimePointImage(image.getImage(),image.getMinMax(),time,aceDataset);
        return ret;
    }

    @Override
    public int getTimes() {
        return maxTime - minTime + 1;
    }

    @Override
    public int getMinTime() {
        return minTime;
    }

    @Override
    public int getMaxTime() {
        return maxTime;
    }

    @Override
    public String getFile() {
        return directory.getPath();
    }

    @Override
    public Collection<DataSetDesc> getDataSets() {
        ArrayList<DataSetDesc> ret = new ArrayList<>();
        DataSetDescImpl ds = new DataSetDescImpl();
        ds.name = aceDataset;
        ret.add(ds);
        return ret;
    }

    @Override
    public void setFirstTime(int minTime) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Element toXML() {
        Element ret = new Element("HDF5DirectorySource");
        ret.setAttribute("directory", directory.getPath());
        ret.setAttribute("typicalFile",fileNames.firstEntry().getValue());
        ret.setAttribute("dataset",aceDataset);
        ret.setAttribute("selected",Boolean.toString(select));
        return ret;
    }
    public int getChannel(){
        return this.channel;
    }
    boolean select;
    int channel;
    int minTime;
    int maxTime;
    File directory;
    String HDF5dataset;
    String aceDataset;
    TreeMap<Integer,String> fileNames = new TreeMap<Integer,String>();    
}
