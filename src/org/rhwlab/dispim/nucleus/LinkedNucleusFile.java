/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import org.rhwlab.BHC.Sphere;

/**
 *
 * @author gevirl
 */
public class LinkedNucleusFile implements NucleusFile {
    
    File file;
    TreeMap<Integer,TreeMap<String,Nucleus>> byTime=new TreeMap<>();  // all the nuclei indexed by time and name
    TreeSet<Integer> curatedSet = new TreeSet<>();  //curated times
    TreeMap<Integer,Integer> thresholdProbs = new TreeMap<>();  // the thresholds probs for each time used to construct nuclei from bhc trees
    TreeMap<Integer,TreeMap<String,Nucleus>> remnants = new TreeMap<>(); // nuclei made from supervoxels that are left after auto linking -  may be false negatives
    
    ArrayList<InvalidationListener> listeners = new ArrayList<>();
    SelectedNucleus selectedNucleus = new SelectedNucleus();
    BHCDirectory bhcTreeDir;
    
    boolean opening = false;
    
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
    public void conflictResolvingAutoLink(Integer[] times,Integer[] threshs,int minVolume)throws Exception {
        Nucleus[] toNucs;
        for (int i=1 ; i<times.length ; ++i){
            Nucleus[] fromNucs = this.getNuclei(times[i-1]).toArray(new Nucleus[0]);
            
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
            
            // do the auto linking for each time - going forward in time
            int t = times[i];
            BHCTree tree = bhcTreeDir.getTree(t,threshs[i]);           
            tree.clearUsed();
            if (isCurated(t)){
                
            } else {
                
            } 
        }
        this.notifyListeners();
    }
    private void uncuratedAutoLink(ArrayList<Nucleus> polar,ArrayList<Nucleus> nonPolar,int time,BHCTree tree,double distance){
        
        // find the best matching tree node for each given nucleus
        HashMap<Nucleus,NucleusLogNode> timeLinks = new HashMap<>();
        bestTimeLinks(polar,time,tree,distance,timeLinks);
        bestTimeLinks(nonPolar,time,tree,distance,timeLinks);
        
        // find potential divisions from nonpolar nuclei
        List<Division> divisions = new ArrayList<>();
        for (Nucleus nuc : nonPolar){
            NucleusLogNode timeLink = timeLinks.get(nuc);
            if (timeLink != null){
                TreeSet<NucleusLogNode> neighbors = new TreeSet<>();
                NucleusLogNode sisterNode = (NucleusLogNode)timeLink.getSister();
                if (sisterNode != null){
                    tree.neighborNodes(nuc, distance, sisterNode, neighbors);
                }
                NucleusLogNode auntNode = (NucleusLogNode)timeLink.getAunt();
                if (auntNode != null){
                    tree.neighborNodes(nuc, distance, sisterNode, neighbors);
                }
                NucleusLogNode divNode = bestInNeighborhood(nuc,time,neighbors,tree); // node that matchs best excluding the timeLink node
                if (divNode != null){
                    Division div = new Division(nuc,timeLink,divNode,time);
                    if (div.isPossible()){
                        divisions.add(div);
                    }
                }
            }
        }
        
        // resolve conflicts
    }
    // fix any time link conflicts - return true if any were fixed
    private boolean fixLinkConflicts(HashMap<Nucleus,NucleusLogNode> timeLinks,int time){
        Object[] links = timeLinks.entrySet().toArray();
        boolean ret = false;
        // find a conflict
        for (int i=0 ; i<links.length-1 ; ++i){
            Entry<Nucleus,NucleusLogNode> entry1 = (Entry<Nucleus,NucleusLogNode>)links[i];
            Nucleus nuc1 = entry1.getKey();
            NucleusLogNode node1 = entry1.getValue();
            int label1 = node1.getLabel();
            for (int j=i+1 ; j<links.length ; ++j){
                Entry<Nucleus,NucleusLogNode> entry2 = (Entry<Nucleus,NucleusLogNode>)links[j];
                Nucleus nuc2 = entry2.getKey();
                NucleusLogNode node2 = entry2.getValue();
                int label2 = node2.getLabel();
                
                if (label1 == label2){
                    // conflict - same exact node linked to different nuclei
                    NucleusLogNode left = (NucleusLogNode)node1.getLeft();
                    NucleusLogNode right = (NucleusLogNode)node1.getRight();
                    double dleft = nuc1.distance(left.getNucleus(time));
                    double dright = nuc1.distance(right.getNucleus(time));
                    if (dleft < dright){
                        timeLinks.put(nuc1, left);
                        timeLinks.put(nuc2, right);
                    } else {
                        timeLinks.put(nuc2, left);
                        timeLinks.put(nuc1, right);                        
                    }
                } else if (node1.isDescendent(node2)){
                    // demote node2
                    timeLinks.put(nuc2, (NucleusLogNode)node1.getSister());
                    
                } else if (node2.isDescendent(node1)){
                    timeLinks.put(nuc1, (NucleusLogNode)node2.getSister());
                }
            }

        }
        return ret;
    }
/*    
    private NucleusLogNode findConflict(List<Division> divisions,HashMap<Nucleus,NucleusLogNode> timeLinks){
        HashMap<NucleusLogNode,Object> map = new HashMap<>();
        for (Entry<Nucleus,NucleusLogNode> entry : timeLinks.entrySet()){

        }
    }
*/
    private void bestTimeLinks(List<Nucleus> nucs,int time,BHCTree tree,double distance,Map<Nucleus,NucleusLogNode> matches){
        for (Nucleus nuc : nucs){
            matches.put(nuc,bestInSubtree(nuc,time,tree,distance));
        }
    }
    // finds the best matching node in a BHC subtree to a given nucleus
    private NucleusLogNode bestInSubtree(Nucleus nuc,int time,BHCTree tree,double distance){
        // make the neighborhood
        TreeSet<NucleusLogNode> neighborhood = new TreeSet<>();
        tree.neighborNodes(nuc, distance, neighborhood);  // finds nodes close to the given nucleus
        
        NucleusLogNode ret = bestInNeighborhood(nuc,time,neighborhood,tree);
        return ret;
    }
    // finds the best matching node in a neighborhood of nodes to a given nucleus
    private NucleusLogNode bestInNeighborhood(Nucleus nuc,int time,TreeSet<NucleusLogNode> neighborhood,BHCTree tree){
        NucleusLogNode ret = null;
        double minScore = Double.MAX_VALUE;
        for (NucleusLogNode logNode : neighborhood){
            Nucleus nodeNuc = logNode.getNucleus(time);
            if (nodeNuc != null){
                double score = Nucleus.similarityScore(nuc,nodeNuc);
                if (score < minScore){
                    ret = logNode;  // the node with the lowest score
                }
            }
        }
        if (tree != null){
            ret = tree.expandUpInNeighborhood(nuc, ret,neighborhood);  // try to expand the node up the tree, staying in the neighborhood
        }
        return ret;
    } 
    public void bestMatchAutoLink(Integer[] times,Integer[] threshs,int minVolume)throws Exception {
       
//        Nucleus[] fromNucs = this.getNuclei(times[0]).toArray(new Nucleus[0]);
        Nucleus[] toNucs;
        for (int i=1 ; i<times.length ; ++i){
            Nucleus[] fromNucs = this.getNuclei(times[i-1]).toArray(new Nucleus[0]);
            int t = times[i];
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
                    NucleusLogNode best = tree.bestMatchInAvailableNodes(nuc,minVolume,null).getNode();
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
                    Match best = tree.bestMatchInAvailableNodes(nuc,minVolume,null);
                    if (best != null){
                        matches.put(nuc,best.getNode());
                        NucleusLogNode expand = tree.expandUp(nuc, best.getNode());
                        expands.put(nuc,expand);
                        expand.markedAsUsed();
                    }
                }
                
                // try to make some divisions
                for (Nucleus nuc : nonPolar){

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
                Set<NucleusLogNode> avails =tree.availableNodes(minVolume,null);
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
            
                avails =tree.availableNodes(minVolume,null);
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
    public void bestMatchAutoLinkLimited(Integer[] times,Integer[] threshs,int minVolume,double neighborhoodRadius)throws Exception {
       
//        Nucleus[] fromNucs = this.getNuclei(times[0]).toArray(new Nucleus[0]);
        Nucleus[] toNucs;
        for (int i=1 ; i<times.length ; ++i){
            Nucleus[] fromNucs = this.getNuclei(times[i-1]).toArray(new Nucleus[0]);
            int t = times[i];
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
                    NucleusLogNode best = tree.bestMatchInAvailableNodes(nuc,minVolume,new Sphere(nuc,neighborhoodRadius)).getNode();
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
                    Match best = tree.bestMatchInAvailableNodes(nuc,minVolume,new Sphere(nuc,neighborhoodRadius));
                    if (best != null){
                        matches.put(nuc,best.getNode());
                        NucleusLogNode expand = tree.expandUp(nuc, best.getNode());
                        expands.put(nuc,expand);
                        expand.markedAsUsed();
                    }
                }
                
                // try to make some divisions
                for (Nucleus nuc : nonPolar){

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
                
                
                for (Nucleus nuc : nonPolar){ 
                    if (!nuc.isDividing()){
                        Set<NucleusLogNode> avails =tree.availableNodes(minVolume,new Sphere(nuc,neighborhoodRadius));
                        for (NucleusLogNode avail : avails){
                            Nucleus availNuc = avail.getNucleus(t);
                            if (availNuc != null){
                                Nucleus[] next = nuc.nextNuclei();
                                if (next.length >0 && next[0] != null){
                                    Division div = new Division(nuc,next[0],availNuc);
                                    if (div.isPossible()){
                                        System.out.println("Division by available");
                                        completeTheDivision(t,nuc,avail,availNuc,toList);
                                        break;
                                    }
                                }
                            }
                        }
                                        
                    }
                }
                // put any remaining segmentations into remant nucs
            
                Set<NucleusLogNode> avails =tree.availableNodes(minVolume,null);
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
        
        Set<NucleusLogNode> avails = tree.availableNodes(minVolume,null);
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

    public class NodeCompare implements Comparator {

        @Override
        public int compare(Object o1, Object o2) {
            NucleusLogNode n1 = (NucleusLogNode)o1;
            NucleusLogNode n2 = (NucleusLogNode)o2;
            int l1 = n1.getLabel();
            int l2 = n2.getLabel();
            if (l1 == l2){
                return 0;
            }
            if (n1.isDescendent(n2)){
                return 0;
            }
            if (n2.isDescendent(n1)){
                return 0;
            }            
            
            return l1 -l2;
            
        }
        
    }
    // two different nuclei linked to two nodes that share some or all voxels
    public class Conflict {
        Nucleus nuc1;
        Nucleus nuc2;
        NucleusLogNode node1;
        NucleusLogNode node2;
    }
}
