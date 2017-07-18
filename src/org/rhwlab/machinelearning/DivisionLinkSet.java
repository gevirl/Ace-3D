package org.rhwlab.machinelearning;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.Division;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */

// used to decide if parent nucleus should link to a daughter as one leg of a division
// this assumes the parent has already been determined to be a likely dividing nucleus
//  and is to be used to find likely daughters of the division
public class DivisionLinkSet extends TrainingSet {
    public DivisionLinkSet(){
        super();
    }
    
    public DivisionLinkSet(Integer minTime,Integer maxTime){
        super(minTime,maxTime);

    }
    @Override
    public void addNucleiFrom(NamedNucleusFile nucFile, double localRegion,double prob) throws Exception {
        for (Integer time : nucFile.getAllTimes()){
            if (minTime == null || time >= minTime){
                if ((maxTime == null | time <=maxTime)){            
                    Set<Nucleus> nucs = nucFile.getNuclei(time);
                    for (Nucleus nuc : nucs){
                        if (nuc.isDividing()){
                            Nucleus[] next = nuc.nextNuclei();
                            addDataRecord(formDataVector("+",nuc,next[0]),prob);   
                            addDataRecord(formDataVector("+",nuc,next[1]),prob);
                        }else {
                            for (Nucleus negNuc : nucFile.localRegionPlusRemnants(nuc, localRegion, 0.2*nuc.getVolume())){
                                addDataRecord(formDataVector("-",nuc,negNuc),prob);
                            }
                        }
                    }
                }
            }
        }        
    }

    
    @Override
    public Comparable[] formDataVector(String cl, Nucleus source, Object nextObj) {
        Nucleus next = (Nucleus)nextObj;
        Comparable[] data = new Comparable[labels.length];
        data[0] = cl;
        data[1] = source.getTime();
        data[2] = source.getCellName();
        data[3] = source.distance(next); 
        data[4] = source.getVolume()/next.getVolume();
        data[5] = source.getAvgIntensity()/next.getAvgIntensity();
        RealVector sourceAxis = source.getAxes()[0];
        RealVector childAxis = next.getAxes()[0];
        data[6] = Division.cosineAngleBetween(sourceAxis, childAxis);
        double[] ecc = next.eccentricity();
        data[7] = ecc[0];
        data[8] = ecc[1];
        data[9] = ecc[2];
        return data;
    }

    @Override
    public String[] getLabels() {
        return labels;
    }
    @Override
    public TreeMap<String,Integer> getLabelsAsMap(){
        if (labelMap == null){
            labelMap = super.buildLabelsMap(labels);
        }
        return labelMap;
    }    
    static public void main(String[] args)throws Exception{
        String[] files = {"/net/waterston/vol9/diSPIM/20161214_vab-15_XIL099/pete3.xml",
                           "/net/waterston/vol9/diSPIM/20161229_hmbx-1_OP656/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170103_B0310.2_OP642/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170105_M03D4.4_OP696/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170118_sptf-1_OP722/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170125_lsl-1_OP720/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170321_unc-130_OP76/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170329_cog-1_OP541/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170405_irx-1_OP536/pete.xml",
                            "/net/waterston/vol9/diSPIM/20170411_mls-2_OP645/pete.xml"
                };
        NamedNucleusFile[] nucFiles = new NamedNucleusFile[files.length];
        for (int i=0 ; i<files.length ; ++i){
            nucFiles[i] = TrainingSet.readNucleusFile(new File(files[i]));
        }
        
        int delTime = 50;
        int overlap = 10;
        for (int i=0 ; i<5 ; ++i){
            if (i==4){
            DivisionLinkSet ts = new DivisionLinkSet(i*delTime-overlap,(i+1)*delTime+overlap);
            for (int f=0 ; f<nucFiles.length ; ++f){
                ts.addNucleiFrom(nucFiles[f],100.0,0.1);
            }
            ts.formDecisionTree(0);
            OutputStream stream = new FileOutputStream(String.format("/net/waterston/vol9/diSPIM/DivisionLinkTree%03d.xml",delTime*(i+1)));
            XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
            out.output(ts.toXML("Root"), stream);
            stream.close();  
            }
            
        } 
    }
    static String[] labels = {"Class","Time","Cell","Distance","VolumeRatio","IntensityRatio","MinorAxisCosine",
                            "ChildEcc1","ChildEcc2","ChildEcc3"}; 
    static TreeMap<String,Integer> labelMap;

    @Override
    public String[] getDataClasses() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
