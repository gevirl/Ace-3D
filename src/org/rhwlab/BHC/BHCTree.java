/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.nucleus.BHCNucleusData;
import org.rhwlab.dispim.nucleus.Division;
import org.rhwlab.dispim.nucleus.Nucleus;

/**
 *
 * @author gevirl
 */
public class BHCTree {
    public BHCTree(String file,int t)throws Exception {
        this.fileName = file;
        readTreeXML(file);
//        time = BHCNucleusDirectory.getTime(new File(file));
        time = t;
    }
    
    public BHCTree(double alpha,double[] s,int nu, double[] mu,List<Node> roots){
        this.roots = roots;
        this.alpha = alpha;
        this.s = s;
        this.nu = nu;
        this.mu = mu;
        this.labelNodes();
    }
    
    public void readTreeXML(String xml)throws Exception {
        roots = new ArrayList<>();
        
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(new File(xml));
        Element root = doc.getRootElement();  
        
        alpha = Double.valueOf(root.getAttributeValue("alpha"));
        
        String sStr = root.getAttributeValue("s");
        String[] tokens = sStr.substring(1).split(" ");
        s = new double[tokens.length-1];
        for (int i=0 ; i<s.length ; ++i){
            s[i] = Double.valueOf(tokens[i]);
        }
        
        nu = Integer.valueOf(root.getAttributeValue("nu"));
        
        String muStr = root.getAttributeValue("mu");
        tokens = muStr.substring(1).split(" ");
        mu = new double[tokens.length-1];
        for (int i=0 ; i<mu.length ; ++i){
            mu[i] = Double.valueOf(tokens[i]);
        }
        
        for (Element nodeEle : root.getChildren("Node")){
            LogNode std = new NucleusLogNode(nodeEle,null);  // build the node and all the children
            roots.add(std);
        }
    }    
    // save the tree as xml
    public void saveAsXML(String file)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCTrees"); 
        root.setAttribute("alpha", Double.toString(alpha));
        
        StringBuilder builder = new StringBuilder();
        builder.append("(");
        for (int i=0 ; i<s.length ; ++i){
            builder.append(s[i]);
            builder.append(" ");
        }
        builder.append(")");
        root.setAttribute("s",  builder.toString());
        
        root.setAttribute("nu", Integer.toString(nu));
        builder = new StringBuilder();
        builder.append("(");
        for (int d=0 ; d<mu.length ; ++d){
            builder.append(mu[d]);
            builder.append(" ");
        }
        builder.append(")");
        root.setAttribute("mu", builder.toString());
        
        for (Node node : roots){
            ((NodeBase)node).saveAsTreeXML(time,root);
            TreeSet<Double> posts = new TreeSet<>();
            ((NodeBase)node).allPosteriors(posts);
            int aoshdfuihs=0;
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();          
    }   
    public Nucleus[] divideBySplit(Nucleus nuc,NucleusLogNode best){
        if (best==null || best.isLeaf()) return null;
            // is it possible to divide the best matching node and make a new cell division?
            Nucleus leftNuc = ((NucleusLogNode)best.getLeft()).getNucleus(time);
            Nucleus rightNuc = ((NucleusLogNode)best.getRight()).getNucleus(time);
            if (leftNuc!= null && rightNuc != null){
                if (!Nucleus.intersect(leftNuc, rightNuc)){
                    Division div = new Division(nuc,leftNuc,rightNuc);
                    if (div.isPossible()){
                        Nucleus[] ret = new Nucleus[2];
                        ret[0] = leftNuc;
                        ret[1] = rightNuc;
                        return ret;
                    }
                }
            }
            return null;
    }
    

    
        public Nucleus divideBySister(Nucleus nuc,NucleusLogNode expanded){
            if (expanded==null) return null;
            // does the given nucleus also match the best's sister very well?
            NodeBase sisterNode = (NodeBase)expanded.getSister();
            if (sisterNode != null){
                Nucleus sisterNuc = ((NucleusLogNode)sisterNode).getNucleus(time);            
                if (sisterNuc!=null && sisterNuc.getCellName().contains("polar")){
                    // try the next level up

                    NodeBase parent = (NodeBase)expanded.getParent();
                    sisterNode = (NodeBase)parent.getSister();
                }  
            }
            if (sisterNode != null){
                Nucleus sisterNuc = ((NucleusLogNode)sisterNode).getNucleus(time);
                if (sisterNuc != null){
                    if (!sisterNode.isUsedRecursive()){
                        double sisterScore = Nucleus.similarityScore(nuc, sisterNuc); 
                        double expandedScore = Nucleus.similarityScore(nuc, expanded.getNucleus(time));
                        double ratio = sisterScore/expandedScore;
                        if (ratio <1.0) ratio = 1.0/ratio;

                System.out.printf("%s - %s,%s  ratio= %f\n",nuc.getName(),expanded.getNucleus(time).getName(),sisterNuc.getName(),ratio);
     //                   if (ratio <1.2){
                        Nucleus expandedNuc = expanded.getNucleus(time);
                        if (!Nucleus.intersect(expandedNuc,sisterNuc)){
                            if (Nucleus.match(sisterNuc, expanded.getNucleus(time))){       
                                Division div = new Division(nuc,expanded.getNucleus(time),sisterNuc);
                                if (div.isPossible()){
                                    Nucleus[] ret = new Nucleus[2];
                                    ret[0] = expanded.getNucleus(time);
                                    ret[1] = sisterNuc;
                                    sisterNode.markedAsUsed();
                                    return sisterNuc;    
                                }
                            }
                        }
                    }
                }
            }
            return null;
    }
    public Nucleus[] bestMatch(Nucleus nuc,boolean dividable){
        Match best = this.bestMatchInAvailableNodes(nuc);
/*        
        double minD = Double.MAX_VALUE;
        for (Node root : roots){
            NucleusLogNode node = (NucleusLogNode)root;
            double score = Nucleus.similarityScore(nuc, node.getNucleus(time));
            Match match = bestMatch(nuc,node,score);
            if (match.score < minD){
                minD = match.score;
                best = match;
            }
        }
*/        
        NucleusLogNode expanded = expandUp(nuc,best.node);
        expanded.markedAsUsed(); 
        best.node = expanded;
        
        if (dividable){
            
            // is it possible to divide the best matching node and make a new cell division?
            Nucleus leftNuc = ((NucleusLogNode)best.node.getLeft()).getNucleus(time);
            Nucleus rightNuc = ((NucleusLogNode)best.node.getRight()).getNucleus(time);
            if (leftNuc!= null && rightNuc != null){
                if (!Nucleus.intersect(leftNuc, rightNuc)){
                    Division div = new Division(nuc,leftNuc,rightNuc);
                    if (div.isPossible()){
                        Nucleus[] ret = new Nucleus[2];
                        ret[0] = leftNuc;
                        ret[1] = rightNuc;
                        return ret;
                    }
                }
            }

            // does the given nucleus also match the best's sister very well?
            NodeBase sisterNode = (NodeBase)best.node.getSister();
            if (sisterNode != null){
                Nucleus sisterNuc = ((NucleusLogNode)sisterNode).getNucleus(time);            
                if (sisterNuc.getCellName().contains("polar")){
                    // try the next level up

                    NodeBase parent = (NodeBase)best.node.getParent();
                    sisterNode = (NodeBase)parent.getSister();
                }  
            }
            if (sisterNode != null){
                Nucleus sisterNuc = ((NucleusLogNode)sisterNode).getNucleus(time);
                if (sisterNuc != null){

                    double sisterScore = Nucleus.similarityScore(nuc, sisterNuc); 
                    double expandedScore = Nucleus.similarityScore(nuc, expanded.getNucleus(time));
                    double ratio = sisterScore/expandedScore;
                    if (ratio <1.0) ratio = 1.0/ratio;
                        
            System.out.printf("%s - %s,%s  ratio= %f\n",nuc.getName(),best.node.getNucleus(time).getName(),sisterNuc.getName(),ratio);
 //                   if (ratio <1.2){
                    if (Nucleus.match(sisterNuc, expanded.getNucleus(time))){       
                        Division div = new Division(nuc,best.node.getNucleus(time),sisterNuc);
                        if (div.isPossible()){
                            Nucleus[] ret = new Nucleus[2];
                            ret[0] = best.node.getNucleus(time);
                            ret[1] = sisterNuc;
                            sisterNode.markedAsUsed();
                            return ret;    
                        }
                    }
                }
            }
        }
        
        Nucleus[] ret = new Nucleus[1];
        ret[0] = expanded.getNucleus(time);
//System.out.printf("Best: %s - %s   %f\n",nuc.getName(),ret[0].getName(),minD);
        return ret;
    }
    public Match bestMatchInAvailableNodes(Nucleus nuc){
        Match veryBest = null;
        Set<NucleusLogNode> availableNodes = this.availableNodes();
        if (this.isAvailable(availableNodes, 7036)){
            int hasfuih=0;
        }
        for (NucleusLogNode availableNode : availableNodes){
            Nucleus availNuc = availableNode.getNucleus(time);
            if (availNuc != null) {
                double score = Nucleus.similarityScore(nuc,availNuc);
                Match match = bestMatch(nuc,availableNode,score);
                if (veryBest == null){
                    veryBest = match;
                }else {
                    if (match.score < veryBest.score){
                        veryBest = match;
                    }
                }
            }
        }
        return veryBest;
    }
    // find the best nucleus to match in the subtree root at the given node
    public Match bestMatch(Nucleus nuc,NucleusLogNode node,double nodeScore){
        Match ret = new Match(node,nodeScore);
        boolean debug = false;
 //       if (nuc.getCellName().equals("polar2")) debug = true;
 /*
if (debug) System.out.printf("Matching nuc= %s(%.2f,%.2f,%.2f) V%.2f I%.2f to node =%d(%.2f) (%.2f,%.2f,%.2f) V%.2f I%.2f dist=%f\n", 
        nuc.getName(),c[0],c[1],c[2],nuc.getVolume(),nuc.getAvgIntensity(),
        node.label,nodeScore,p[0],p[1],p[2],node.volume,node.avgIntensity,dd);
 */
        if (node.isLeaf()){
if (debug) System.out.printf("Leaf returning from %d(%f) as best \n",node.label ,nodeScore);             
            return ret;
        }
        
        if (nuc.getVolume() > 2.0*node.getVolume()){
if (debug) System.out.printf("Volume returning from %d(%f) as best \n",node.label ,nodeScore);             
            return ret;  // nodes beyond here will be too small, so stop
        }
        
        Nucleus leftNuc = null;
        double leftScore = Double.MAX_VALUE;
        Match leftMatch = null;
        if (!node.getLeft().isUsed()){
            leftNuc = ((NucleusLogNode)node.getLeft()).getNucleus(time);
            if (leftNuc != null){
                leftScore = Nucleus.similarityScore(nuc,leftNuc);
            }
            leftMatch = bestMatch(nuc,(NucleusLogNode)node.getLeft(),leftScore);  
            if (leftMatch.score < ret.score){
                ret = leftMatch;
            }
        }
 
        Nucleus rightNuc=null;
        double rightScore = Double.MAX_VALUE;
        Match rightMatch = null;
        if (!node.getRight().isUsed()){
            rightNuc = ((NucleusLogNode)node.getRight()).getNucleus(time);
            if (rightNuc != null){
                rightScore = Nucleus.similarityScore(nuc,rightNuc);
            }
            rightMatch = bestMatch(nuc,(NucleusLogNode)node.getRight(),rightScore);
            if (rightMatch.score < ret.score){
                ret = rightMatch;
            }
        }
        
        return ret;

/*
        if (node.isUsedRecursive()){
            if (leftMatch.score < rightMatch.score){
if (debug) System.out.printf("Recursive returning from %d best score left %d(%f) \n",node.label,leftMatch.node.label,leftMatch.score);                
                return leftMatch;
            } else { 
if (debug) System.out.printf("Recursive returning from %d best score right %d(%f) \n",node.label ,rightMatch.node.label,rightMatch.score);                  
                return rightMatch;
            }
        }
        if (leftMatch.score < rightMatch.score && leftMatch.score < nodeScore){
if (debug) System.out.printf("returning from %d best score left %d(%f) \n",node.label,leftMatch.node.label,leftMatch.score);
            return leftMatch;
        } else if (rightMatch.score < leftMatch.score && rightMatch.score< nodeScore){
if (debug) System.out.printf("returning from %d best score right %d(%f) \n",node.label ,rightMatch.node.label,rightMatch.score);            
            return rightMatch;
        }
if (debug) System.out.printf("returning from %d(%f) as best \n",node.label ,nodeScore);        
        return new Match(node,nodeScore);
*/        
    }

    // expand the given match to the largest possible match
    public NucleusLogNode expandUp(Nucleus source,NucleusLogNode match){
        NucleusLogNode par = (NucleusLogNode)match.getParent();
        if (par == null){
            return match;
        }
        NodeBase sister = (NodeBase)match.getSister();
        if (sister.isUsedRecursive()){   // cannot go up if any part of the sister has already been used
            return match;
        }
        
        if (Nucleus.matchForExpansion(source, par.getNucleus(time))){
            return expandUp(source,par);
        }
        return match;
    }
    public boolean isAvailable(Set<NucleusLogNode> avails,int lab){
        for (NucleusLogNode avail : avails){
            Node found = avail.findNodeWithlabel(lab);
            if (found != null){
                return true;
            }
        }
        return false;
    }
    public Set<NucleusLogNode> availableNodes(){
        HashSet<NucleusLogNode> ret = new HashSet<>();
        for (Node root : roots){
            availableNodes((NucleusLogNode)root,ret);
        }
        return ret;
    }
    
    static public void availableNodes(NucleusLogNode root,HashSet<NucleusLogNode> availNodes){
        if (!root.isUsedRecursive()){
            availNodes.add(root);
            return;
        }
        if (root.isLeaf()){
            return;  // nothing to add - the root is a leaf that has already been used
        }
        if (root.isUsed()){
            return;
        }
        availableNodes((NucleusLogNode)root.getLeft(),availNodes);
        availableNodes((NucleusLogNode)root.getRight(),availNodes);
    }
    
    public static void saveXML(String file,Element root)throws Exception {
        File f = new File(file);
        File outFile = new File(f.getParent(),f.getName());
        OutputStream stream = new FileOutputStream(outFile);
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();         
    }
   
 /*   
    public void saveCutAtThresholdAsXML(String file,double thresh)throws Exception {
        saveXML(file,cutTreeAtThreshold(thresh));
    }  
    public BHCNucleusSet cutToNucleusFile(double threshold){
        return new BHCNucleusSet(cutTreeAtThreshold(threshold));
    }
    
    // cut this tree at the given threshold into an XML element
    // the children of the returned Element are the GaussianMixtureModel descriptions of a nucleus
    public Element cutTreeAtThreshold(double threshold){
        Element root = new Element("BHCNucleusList"); 
        if (fileName != null) root.setAttribute("treefile",fileName);
        root.setAttribute("threshold", Double.toString(threshold));
        root.setAttribute("time", Integer.toString(time));
        int id = 1;
        for (Node cl : roots){
            int used = cl.saveAsXMLByThreshold(root,threshold,id);  // save as a Gaussian Mixture Model
            if (used != -1){
                id = used + 1;
            }
        }
        return root;
    } 
*/    
    /*
    public int nodeCountAtThreshold(double thresh){
        Element el = this.cutTreeAtThreshold(thresh);
        return el.getChildren("GaussianMixtureModel").size();
    }
   */
    
    // find a minimum number of nuclei that exceed the minimum volume and are not overlapping
    // the max probability is supplied to stop search if minimum number can't be reached with the given minimum volume
    // max prob is just a safety net, 
    // should open nodes until a maximal set of non-overlapping nodes are found that meet the volume criteria
    public Set<Nucleus> cutToMinimum(int minNucs,double minVolume,double maxProb){
        double logProb = Math.log(maxProb);
        TreeSet<NucleusLogNode> cut = firstTreeCut();
        while (true){
            NucleusLogNode[] next = nextTreeCut(cut);
            if (cut.size() >= minNucs){
                // are the next nuclei overlapping
                Nucleus nuc0 = next[0].getNucleus(time);
                Nucleus nuc1 = next[1].getNucleus(time);
                
                if (nuc0==null || nuc1==null || Nucleus.intersect(nuc0, nuc1)){
                    break;
                }
            }

            if (next[0].getVolume() > minVolume){
                cut.add(next[0]);
            }
            if (next[1].getVolume() > minVolume){
                cut.add(next[1]);
            }
            if (next[0].getVolume() > minVolume || next[1].getVolume() > minVolume){
                cut.remove(next[2]);
            }
            System.out.printf("logProb[0]=%f\n",next[0].getLogPosterior());
            System.out.printf("logProb[1]=%f\n",next[1].getLogPosterior());
            System.out.printf("logProb[2]=%f\n",next[2].getLogPosterior());
            if (next[0].getLogPosterior()==0.0 && next[1].getLogPosterior()==0.0){
                break;
            }
        }
        Set<Nucleus> ret = new TreeSet<>();
        for (NucleusLogNode node : cut){
            Nucleus nuc = node.getNucleus(time);
            nuc.setTime(time);
            ret.add(nuc);  
        }
        return ret;
    }

    // cut the tree to at least N given a minimum volume and a maximum probability
    // may have to return less than N nuclei to meet volume and prob criteria
    public Nucleus[] cutToN(int n,double minVolume,double maxProb){
        int cutN = n;
        TreeSet<NucleusLogNode>  volReducedCut;
        ArrayList<Nucleus> retList = new ArrayList<>();
        while(true){
            TreeSet<NucleusLogNode> cut = cutToExactlyN_Nodes(cutN);  // cuts to exactly cutN
            volReducedCut = new TreeSet<>();
            int i=1;
            retList.clear();
            for (NucleusLogNode logNode : cut){
                BHCNucleusData nucData = BHCNucleusData.factory(logNode,time);
                if (nucData!=null && nucData.getVolume()>=minVolume){
                    volReducedCut.add(logNode);
                    retList.add(new Nucleus(nucData));
                    ++i;
                }
            }
            double prob = Math.exp(cut.first().getLogPosterior());
            if (prob <=maxProb){
                ++cutN;
            }else {
                break;
            }
        }
        return retList.toArray(new Nucleus[0]);
    }
    public Nucleus[] cutToExactlyN_Nuclei(int n){
        Set<NucleusLogNode> logNodeSet  = this.cutToExactlyN_Nodes(n);
        Nucleus[] toNucs = new Nucleus[logNodeSet.size()];
        int i=0;
        for (NucleusLogNode logNode : logNodeSet){
            BHCNucleusData nucData = BHCNucleusData.factory(logNode,time);
            toNucs[i] = new Nucleus(nucData);
            ++i;
        }
        return toNucs        ;
    }
    
    // cut tree to exactly n nodes
    public TreeSet<NucleusLogNode> cutToExactlyN_Nodes(int n){
        TreeSet<NucleusLogNode> cut = firstTreeCut();
        while (cut.size()<n) {
            NucleusLogNode[] next = nextTreeCut(cut);
            if (next == null) return cut;
            cut.remove(next[2]);
            cut.add(next[0]);
            cut.add(next[1]);
        } 
        return cut;
        
    }
    
    public TreeSet<NucleusLogNode> firstTreeCut(){
        TreeSet<NucleusLogNode> cut = new TreeSet<>();
        for (Node root : roots){
            cut.add((NucleusLogNode)root);
        }
        return cut;
    }
    public TreeMap<Integer,TreeSet<NucleusLogNode>> allTreeCuts(int maxNodes){
        TreeMap<Integer,TreeSet<NucleusLogNode>> ret = new TreeMap<>();
        
        TreeSet<NucleusLogNode> cut = firstTreeCut();
        ret.put(cut.size(),cut);       
        while (cut.size()<maxNodes) {
            NucleusLogNode[] next  = nextTreeCut(cut);
            if (next == null){
                return ret;
            }
            TreeSet<NucleusLogNode> nextSet = new TreeSet<>();
            nextSet.addAll(cut);
            nextSet.remove(next[2]);
            nextSet.add(next[0]);
            nextSet.add(next[1]);
            ret.put(nextSet.size(), nextSet);
            cut = nextSet;
        }
        return ret;
    }
    // cuts the tree at the next level - produce one more node than previous cut
    public NucleusLogNode[] nextTreeCut(TreeSet<NucleusLogNode> previous){
        NucleusLogNode[] ret = new NucleusLogNode[3];
        // find the minimum probability node that can be split
        Iterator<NucleusLogNode> iter = previous.iterator();
        while(iter.hasNext()){  
            NucleusLogNode node = iter.next();  // search for the lowest probability  node with children - not a leaf
            if (node.getLeft() != null && node.getRight() != null){

                ret[0] = (NucleusLogNode)node.getLeft();  // split the node wtih the lowest probability
                ret[1] = (NucleusLogNode)node.getRight();
                ret[2] = node;
                return ret;
            }             
        }     
        return null;
    }
       public TreeMap<Integer,Double> allPosteriorProb( int maxProbs){
        TreeMap<Integer,TreeSet<NucleusLogNode>> allCuts = allTreeCuts(maxProbs);
        TreeMap<Integer,Double> ret = new TreeMap<>();
        for (Integer i : allCuts.keySet()){
            TreeSet<NucleusLogNode> nodes = allCuts.get(i);
            double p = Math.exp(nodes.first().getLogPosterior());
            ret.put(i,p);
        }

        return ret;
    } 
   
   /*
    public TreeSet<Double> allPosteriors(){
        if (this.allPosts == null){
            this.allPosts = new TreeSet<>();
            for (Node root : roots){
                ((NodeBase)root).allPosteriors(this.allPosts);
            }
        }        
        return allPosts;
    }

    public void allPosteriorProb(TreeSet<Node> leaves,TreeMap<Double,Integer> probs,double minVolume,int maxProbs){
        
        if (leaves.isEmpty() || probs.size()==maxProbs){
            return ;
        }
        // find the leaf with the lowest probability
        Node minNode = leaves.first();
        probs.put(minNode.getLogPosterior(),leaves.size());

        
        // update the leaf set
        leaves.remove(minNode);
        if (((NodeBase)minNode).getLabel()==2){
            int sdkjfnsdiu=0;
        }
        // add the children of minNode to the input list
        if (minNode.getLeft()!=null && minNode.getRight()!=null){
            if (((NucleusLogNode)minNode.getLeft()).getVolume() >= minVolume){
                leaves.add(minNode.getLeft());
            }
            if (((NucleusLogNode)minNode.getRight()).getVolume() >= minVolume){
                leaves.add(minNode.getRight());
            }
    }
        allPosteriorProb(leaves,probs,minVolume,maxProbs);
    }
    // form a set of nuclei that meet the probability threshold and minimum volume
    public BHCNucleusSet cutAtProbability(double prob,double minVolume){
        TreeSet<Node> nodes = new TreeSet<>();
        for (Node root : this.roots){
            cutAtProbability((LogNode)root,prob,nodes);
        }
        
        TreeSet<BHCNucleusData> nucSet = new TreeSet<>();
        int i = 1;
        for (Node node : nodes){
            if (((NucleusLogNode)node).getVolume() >=minVolume){
                Element ele = ((NodeBase)node).formElementXML(i);
                BHCNucleusData bhcNuc = new BHCNucleusData(time,ele);
                nucSet.add(bhcNuc);
                ++i;
            }
        }
        return new BHCNucleusSet(time,fileName,nucSet);
    }
    // cut the tree rooted at the given node to the given probability
    public void cutAtProbability(LogNode node,double prob,TreeSet<Node> leaves){
        if (Math.exp(node.getLogPosterior()) >= prob) {
            leaves.add(node);
            return;
        }
        cutAtProbability((LogNode)node.getRight(),prob,leaves);
        cutAtProbability((LogNode)node.getLeft(),prob,leaves);
    }
    


    public void cutToN(int n,TreeSet<Node> leaves){
        if (leaves.size() == n){
            return;  // done - found n nodes
        }
        // find the minimum node that has children    
        Iterator<Node> iter = leaves.iterator();
        Node minNode = iter.next();
        while (minNode.isLeaf()){
            if (!iter.hasNext()){
                return;  // all the leaves are nodes , can't add any more nodes
            }
            minNode = iter.next();
        }
        
        // increases the leaves by one
        leaves.remove(minNode);
        leaves.add(minNode.getLeft());
        leaves.add(minNode.getRight());
        cutToN(n,leaves);
    }
    
   */ 
    
    public int getTime(){
        return time;
    }
    public String getBaseName(){
        File f = new File(fileName);
        String name = f.getName().substring(0,f.getName().indexOf("BHCTree"));
        return new File(f.getParentFile(),name).getPath();
    }
    public void labelNodes(){
        int start = 1;
        for (Node root : roots){
            int used = ((NodeBase)root).labelNode(start);
            start = used + 1;
        }
    }
    /*
    // cuts the BHC Tree to obtain the given number of nuclei/cells
    public Element cutTreeToCount(int nCells){
        int nDel = 20;
        TreeSet<Double> posteriors = this.allPosteriors();
        Double[] r = posteriors.toArray(new Double[0]);
        for (int start=0 ; start < posteriors.size()-nDel ; start = start + nDel ){
            Element e = this.formXML(r[start]);
            int n = e.getChildren("GaussianMixtureModel").size();
            if (n == nCells){
                return e;
            }
            if (n > nCells){
                // work backwards until found
                for (int i=1 ; i<=nDel ; ++i){
                    e = this.formXML(r[start-i]);
                    n = e.getChildren("GaussianMixtureModel").size();
                    if (n <= nCells){
                        return e;
                    }
                    
                }
            }            

        }
        return null;
    }
*/
    // find the node with a given label
    public Node findNode(int label){
        for (Node root : roots){
            NodeBase nodeBase = (NodeBase)root;
            Node ret = nodeBase.findNodeWithlabel(label);
            if (ret != null){
                return ret;
            }
        }
        return null;
    }
    // determine if there is a parent child relationship between two nodes given their labels
    public boolean areRelated(int parent,int child){
        NodeBase parentNode = (NodeBase)this.findNode(parent);
        Node childNode = parentNode.findNodeWithlabel(child);
        return childNode != null;
    }
/*    
    public Element[] cutTreeWithLinearFunction(){
        Double[] posteriors = this.allPosteriors().toArray(new Double[0]);
        int x0 = 0;
        int x1 = posteriors.length-1;
        boolean better = true;
        double eMin = lmsError(x0,x1,posteriors);
        while (x0 < x1 && better){
            better = false;
            // try to move x0 up
            while (x0 < x1){
                double e = lmsError(x0+1,x1,posteriors);
                if (e < eMin){
                    eMin = e;
                    ++x0;
                    better = true;
                } else{
                    break;
                }
            }   
            // try to move x1 down
            while (x0 < x1){
                double e = lmsError(x0,x1-1,posteriors);
                if (e < eMin){
                    eMin = e;
                    --x1;
                    better = true;
                } else{
                    break;
                }
            }             

        }
        Element[] ret = new Element[x1-x0+1];
        for (int i=0 ; i<ret.length ; ++i){
            ret[i] = this.cutTreeAtThreshold(posteriors[x0+i]);
        }
        return ret;
    }
*/
    double lmsError(int x0,int x1,Double[] y){
        double e = 0.0;
        double xDel = (double)(x1-x0);
        for (int i=0 ; i<y.length ; ++i){
            double del;
            if (i < x0 ){
                del = y[i];
            } else if (i > x1){
                del = y[i] - 1.0;
            }  else if (xDel != 0.0 ){
                del = y[i] - ((double)(i-x0))/(xDel);
            } else {
                del = Math.min(y[i],1.0-y[i]);
            }
            e = e + del * del;
        }
        return e;        
    }
    /*
    public Element cutTreeWithLogisticFunction(){
        Double[] posteriors = this.allPosteriors().toArray(new Double[0]);
        WeightedObservedPoints points = new WeightedObservedPoints();
        for (int i=0 ; i<posteriors.length ; i = i+10){
            if (i >=410 && i<=520){
            points.add(i, posteriors[i]);
 //           System.out.printf("%d  %f\n",i,posteriors[i]);
            }
        }
        double[] parameters = new double[6];
        parameters[0] = 1.0;  // k - upper bound
        parameters[1]= 475.0;  //m -  x value at maximum growth 
        parameters[2] = 1.0;  //b - growth rate
        parameters[3] = 1.0;  // q - related to Y(0)
        parameters[4] = 0.0;  //a - lower bound
        parameters[5] = 1.0;  // nu - affect near which asymptote max growth occurs
        
        Logistic.Parametric func = new Logistic.Parametric();
        for (int i=0 ; i<posteriors.length ; ++i){
            double x = (double)i;
            double value = func.value(x, parameters);
            double[] grad = func.gradient(x, parameters);
//            System.out.printf("%d  %f\n", i,value);
            for (int j=0 ; j<grad.length ; ++j){
 //               System.out.printf("\t%f\n",grad[j]);
            }
        }
        
        SimpleCurveFitter fitter = SimpleCurveFitter.create(new Logistic.Parametric(), parameters);
//        fitter.withMaxIterations(1);
        double[] params = fitter.fit(points.toList());
        return null;
//        double[] results = fitter.fit(points);
    }
*/
    public double getAlpha(){
        return alpha;
    }
    public double[] getS(){
        return s;
    }
    public int getNu(){
        return nu;
    }
    public Node trimSubTree(Node subtree,double minProb){
        return null;
    }
    static public void main(String[] args) throws Exception {
        String f = "/net/waterston/vol2/home/gevirl/rnt-1/xml/img_TL017_Simple_SegmentationBHCTree.xml";
//        BHCTree tree = new BHCTree(f);
 //       tree.cutTreeWithLinearFunction();

    } 
    public String getFileName(){
        return fileName;
    }
    public void clearUsed() {
        for (Node root : roots){
            NodeBase base = (NodeBase)root;
            base.clearUsedMarks();
        }
    }

    String fileName;
    int time;
    List<Node> roots;
    double alpha;
    double[] s;
    int nu;
    double[] mu;
    TreeSet<Double> allPosts = null;

            
    public class TreeCut{
        public double posterior;
        public double volume;
        
    }
    
    public class Match{
        public Match(NucleusLogNode node,double score){
            this.node = node;
            this.score = score;
        }
        public NucleusLogNode getNode (){
            return this.node;
        }
        NucleusLogNode node;
        double score;
        
    }
}
