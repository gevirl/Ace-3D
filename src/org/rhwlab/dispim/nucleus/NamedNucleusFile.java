package org.rhwlab.dispim.nucleus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.descriptive.moment.VectorialCovariance;
import org.apache.commons.math3.stat.descriptive.moment.VectorialMean;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public class NamedNucleusFile extends LinkedNucleusFile{
    public NamedNucleusFile(){
        super();
        if (divisionMap == null){
            divisionMap = new TreeMap<>();
    
            InputStream s = this.getClass().getResourceAsStream("/org/rhwlab/dispim/nucleus/NewRules.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(s));
            try {
                String line = reader.readLine();
                line = reader.readLine();
                while (line != null){
                    String[] tokens = line.split("\t");
                    double[] v = new double[3];
                    v[0] = Double.valueOf(tokens[4]);
                    v[1] = Double.valueOf(tokens[5]);
                    v[2] = Double.valueOf(tokens[6]);
                    Division div = new Division(tokens[2],tokens[3],v);
                    divisionMap.put(tokens[0],div);
                    line = reader.readLine();
                }
            } catch (Exception exc){
                exc.printStackTrace();
            } 
        } 
        if (specialMap == null){
            specialMap = new TreeMap<>();
            InputStream s = this.getClass().getResourceAsStream("/org/rhwlab/dispim/nucleus/SpecialRules.csv");
            BufferedReader reader = new BufferedReader(new InputStreamReader(s));
            try {
                String line = reader.readLine();
                line = reader.readLine();
                while (line != null){
                    if (!line.startsWith("#")){
                        String[] tokens = line.split(",");
                        SpecialDivision special = new SpecialDivision(tokens);
                        specialMap.put(special.parent,special);
                    }
                    line = reader.readLine();
                }
            } catch (Exception exc){
                exc.printStackTrace();
            }             
        }
    }
    // toogle the name of the containing cell of the given nucleus with the sister cell
    public void toggleCellName(Nucleus nuc,boolean notify){
        Nucleus first = nuc.firstNucleusInCell();
        if (first.getParent() == null){
            return; // cannot toggle names of a root cell = there is no sister
        }

        Nucleus parent = first.getParent();
        Nucleus c1 = parent.getChild1();
        String c1Name = c1.getCellName();
        Nucleus c2 = parent.getChild2();
        String c2Name = c2.getCellName();
        parent.setDaughters(c2, c1);
        
        Nucleus.nameCellRecursive(parent.getChild1(), c1Name, true);
        Nucleus.nameCellRecursive(parent.getChild2(), c2Name, true);
        
        // name the children using the embryo orientation
        nameChildren(c1);
        nameChildren(c2);
        
        if (notify){
            this.notifyListeners();
        }
    }

    // name the children of the given nucleus using division file and the embryo orientatation rotation matrix(if confiremed)
    // if the roation matrix has not been confirmed the names are random
    static public void nameChildren(Nucleus nuc){
        if (nuc.getCellName().equals("P2")){
            int asidfsud=0;
        }
        Nucleus last = LinkedNucleusFile.lastNucleusInCell(nuc);
        if (last.isLeaf()) return ;  // no children
        
        Division div = divisionMap.get(last.getCellName());
        if (div != null) {
            if (R != null){
                Nucleus c1 = last.getChild1();
                Nucleus c2 = last.getChild2(); 
                
                RealVector v1 = R.operate(homogeneous(c1.getCenter()));
                RealVector v2 = R.operate(homogeneous(c2.getCenter()));
                RealVector delV = v2.subtract(v1);
                
                RealVector divDir = div.getVector();
/*               
                RealVector direction = v2.subtract(v1);
                double dot = direction.dotProduct(divDir);
                double d0 = R.operate(direction).dotProduct(div.getVector());
                double d1 = R.operate(direction.mapMultiply(-1)).dotProduct(div.getVector());
*/
                // special division??
                
                SpecialDivision special = specialMap.get(last.getCellName());
                
                if (special == null){
                
                    if (div.child2.endsWith("p") && v2.getEntry(0)-v1.getEntry(0) < 0.0 ){
                        last.setDaughters(c2, c1);  //swap the daughters
                    } else if (div.child2.endsWith("v") && v2.getEntry(2)-v1.getEntry(2) < 0.0 ){
                        last.setDaughters(c2, c1);  //swap the daughters
                    } else if (div.child2.endsWith("r") && v2.getEntry(1)-v1.getEntry(1) < 0.0 ){
                        last.setDaughters(c2, c1);  //swap the daughters
                    }
                } else{
                    int index = Math.abs(special.value)-1;
                    if (special.value*(v2.getEntry(index)-v1.getEntry(index)) < 0.0){
                        last.setDaughters(c2, c1);  //swap the daughters
                    }
                }
            }
            Nucleus.nameCellRecursive(last.getChild1(),div.child1, false);
            Nucleus.nameCellRecursive(last.getChild2(),div.child2, false);   
        } else {
            // unknown division - can't rename the children
            Nucleus.nameCellRecursive(last.getChild1(),null, false);
            Nucleus.nameCellRecursive(last.getChild2(),null, false);             
        }
        // rename nuclei in the subtrees
        nameChildren(last.getChild1());
        nameChildren(last.getChild2());
  
    } 
    static public RealVector homogeneous(double[] v){
        ArrayRealVector ret = new ArrayRealVector(v.length+1);
        for (int i=0 ; i<v.length ; ++i){
            ret.setEntry(i, v[i]);
        }
        ret.setEntry(v.length,1.0);
        return ret;
    }
    public RealMatrix orientEmbryoByFourCells(int time){
        RealVector P2 = null;
        RealVector ABa = null;
        RealVector EMS = null;
        RealVector ABp = null;
        // get the four cells at the given time
        Set<Nucleus> nucs = this.getNuclei(time);
        ArrayList<RealVector> four = new ArrayList<>();
        for (Nucleus nuc : nucs){
            for (String cell : fourCells){
                if (nuc.getCellName().equals(cell)){
                    four.add(new ArrayRealVector(nuc.getCenter()));
                    if (nuc.getCellName().equals("P2")){
                        P2 = homogeneous(nuc.getCenter());
                    }
                    else if (nuc.getCellName().equals("ABa")){
                        ABa = homogeneous(nuc.getCenter());
                    }
                    else if (nuc.getCellName().equals("ABp")){
                        ABp = homogeneous(nuc.getCenter());
                    } 
                    else if (nuc.getCellName().equals("EMS")){
                        EMS = homogeneous(nuc.getCenter());
                    }                    
                }
            }
        }
        if (four.size() != 4) return null;
        
        // find the mean location of the four cells
        RealVector mu = new ArrayRealVector(3);
        for (RealVector f : four){
            mu = mu.add(f);
        }
        mu = mu.mapDivideToSelf(4.0);
System.out.printf("mu: (%.0f,%.0f,%.0f)\n ", mu.getEntry(0),mu.getEntry(1),mu.getEntry(2));
        
        // normal to the plane fitting the four
        RealMatrix C = new Array2DRowRealMatrix(3,3);
        for (RealVector v : four){
            RealVector del = v.subtract(mu);
            C = C.add(del.outerProduct(del));
        }
        EigenDecomposition eigen = new EigenDecomposition(C);
        double[] real = eigen.getRealEigenvalues();
        double[] imag = eigen.getImagEigenvalues();
        RealVector N = eigen.getEigenvector(2);
        double u = N.getEntry(0);
        double v = N.getEntry(1);
        double w = N.getEntry(2);
        
System.out.printf("N: (%f,%f,%f)\n",u,v,w);

        Array2DRowRealMatrix Txz = new Array2DRowRealMatrix(4,4);  //rotate around z-axis to the xz plane
        double denom2 = Math.sqrt(u*u + v*v);
        Txz.setEntry(0,0,u/denom2);
        Txz.setEntry(1,1,u/denom2);
        Txz.setEntry(1,0,-v/denom2);
        Txz.setEntry(0,1,v/denom2);
        Txz.setEntry(2,2,1.0);
        Txz.setEntry(3,3,1.0);
        
        Array2DRowRealMatrix Tz = new Array2DRowRealMatrix(4,4);  // rotate around y-axis to line up with the z-axis
        double denom = Math.sqrt(u*u + v*v + w*w);
        Tz.setEntry(0,0,w/denom);
        Tz.setEntry(2,0,denom2/denom);
        Tz.setEntry(1,1,1.0);
        Tz.setEntry(0,2,-denom2/denom);
        Tz.setEntry(2,2,w/denom);
        Tz.setEntry(3,3,1.0);
        
        Array2DRowRealMatrix Tp = new Array2DRowRealMatrix(4,4);  // translate mean to the origin
        Tp.setEntry(0,0,1.0);
        Tp.setEntry(1,1,1.0);
        Tp.setEntry(2,2,1.0);
        Tp.setEntry(3,3,1.0);
        Tp.setEntry(0,3,-mu.getEntry(0));
        Tp.setEntry(1,3,-mu.getEntry(1));
        Tp.setEntry(2,3,-mu.getEntry(2));
     
        Array2DRowRealMatrix Tb = new Array2DRowRealMatrix(4,4);  // translate back to mean 
        Tb.setEntry(0,0,1.0);
        Tb.setEntry(1,1,1.0);
        Tb.setEntry(2,2,1.0);
        Tb.setEntry(3,3,1.0);
        Tb.setEntry(0,3,mu.getEntry(0));
        Tb.setEntry(1,3,mu.getEntry(1));
        Tb.setEntry(2,3,mu.getEntry(2));   
        
        RealMatrix Tfirst = Tb.multiply(Tz.multiply(Txz.multiply(Tp)));  // normal to the 4 cell plane is oriented with z-axiz, 
        
        // apply the first transform to P2 and ABa
        RealVector P2f = Tfirst.operate(P2);
        RealVector ABaf = Tfirst.operate(ABa);
     
        Array2DRowRealMatrix Txy = new Array2DRowRealMatrix(4,4);  // rotate in xy plane around z axis
        RealVector delta = P2f.subtract(ABaf);
        u = delta.getEntry(0);
        v = delta.getEntry(1);
        
        u = delta.getEntry(1);
        v = delta.getEntry(0);
        
        denom2 = Math.sqrt(u*u + v*v);
        Txy.setEntry(0,0,-v/denom2);
        Txy.setEntry(1,1,-v/denom2);
        Txy.setEntry(1,0,-u/denom2);
        Txy.setEntry(0,1,u/denom2);

        Txy.setEntry(0,0,v/denom2);
        Txy.setEntry(1,1,v/denom2);
        Txy.setEntry(1,0,-u/denom2);
        Txy.setEntry(0,1,u/denom2);
        
        Txy.setEntry(2,2,1.0);
        Txy.setEntry(3,3,1.0);  
        
        Array2DRowRealMatrix TABa = new Array2DRowRealMatrix(4,4); // translate ABapp to origin
        TABa.setEntry(0,0,1.0);
        TABa.setEntry(1,1,1.0);
        TABa.setEntry(2,2,1.0);
        TABa.setEntry(3,3,1.0);
        TABa.setEntry(0,3,-ABaf.getEntry(0));
        TABa.setEntry(1,3,-ABaf.getEntry(1));
        TABa.setEntry(2,3,-ABaf.getEntry(2));        
        
        Array2DRowRealMatrix TABab = new Array2DRowRealMatrix(4,4); // translate ABapp to back
        TABab.setEntry(0,0,1.0);
        TABab.setEntry(1,1,1.0);
        TABab.setEntry(2,2,1.0);
        TABab.setEntry(3,3,1.0);
        TABab.setEntry(0,3,ABaf.getEntry(0));
        TABab.setEntry(1,3,ABaf.getEntry(1));
        TABab.setEntry(2,3,ABaf.getEntry(2));         
        
        RealMatrix Tsecond = TABab.multiply(Txy.multiply(TABa));
        
        // rotate 90 degrees around the x axis       
        RealVector ABpfs = Tsecond.operate(Tfirst.operate(ABp));
        RealVector EMSfs = Tsecond.operate(Tfirst.operate(EMS));
        RealVector ABas = Tsecond.operate(ABaf);
        
        Array2DRowRealMatrix TABas = new Array2DRowRealMatrix(4,4); // translate ABas to origin
        TABas.setEntry(0,0,1.0);
        TABas.setEntry(1,1,1.0);
        TABas.setEntry(2,2,1.0);
        TABas.setEntry(3,3,1.0);
        TABas.setEntry(0,3,-ABas.getEntry(0));
        TABas.setEntry(1,3,-ABas.getEntry(1));
        TABas.setEntry(2,3,-ABas.getEntry(2));        

        Array2DRowRealMatrix TABasb = new Array2DRowRealMatrix(4,4); // translate  back to ABas
        TABasb.setEntry(0,0,1.0);
        TABasb.setEntry(1,1,1.0);
        TABasb.setEntry(2,2,1.0);
        TABasb.setEntry(3,3,1.0);
        TABasb.setEntry(0,3,ABas.getEntry(0));
        TABasb.setEntry(1,3,ABas.getEntry(1));
        TABasb.setEntry(2,3,ABas.getEntry(2));    
        
        RealMatrix Tx = new Array2DRowRealMatrix(4,4);
        Tx.setEntry(0,0,1);
        Tx.setEntry(1,1,0);
        Tx.setEntry(2,2,0);
        Tx.setEntry(3,3,1);
        if (ABpfs.getEntry(1) < EMSfs.getEntry(1)){
            Tx.setEntry(1,2,-1);
            Tx.setEntry(2,1,1);
        }else {
            Tx.setEntry(1,2,1);
            Tx.setEntry(2,1,-1);            
        }
        Tsecond = TABasb.multiply(Tx.multiply(TABas.multiply(Tsecond)));
/*        
        if (ABpfs.getEntry(0) > EMSfs.getEntry(0)){
            RealVector ABas = Tsecond.operate(ABaf);
            Array2DRowRealMatrix TABas = new Array2DRowRealMatrix(4,4); // translate ABas to origin
            TABas.setEntry(0,0,1.0);
            TABas.setEntry(1,1,1.0);
            TABas.setEntry(2,2,1.0);
            TABas.setEntry(3,3,1.0);
            TABas.setEntry(0,3,-ABas.getEntry(0));
            TABas.setEntry(1,3,-ABas.getEntry(1));
            TABas.setEntry(2,3,-ABas.getEntry(2));        

            Array2DRowRealMatrix TABasb = new Array2DRowRealMatrix(4,4); // translate  back to ABas
            TABasb.setEntry(0,0,1.0);
            TABasb.setEntry(1,1,1.0);
            TABasb.setEntry(2,2,1.0);
            TABasb.setEntry(3,3,1.0);
            TABasb.setEntry(0,3,ABas.getEntry(0));
            TABasb.setEntry(1,3,ABas.getEntry(1));
            TABasb.setEntry(2,3,ABas.getEntry(2)); 

            Array2DRowRealMatrix Tflip = new Array2DRowRealMatrix(4,4); 
            Tflip.setEntry(0,0,-1.0);
            Tflip.setEntry(1,1,1.0);
            Tflip.setEntry(2,2,-1.0);
            Tflip.setEntry(3,3,1.0);
            Tsecond = TABasb.multiply(Tflip.multiply(TABas.multiply(Tsecond)));
        }
*/        
        RealMatrix T = Tsecond.multiply(Tfirst);
        for (Nucleus nuc : nucs){
            RealVector vec = homogeneous(nuc.getCenter());
            RealVector vecp =T.operate(vec); 
            System.out.printf("%s: (%.0f,%.0f,%.0f)  (%.0f,%.0f,%.0f)\n",
                    nuc.getCellName(),vec.getEntry(0),vec.getEntry(1),vec.getEntry(2),
                    vecp.getEntry(0),vecp.getEntry(1),vecp.getEntry(2));
        }
        return T;
    }
/*    
    public RealMatrix orientEmbryo(int time){
        Nucleus selected = this.getSelected();
        Nucleus sister = selected.getSister();
        if (sister == null) return null;
        Nucleus parent = selected.getParent();
               
        Vector3D cellDirection;
        Division div = divisionMap.get(parent.getCellName());
        if (div.child1.equals(selected.getCellName())){
            cellDirection = divisionDirection(selected,sister);
        }else {
            cellDirection = divisionDirection(sister,selected);
        }
        RealMatrix rotMat =  rotationMatrix(cellDirection,div.getV());
        return rotMat;
        
    }
*/
    // determine the direction of a division, given the two just divided nuclei
    static public RealVector divisionDirection(Nucleus nuc1,Nucleus nuc2){
        RealVector v0 = homogeneous(nuc1.getCenter());
        RealVector v1 = homogeneous(nuc2.getCenter());
        return v1.subtract(v0);
    }    
    //find the rotation matrix that rotates the A vector onto the B vector
    static public RealMatrix rotationMatrix(Vector3D A,Vector3D B){
        Vector3D a = A.normalize();
        Vector3D b = B.normalize();
        Vector3D v = a.crossProduct(b);
        
        double s = v.getNormSq();
        double c = a.dotProduct(b);
        
        RealMatrix vx = MatrixUtils.createRealMatrix(3, 3);
        vx.setEntry(1, 0, v.getZ());
        vx.setEntry(0, 1, -v.getZ());
        vx.setEntry(2, 0, -v.getY());
        vx.setEntry(0, 2, v.getY());
        vx.setEntry(2, 1, v.getX());
        vx.setEntry(1, 2, -v.getX());  

        RealMatrix vx2 = vx.multiply(vx);
        RealMatrix scaled = vx2.scalarMultiply((1.0-c)/s);
        
        RealMatrix ident = MatrixUtils.createRealIdentityMatrix(3);
        RealMatrix sum = vx.add(scaled);
        RealMatrix ret = ident.add(sum);

        return ret;
    } 
    static public void setOrientation(RealMatrix r){
        R = r;
    }
    @Override
    public void fromXML(Element nucleiEle) {
        super.fromXML(nucleiEle);
        
        String orient = nucleiEle.getAttributeValue("orientation");
        if (orient != null){
            R = NucleusData.precisionFromString(orient);
        }
    }    
    @Override
    public Element toXML(){
        Element ret = super.toXML();
        if (R != null){
            StringBuilder builder = new StringBuilder();
            for (int row=0 ; row<R.getRowDimension() ; ++row){
                for (int col=0 ; col<R.getColumnDimension() ; ++col){
                    if (row>0 || col>0){
                        builder.append(" ");
                    }
                    builder.append(R.getEntry(row, col));
                }
            }
            ret.setAttribute("orientation", builder.toString());
        }
        return ret;
    }
    
    public void divisionReport(PrintStream stream){
        stream.println("Time\t  ParentCell\td\td1\td2\tvol");
        for (Integer time : this.byTime.keySet()){
            Set<Nucleus> nucs = this.getNuclei(time);
            for (Nucleus nuc : nucs){
                if (nuc.isDividing()){
                    Nucleus[] children = nuc.nextNuclei();
                    double d1 = nuc.distance(children[0]);
                    double d2 = nuc.distance(children[1]);
                    double d = children[0].distance(children[1]);
                    double volRatio = children[0].getVolume()/children[1].getVolume();
                    if (volRatio < 1.0) volRatio = 1.0/volRatio;
                    stream.printf("%d\t%12s\t%.2f\t%.2f\t%.2f\t%.2f\n",time,nuc.getCellName(),d,d1,d2,volRatio);
                }
            }
        }
    }
    public void timeLinkageReport(PrintStream stream){
        stream.println("Time\t  ParentCell\td\tvol");
        double dMax = 0.0;
        String dMaxCell = null;
        String volMaxCell = null;
        double volMax = 0.0;
        for (Integer time : this.byTime.keySet()){
            Set<Nucleus> nucs = this.getNuclei(time);
            for (Nucleus nuc : nucs){
                if (!nuc.isDividing()&&!nuc.isLeaf()){
                    Nucleus[] children = nuc.nextNuclei();
                    double d = nuc.distance(children[0]);
                    double volRatio = nuc.getVolume()/children[0].getVolume();
                    if (volRatio < 1.0) volRatio = 1.0/volRatio;
                    if (d > dMax ){
                        dMax = d;
                        dMaxCell = nuc.getName();
                    }
                    if (volRatio > volMax){
                        volMax = volRatio;
                        volMaxCell = nuc.getName();
                    }
                    stream.printf("%d\t%12s\t%.2f\t%.2f\n",time,nuc.getCellName(),d,volRatio);
                }
            }
        }
        stream.printf("\n\n%s\tDistance max = %.2f\n",dMaxCell,dMax);
        stream.printf("%s\tVolume Ratio max = %.2f\n",volMaxCell,volMax);
    }
    static String[] fourCells = {"ABa","ABp","EMS","P2"};
    static RealMatrix R;  // embryo tranformation matrix -  aligns the embryo with the divisions file
    static TreeMap<String,Division> divisionMap; 
    static TreeMap<String,SpecialDivision> specialMap;

    class Division{
        public Division(String d1,String d2,double[] v){
            this.child1 = d1;
            this.child2 = d2;
            this.v = v;
        }
        public Vector3D getV(){
            return new Vector3D(v);
        }
        public RealVector getVector(){
            return homogeneous(v);
        }
        String child1;
        String child2;
        double[] v;
    }
    class SpecialDivision {
        public SpecialDivision(String p,String c1,String c2,int v){
            this.parent = p;
            this.child1 = c1;
            this.child2 = c2;
            this.value = v;
        }
        public SpecialDivision(String[] tokens){
            parent = tokens[0];
            child1 = tokens[1];
            child2 = tokens[2];
            value = Integer.valueOf(tokens[3]);
        }
        String parent;
        String child1;
        String child2;
        int value;
    }
}