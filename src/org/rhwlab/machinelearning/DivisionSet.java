/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.machinelearning;


import java.io.File;
import java.util.Set;
import org.rhwlab.dispim.nucleus.NamedNucleusFile;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */

public class DivisionSet extends TrainingSet {

    @Override
    public void addNucleiFrom(File sessionXML, double localRegion) throws Exception {
        NamedNucleusFile nucFile = super.readNucleusFile(sessionXML);
        for (Integer time : nucFile.getAllTimes()){
            Set<Nucleus> nucs = nucFile.getNuclei(time);
            // create the positive set
            for (Nucleus nuc : nucs){
                if (nuc.isDividing()){
                    Nucleus[] next = nuc.nextNuclei();
                    data.add(formDataVector("+",nuc,next));
                }
            }
            // create a negative set
            for (Nucleus source : nucs){
                Set region = nucFile.localRegion(source, 100.0);
                if (region.size() < 2){
                    continue;  // there is not two nuclei in range
                }
                Set<Utils.Pair> pairs = Utils.allPairsCombinations(region);
            }
        }
    }

    @Override
    public Comparable[] formDataVector(String classification, Nucleus source, Nucleus[] next) {
        Comparable[] data = new Comparable[labels.length];
        data[0] = classification;
        data[1] = source.getTime();
        data[2] = source.getCellName();
        data[3] = source.distance(next[0]); 
        data[4] = source.distance(next[1]); 
        data[5] = next[0].distance(next[1]);
        
        return data;
    }

    @Override
    public String[] getLabels() {
        return labels;
    } 
    static String[] labels ={"Class","Time","Cell","Distance1","Distance2","Distance12"};    
}