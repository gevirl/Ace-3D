/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import org.jdom2.Element;
import org.rhwlab.BHC.BHCTree;
import org.rhwlab.BHC.BHCTree.Match;
import org.rhwlab.BHC.NucleusLogNode;

/**
 *
 * @author gevirl
 */
public class LinkedNucleusFile implements NucleusFile {
    public LinkedNucleusFile(){
        
    }

    @Override
    public void fromXML(Element nucleiEle) {

//        selectedNucleus = new SelectedNucleus();
        
        TreeMap<Integer,Element> timeEleMap = new TreeMap<>();
        for (Element timeEle : nucleiEle.getChildren("Time")){
            int t = Integer.valueOf(timeEle.getAttributeValue("time"));
            timeEleMap.put(t, timeEle);
        }
        for (int t : timeEleMap.descendingKeySet()){
//System.out.printf("time %d\n",t);
            TreeMap<String,Nucleus> nucMap = new TreeMap<>();
            TreeMap<String,Nucleus> remMap = new TreeMap<>();
            Element timeEle = timeEleMap.get(t);
            if (Boolean.valueOf(timeEle.getAttributeValue("curated"))){
                this.curatedSet.add(t);
            }
            String prob = timeEle.getAttributeValue("segmentedProb");
            if (prob != null){
                this.thresholdProbs.put(t, Integer.valueOf(prob));
            }

            for (Element nucEle : timeEle.getChildren("Nucleus")){
                BHCNucleusData bhcNuc = new BHCNucleusData(nucEle);             
                Nucleus nuc = new Nucleus(bhcNuc);  
                String active = nucEle.getAttributeValue("active");
                if (active==null || active.equals("true")){

                    nuc.setCellName(nucEle.getAttributeValue("cell"), Boolean.valueOf(nucEle.getAttributeValue("usernamed")));
                    nucMap.put(nuc.getName(), nuc);

                    // link children
                    String c1 = nucEle.getAttributeValue("child1");
                    if (c1 != null){
                        TreeMap<String,Nucleus> nextNucs = byTime.get(t+1);
                        if (nextNucs != null){
                            Nucleus child1Nuc = nextNucs.get(c1);
                            Nucleus child2Nuc = null;
                            String c2 = nucEle.getAttributeValue("child2");
                            if (c2 != null){
                                child2Nuc = nextNucs.get(c2);
                            }
                            nuc.setDaughters(child1Nuc, child2Nuc);
                        }
                    }
                }else {
                    remMap.put(nuc.getName(), nuc);
                }
            }
            this.byTime.put(t, nucMap);
            this.remnants.put(t, remMap);
        }
    }
    
    public Element toXML(){
        Element ret = new Element("Nuclei");
        ret.setAttribute("BHCTreeDirectory", this.bhcTreeDir.dir.getPath());
        for (Integer t : this.byTime.keySet()){
            ret.addContent(timeAsXML(t));
        }
        return ret;
    }
    public Element timeAsXML(int time){
        Element ret = new Element("Time");
        ret.setAttribute("time", Integer.toString(time));
        if (curatedSet.contains(time)){
            ret.setAttribute("curated", Boolean.toString(true));
        }else {
            ret.setAttribute("curated", Boolean.toString(false));
        }
        Integer prob = this.thresholdProbs.get(time);
        if (prob != null){
            ret.setAttribute("segmentedProb", prob.toString());
        }
        
        for (Nucleus nuc : byTime.get(time).values()){
            Element nucEle = nuc.asXML();
            nucEle.setAttribute("active", "true");
            ret.addContent(nucEle);
        }
        TreeMap<String,Nucleus> rems = this.remnants.get(time);
        if (rems != null){
            for (Nucleus nuc : rems.values()){
                Element nucEle = nuc.asXML();
                nucEle.setAttribute("active", "false");
                ret.addContent(nucEle);                
            }
        }
        
        return ret;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("BHC", this.bhcTreeDir.dir.getPath());
        builder.add("Times", this.timesAsJson());
        return builder;
    }    
    public JsonArrayBuilder timesAsJson(){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Integer time : byTime.navigableKeySet()){
            Set<Nucleus> roots = this.getRoots(time);
            if (roots.size()>0){
                builder.add(timeAsJson(time,this.curatedSet.contains(time)));
            }
        } 
        return builder;
    }
    public JsonObjectBuilder timeAsJson(int t,boolean cur){
 
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Time",t);
        builder.add("Curated", cur);
        builder.add("Nuclei", nucleiAsJson(t));
        
        return builder;
    }
    public JsonArrayBuilder nucleiAsJson(int t){
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (Nucleus nuc : byTime.get(t).values()){
            if (nuc.getParent()==null){
                builder.add(nuc.asJson());
            }
        }
        return builder;
    }   

    @Override
    public Set<Nucleus> getNuclei(int time) {
        TreeSet ret = new TreeSet<>();
        if (this.byTime.get(time) != null){
            ret.addAll(this.byTime.get(time).values());
        }
        return ret;
    }

    @Override
    public File getFile() {
        return this.file;
    }

    @Override
    public Set<Nucleus> getRoots(int time) {
        TreeSet<Nucleus> ret = new TreeSet<>();
        Set<Nucleus> all = this.getNuclei(time);
        if (all != null){
            for (Nucleus nuc : all){
                if (nuc.getParent()==null){
                    ret.add(nuc);
                }
            }
        }
        return ret;
    }
    @Override
    public Set<Nucleus> getLeaves(int time) {
        TreeSet<Nucleus> ret = new TreeSet<>();
        Set<Nucleus> all = this.getNuclei(time);
        if (all != null){
            for (Nucleus nuc : all){
                if (nuc.isLeaf()){
                    ret.add(nuc);
                }
            }
        }
        return ret;
    }    

    @Override
    public List<Nucleus> linkedForward(Nucleus nuc) {
        ArrayList<Nucleus> ret = new ArrayList<>();
        for (Nucleus child : nuc.nextNuclei()){
            ret.add(child);
        }
        return ret;
    }

    @Override
    public Nucleus linkedBack(Nucleus nuc) {
        return nuc.getParent();
    }

    @Override
    public Nucleus sister(Nucleus nuc) {
        return nuc.getSister();
    }

    public void addNucleusRecursive(Nucleus nuc){
        this.addNucleus(nuc);
        if (nuc.getChild1() != null){
            addNucleusRecursive(nuc.getChild1());
        }
        if (nuc.getChild2()!= null){
            addNucleusRecursive(nuc.getChild2());
        }
    }
    public void addCuratedNucleus(Nucleus nuc){
        this.addNucleus(nuc);
        
        this.curatedSet.add(nuc.getTime());
    }
    @Override
    public void addNucleus(Nucleus nuc) {
        int t= nuc.getTime();
        TreeMap<String,Nucleus> nucMap = byTime.get(t);
        if (nucMap == null){
            nucMap = new TreeMap<>();
            byTime.put(t, nucMap);
        }
        nucMap.put(nuc.getName(),nuc);
    }

    @Override
    public Set<Integer> getAllTimes() {
        return this.byTime.descendingKeySet();
    }

    @Override
    public void setSelected(Nucleus nuc) {
        this.selectedNucleus.setSelectedNucleus(nuc);
    }

    @Override
    public Nucleus getSelected() {
        return this.selectedNucleus.getSelected();
    }

    @Override
    public void addListener(InvalidationListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        this.listeners.remove(listener);
    }
    
    @Override
    public void addNuclei(int probThresh,BHCNucleusSet bhcToAdd,boolean curated){
        if (curated) curatedSet.add(bhcToAdd.getTime());
        
        TreeMap<String,Nucleus> nucsAtTime = new TreeMap<>();
        for (BHCNucleusData nuc : bhcToAdd.getNuclei()){
            Nucleus linkedNuc = new Nucleus(nuc);

            nucsAtTime.put(linkedNuc.getName(),linkedNuc);
        }
        byTime.put(bhcToAdd.getTime(),nucsAtTime);
        this.thresholdProbs.put(bhcToAdd.getTime(), probThresh);
        notifyListeners();
    }
    public void notifyListeners(){
        if (opening){
            return;
        }
        for (InvalidationListener listener : listeners){
            if (listener != null){
                listener.invalidated(this);
            }
        }
    }
    @Override
    public BHCDirectory getTreeDirectory(){
        return this.bhcTreeDir;
    }  

    @Override
    public void addSelectionOberver(ChangeListener obs){
        selectedNucleus.addListener(obs);
    }
    
    public Nucleus[] cloneTime(int t){
        Collection<Nucleus> src = byTime.get(t).values();
        Nucleus[] ret = new Nucleus[src.size()];
        int i=0;
        for (Nucleus nuc : src){
            ret[i] = nuc.clone();
            ++i;
        }
        return ret;
    }
    @Override
    public void setFile(File f){
        this.file = f;
    }

    @Override
    public void setBHCTreeDirectory(BHCDirectory bhc) {
        this.bhcTreeDir = bhc;
    } 
    // unlink all the nuclei at a time point from parents
    public void unlinkTime(int time,boolean notify){
        Set<Nucleus> nucs = this.getNuclei(time);
        for (Nucleus nuc : nucs){
            this.unlinkNucleus(nuc, false);
        }
        if (notify){
            this.notifyListeners();
        }
    }
    // unlink a nucleus from its parent
    public void unlinkNucleus(Nucleus nuc,boolean notify){
        Nucleus parent = nuc.getParent();
        if (parent == null){
            return;
        }
        // the nucleus being unlinked must get a new cellname
        nuc.renameContainingCell(nuc.getName());
        
        if (parent.getChild1()==nuc){
            // move parents child2 into child1
            parent.setDaughters(parent.getChild2(), null);
        }else {
            // unlinking parents child2
            parent.setDaughters(parent.getChild1(), null);
        }
        if (parent.getChild1()!=null){
            // child1 now part of parents cell
            parent.getChild1().renameContainingCell(parent.getCellName());            
        }
        nuc.setParent(null);
        
        if (notify){
            this.notifyListeners();
        }
    }

    // remove all the nclei at a time point
    public void removeNuclei(int time,boolean notify){
        Nucleus[] nucs = this.getNuclei(time).toArray(new Nucleus[0]);
        for (int i=0 ; i<nucs.length ; ++i){
            this.removeNucleus(nucs[i], false);
        }
        
        // remove any remanants too
        TreeMap<String,Nucleus> map = this.remnants.get(time);
        if (map != null) {
            map.clear();
        }
        
        if (notify){
            this.notifyListeners();
        }
        
    }
    @Override
    public void removeNucleus(Nucleus nuc,boolean notify) {
        
        if (selectedNucleus.getSelected()!=null && selectedNucleus.getSelected().equals(nuc)){
            this.setSelected(null);
        }
        
        // unlink any child fron the nuc being deleted
        if (nuc.getChild1() != null){
            unlinkNucleus(nuc.getChild1(),false);
        }
        if (nuc.getChild2() != null){
            unlinkNucleus(nuc.getChild2(),false);
        }
        // unlink from parent
        unlinkNucleus(nuc,false);  

        TreeMap<String,Nucleus> map = byTime.get(nuc.getTime());
        map.remove(nuc.getName());
        
        if (map.isEmpty()){
            byTime.remove(nuc.getTime());
            curatedSet.remove(nuc.getTime());
        } else {
            curatedSet.add(nuc.getTime());
        }
        if (notify){
            this.notifyListeners();
        }
    }
    
    public Nucleus getMarked(){
        return selectedNucleus.getMarked();
    }
    public void setMarked(Nucleus toMark){
        selectedNucleus.setMarked(toMark);
    }
    // remove all the nuclei between curated time points
    public void clearInterCuratedRegion(int time,boolean notify){
        Integer[] range = curatedTimes(time);
        if (range[0] == null || range[1] == null || (range[0].intValue()==range[1].intValue())) return;  // not a clearable region
        this.unlinkTime(range[1], false);
        for (int t=range[1]-1 ; t>range[0] ; --t){
            this.removeNuclei(t, false);
        }
        if (notify){
            this.notifyListeners();
        }
    }
    // determine the range of times for an inter curated region
    public Integer[] curatedTimes(int time){
        Integer[] ret = new Integer[2];
        if (this.curatedSet.contains(time)){
            Integer floor = curatedSet.floor(time-1);
            if (floor != null){
                ret[0] = floor;
                ret[1] = time;
            }else {
                Integer ceil = curatedSet.ceiling(time+1);
                if (ceil != null){
                    ret[0] = time;
                    ret[1] = ceil;
                }else {
                    ret[0] = ret[1] = time;
                } 
            }
            
        } else {
            ret[0] = curatedSet.floor(time);
            ret[1] = curatedSet.ceiling(time);
        }
        return ret;
    }
    public void bestMatchAutoLink(Integer[] times,Integer[] threshs,int minVolume)throws Exception {
       
//        Nucleus[] fromNucs = this.getNuclei(times[0]).toArray(new Nucleus[0]);
        Nucleus[] toNucs;
        for (int i=1 ; i<times.length ; ++i){
            Nucleus[] fromNucs = this.getNuclei(times[i-1]).toArray(new Nucleus[0]);
            int t = times[i];
            if (t == 24){
                int uisahdfs=0;
            }
            BHCTree tree = bhcTreeDir.getTree(t,threshs[i]);
            
            tree.clearUsed();
            if (isCurated(t)){
                toNucs = this.getNuclei(t).toArray(new Nucleus[0]);
                Linkage linkage = new Linkage(fromNucs,toNucs);
                linkage.formLinkage();                
            } else {
                TreeMap<String,Nucleus> remnantMap = this.remnants.get(t);
                if (remnantMap == null){
                    remnantMap = new TreeMap<>();
                    remnants.put(t,remnantMap);
                }    
                remnantMap.clear();
                this.removeNuclei(t, false);
                this.thresholdProbs.put(t,threshs[i]);
                ArrayList<Nucleus> toList = new ArrayList<>();
                // separate polar and non-polar
                ArrayList<Nucleus> polar = new ArrayList<>();
                ArrayList<Nucleus> nonPolar = new ArrayList<>();
                for (Nucleus nuc : fromNucs){
                    if (nuc.getCellName().contains("polar")){
                        polar.add(nuc);
                    }else {
                        nonPolar.add(nuc);
                    }
                }
                // do the polar bodies - no division considered
                for (Nucleus nuc : polar){
                    NucleusLogNode best = tree.bestMatchInAvailableNodes(nuc,minVolume).getNode();
                    NucleusLogNode expand = tree.expandUp(nuc, best);
                    Nucleus bestNuc = best.getNucleus(t);
                    Nucleus expandNuc = expand.getNucleus(t);
                    if (bestNuc != null){
                        expand.markedAsUsed();
                        toList.add(expandNuc);
                        this.addNucleus(expandNuc);
                        nuc.linkTo(expandNuc);  
                    }
                }
                
                // find the best matches to all the nonpolar
                TreeMap<Nucleus,NucleusLogNode> matches = new TreeMap<>();
                TreeMap<Nucleus,NucleusLogNode> expands = new TreeMap<>();
                for (Nucleus nuc : nonPolar){
                    if (nuc.getName().equals("106_4710")||nuc.getName().equals("107_6272")){
                        int fsdiusadgf=0;
                    }
                    Match best = tree.bestMatchInAvailableNodes(nuc,minVolume);
                    if (best != null){
                        matches.put(nuc,best.getNode());
                        NucleusLogNode expand = tree.expandUp(nuc, best.getNode());
                        expands.put(nuc,expand);
                        expand.markedAsUsed();
                    }
                }
                
                // try to make some divisions
                for (Nucleus nuc : nonPolar){
                    if (nuc.getName().equals("111_6070")){
                        int fsdiusadgf=0;
                    }
                    NucleusLogNode matchNode = matches.get(nuc);   
                    Nucleus[] divided = tree.divideBySplit(nuc, matchNode);
                    if (divided != null){
                        // best match divides - add both children
System.out.println("Division by split")   ;
                        toList.add(divided[0]);
                        this.addNucleus(divided[0]);
                        nuc.linkTo(divided[0]); 
                        
                        toList.add(divided[1]);
                        this.addNucleus(divided[1]);
                        nuc.linkTo(divided[1]);                         
                        
                    } else {
                        // just add the best expanded match
                        NucleusLogNode expanded = expands.get(nuc);
                        if (expanded != null){
                            Nucleus expandedNuc = expanded.getNucleus(t);
                           
                            toList.add(expandedNuc);
                            this.addNucleus(expandedNuc);
                            nuc.linkTo(expandedNuc);   
                        }
                    }
                }
                
                // try to make divisions with any remaining unused nodes
                Set<NucleusLogNode> avails =tree.availableNodes(minVolume);
                for (NucleusLogNode avail : avails){
                    Nucleus availNuc = avail.getNucleus(t);
                    if (availNuc != null){
                        TreeSet<Division> possibleDivs = new TreeSet<>();
                        for (Nucleus nuc : nonPolar){
                            if (availNuc.getName().equals("203_7018") && nuc.getName().equals("202_17952")){
                                int sauifhuisdf=0;
                            }
                            if (!nuc.isDividing()){
                                Nucleus[] next = nuc.nextNuclei();
                                if (next.length >0 && next[0] != null){
                                    Division div = new Division(nuc,next[0],availNuc);
                                    if (div.isPossible()){
                                        System.out.println("Division by available");
                                        possibleDivs.add(div);
//                                        if (completeTheDivision(t,nuc,avail,availNuc,toList)){
 //                                           break;
                                    }
                                }
                            }
                        }
                        if (!possibleDivs.isEmpty()){
                            if (possibleDivs.size()>1){
                                int sauifhuishdf=0;
                            }
                            Division div = possibleDivs.first();
                            completeTheDivision(t,div.parent,avail,div.child2,toList);
                        }                        
                    }
                }
                // put any remaining segmentations into remant nucs
            
                avails =tree.availableNodes(minVolume);
                for (NucleusLogNode avail : avails){   
                    Nucleus availNuc = avail.getNucleus(t);
                    if (availNuc !=null && availNuc.getVolume() > 500 )
                    remnantMap.put(availNuc.getName(),availNuc);                
                }             
                toNucs = toList.toArray(new Nucleus[0]);
                fromNucs = toNucs; 
            }          
        }
        this.notifyListeners();
    }
    private boolean completeTheDivision(int t,Nucleus nuc,NucleusLogNode avail,Nucleus availNuc,ArrayList<Nucleus> toList){
            avail.markedAsUsed();
            toList.add(availNuc);
            this.addNucleus(availNuc);
            nuc.linkTo(availNuc);
            return true;
    }
/*            
        boolean overlap = false;
        for (Nucleus existingNuc : this.getNuclei(t)){
            if (existingNuc.distance(availNuc)<200.0){
                if (Nucleus.intersect(availNuc,existingNuc)){
                    overlap = true;
                    break;
                }
            }
        }
        if (!overlap){
            avail.markedAsUsed();
            toList.add(availNuc);
            this.addNucleus(availNuc);
            nuc.linkTo(availNuc);
            return true;
        } 
        return false;
    }
*/
    // remove all the nuclei in the cell containing the given nucleus
    public void removeCell(Nucleus nuc,boolean notify){
        Nucleus last = lastNucleusInCell(nuc);
        removeAncestorsInCell(last);
        if (notify){
            this.notifyListeners();
        }
    }

    // remove all the ancestor nuclei in the cell containing this nucleus
    public void removeAncestorsInCell(Nucleus nuc){
        Nucleus par = nuc.getParent();
        if (par != null){
            if (!par.isDividing()){
                removeAncestorsInCell(par);
            }
        }
        removeNucleus(nuc,false);
    }
    // return the last nucleus in the cell containing the given nucleus
    static public Nucleus lastNucleusInCell(Nucleus nuc){
        if (nuc.isDividing() || nuc.isLeaf()){
            return nuc;
        } else {
            return lastNucleusInCell(nuc.getChild1());
        }
    }
    //@Override
    public boolean isCurated(int time) {
        return curatedSet.contains(time);
    }
    
    public void clear(boolean notify){
        byTime = new TreeMap<>();
        curatedSet = new TreeSet<>();
        selectedNucleus.setSelectedNucleus(null);
        selectedNucleus.setMarked(null);
        if (notify){
            this.notifyListeners();
        }
    }
    public boolean selectionChanged(){
        return selectedNucleus.selectedHasChanged();
    }
    public Integer getThresholdProb(int time){
        return thresholdProbs.get(time);
    }
    
    // activate the remnant at the given position and time
    public Nucleus activateRemnant(int time,long[] pos,int minVolume)throws Exception{
        TreeMap<String,Nucleus> rems = remnants.get(time);
        Nucleus closest = null;
        double d = Double.MAX_VALUE;
        if (rems != null && !rems.isEmpty()){
            String key = null;
            for (String remnantName : rems.keySet()){
                Nucleus rem = rems.get(remnantName);
                double[] center = rem.getCenter();
                double sum = 0.0;
                for (int i=0 ; i<pos.length ; ++i){
                    double del = center[i] - pos[i];
                    sum = sum + del*del;
                }
                sum = Math.sqrt(sum);
                if (sum < d){
                    d = sum;
                    closest = rem;
                    key = remnantName;
                }
            }
            long[] radii = closest.getRadii();
            if (d <= radii[2]){
                this.addNucleus(closest);
                this.buildRemnants(time,minVolume);
                this.notifyListeners();
                curatedSet.add(time);
                return closest;
            }
        }
        return null;
    }
    public Set<Nucleus> getRemnants(int time,double minVolume){
        TreeSet<Nucleus> ret = new TreeSet();
        TreeMap<String,Nucleus> remnantMap = remnants.get(time);
        if (remnantMap != null){
            for (Nucleus nuc : remnantMap.values()){
                if (nuc.getVolume() >= minVolume){
                    ret.add(nuc);
                }
            }
        }
        return ret;
    }
    // rebuild the list of remnants at the given time based on the active nuclei at the time
    public void buildRemnants(int time,int minVolume)throws Exception {
        int probThresh =this.thresholdProbs.get(time);
        Set<Nucleus> activeNucs = this.getNuclei(time);
        BHCTree tree = bhcTreeDir.getTree(time, probThresh);
        tree.clearUsed();
        TreeMap<String,Nucleus> rems = remnants.get(time);
        rems.clear();
        
        // mark the tree nodes corresponding to active nuclei as used 
        for (Nucleus activeNuc : activeNucs){
            BHCNucleusData nucData = (BHCNucleusData)activeNuc.getNucleusData();
            String srcNode = nucData.getSourceNode();
            NucleusLogNode node = (NucleusLogNode)tree.findNode(Integer.valueOf(srcNode));
            node.markedAsUsed();
        }
        
        Set<NucleusLogNode> avails = tree.availableNodes(minVolume);
        for (NucleusLogNode avail : avails){
            Nucleus nuc = avail.getNucleus(time);
            if (nuc != null){
                rems.put(nuc.getName(), nuc);
            }
        }
        this.notifyListeners();
    }
    // find the set of nuclei in the local region of a source nucleus
    public Set<Nucleus> localRegion(Nucleus source,double radius){
        HashSet<Nucleus> ret = new HashSet<>();
        for (Nucleus nuc : this.getNuclei(source.getTime()+1)){
            if (source.distance(nuc) <= radius ){
                ret.add(nuc);
            }
        }
        return ret;
    }
    
    File file;
    TreeMap<Integer,TreeMap<String,Nucleus>> byTime=new TreeMap<>();  // all the nuclei indexed by time and name
    TreeSet<Integer> curatedSet = new TreeSet<>();  //curated times
    TreeMap<Integer,Integer> thresholdProbs = new TreeMap<>();  // the thresholds probs for each time used to construct nuclei from bhc trees
    TreeMap<Integer,TreeMap<String,Nucleus>> remnants = new TreeMap<>(); // nuclei made from supervoxels that are left after auto linking -  may be false negatives
    
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
    SelectedNucleus selectedNucleus = new SelectedNucleus();
    BHCDirectory bhcTreeDir;
    
    boolean opening = false;
}
