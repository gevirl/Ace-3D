/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;

import java.io.File;

import java.io.OutputStream;
import java.util.Set;
import java.util.TreeMap;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 **/

public class TimeLinkageSet extends TrainingSet {

   
    @Override
    public void addNucleiFrom(File sessionXML,double localRegion)throws Exception {
        NamedNucleusFile nucFile = super.readNucleusFile(sessionXML);
        for (Integer time : nucFile.getAllTimes()){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            // create the positive set
            for (Nucleus nuc : nucs){
                if (!nuc.isDividing() && !nuc.isLeaf()){
                    Nucleus[] next = nuc.nextNuclei();
                    data.add(formDataVector("+",nuc,next));
                }
            }
            // create a negative set
            for (Nucleus source : nucs){
                if (!source.isLeaf()){
                    Nucleus[] next = source.nextNuclei();
                    for (Nucleus nuc : nucFile.getNuclei(source.getTime()+1)){
                        if (nuc != next[0]){
                            if (next.length==2 && nuc != next[1]){
                                if (source.distance(nuc) <= localRegion){
                                    Nucleus[] negNuc = new Nucleus[1];
                                    negNuc[0] = nuc;
                                    data.add(formDataVector("-",source,negNuc));
                                }
                            }
                        }
                    }
                    for (Nucleus remnant : nucFile.getRemnants(source.getTime(), 100)){
                        if (source.distance(remnant)<=localRegion){
                            Nucleus[] negNuc = new Nucleus[1];
                            negNuc[0] = remnant;
                            data.add(formDataVector("-",source,negNuc));
                        }
                    }
                    
                }
            }
        }
    }


    @Override
    public Comparable[] formDataVector(String classification,Nucleus source, Nucleus[] next) {
        Comparable[] data = new Comparable[labels.length];
        data[0] = classification;
        data[1] = source.getTime();
        data[2] = source.getCellName();
        data[3] = source.distance(next[0]); 
        double volratio = source.getVolume()/next[0].getVolume();
        if (volratio < 1.0) volratio = 1.0/volratio;
        data[4] = volratio;
        double intratio = source.getAvgIntensity()/next[0].getAvgIntensity();
        if (intratio < 1.0) intratio = 1.0/intratio;
        data[5] = intratio;
        int postTime = source.timeSinceDivsion();
        if (postTime == -1) postTime = 0;
        data[6] = postTime;
        return data;
    }  
    public String[] getLabels(){
        return labels;
    }
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = new TreeMap<>();
            for (int i=0 ; i<labels.length ; ++i){
                labelMap.put(labels[i],i);
            }
        }
        return labelMap;
    }
    static public void main(String[] args)throws Exception{
        TimeLinkageSet ts = new TimeLinkageSet();
        ts.addNucleiFrom(new File("/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/pete3.xml"),100.0);
/*      
        double delMax = 0.0;
        int count=0;
        double splitValue  = 0;
        for (double v =2.0 ; v<=98.0 ; v = v + 1.0){
            ts.split(3, v);
            double del = ts.getDelta();
            if (del > delMax){
                delMax = del;
                count = ts.lessSet.data.size();
                splitValue = v;
            }
            System.out.printf("value=%.0f  delta=%s\n", v,del);
        }
        System.out.println(count);
        System.out.println(splitValue);
        ts.split(3,splitValue);
       
        for (int c =3 ; c<labels.length ; ++c){
            ts.sort(c);
            ColumnGain gain = ts.bestSplitInRange(0,ts.data.size());
            System.out.printf("%s,%d,%s,%f\n",labels[c], gain.index,ts.data.get(gain.index)[c].toString(),gain.gain);
        }
  //      ts.saveData(System.out, labels);
  */
        ts.formDecisionTree(0);
        
//        OutputStream stream = new FileOutputStream(xml);     
        OutputStream stream = System.out;
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(ts.toXML("Root"), stream);
        stream.close(); 
        
        int iusagdfugsd=0;
    }
    static String[] labels = {"Class","Time","Cell","Distance","VolumeRatio","IntensityRatio","PostDivisionTime"};
    static TreeMap<String,Integer> labelMap;


}
