/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
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
    
//            InputStream s= ClassLoader.getSystemResourceAsStream("/NewRules.txt");
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
        Nucleus last = LinkedNucleusFile.lastNucleusInCell(nuc);
        if (last.isLeaf()) return ;  // no children
        
        Division div = divisionMap.get(last.getCellName());
        if (div != null) {
            if (R != null){
                Nucleus c1 = last.getChild1();
                Nucleus c2 = last.getChild2(); 
                Vector3D direction = divisionDirection(c1,c2);
                double d0 = new Vector3D(R.operate(direction.toArray())).dotProduct(div.getV());
                double d1 = new Vector3D(R.operate(direction.scalarMultiply(-1.0).toArray())).dotProduct(div.getV());
                if (d0 < d1)            {
                    Nucleus parent = c1.getParent();
                    parent.setDaughters(c2, c1);  //swap the daughters
                }
            }
            Nucleus.nameCellRecursive(last.getChild1(),div.child1, false);
            Nucleus.nameCellRecursive(last.getChild2(),div.child2, false);   
        } else {
            Nucleus.nameCellRecursive(last.getChild1(),null, false);
            Nucleus.nameCellRecursive(last.getChild2(),null, false);             
        }
        // rename nuclei in the subtrees
        nameChildren(last.getChild1());
        nameChildren(last.getChild2());
  
    }   
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
/*        
        // get all the cells at the given time
        Set<Nucleus> nucs = this.getNuclei(time);
        TreeMap<String,Nucleus> nucMap = new TreeMap<>();
        TreeSet<String> nucNames = new TreeSet<>();
        for (Nucleus nuc : nucs){
            nucNames.add(nuc.getCellName());
            nucMap.put(nuc.getCellName(),nuc);
        }
        
        // find all divisions that could have generated the cells at this time
        TreeMap<String,Vector3D> cellDirs = new TreeMap<>();
        Vector3D cellDirection = new Vector3D(0,0,0);
        ArrayList<Division> divList = new ArrayList<>();
        for (Division div : divisionMap.values()){
            if (nucNames.contains(div.child1) && nucNames.contains(div.child2)){
                divList.add(div);
                
                Vector3D cellDir = divisionDirection(nucMap.get(div.child1), nucMap.get(div.child2));
                cellDir = cellDir.normalize();
                cellDirs.put(div.child1,cellDir);
                cellDirection = cellDirection.add(cellDir);
            }
        }
        // add up all the vectors
        Vector3D divDirection = new Vector3D(0,0,0);
        for (Division div : divList){
            divDirection = divDirection.add(div.getV());
        }
        RealMatrix rotMat =  rotationMatrix(cellDirection,divDirection);
        
        Vector3D div = divList.get(0).getV();
        Vector3D cell = cellDirs.get(divList.get(0).child1);
        rotMat =  rotationMatrix(cell,div);
        
        for (String key : cellDirs.keySet()){
            Vector3D direct = cellDirs.get(key);
           double[] result = rotMat.operate(direct.toArray());
           double sum = 0.0;
           for (int i=0 ; i<result.length ; ++i){
                sum = sum + result[i]*result[i];
           }
           sum = Math.sqrt(sum);
           for (int i=0 ; i<result.length ; ++i){
                result[i] = result[i]/sum;
           }           
           int jasdfuisd=0;
        }
        return rotMat;
*/        
    }
/*
    // use a nucleus to determine the rotation matrix for the embryo
    public RealMatrix rotationMatrix(Nucleus nuc){
        RealMatrix ret = null;
        
        Nucleus last = this.lastNucleusInCell(nuc);
        if (last.isLeaf()) return ret;  // no children
        
        Division div = divisionMap.get(last.getCellName());
        if (div == null) return ret; // cannot use an unknown cell
        
        Nucleus[] children = last.nextNuclei();
        Vector3D A= divisionDirection(children[0],children[1]);        
        Vector3D B = div.getV();
        ret = rotationMatrix(A,B);        
        return ret;
    }
*/
    // determine the direction of a division, given the two just divided nuclei
    static public Vector3D divisionDirection(Nucleus nuc1,Nucleus nuc2){
        double[] p0 = nuc1.getCenter();
        Vector3D v0 = new Vector3D(p0[0],p0[1],p0[2]);
        
        double[] p1 = nuc2.getCenter();
        Vector3D v1 = new Vector3D(p1[0],p1[1],p1[2]);

        return v1.subtract(v0)        ;
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
    static RealMatrix R;  // embryo tranformation matrix -  aligns the embryo with the divisions file
    static TreeMap<String,Division> divisionMap;   

    class Division{
        public Division(String d1,String d2,double[] v){
            this.child1 = d1;
            this.child2 = d2;
            this.v = v;
        }
        public Vector3D getV(){
            return new Vector3D(v);
        }
        String child1;
        String child2;
        double[] v;
    }
}
