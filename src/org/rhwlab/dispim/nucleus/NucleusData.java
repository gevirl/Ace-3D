/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import javafx.beans.InvalidationListener;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DiagonalMatrix;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jdom2.Element;
import org.rhwlab.ace3d.SingleSlicePanel;


/**
 *
 * @author gevirl
 */
public class NucleusData implements Comparable {
    public NucleusData(JsonObject jsonObj){
        this.time = jsonObj.getInt("Time");
        this.name = jsonObj.getString("Name");
        String precString = jsonObj.getJsonString("Precision").getString();
        this.A = precisionFromString(precString);
        this.xC = jsonObj.getJsonNumber("X").longValue();
        this.yC = jsonObj.getJsonNumber("Y").longValue();
        this.zC = jsonObj.getJsonNumber("Z").longValue();
        this.exp = jsonObj.getJsonNumber("Expression").longValue();

        
        this.eigenA = new EigenDecomposition(A);
        this.adjustedA = this.A.copy();
        this.adjustedEigenA = new EigenDecomposition(adjustedA);
        
        double[] adj = new double[3];
        adj[0] = adj[1] = adj[2] =1.0;        
        this.setAdjustment(adj);
    }
    public NucleusData(Element ele){
        
        this.time = Integer.valueOf(ele.getAttributeValue("time"));
        this.name = ele.getAttributeValue("name");
        String precString = ele.getAttributeValue("precision");
        this.A = precisionFromString(precString);
        this.xC = Double.valueOf(ele.getAttributeValue("x"));
        this.yC = Double.valueOf(ele.getAttributeValue("y"));
        this.zC = Double.valueOf(ele.getAttributeValue("z"));
        
        this.eigenA = new EigenDecomposition(A);
        this.adjustedA = this.A.copy();
        this.adjustedEigenA = new EigenDecomposition(adjustedA);
        
        double[] adj = new double[3];
        adj[0] = adj[1] = adj[2] =1.0;        
        this.setAdjustment(adj);        
    }


    public NucleusData (int time,double[] center,double radius){
        this(time,randomName(),center);
    }
    public NucleusData (int time,String name,double[] center){
        this.time = time;

        this.name = name;
        this.xC = center[0];
        this.yC = center[1];
        this.zC = center[2];
//        this.radius = radius;
    } 
    public NucleusData.Ellipse2d zPlaneEllipse(double v){
        return ellipse(0,1,2,v);
    }
    public NucleusData.Ellipse2d yPlaneEllipse(double v){
        return ellipse(0,2,1,v);
    }
    public NucleusData.Ellipse2d xPlaneEllipse(double v){
        return ellipse(1,2,0,v);
    }
    public NucleusData.Ellipse2d ellipse(int xi,int yi,int zi,double v){
        NucleusData.Coeff coef = coef(xi,yi,zi,v);
        return ellipse(xi,yi,zi,coef);
    } 
    
    // coefficients of the resulting ellipse 
    // when the ellipsoid is cut at slice = v
    // xi, yi,zi determine which direction of the ellipsoid is being cut
    // zi cooresponds to the slice direction
    public Coeff coef(int xi,int yi,int zi,double v){
        Coeff c = new Coeff();
        double[] ce = this.getCenter();
        double[][] a = adjustedA.getData();
  //      double ff = - Math.log(eigenA.getDeterminant());
        v = v-ce[zi];
        c.A = ff*a[xi][xi];
        c.B = ff*(a[xi][yi] + a[yi][xi]);
        c.C = ff*a[yi][yi];
        c.D = ff*v*(a[xi][zi] + a[zi][xi]);
        c.E = ff*v*(a[yi][zi] + a[zi][yi]);
        c.F = ff*a[zi][zi]*v*v-1.0;
        return c;
    }   
    public  Ellipse2d ellipse(int xi,int yi,int zi,Coeff coef){
        Array2DRowRealMatrix Q =  new Array2DRowRealMatrix(3,3);
        Q.setEntry(0,0,coef.A);
        Q.setEntry(1,0,coef.B/2);
        Q.setEntry(0,1,coef.B/2);
        Q.setEntry(1,1,coef.C);
        Q.setEntry(2,0,coef.D/2);
        Q.setEntry(0,2,coef.D/2);
        Q.setEntry(2,1,coef.E/2);
        Q.setEntry(1,2,coef.E/2);
        Q.setEntry(2,2,coef.F);
        EigenDecomposition ed = new EigenDecomposition(Q);
        double detQ = ed.getDeterminant();
        
        RealMatrix rm = new Array2DRowRealMatrix(2,2);
        rm.setEntry(0,0, coef.A);
        rm.setEntry(1,1, coef.C);
        rm.setEntry(0,1,coef.B/2.0);
        rm.setEntry(1,0,coef.B/2.0);
        EigenDecomposition eigenDecomp = new EigenDecomposition(rm);
        double detA33 = eigenDecomp.getDeterminant();
        double[] eigenValues = eigenDecomp.getRealEigenvalues();
//System.out.printf("Eigenvalues: %f,%f\n",eigenValues[0],eigenValues[1])        ;
        RealVector eigenvector0 = eigenDecomp.getEigenvector(0);
        Ellipse2d e = new Ellipse2d();
        double cot2theta  = (coef.A-coef.C)/coef.B;
        if (Double.isFinite(cot2theta)){
            double d= Math.sqrt(1.0+cot2theta*cot2theta);
            double cos2theta = cot2theta/d;
            e.cosine = Math.sqrt((1.0 + cos2theta)/2.0);
            e.sine = Math.sqrt((1.0 - cos2theta)/2.0);
        } else {
            e.cosine = 1.0;
            e.sine = 0.0;            
        }
       
        double dd =  coef.B*coef.B - 4.0*coef.A*coef.C;
        double xc = ( 2.0*coef.C*coef.D - coef.B*coef.E)/dd;
        double yc = ( 2.0*coef.A*coef.E - coef.B*coef.D)/dd;
// System.out.printf("dd=%f,xc=%f,xcn=%f,yc=%f,ycn%f\n",dd,xc,xcn,yc,ycn);
        double[] ce = this.getCenter();
        e.x = ce[xi] + xc;
        e.y = ce[yi] + yc;
       
 //       double f = -detQ/detA33 * 2;
        double f = -detQ/detA33;
        double a = eigenValues[0]/f;
        double b = eigenValues[1]/f;
        if (a <=0.0 || b<=0.0){
            return null;
        }
        e.a = 1.0/Math.sqrt(a);
        e.b = 1.0/Math.sqrt(b);
//System.out.printf("detQ=%e,detA33=%e,f=%f,a=%e,b=%e\n",detQ,detA33,f,e.a,e.b);        
//System.out.printf("eigenValues (%f,%f)\n",eigenValues[0],eigenValues[1]);
        e.cosine = eigenvector0.getEntry(0);
        e.sine = eigenvector0.getEntry(1);         
        e.low[xi] = (long)(e.x - e.a);
        e.low[yi] = (long)(e.y - e.b);
        e.low[zi] = 0;
        e.high[xi] = (long)(e.x + e.a);
        e.high[yi] = (long)(e.y + e.b);
        e.high[zi] = 0;
//System.out.printf("Ellipse: a=%f,b=%f,x=%f,y=%f\n", e.a,e.b,e.x,e.y);
        return e;
    }    
    static public String randomName(){
        if (rnd == null){
            rnd = new Random();
        }
        return String.format("Nuc_%d",(new Date()).getTime());
    }
    static public void saveHeadings(PrintStream stream){
        stream.println("Time,Name,X,Y,Z,Radius,Child1,Child2");
    }
    public void saveNucleus(PrintStream stream){
//        stream.printf("%d,%s,%d,%d,%d,%f,%s,%s\n",time,getName(),x,y,z,radius,getChild1(),getChild2());
    }
    public int getTime(){
        return this.time;
    }
   
    public double[] getCenter(){
        double[] center = new double[3];
        center[0] = xC;
        center[1] = yC;
        center[2] = zC;
        return center;
    }
    public void setCenter(long[] c){
        xC = c[0];
        yC = c[1];
        zC = c[2];
    }
    public void setCenter(double[] c){
        xC = c[0];
        yC = c[1];
        zC = c[2];
    }    
    public String getName(){
        if (name == null){
            return this.toString();
        }
        return name;
    }

    public void setMarked(boolean s){
        this.marked = s;
    }
    public boolean getMarked(){
        return this.marked;
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
        Element ret = new Element("Nucleus");
        ret.setAttribute("name", name);
        ret.setAttribute("time",Integer.toString(time));
        ret.setAttribute("x",Double.toString(xC));
        ret.setAttribute("y",Double.toString(yC));
        ret.setAttribute("z",Double.toString(zC));
        ret.setAttribute("precision", precisionAsString(adjustedA));
        ret.setAttribute("expression", Double.toString(exp));
        return ret;
    }
    public JsonObjectBuilder asJson(){
        JsonObjectBuilder builder = Json.createObjectBuilder();
        builder.add("Name", name);
        builder.add("Time", time);
        builder.add("X", xC);
        builder.add("Y", yC);
        builder.add("Z", zC);
        builder.add("Precision",precisionAsString(adjustedA));

        builder.add("Expression",this.exp);
        return builder;
    }

    @Override
    public int compareTo(Object o) {
        return this.name.compareTo(((NucleusData)o).name);
    }  
    public boolean getLabeled(){
        return this.labeled;
    }
    public void setLabeled(boolean lab){
        this.labeled = lab;
    }
    public double getExpression(){
        return exp;
    }
    public void setExpression(double e){
        this.exp = e;
    }
    
    public boolean isVisible(long slice,int dim){
        Ellipse2d e;
        switch(dim){
            case 0:
                e = xPlaneEllipse((double)slice);
                break;
            case 1:
                e = yPlaneEllipse((double)slice);
                break;            
            default:
                e = zPlaneEllipse((double)slice);
                break;                 
        }
        return e!=null;
    }

    public Shape getShape(long slice,int dim,int bufW,int bufH){

//System.out.printf("Ellipsoid center = (%d,%d,%d)\n",this.xC,this.yC,this.zC);
        Ellipse2d e;
        switch(dim){
            case 0:
                e = xPlaneEllipse((double)slice);
                break;
            case 1:
                e = yPlaneEllipse((double)slice);
                break;            
            default:
                e = zPlaneEllipse((double)slice);
                break;                 
        }
        if (e != null){
//System.out.printf("%s dim=%d  slice=%d\n",this.getName(),dim,slice);
            AffineTransform toOrigin = AffineTransform.getTranslateInstance(-e.x,-e.y);
            AffineTransform back = AffineTransform.getTranslateInstance(e.x, e.y);
            AffineTransform xform = AffineTransform.getRotateInstance(e.cosine, e.sine);
            int scrX = SingleSlicePanel.screenX(e.low,dim,bufW);
            int scrY = SingleSlicePanel.screenY(e.low,dim,bufH);
            int scrHighX = SingleSlicePanel.screenX(e.high,dim,bufW);
            int scrHighY = SingleSlicePanel.screenY(e.high,dim,bufH);
            Shape shape = new Ellipse2D.Double(scrX,scrY,scrHighX-scrX,scrHighY-scrY); 
            shape = toOrigin.createTransformedShape(shape);
            shape =  xform.createTransformedShape(shape);
            shape = back.createTransformedShape(shape);
//System.out.printf("e.a:%f e.b:%f e.x:%f e.y:%f\n",e.a,e.b,e.x,e.y);
            return shape;
        }
        return null;
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
        return this.R;
    }
    public void setAdjustment(Object o){
        double[] v = (double[])o;
        R = new double[v.length];
        R[0] = v[0];
        R[1] = v[1];
        R[2] = v[2];
        adjustedA = adjustPrecision();
        adjustedEigenA = new EigenDecomposition(adjustedA);
        double[][]a = adjustedA.getData();
        ff = -Math.log(adjustedEigenA.getDeterminant());
        ff = 1.0;
    }       
    public RealMatrix adjustPrecision(){
        DiagonalMatrix D = new DiagonalMatrix(eigenA.getRealEigenvalues());
        for (int i=0 ; i<R.length ; ++i){
            double lambda = D.getEntry(i,i);
            D.setEntry(i,i,lambda/(R[i]*R[i]));
        }
        RealMatrix ret = eigenA.getV().multiply(D.multiply(eigenA.getVT()));
        return ret;
    } 

    static public String precisionAsString(RealMatrix m){
        StringBuffer buf = new StringBuffer();
        for (int r=0 ; r<m.getRowDimension() ; ++r){
            for (int c=0 ; c<m.getColumnDimension() ; ++c){
                if (r !=0 || c!=0){
                    buf.append(" ");
                }
                buf.append(Double.toString(m.getEntry(r, c)));
            }
        }
        return buf.toString();
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

        RealVector v = new ArrayRealVector(p);
        double d2 = Math.sqrt(this.adjustedEigenA.getDeterminant());
        double ex = -0.5*v.dotProduct(adjustedA.operate(v));
        return d2 * Math.exp(ex); 
    }
    public double getRadius(int d){

        double eigenVal = adjustedEigenA.getRealEigenvalue(d);
 //       double r = 1.0/Math.sqrt(Ace3D_Frame.R*eigenVal); 
        double r = 1.0/Math.sqrt(eigenVal*ff);
        return r;
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
        TreeMap<Double,RealVector> map = new TreeMap<>();
        for (int d=0 ; d<this.getCenter().length ; ++d){
            map.put(this.getRadius(d), this.adjustedEigenA.getEigenvector(d));
        }
        return map.values().toArray(new RealVector[0]);
    }
    public String getFrobenius(){
 //       return (this.adjustedEigenA.getV().multiply(this.adjustedEigenA.getD()).subtract(MatrixUtils.createRealIdentityMatrix(3))).getFrobeniusNorm();
      double f = this.adjustedA.getFrobeniusNorm();
        double[] eigenValues = this.adjustedEigenA.getRealEigenvalues();
        double sum = 0.0;
        for (int i=0 ; i<eigenValues.length ; ++i){
            sum = sum + eigenValues[i]*eigenValues[i];
        }
        return String.format("%.4f %.4f",Math.sqrt(sum),f);
    }
    
    public double[][] getEigenVectors(){
        return adjustedEigenA.getV().getData();
    }
    public double[][] getEigenVectorsT(){
        return adjustedEigenA.getVT().getData();
    }   
    

    // measure the distance to another nucleus
    public double distance(NucleusData other){
        double delx = xC - other.xC;
        double dely = yC - other.yC;
        double delz = zC - other.zC;
        return Math.sqrt(delx*delx + dely*dely + delz*delz);
    }
    public double shapeDistance(NucleusData other){
        double ret = 0.0;
        double[] l = other.adjustedEigenA.getRealEigenvalues();
        for (int i=0 ; i<l.length ; ++i){
            l[i] = 1.0/Math.sqrt(l[i]);
        }
        Arrays.sort(l);
        double[] k = this.adjustedEigenA.getRealEigenvalues();
        for (int i=0 ; i<k.length ; ++i){
            k[i] = 1.0/Math.sqrt(k[i]);
        }
        Arrays.sort(k);  
        
        for (int i=0 ; i<k.length ; ++i){
            double del = k[i]-l[i];
            ret = ret + del*del;
        }
        ret = Math.sqrt(ret);
        return ret;
/*        
        RealMatrix Vt = other.adjustedEigenA.getVT();
        RealMatrix V = other.adjustedEigenA.getV();
        DiagonalMatrix D = new DiagonalMatrix(l);
        
        RealMatrix Bp = D.multiply(Vt.multiply(this.adjustedA.multiply(V.multiply(D))));
//        reportMatrix(System.out,"\n\nBp",Bp);
        EigenDecomposition dec = new EigenDecomposition(Bp);
        double [] eigen = dec.getRealEigenvalues();
        ret = eigen[0]/eigen[2];
        if (ret < 1.0){
            ret = 1.0/ret;
        }
        ret = ret*ret;
        
        double sum = 0.0;
        for (int i=0 ; i<eigen.length ; ++i){
            double del = 1.0 - 1.0/(eigen[i]*eigen[i]);
            sum = sum + del*del;
            System.out.printf("%f ",1.0/(eigen[i]*eigen[i]));
        }
        System.out.println();
        RealMatrix del = Bp.subtract(MatrixUtils.createRealIdentityMatrix(l.length));
 //       reportMatrix(System.out,"del",del);
        double f = del.getFrobeniusNorm();
//        System.out.printf("%f \n",f);
//System.out.printf("%s:%s  (%e,%e,%e):(%e,%e.%e)\n",this.getName(),other.getName(),this.getRadius(0),this.getRadius(1),this.getRadius(2),other.getRadius(0),other.getRadius(1),other.getRadius(2));
        return ret;
*/
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
    
    public RealMatrix quadraticSurfaceMatrix(){
        int D = 4;
        int Dm1 = 3;
        
        RealMatrix T = new Array2DRowRealMatrix(D,D);
        for (int r=0 ; r<D ; ++r){
            for (int c=0 ; c<D ; ++c ){
                if(r == c){
                    T.setEntry(r,c,1.0);
                }
                else {
                    T.setEntry(r,c,0.0);
                }
            }
        }
        T.setEntry(Dm1,0, -xC);
        T.setEntry(Dm1,1, -yC);
        T.setEntry(Dm1,2, -zC);
        
        RealMatrix TT = T.transpose();
        
        RealMatrix C = new Array2DRowRealMatrix(D,D);
        for (int r=0 ; r<adjustedA.getRowDimension() ; ++r){
            for (int c=0 ; c<adjustedA.getColumnDimension() ; ++c ){
                C.setEntry(r, c, .8*adjustedA.getEntry(r, c));  // make the effective radii bigger to get separation of the nuclei
            }
        }
        for (int d=0 ; d<Dm1 ; ++d ){
            C.setEntry(Dm1, d, 0.0);
            C.setEntry(d, Dm1, 0.0);
        }
        C.setEntry(Dm1, Dm1, -1.0);
        
        return T.multiply(C.multiply(TT));
    }
    
    static public boolean intersect(NucleusData nuc1,NucleusData nuc2){
        RealMatrix qs1 = nuc1.quadraticSurfaceMatrix();
        RealMatrix qs2 = nuc2.quadraticSurfaceMatrix();
        
         LUDecomposition lu = new LUDecomposition(qs1);
         RealMatrix qs1Inv = lu.getSolver().getInverse();
         
         EigenDecomposition ed = new EigenDecomposition(qs1Inv.multiply(qs2));
         double[] realValues = ed.getRealEigenvalues();
         double[] imagValues = ed.getImagEigenvalues();
         
         
         // are there two distinct negative real eigenvalues
         
         for (int i=0 ; i<realValues.length-1 ; ++i){
            for (int j=i ; j<realValues.length ; ++j){
                if (realValues[i]<0.0 && realValues[j]<0.0 && imagValues[i]==0.0 && imagValues[j]==0.0 && realValues[i]!=realValues[j]){
                    return false;
                }
            }
         }
         int sahdfuihs=0;
        
        return true;
    }
    public void setTime(int t){
        this.time = t;
    }
    
    

    private int time;
    private String name;
    private double xC;
    private double yC;
    private double zC;
    
    private boolean marked = false;
    private boolean labeled = false;
    static Random rnd;
    double exp=100.0;
    
    RealMatrix A;  // unadjusted precision matrix
    EigenDecomposition eigenA;  // eigendecompensation of the unadjusted precsion matrix
    EigenDecomposition adjustedEigenA; 
    double[] R;  // the adjustments to apply to the axis of the ellipsoid
    RealMatrix adjustedA;  // the adjusted precsion matrix  
    double ff;
    List<InvalidationListener> listeners = new ArrayList<>();

    
    public class Coeff {
        double A;
        double B;
        double C;
        double D;
        double E;
        double F; 
    }
    public class Ellipse2d {
        double x;
        double y;
        double sine;
        double cosine;
        double a;
        double b;
        long[] low = new long[3];
        long[] high = new long[3];
    }
}
