/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.util.Set;
import java.util.TreeSet;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.rhwlab.BHC.NodeBase;
import org.rhwlab.BHC.NucleusLogNode;

/**
 *
 * @author gevirl
 */
public class BHCNucleusData extends NucleusData {
    public BHCNucleusData(NodeBase base,int time){
        this(time,base.formElementXML());
        this.nodeBase = base;
    }
    // construct from Nucleus xml element saved in a file
    public BHCNucleusData(Element nucleusEle){
        super(nucleusEle);
//this.id = super.getName().substring(super.getName().indexOf('_')+1);
        count = Integer.valueOf(nucleusEle.getAttributeValue("count"));
        sourceNode = nucleusEle.getAttributeValue("sourceNode");
        totalIntensity = Long.valueOf(nucleusEle.getAttributeValue("intensity"));
        intensityRSD = Double.valueOf(nucleusEle.getAttributeValue("intensityRSD"));
        voxels = Integer.valueOf(nucleusEle.getAttributeValue("voxels"));
        try {
        posteriorProb = Double.valueOf(nucleusEle.getAttributeValue("posterior"));
        } catch (Exception exc){
            posteriorProb=0.;
        }
        init();        
        
    }
    // contruct from a BHC nodebase element
    public BHCNucleusData(int time,Element gmm){
        super(time,name(time,gmm.getAttributeValue("sourceNode")),center(gmm));  
//id = gmm.getAttributeValue("id");
        count = Integer.valueOf(gmm.getAttributeValue("count"));
        sourceNode = gmm.getAttributeValue("sourceNode");
        totalIntensity = Long.valueOf(gmm.getAttributeValue("intensity"));
        intensityRSD = Double.valueOf(gmm.getAttributeValue("intensityRSD"));
        voxels = Integer.valueOf(gmm.getAttributeValue("voxels"));
        try {
        posteriorProb = Double.valueOf(gmm.getAttributeValue("posterior"));
        } catch (Exception exc){
            posteriorProb=0.;
        }        
        A = precisionFromString(gmm.getAttributeValue("precision"));
        eigenA = new EigenDecomposition(A);
        adjustedA = A.copy();
        adjustedEigenA = new EigenDecomposition(adjustedA);
        R = new double[3];
        R[0] = R[1] = R[2] = 2.5;
        this.setAdjustment(R);
        init();

    }
    static public Set<BHCNucleusData> factory(TreeSet<NucleusLogNode> cut,double minVolume,int time){
        TreeSet<BHCNucleusData> ret = new TreeSet<>();
        int i=1;
        for (NucleusLogNode logNode : cut){
            BHCNucleusData nucData = BHCNucleusData.factory(logNode, time);
            if (nucData!=null && nucData.getVolume()>=minVolume){
                ret.add(nucData);
                ++i;
            }
        }  
        return ret;
    }
    static public BHCNucleusData factory(NodeBase node,int time){
        Element ele = node.formElementXML();
        if (ele != null){
            return  new BHCNucleusData(time,ele);
        }
        return null;
    }
    private void init(){
        volume = vf;
        for (int i=0 ; i<3 ; ++i){
            volume = volume*this.getRadius(i);
        }
        voxelDensity = voxels/volume;    
        intensityDensity = totalIntensity/volume;
    }
    public Element asXML(){
        Element ret = super.asXML();
        ret.setAttribute("sourceNode", this.sourceNode);
        ret.setAttribute("count",Integer.toString(this.count));
        ret.setAttribute("voxels",Integer.toString(this.voxels));
        ret.setAttribute("intensity", Long.toString(totalIntensity));
        ret.setAttribute("intensityRSD", Double.toString(intensityRSD));
        return ret;
    }

    public JsonObjectBuilder asJson(){
        JsonObjectBuilder ret = super.asJson();
        ret.add("SourceNode", this.sourceNode);
        ret.add("Count",this.count);
        ret.add("Voxels",this.voxels);
        ret.add("Intensity",this.totalIntensity);
        return ret;
    }
    public void printMat(String label,RealMatrix m){
        System.out.println(label);
        for (int r=0 ; r<m.getRowDimension() ; ++r){
            for (int c=0 ; c<m.getColumnDimension() ; ++c){
                System.out.printf("%f\t",m.getEntry(r, c));
            }
            System.out.println();
        }
    }

    static double[] center(Element ele){
        double[] ret = new double[3];
        ret[0] = Double.valueOf(ele.getAttributeValue("x"));
        ret[1] = Double.valueOf(ele.getAttributeValue("y"));
        ret[2] = Double.valueOf(ele.getAttributeValue("z"));

        return ret;
    }


    static String name(int time,String id){
        int n = Integer.valueOf(id);
        return String.format("%03d_%03d", time,n);
    }
/*
    static String name(Element gmm){
        return gmm.getAttributeValue("name");
    }
*/
    public String getID(){
//        return id;
        return this.sourceNode;
    }

    @Override
    public String toString(){
        return name(this.getTime(),this.getID());
    }
    public String getRadiusLabel(int i){
        // find the adjusted eigenvector closest to the original unadjusted eigenvector for dimension i
        // this keeps the order of the adjusted eigenvectors the same as the original unadjusted eigenvectors
        // the eigendecomposition returns the eigenvectors sorted by eigenvalue
        // this procedure puts them back in their original order
        int adjustedI = 0;
        double maxD = 0.0;
        RealVector aV = eigenA.getEigenvector(i);
        for (int j=0 ; j<A.getColumnDimension() ; ++j){
            RealVector v = adjustedEigenA.getEigenvector(j);
            double d = Math.abs(v.dotProduct(aV));
            if (d > maxD){
                maxD = d;
                adjustedI = j;
            }
        }
        RealVector v = adjustedEigenA.getEigenvector(adjustedI);
//        double eigenVal = adjustedEigenA.getRealEigenvalue(adjustedI);
//        double r = 1.0/Math.sqrt(Ace3D_Frame.R*eigenVal);
        double r = this.getRadius(adjustedI);
        return String.format("%4.1f(%.2f,%.2f,%.2f)",r, v.getEntry(0),v.getEntry(1),v.getEntry(2));
    }
    public String getSourceNode(){
        return this.sourceNode;
    }
    public double getVolume(){
        return volume;
    }
    public double getVoxelDensity(){
        return voxelDensity;
    }
    public double getAverageIntensity(){
        return ((double)totalIntensity)/voxels;
    }
    public double getIntensityRSD(){
        return this.intensityRSD;
    }
    public double getIntensityDensity(){
        return this.intensityDensity;
    }
    public double getPosteriorProb(){
        return this.posteriorProb;
    }
    // distance weighted by intensity and volume
    public double weightedDistance(BHCNucleusData other){
        double v = this.volume/other.volume;
        if (v <1.0){
            v = 1.0/v;
        }
        double ir = this.getAverageIntensity()/other.getAverageIntensity();
        if (ir<1.0){
            ir = 1.0/ir;
        }
        double d = super.distance(other);
        double ret = 4.0*v+ir+d;
 /*       
        System.out.printf("Volumes: %f,%f\n", this.volume,other.volume);
        System.out.printf("AvgInt: %f,%f\n",this.getAverageIntensity(),other.getAverageIntensity());
        System.out.printf("distance: %f\n", d);        
        System.out.printf("score: %f\n\n",ret);
*/
        return ret;
    }
    public NodeBase getNodeBase(){
        return this.nodeBase;
    }
    static public double similarityScore(BHCNucleusData nuc0,BHCNucleusData nuc1){
        return nuc0.weightedDistance(nuc1);
    }
    static double vf = 4.0*Math.PI/3.0;
//    String id;
    int count;  // number of micro clusters in the nucleus
    String sourceNode;  // the BHC tree node from which this nucleus is built
    NodeBase nodeBase;  // the BHC Tree node that this nucleus was constructed from
    double volume;  // volume of nucleus
    double voxelDensity;  // voxels per unit volume
    long totalIntensity;        // sum of all voxel intensities
    double intensityDensity;  // intensity per unit volume
    double intensityRSD;  // intensity stadard deviation / mean
//    double segmentedProb;  // average segmented probability of all the microclusters 
    double posteriorProb;
    int voxels;  // number of voxels in the nucleus
}

