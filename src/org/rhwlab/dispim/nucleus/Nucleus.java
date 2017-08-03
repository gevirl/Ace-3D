package org.rhwlab.dispim.nucleus;

import java.awt.Shape;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeMap;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class Nucleus implements Comparable {
    public Nucleus(NucleusData data){
        this.child1 = null;
        this.child2 = null;
        this.parent = null;
        this.nucData = data;
        this.cellName = data.getName();
    }


    public Nucleus clone(){
        Nucleus clone = new Nucleus(this.nucData);
        clone.child1 = this.child1;
        clone.child2 = this.child2;
        clone.cellName = this.cellName;
        clone.userNamed = this.userNamed;
        clone.parent = this.parent;
        return clone;
    }
    static public String randomName(){
        return NucleusData.randomName();
    }
    static public void saveHeadings(PrintStream stream){
        stream.println("Time,Name,X,Y,Z,Radius,Child1,Child2");
    }
    public void saveNucleus(PrintStream stream){
//        stream.printf("%d,%s,%d,%d,%d,%f,%s,%s\n",time,getName(),x,y,z,radius,getChild1(),getChild2());
    }
    public int getTime(){
        return nucData.getTime();
    }
   
    public double[] getCenter(){
        return nucData.getCenter();
    }
    public void setCenter(long[] c){
        nucData.setCenter(c);;
    }
    public void setCenter(double[] c){
        nucData.setCenter(c);
    } 
    // returns the nucleus unique identfier 
    public String getName(){
        return nucData.getName();
    }
    public String getFullName(){
        return String.format("%s|%s",getName(),getCellName());
    }

    public void setMarked(boolean s){
        nucData.setMarked(s);
    }
    public boolean getMarked(){
        return nucData.getMarked();
    }

    public double distanceSqaured(long[] p){
        double d = 0.0;
        double[] c = this.getCenter();
        for (int i=0 ; i<p.length ; ++i){
            double delta = p[i]-c[i];
            d = d + delta*delta;
        }
        return d;
    }
    public Element asXML(){
        Element ret = nucData.asXML();
        ret.setAttribute("cell", cellName);
        ret.setAttribute("usernamed", Boolean.toString(userNamed));
        if (this.parent != null){
            ret.setAttribute("parent", this.parent.getName());
        }
        if (this.child1 != null){
            ret.setAttribute("child1", this.child1.getName());
        }
        if (this.child2 != null){
            ret.setAttribute("child2", this.child2.getName());
        }        
        return ret;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = nucData.asJson();
        builder.add("cell", cellName);
        builder.add("usernamed",this.userNamed);
        if (this.parent != null){
            builder.add("parent",this.parent.getName());
        }
        if (child1 != null){
            builder.add("child1",this.child1.asJson() );
        }    
        if (child2 != null){
            builder.add("child2",this.child2.asJson() );
        }         
        return builder;
    }
    // names all the nuclei in the cell that contains this nucleus
    public void nameAllNucleiInCell(String cellname,boolean user){
        Nucleus first = this.firstNucleusInCell();
        nameCellRecursive(first,cellname,user);
    }
    
    // name all the descendent nuclei in the cell
    static public void nameCellRecursive(Nucleus nuc,String cellname,boolean user){
        if (cellname == null){
            cellname = nuc.getName();
        }
        nuc.setCellName(cellname, user);   
        if (nuc.isDividing() || nuc.isLeaf()){
            return;
        }
        nameCellRecursive(nuc.getChild1(),cellname,user);
    }
    
    // assign the cell name to this nucleus
    public void setCellName(String name,boolean user){
        this.cellName = name;
        this.userNamed = user;
        
    }
    public String getCellName(){
        if (cellName == null){
            return this.getName();  // return the nuc name if nuc not given a cell name yet
        }
        return this.cellName;
    }
    public boolean isUsernamed(){
        return this.userNamed;
    }
    @Override
    public int compareTo(Object o) {
        return nucData.compareTo(((Nucleus)o).getNucleusData());
    }  
    public boolean getLabeled(){
        return nucData.getLabeled();
    }
    public void setLabeled(boolean lab){
        nucData.setLabeled(lab);
    }
    public double getExpression(){
        return nucData.getExpression();
    }
    public void setExpression(double e){
        nucData.setExpression(e);
    }
    
    public boolean isVisible(long slice,int dim){
        return nucData.isVisible(slice, dim);
    }

    public Shape getShape(long slice,int dim,int bufW,int bufH){
        return nucData.getShape(slice, dim, bufW, bufH);
    }    
    public int imageXDirection(int dim){
        if (dim==0){
            return 1;
        }
        return 0;
    }    
    public int imageYDirection(int dim){
        if (dim==2){
            return 1;
        }
        return 2;
    } 
    public Object getAdjustment(){
        return nucData.getAdjustment();
    }
    public void setAdjustment(Object o){
        nucData.setAdjustment(o);
    }       
    public RealMatrix adjustPrecision(){
        return nucData.adjustPrecision();
    } 
    public String getRadiusLabel(int i){
        return ((BHCNucleusData)nucData).getRadiusLabel(i);
    }

    public static RealMatrix precisionFromString(String s){
        double [][] ret = new double[3][3];
        String[] tokens = s.split(" ");
        for (int i=0 ; i<3 ; ++i){
            for (int j=i ; j<3 ; ++j){
                ret[i][j] = Double.valueOf(tokens[3*i+j]);
                ret[j][i] = ret[i][j];
            }
        }
        return new Array2DRowRealMatrix(ret);
    }
    // probability the given position (relative to the center)  belongs to this nucleus
    public double prob(double[] p){
        return nucData.prob(p);
    }
    public double getRadius(int d){
        return nucData.getRadius(d);
    } 
    public long[] getRadii(){
        long[] radii = new long[3];
        
        radii[0] = (long)getRadius(0);
        radii[1] = (long)getRadius(1);
        radii[2] = (long)getRadius(2);
        return radii;
    }
    // return the direction vectors of the ellipsoid axes sorted by length of radii
    public RealVector[] getAxes(){
        return nucData.getAxes();
    }
    public String getFrobenius(){
        return nucData.getFrobenius();
    }
    public double[][] getEigenVectors(){
        return nucData.getEigenVectors();
    }
    public double[][] getEigenVectorsT(){
        return nucData.getEigenVectorsT();
    }   
    
    // determine if this nucleus is dividing
    public boolean isDividing(){
        return child1!=null && child2!=null;
    }
    
    // return the next nuclei this nucleus is linked to
    // return Nucleus[0] if not linked
    // return Nulcues[1] if linked in time
    // return Nucleus[2] if dividing
    public Nucleus[] nextNuclei(){
        if (isDividing()){
            Nucleus[] ret = new Nucleus[2];
            ret[0] = child1;
            ret[1] = child2;
            return ret;  // dividing
        }else if (child1==null && child2==null){
            Nucleus[] ret = new Nucleus[0];
            return ret;  // leaf nucleus
        } else {
            Nucleus[] ret = new Nucleus[1];
            ret[0] = child1;
            return ret;  // linked in time
        }

    }
    // measure the distance to another nucleus
    public double distance(Nucleus other){
        return nucData.distance(other.nucData);
    }
    public double shapeDistance(Nucleus other){
        return nucData.shapeDistance(other.nucData);
    }
    private void reportMatrix(PrintStream stream,String label,RealMatrix m){
        stream.printf("%s: ",label);
        for (int r=0 ; r<m.getRowDimension() ; ++r){
            for (int c=0 ; c<m.getColumnDimension() ; ++c){
                stream.printf("%f ",m.getEntry(r, c));
            }
            stream.print(" : ");
        }
        stream.println();
    }
    private RealMatrix reverseHandedness(RealMatrix m){
        RealMatrix ret = m.copy();
        for (int c=0 ; c<m.getColumnDimension();++c){
            
            ret.setEntry(0, c, -m.getEntry(0, c));
        }
        return ret;
    }
   
    public String briefReport(){
        double[] c = this.getCenter();
        return String.format("V=%.0f (%.0f,%.0f,%.0f)",this.getVolume(),c[0],c[1],c[2]);
    }
    public void report(PrintStream stream){
         
        if (cellName != null){
            stream.printf("Nucleus:%s time=%d  cell=%s\n",nucData.getName(),nucData.getTime(),cellName);
        } else {
            stream.printf("Nucleus:%s time=%d  no cell\n",nucData.getName(),nucData.getTime());
        }
        
    }
    // return the root cell leading to this nucleus
    public String getRoot(){
        if (this.parent==null){
            return this.cellName;
        }
        return parent.getRoot();
    }
    // return the sister nucleus if parent just divided
    public Nucleus getSisterNucleus(){
        return this.getSister();
    }
    public double[] eccentricity(){
        double[] r = new double[3];
        for (int i=0 ; i<3 ; ++i){
            r[i] = this.getRadius(i);
        }
        Arrays.sort(r);
        
        double[] e = new double[3];
        e[0] = ecc(r[0],r[1]);
        e[1] = ecc(r[0],r[2]);
        e[2] = ecc(r[1],r[2]);

        return e;
    }
    private double ecc(double axis1,double axis2){
        double f = axis1/axis2;
        return Math.sqrt((1.0- f*f));        
    }
    
    // the time since this nucleus last divided
    public int timeSinceDivsion(){
        if (this.parent == null){
            return -1;
        }
        if (this.parent.child2!= null){
            return 1;
        }
        int t = this.parent.timeSinceDivsion();
        if (t != -1){
            ++t;
        }
        return t;
    }
    public NucleusData getNucleusData(){
        return nucData;
    }
    public Nucleus getSister(){
        if (parent == null){
            return null;  // must have a parent to have a sister
        }
        if (parent.child2 == null){
            return null;  // must have a dividing parent
        }
        if (parent.child1.equals(this)){
            return parent.child2;
        }
        return parent.child1;
    }
    public Nucleus getParent(){
        return this.parent;
    }
    // link this nucleus to a daughter
    // if this nucleus is dividing, then this cannot be done and false is returned
    // if this nucleus is already linked in time then it will result in a division
    public boolean linkTo(Nucleus daughter){
        daughter.unlink();
        if (child2 != null){
            return false;  // can't link
        }
        if (child1 != null){
            // now a division 
            child2 = daughter;
//            String newName = child1.getName();
//            child1.renameContainingCell(newName);
            
        } else {
            this.child1 = daughter;  // linking in time
            daughter.renameContainingCell(this.getCellName());  // daughter and parent are in the same cell now
        }
        daughter.parent = this;
        NamedNucleusFile.nameChildren(this);
        return true;
    }
    @Override
    public String toString(){        
        return String.format("%s",this.getCellName());
    }

    public void setDaughters(Nucleus c1,Nucleus c2){
        this.child1 = c1;
        if (this.child1 != null) {
            this.child1.parent = this;
        }
        
        this.child2 = c2;
        if (c2 != null){
            this.child2.parent = this;
        }
    }

    public void setParent(Nucleus p){
        this.parent = p;
    }
    public Nucleus getChild1(){
        return this.child1;
    }
    public Nucleus getChild2(){
        return this.child2;
    }  
    public Nucleus firstNucleusInCell(){
        if (this.parent == null){
            return this;
        }
        if (this.parent.isDividing()){
            return this;
        }
        return parent.firstNucleusInCell();
    }
    // rename this nucleus and all its time descendents
    public void renameContainingCell(String name){
        this.cellName = name;
        if (child1 == null){
            return ;
        }
        if (this.isDividing()){
            return;  // do not go past division
        }
        child1.renameContainingCell(name);
    }
    // find the terminal nuclei under this nucleus
    public void findLeaves(Set<Nucleus> leaves){
        if (this.isLeaf()){
            leaves.add(this);
            return;
        }
        if (child1 != null){
            child1.findLeaves(leaves);
        }
        if (child2 != null){
            child2.findLeaves(leaves);
        }
    }
    public boolean isLeaf(){
        return this.child1==null && this.child2==null;
    }
    public Nucleus lastNucleusOfCell(){
        if (this.isDividing()){
            return this;
        }
        if (child1==null){
            return this;
        }
        return child1.lastNucleusOfCell();
    }
    public void descedentsInCell(TreeMap<Integer,Nucleus> ret){
        ret.put(this.getTime(), this);
        if (this.isDividing() || this.isLeaf()){
            return ;
        }
        this.child1.descedentsInCell(ret);
        
    }
    // unkin this nucleus from its parent
    public void unlink(){
        Nucleus parent = this.getParent();
        if (parent == null){
            return;
        }
        // the nucleus being unlinked must get a new cellname
        this.renameContainingCell(this.getName());
        
        if (parent.getChild1()==this){
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
        this.setParent(null);
    }
    
    public void setTime(int time){
        nucData.setTime(time);
    }
    
    public double getVolume(){
        return ((BHCNucleusData)nucData).getVolume();
    }
    public double getAvgIntensity(){
        return ((BHCNucleusData)nucData).getAverageIntensity();
    }
    static public boolean intersect(Nucleus nuc1,Nucleus nuc2){
        return NucleusData.intersect(nuc1.nucData, nuc2.nucData);
    }
    static public double similarityScore(Nucleus nuc1,Nucleus nuc2){
        return BHCNucleusData.similarityScore((BHCNucleusData)nuc1.nucData, (BHCNucleusData)nuc2.nucData);
    }
    // does nuc2 match nuc1 well enough to be used for expansion
    static public boolean matchForExpansion(Nucleus nuc1,Nucleus nuc2){
        long[] radii = nuc2.getRadii();
        if (radii[2]>38){
            return false;  //do not expand up beyond a maximum radius of 38
        }
        if (nuc1.getCellName().equals("polar2")){
            int asiufh=0;
        }
        double d = nuc1.distance(nuc2);
        if (d > expansionDistanceThresh){
            return false;
        }
        
        double[] ecc1 = nuc1.eccentricity();
        double[] ecc2 = nuc2.eccentricity();
        if (ecc2[2] > .95 && ecc2[1] > .95){
            return false;
        }
        
        double volRatio = nuc1.getVolume()/nuc2.getVolume();
        if (volRatio < 1.0) volRatio = 1.0/volRatio;
        if (volRatio > 1.5){
            return false;
        }
        
        double intRatio = nuc1.getAvgIntensity()/nuc2.getAvgIntensity();
        if (intRatio < 1.0)  intRatio = 1.0/intRatio;
        if (intRatio > 1.8){
            return false;
        }
        
        return true;
    }
    // do two nuclei match up well enough to be sisters
    static public boolean sisterMatch(Nucleus nuc1,Nucleus nuc2){
        double d = nuc1.distance(nuc2);
        if (d > distThreshold){
            return false;
        }
        
        double[] ecc1 = nuc1.eccentricity();
        double[] ecc2 = nuc2.eccentricity();
        if (ecc2[2] > .95 && ecc2[1] > .95){
            return false;
        }
        
        double volRatio = nuc1.getVolume()/nuc2.getVolume();
        if (volRatio < 1.0) volRatio = 1.0/volRatio;
        if (volRatio > 2.0){
            return false;
        }
        
        double intRatio = nuc1.getAvgIntensity()/nuc2.getAvgIntensity();
        if (intRatio < 1.0)  intRatio = 1.0/intRatio;
        return intRatio <= 2.0;
    }
    // is this nucleus between two other nuclei
    public boolean between(Nucleus nuc1,Nucleus nuc2){
        return between(this.getCenter(),nuc1,nuc2);
    }
    public double getDirectionCosine(Nucleus other1,Nucleus other2){
        RealVector v = new ArrayRealVector(this.getCenter());
        RealVector v1 = new ArrayRealVector(other1.getCenter());
        RealVector v2 = new ArrayRealVector(other2.getCenter());
        return v1.subtract(v).cosine(v2.subtract(v));
    }
    
    // determine if a given point is between two nuclei
    static public boolean between(double[] p,Nucleus nuc1,Nucleus nuc2){
        int D = p.length+1;
        RealVector P = new ArrayRealVector(D);
        double[] c1 = nuc1.getCenter();
        double[] c2 = nuc2.getCenter();
        double[] del = new double[p.length];
        double rMax = 0.0;
        long[] r1 = nuc1.getRadii();
        long[] r2 = nuc2.getRadii();
        
        double sum3 = 0;
        double sum2 = 0.0;
        RealMatrix toOrigin = new Array2DRowRealMatrix(D,D);
        RealMatrix Txz = new Array2DRowRealMatrix(D,D);
        RealMatrix Tz = new Array2DRowRealMatrix(D,D);
        for (int i=0 ; i<p.length ; ++i){
            P.setEntry(i, p[i]);
            Txz.setEntry(i, i, 1.0);
            Tz.setEntry(i, i, 1.0);
            del[i] = c2[i] - c1[i];
            toOrigin.setEntry(i, i, 1.0);
            toOrigin.setEntry(i,p.length,-c1[i]);
            double del2 = del[i]*del[i];
            sum3 = sum3 + del2;
            if (i<2){
                sum2 = sum2 + del2;
            }
            if (r1[i] > rMax){
                rMax = r1[i];
            }
            if (r2[i] > rMax){
                rMax = r2[i];
            }
        }
//        rMax = 1.2* rMax;
        P.setEntry(p.length, 1.0);
        toOrigin.setEntry(p.length,p.length, 1.0);
        P = toOrigin.operate(P);  // translation of nuc1 to the origin
        
        double l = Math.sqrt(sum3);
        if (l == 0.0){
            // nuclei are right on top of each other
            return false;
        }
        double lxy = Math.sqrt(sum2);        
        if (lxy != 0.0){
            // rotation needed - nuc2 rotated to the z axis
            Txz.setEntry(0, 0, del[0]/lxy);
            Txz.setEntry(0, 1, del[1]/lxy);
            Txz.setEntry(1, 1, del[0]/lxy);
            Txz.setEntry(1, 0, -del[1]/lxy);   

            Tz.setEntry(0, 0, del[2]/l);
            Tz.setEntry(0, 2, -lxy/l);
            Tz.setEntry(2, 2, del[2]/l);
            Tz.setEntry(2, 0, lxy/l); 

            
            Txz.setEntry(p.length,p.length, 1.0);
            Tz.setEntry(p.length,p.length, 1.0);

            P = Txz.operate(P);  // rotate into the xz plane
            P = Tz.operate(P);  // rotate to the z axis
            RealVector N = new ArrayRealVector(D);
            for (int i=0 ; i<del.length ; ++i){
                N.setEntry(i, del[i]);;
            }
            N.setEntry(del.length, 1.0);
            N = Txz.operate(N);
            N = Tz.operate(N);
            int wjkerh=0;
        }

        double r = Math.sqrt(P.getEntry(0)*P.getEntry(0) + P.getEntry(1)*P.getEntry(1));
        if (r > rMax){
            return false;
        }
        double z = P.getEntry(2);
        if (z < -rMax){
            return false;
        }
        if (z > l + rMax){
            return false;
        }
        return true;
    }
    public String getLineage(){
        for (String lineage : lineages){
            if (cellName.startsWith(lineage)){
                return lineage;
            }
        }
        return "";
    }
    
    public double getIntensityRSD(){
        return ((BHCNucleusData)nucData).getIntensityRSD();
    }
    private Nucleus child1;
    private Nucleus child2;
    private Nucleus parent; 
    private String cellName;  // the cell to which this nucleus belongs 
    boolean userNamed = false;  // indicates if the user has named the cell to which this nucleus belongs
    final private NucleusData nucData;
    
    static double distThreshold=50;
    static double expansionDistanceThresh = 25;
    static String[] lineages = {"AB","MS","E","C","D","Z","P"};
}
