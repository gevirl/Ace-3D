/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.StatUtils;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.rhwlab.dispim.datasource.MicroCluster;
import org.rhwlab.dispim.nucleus.BHCNucleusData;

/**
 *
 * @author gevirl
 */
abstract public class NodeBase implements Node {
    public NodeBase(){
        
    }
    public NodeBase(MicroCluster micro) {
        this.micro = micro;
        this.left = null;
        this.right = null;
        this.parent = null;
/*        
        d = alpha;
        pi = 1.0;
        onePi = 0.0;
        posterior();
        */
    } 
    public NodeBase(NodeBase l,NodeBase r) throws ArithmeticException {
        this.micro = null;
        this.left = l;
        this.right = r; 
        ((NodeBase)this.left).parent = this;
        ((NodeBase)this.right).parent = this;
 //       posterior();
    }    
    @Override
    public void getDataAsRealVector(List<RealVector> list) {
        if (micro != null){
            list.add(micro.asRealVector());
            return;
        }
        left.getDataAsRealVector(list);
        right.getDataAsRealVector(list);
    }    
    @Override
    public void getDataAsMicroCluster(List<MicroCluster> list) {
        if (micro != null){
            list.add(micro);
            return;
        }
        left.getDataAsMicroCluster(list);
        right.getDataAsMicroCluster(list);
    } 

    // save a list of root nodes into an xml file
    static public void saveAsTreeListXML(int time,String file, List<Node> nodes)throws Exception {
        OutputStream stream = new FileOutputStream(file);
        Element root = new Element("BHCTrees"); 
        for (Node node : nodes){
            ((NodeBase)node).saveAsTreeXML(time,root);
        }
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        out.output(root, stream);
        stream.close();         
    }
    
    // save this node and all its children into an xml element
    public int saveAsTreeXML(int time,Element root){
        int nodeCount = 1;
        Element nodeEle = new Element("Node");
        nodeEle.setAttribute("label", Integer.toString(label));
        nodeEle.setAttribute("posterior",Double.toString(lnR));
        Element nucEle = this.formElementXML();
        if (nucEle != null){
            BHCNucleusData bhcNuc = new BHCNucleusData(time,nucEle);  
            nodeEle.setAttribute("volume", Double.toString(bhcNuc.getVolume()));
            double[] ecc = bhcNuc.eccentricity();
            StringBuilder builder = new StringBuilder();
            for (double e : ecc){
                builder.append(e);
                builder.append(" ");
            }
            nodeEle.setAttribute("eccentricity", builder.toString());
            nodeEle.setAttribute("avgIntensity", Double.toString(bhcNuc.getAverageIntensity()));
            nodeEle.setAttribute("intensityRSD", Double.toString(bhcNuc.getIntensityRSD()));
            
        }
        if (left != null){
            nodeCount = ((NodeBase)left).saveAsTreeXML(time,nodeEle);
            nodeCount = nodeCount + ((NodeBase)right).saveAsTreeXML(time,nodeEle);
            nodeEle.setAttribute("count",Integer.toString(nodeCount));
        } else {
            int count = addContent(nodeEle);
            nodeEle.setAttribute("points",Integer.toString(count));
        }
        root.addContent(nodeEle);
        return nodeCount;
    }   
    public int addContent(Element ele){
        return micro.addContent(ele);
    } 
/*    
    // saves the node as GMM and returns the last used id
    // cuts the tree at the given threshold
    // saves all nodes below this node as GMM based on given threshold
    public  int saveAsXMLByThreshold(Element root,double threshold,int id){
        if (this.getLogPosterior()>=threshold){
            int used = saveAsXML(root,id);
            return used;
        }
        int idUsed = left.saveAsXMLByThreshold(root, threshold,id);
        if (idUsed == -1){
            idUsed = right.saveAsXMLByThreshold(root, threshold,id);
        }else {
            idUsed = right.saveAsXMLByThreshold(root, threshold,idUsed+1);    
        }
        if (idUsed == -1){
            return id;
        }
        return idUsed;
    }    

    
    // add as a child, just this node as a GaussinaMixtureModel xml element into a root Element
    // return -1 if not saved
    public int saveAsXML(Element root,int id) {
        Element clusterEle = formElementXML(id);
        if (clusterEle != null){
            root.addContent(clusterEle);
            return id;            
        }else {
            return -1;
        }

    }
    */
    // calculate the relative standard deviation of the intensities
    public double getIntensityRSD(){
        List<MicroCluster> micros = new ArrayList<>();
        this.getDataAsMicroCluster(micros);
        List<Double> intensities = new ArrayList<>();
        for (MicroCluster micro : micros){
            int[] rawIs = micro.getIntensities();
            for (int inten : rawIs){
                intensities.add((double)inten);
            }            
        }
        double[] intensityValues = new double[intensities.size()];
        int i=0;
        for (Double v : intensities){
            intensityValues[i] = v;
            ++i;
        }  
        double iMean = StatUtils.mean(intensityValues);
        return Math.sqrt(StatUtils.variance(intensityValues, iMean)) / iMean;        
    }
    // form an XML Element from this node
    public Element formElementXML(){
        List<MicroCluster> micros = new ArrayList<>();
        this.getDataAsMicroCluster(micros);
        int voxels = 0;
        long intensity = 0;
        
        for (MicroCluster micro : micros){
            voxels = voxels + micro.getPointCount();
            intensity = intensity + micro.getTotalIntensity();
        }
        RealVector mu = MicroCluster.mean(micros);
        RealMatrix W = MicroCluster.precision(micros, mu);
        if (W != null){
            Element clusterEle = new Element("GaussianMixtureModel");
//clusterEle.setAttribute("id", String.format("%d", id));
 //           clusterEle.setAttribute("parent", "-1");
            clusterEle.setAttribute("count", String.format("%d",micros.size()));
            clusterEle.setAttribute("voxels", String.format("%d",voxels));
            clusterEle.setAttribute("intensity", String.format("%d",intensity));

            clusterEle.setAttribute("intensityRSD", Double.toString(getIntensityRSD()));
            clusterEle.setAttribute("sourceNode", String.format("%d", label));

            clusterEle.setAttribute("posterior",Double.toString(lnR));
            
            clusterEle.setAttribute("x", Double.toString(mu.getEntry(0)));
            clusterEle.setAttribute("y", Double.toString(mu.getEntry(1)));
            clusterEle.setAttribute("z", Double.toString(mu.getEntry(2)));
            
            StringBuilder builder = new StringBuilder();
            for (int row=0 ; row<W.getRowDimension() ; ++row){
                for (int col=0 ; col<W.getColumnDimension() ; ++col){
                    if (row>0 || col>0){
                        builder.append(" ");
                    }
                    builder.append(W.getEntry(row, col));
                }
            }
            clusterEle.setAttribute("precision", builder.toString());
            return clusterEle;
        }else {
            return null;
        }
    }
    @Override
    public Node getLeft() {
        return this.left;
    }

    @Override
    public Node getRight() {
        return this.right;
    }
    /*
    static public void setDfpField(DfpField fld){
        field = fld;
    }
*/
    public int getN(){

        if (N == null){
            ArrayList list = new ArrayList<>();
            this.getDataAsMicroCluster(list);
            this.N = list.size();
        }
        return N;
    }
    @Override
    public int compareTo(Object o) {
        NodeBase other = (NodeBase)o;
        int ret = Double.compare(this.lnR,other.lnR);
//        int ret = Double.compare(this.lnLike,other.lnLike);        
        if (ret == 0){
            ret = Integer.compare(this.hashCode(), other.hashCode());
        }
        return ret;
    }

    public  void allPosteriors(TreeSet<Double> posts){
        posts.add(lnR);
        
        if (left != null){
            ((NodeBase)left).allPosteriors(posts);
        }
        if (right != null){
            ((NodeBase)right).allPosteriors(posts);
        }        
    }
    // label this node and all its children, given a starting label
    // return the highest label used
    // this results in the labels as keys in a binary tree so a given node can be found quickly by its label
    public int labelNode(int startingWith){
        if (left == null && right == null){
            label = startingWith;
            return startingWith;
        }
        int used = ((NodeBase)left).labelNode(startingWith);
        this.label = used + 1;
        int ret = ((NodeBase)right).labelNode(this.label + 1);
        return ret;
    }
    // find a node with a given label in the subtree of this node
    public Node findNodeWithlabel(int labelToFind){
        if (this.label == labelToFind){
            return this;
        }
        if (this.left == null){
            return null;
        }
        if (this.label < labelToFind){
            return ((NodeBase)right).findNodeWithlabel(labelToFind);
        } else {
            return ((NodeBase)left).findNodeWithlabel(labelToFind);
        }
    }
    public Node getParent(){
        return this.parent;
    }
    public Node getSister(){
        if (this.parent != null){
            if (this.parent.getRight().equals(this)){
                return this.parent.getLeft();
            }else {
                return this.parent.getRight();
            }
        }
        return null;
    }
    public int getLabel(){
        return label;
    }
    public boolean isLeaf(){
        return left==null && right == null;
    }
    static public void setAlpha(double a){
        alpha = a;
        lnAlpha = Utils.eln(alpha);
    }    
    static void setParameters(int n,double b,double[] mu,double[] s){
        nu = n;
        
        S = new Array2DRowRealMatrix(s.length,s.length);
        for (int i=0 ; i<s.length ; ++i){
            S.setEntry(i, i, s[i]);
        }
        LUDecomposition ed = new LUDecomposition(S);
        detS = Math.pow(ed.getDeterminant(),nu/2.0);
        logdetSnu = Utils.eln(detS);        

        beta = b;
        lnBeta = Utils.eln(beta);
        m = new ArrayRealVector(mu);
        rmm = m.outerProduct(m).scalarMultiply(beta);
        ratio = new GammaRatio(mu.length,n);
    }    
    
    // determine if a given node is a descendent
    public boolean isDescendent(Node other){
        if (this.isLeaf()){
            return false;  // a leaf has no descentdents
        }
        if (left.equals(other)){
            return true;
        }
        if (right.equals(other)){
            return true;
        }        
        return left.isDescendent( other) || right.isDescendent(other);
    }
    // find a parent of this node who is also parent of the given node
    // find the common ancestory
    public Node commonAncestor(Node other){
        if (isDescendent(other)) {
            return this;  // the other is a descendent of this node, so this is the common ancestor
        }
        
        if (this.parent == null){
            return null;  // there can be no common ancestor if this is a root and other is not a descendent
        }
        
        return this.parent.commonAncestor(other);  // the common ancestor will be this parents common ancestor
    }
    private void setUsed(boolean u){
        NodeBase desc = findAncestor(7036);
        if (u && desc != null){
            int iousadfuihs=0;
        }
        this.used = u;
    }
    private NodeBase findDescendent(int lab){
        if (this.label == lab){
            return this;
        }
        if (this.isLeaf()) return null;
        
        NodeBase desc = ((NodeBase)this.getLeft()).findDescendent(lab);
        if (desc != null){
            return desc;
        }
        
        desc = ((NodeBase)this.getRight()).findDescendent(lab);
        if (desc != null){
            return desc;
        }  
        return null;
    }
    private NodeBase findAncestor(int label){
        if (this.getParent()==null){
            return null;
        }
        NodeBase par = (NodeBase)this.getParent();
        if (par.label == label ){
            return par;
        }
        return par.findAncestor(label);
    }
    // mark this node as used, this will propagate up if sister is also been marked used
    public void markedAsUsed(){
        this.setUsed(true);
        Node sister = this.getSister();
        if (sister == null){
            return;
        }
        if (sister.isUsed()){
            NodeBase parent = (NodeBase)this.getParent();        
            parent.markedAsUsed();
        }
    }
    
    // clear the subtree rooted at this node of marked status
    public void clearUsedMarks(){
        this.setUsed(false);
        if (!this.isLeaf()){
            NodeBase leftBase = (NodeBase)this.getLeft();
            leftBase.clearUsedMarks();
            NodeBase rightBase = (NodeBase)this.getRight();
            rightBase.clearUsedMarks();
        }
    }
    
    // is this node only marked as used
    public boolean isUsed(){
        return used;
    }
    
    // is any node in the subtree of this node marked as used
    public boolean isUsedRecursive(){
        if (this.isUsed()){
            return true;
        }
        
        if ( left!=null && ((NodeBase)left).isUsedRecursive()  ){
            return true;
        }
        
        if (right!=null &&  ((NodeBase)right).isUsedRecursive()  ){
            return true;
        }
        return false;
    }

    Integer N; // number of microclusters assigned to this node  
    MicroCluster micro;  // micro cluster if this is a terminal node 
    Node left;
    Node right;
    int label;
    Node parent;
    double lnR;  // log of the posterior 
    Double lnLike;
    boolean used = false;

//    Dfp r;   // posterior of the merged hypothesis
//    double realR;
    
//    static DfpField field = new DfpField(20);  // 20 decimal digits  
    public static int maxN;
    
    static double nu;
    static RealVector m;
    static RealMatrix S; 
    static double beta;
    static double lnBeta;
    static RealMatrix rmm;
    static Double logdetSnu;
    static Double lnAlpha;
    
    static Double logPi =  Utils.eln(Math.PI);
    static double alpha;
    static double detS;
    static GammaRatio ratio;    
}
