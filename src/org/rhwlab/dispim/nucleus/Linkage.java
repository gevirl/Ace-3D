/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 * @author gevirl
 */
public class Linkage implements Comparable {
    
    public Linkage(Nucleus[] fromN,Nucleus[] toN){
        this.from = fromN;
        this.to = toN;
    }
/*
    // link the unlinked nuclei at a given time point to the previous time point
    // will attempt to segment the previous time point optimally, if not a curated time point
    static public Nucleus[] linkBack(ImagedEmbryo embryo,int time,BHCTreeDirectory bhcTreeDir)throws Exception {
        int earlyTime = time-1;
        // get the unlinked nuclei at the given time
        ArrayList<Nucleus> laterNucsToLink = new ArrayList<>();
        Set<Nucleus> nucsFromSet = embryo.getNuclei(time);
        for (Nucleus nuc : nucsFromSet){
            if (nuc.getParent() == null){
                laterNucsToLink.add(nuc);   // only looking at roots
            }
        }
        Set<Nucleus> existingEarlyNucs = embryo.getNuclei(earlyTime);
        if (existingEarlyNucs.isEmpty()){
            // form the same number of nuclei as the from time
            BHCTree tree= bhcTreeDir.getTree(earlyTime);
            BHCNucleusSet  earlyNucsSet = tree.cutToN(nucsFromSet.size());
            
            // link with Hungarian algorithm
            HungarianLinkInTime(earlyNucsSet.getNuclei(),laterNucsToLink);
            
            // form possible divisions by joining early nucs using BHC tree 
            
        }else if (embryo.getNucleusFile().isCurated(earlyTime)){
            // get the nuclei still available to link to
            ArrayList<Nucleus> leafEarlyNucs = new ArrayList<>();
            for (Nucleus nuc : existingEarlyNucs){
                if (nuc.isLeaf()){
                    leafEarlyNucs.add(nuc);
                }
            }
            // use Hungarian to link to early time point leaves
            HungarianLinkInTime(leafEarlyNucs,laterNucsToLink);
            
            
        } else {
            
        }
        
            
        
        
        return null;
    }
    */
    static void HungarianLinkInTime(Collection from,Collection to){
        Nucleus[] fromNucs = (Nucleus[])from.toArray(new Nucleus[0]);
        Nucleus[] toNucs = (Nucleus[])to.toArray(new Nucleus[0]);
        HungarianLinkInTime(fromNucs,toNucs);
    }
    // link a set of from nucs to a set of to nucs in time
    // the from nucs must be leaves and the to nucs must be roots
    static void HungarianLinkInTime(Nucleus[] fromNucs,Nucleus[] toNucs){
        // compute all pairwise distance between nuclei in the two adjacent time points
        double[][] dist = new double[fromNucs.length][];
        for (int r=0 ; r<dist.length ; ++r){
            dist[r] = new double[toNucs.length];
            for (int c=0 ; c<toNucs.length ; ++c){
                dist[r][c] = fromNucs[r].distance(toNucs[c]);  // this distance is weighted by intensity and volume
            }
        }
        
        // use Hungarian Algorithm to assign linking
        HungarianAlgorithm hungarian = new HungarianAlgorithm(dist);
        int[] linkage = hungarian.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                double d = fromNucs[i].distance(toNucs[linkage[i]]);
                if (d < timeLinkDistThresh){
                    fromNucs[i].linkTo(toNucs[linkage[i]]);

                    // if the from nuc is in a named cell , put child nuc in same cell
                    String cellname = fromNucs[i].getCellName();
                    toNucs[linkage[i]].setCellName(cellname,fromNucs[i].isUsernamed());
                }
            }
        }          
    }

/*    
    static Nucleus[] autoLinkage(Nucleus[] nucsToLink,int fromTime,BHCTreeDirectory bhcTreeDir)throws Exception {
        if (nucsToLink.length == 0)return null;
        
        BHCTree nextTree = bhcTreeDir.getTree(fromTime+1);
        if (nextTree == null){
            return null; // no tree built for the to time
        }
        
        Nucleus[] toNucs = bestCut(nucsToLink.length,nextTree,3000.0);
        Linkage next = new Linkage(nucsToLink,toNucs);
        next.formLinkage();        
        
        return toNucs;
           
    }
    */
    /*
    // automatic segmentation and linkage to next time point
    static Nucleus[] autoLinkage(Nucleus[] nucsToLink,int fromTime,BHCDirectory bhcTreeDir)throws Exception {

        Nucleus[] currentFrom = cloneNuclei(nucsToLink);
        if (nucsToLink.length == 0)return null;
        
        BHCTree nextTree = bhcTreeDir.getTree(fromTime+1);
        if (nextTree == null){
            return null; // no tree built for the to time
        }
        int nextN = currentFrom.length;  // start with the same number of nuclei as prior time
        TreeMap<Integer,Double> probMap = nextTree.allPosteriorProb(Math.min(10*nextN,600));    
        
       // find a probability at which to cut the nextTree
        double prob = probMap.get(nextN);
        while (prob < threshold){
            ++nextN;  // skipping cuts that have a probability less than the threshold
            if (probMap.get(nextN)==null){
                int iosdfihsd=0;
            }
            prob = probMap.get(nextN);
        }  
      
        // form a first  potential linkage
        Nucleus[] currentTo = nextTree.cutToN(nextN, minVolume(fromTime+1), 0.95);
        double nextProb = Math.exp(probMap.get(nextN));        
        Linkage current = new Linkage(currentFrom,currentTo);
        current.formLinkage();
        
        if (nextProb < .95){
            do {
                ++nextN;
                nextProb = Math.exp(probMap.get(nextN));
                Nucleus[] nextFrom = cloneNuclei(nucsToLink);
                Nucleus[] nextTo = nextTree.cutToN(nextN, minVolume(fromTime+1), 0.95);
                
                // find the minimum nucleus
                boolean tooSmall=false;
                for (Nucleus nuc : nextTo){
                    BHCNucleusData nucData = (BHCNucleusData)nuc.getNucleusData();
                    double vol = nucData.getVolume();
                    if (vol < minVolume(nuc.getTime())){
                        tooSmall = true;
                        break;  // oversegmentating
                    }
                }
                if (tooSmall){
                    break;
                }
                Linkage next = new Linkage(nextFrom,nextTo);
                next.formLinkage();
                
                if (next.compareTo(current)>0 || next.getToNuclei().size() < nextN){
                    break;  // the next linkage got worse - stop 
                }
                current = next;
            } while (true && nextProb <.95 );
        }
        
        // fix the child links in the original from nuclei to point to the final to nuclei
        for (int i=0 ; i<nucsToLink.length ; ++i){
            nucsToLink[i].setDaughters(current.from[i].getChild1(),current.from[i].getChild2());
        }
        for (int i=0 ; i<current.to.length ; ++i){
            
        }
        
        return current.getTo();
        
        
    }
    */
    static Nucleus[] cloneNuclei(Nucleus[] nucs){
        Nucleus[] ret = new Nucleus[nucs.length];
        for (int i=0 ; i<nucs.length ; ++i){
            ret[i] = nucs[i].clone();
        }
        return ret;
    }/*
    static private Nucleus[] cutTreeToNuclei(BHCTree nextTree,int n){
        BHCNucleusSet nextNucSet = nextTree.cutToN(n);
        Set<BHCNucleusData> nucData = nextNucSet.getNuclei();
        Nucleus[] toNucs = new Nucleus[nucData.size()];
        int i=0;
        for (BHCNucleusData nuc : nucData){
            toNucs[i] = new Nucleus(nuc);
            ++i;
        }    
        return toNucs;
    }
*/
    public void formLinkage(){
         // make sure the to nuclei are unlinked
         for (Nucleus nuc : this.to){
             nuc.unlink();
         }   
        List<Nucleus> fromList = noChildren(from);
        List<Nucleus> toList = noParents(to);
        
        Nucleus[] fromNucs = fromList.toArray(new Nucleus[0]);
        Nucleus[] toNucs = toList.toArray(new Nucleus[0]);

        Map<Nucleus,Nucleus> nearestMap = nearestNeighbors(fromNucs,toNucs);
        
        // link the nearest neigbors
        for (Entry<Nucleus,Nucleus> entry : nearestMap.entrySet()){
            double d = entry.getKey().distance(entry.getValue());
            if (d < timeLinkDistThresh){
                entry.getKey().linkTo(entry.getValue());

                // if the from nuc is in a named cell , put child nuc in same cell
                String cellname = entry.getKey().getCellName();
                entry.getValue().setCellName(cellname,entry.getKey().isUsernamed());
            }            
        }
        
        // find unlinked nuclei
        fromList = noChildren(from);
        toList = noParents(to);        
        
        // link the divisions
        HashMap<Nucleus,Division> best = Division.bestDivisions(fromList, toList);
        if (!best.isEmpty()){
            // link the best divisions, if any
            for (Division div : best.values()){
                div.parent.linkTo(div.child1);
                div.parent.linkTo(div.child2);
            }
        }
        
    }
    // form the linkage between the two sets of nuclei in this linkage
     public void formLinkageSave(){
         
         // make sure the to nuclei are unlinked
         for (Nucleus nuc : this.to){
             nuc.unlink();
         }
         
        // link the polar bodies first
//        this.linkPolarBodies();
        
        List<Nucleus> fromList = noChildren(from);
        List<Nucleus> toList = noParents(to);
        
        // link the divisions
        if (fromList.size() < toList.size()){
            HashMap<Nucleus,Division> best = Division.bestDivisions(fromList, toList);
            if (!best.isEmpty()){
                // link the best divisions, if any
                for (Division div : best.values()){
                    div.parent.linkTo(div.child1);
                    div.parent.linkTo(div.child2);
                }
                fromList = noChildren(from);
                toList = noParents(to);  
            }
        }
        
        // compute all pairwise distance between nuclei in the two adjacent time points
        Nucleus[] fromNucs = fromList.toArray(new Nucleus[0]);
        Nucleus[] toNucs = toList.toArray(new Nucleus[0]);
        double[][] dist = pairwiseDistance(fromNucs,toNucs);

        
        // use Hungarian Algorithm to assign linking
        HungarianAlgorithm hungarian = new HungarianAlgorithm(dist);
        int[] linkage = hungarian.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                double d = fromNucs[i].distance(toNucs[linkage[i]]);
                if (d < timeLinkDistThresh){
                    fromNucs[i].linkTo(toNucs[linkage[i]]);

                    // if the from nuc is in a named cell , put child nuc in same cell
                    String cellname = fromNucs[i].getCellName();
                    toNucs[linkage[i]].setCellName(cellname,fromNucs[i].isUsernamed());
                }
            }
        }        
    }
     
    static public double[][] pairwiseDistance(Nucleus[] fromNucs,Nucleus[] toNucs){
        double[][] dist = new double[fromNucs.length][];
        for (int r=0 ; r<dist.length ; ++r){
            dist[r] = new double[toNucs.length];
            for (int c=0 ; c<toNucs.length ; ++c){
                dist[r][c] = fromNucs[r].distance(toNucs[c]);  // this distance is weighted by intensity and volume
            }
        }
        return dist;
     }
    
    static public Map<Nucleus,Nucleus> nearestNeighbors(Nucleus[] t0,Nucleus[] t1){
        for (int i=0 ; i<t0.length ; ++i){
            for (int j=0 ; j<t1.length ;++j){
// System.out.printf("%s - %s %f\n",t0[i].getName(),t1[j].getName(),t0[i].distance(t1[j]));
            }
        }
        HashMap ret = new HashMap<>();
        for (int f=0 ; f<t0.length ; ++f){
            Nucleus source = t0[f];
            Nucleus sourceT0 = nearest(source,t0);
            Nucleus sourceT1 = nearest(source,t1);
// System.out.printf("Trying:%s   nearest:%s,%s\n", source.getName(),sourceT0.getName(),sourceT1.getName());
//            if (source.distance(sourceT1) < source.distance(sourceT0)/2.0){
//System.out.printf("distance ok %s nearest to %s\n",sourceT1.getName(),nearest(sourceT1,t0))                ;
                if (nearest(sourceT1,t0) == source){
//System.out.println("Mutable NN Trying t0 usable")                    ;
                    boolean usable = true;
                    for (int ff=0 ; ff<t0.length ; ++ff){
                        if (ff!=f){
                            if (nearest(t0[ff],t1) == sourceT1){
//System.out.printf("Not usable: nearest of %s = %s\n",t0[ff].getName(),sourceT1.getName());
                                usable =false;
                                break;
                            }
                        }
                    }
                    if (usable){
//System.out.println("Tying t1 usable")                        ;
                        for (int tt=0 ; tt<t1.length ; ++tt){
                            if (!t1[tt].getName().equals(sourceT1.getName())){
                                if (nearest(t1[tt],t0)==source){
//System.out.printf("Not usable: nearest of %s = %s\n",t1[tt].getName(),source.getName());                                    
                                    usable = false;
                                    break;
                                }
                            }
                        }
                    }
                    if (usable){
                        ret.put(source,sourceT1);
// System.out.printf("NN: %s,%s\n", source.getName(),sourceT1.getName());
                    }
 //               }
            }
            

        }
        return ret;
        
    }
    static private Nucleus nearest(Nucleus source,Nucleus[] dest){
        double dMin = Double.MAX_VALUE;
        int index = -1;
        for (int i=0 ; i<dest.length ; ++i){
            if (source != dest[i]){
                double d = source.distance(dest[i]);
                if (d < dMin){
                    index = i;
                    dMin = d;
                }
            }
        }
        return dest[index];
    }
    // make a list of nuclei that have no children
    static public  List<Nucleus> noChildren(Nucleus[] nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (nuc.getChild1()==null){
                ret.add(nuc);
            }
        }
        return ret;
    }
    static public List<Nucleus> noParents(Nucleus[] nucs){
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus nuc : nucs){
            if (nuc.getParent()==null){
                ret.add(nuc);
            }
        }
        return ret;        
    }

    @Override
    public int compareTo(Object o) {
        Linkage other = (Linkage)o;
        int ret = Integer.compare(this.getBirths().size(),other.getBirths().size());
        if (ret==0){
            ret = Integer.compare(this.getDeaths().size(), other.getDeaths().size());
        }
        return ret;
    }
    
    // get the roots  in the to nuclei
    public Set<Nucleus> getBirths(){
        TreeSet<Nucleus> ret = new TreeSet<>();
        for (Nucleus nuc : to){
            if (nuc.getParent() == null){
                ret.add(nuc);
            }
        }
        return ret;
    }
    public Set<Nucleus> getDeaths(){
        TreeSet<Nucleus> ret = new TreeSet<>();
        for (Nucleus nuc : from){
            if (nuc.nextNuclei().length == 0){
                ret.add(nuc);
            }
        }
        return ret;        
    }
    public TreeMap<String,Nucleus> getToNuclei(){
        TreeMap ret = new TreeMap<>();
        for (Nucleus nuc : to){
            ret.put(nuc.getName(), nuc);
        }
        return ret;
    }
    public TreeMap<String,Nucleus> getFromNuclei(){
        TreeMap ret = new TreeMap<>();
        for (Nucleus nuc : from){
            ret.put(nuc.getName(), nuc);
        }
        return ret;
    }
    public void linkPolarBodies(){
        // are any of the from nuclei labeled as polar
        ArrayList<Nucleus> polarFromList = new ArrayList<>();
        for (Nucleus nuc : from){
            if (nuc.getCellName().toLowerCase().startsWith("polar")){
                polarFromList.add(nuc);
            }
        }
        if (polarFromList.isEmpty()){
            return;
        }

        // find the same number of nuclei in the to list that are polar
        // sort the to nuclei by intensity density
        Nucleus[] copyTo = Arrays.copyOf(to, to.length);
        Arrays.sort(copyTo, new Comparator(){
            @Override
            public int compare(Object o1, Object o2) {
                BHCNucleusData d1 = (BHCNucleusData)((Nucleus)o1).getNucleusData();
                BHCNucleusData d2 = (BHCNucleusData)((Nucleus)o2).getNucleusData();
                
                return -Double.compare(d1.getAverageIntensity(),d2.getAverageIntensity());
            }
        });
        // compute distance array for polar bodies
        double d[][] = new double[polarFromList.size()][];
        for (int i=0 ; i<polarFromList.size() ; ++i){
            d[i] = new double[polarFromList.size()];
            for (int j=0 ; j<polarFromList.size() ; ++j){
                d[i][j] = polarFromList.get(i).distance(copyTo[j]);
            }
        }
        
        // use Hungarian Algorithm to find best connection (overkill, but general in number of polar bodies)
        HungarianAlgorithm hung = new HungarianAlgorithm(d);
        int[] linkage = hung.execute();
        
        // link the nuclei
        for (int i=0 ; i<linkage.length ; ++i){
            if (linkage[i]!=-1){
                polarFromList.get(i).linkTo(copyTo[linkage[i]]);
                
                // if the from nuc is in a named cell , put child nuc in same cell
                String cellname = polarFromList.get(i).getCellName();
                copyTo[linkage[i]].setCellName(cellname,polarFromList.get(i).isUsernamed());
            }
        }
    }
    public Nucleus[] getFrom(){
        return from;
    }
    public Nucleus[] getTo(){
        return to;
    }

    static public double minVolume(int time){
        if (minVolumes==null){
            minVolumes = new TreeMap<>();

            minVolumes.put(15, 4000.);
            minVolumes.put(80, 3000.);
            minVolumes.put(125, 1500.);
            minVolumes.put(150, 750.);
            minVolumes.put(900, 20.);

        }
        Entry e = minVolumes.ceilingEntry(time);
        if (e != null){
            return (Double)e.getValue();
        }
        return minVolume;
    }    
    Nucleus[] from;
    Nucleus[] to;   
    
    static double timeLinkDistThresh = 50;
    static double minVolume = 1000.;
    static TreeMap<Integer,Double> minVolumes = null;
}
