/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.process.LUT;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.TreeMap;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.CellImage;
import org.rhwlab.dispim.nucleus.CellImage.CellLocation;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class NavigationTreePanel extends JPanel implements ChangeListener{
    public NavigationTreePanel(ImagedEmbryo emb){
        lut = LUT.createLutFromColor(Color.GREEN);
        embryo = emb;
        lut.min = 0;
        lut.max = 255;
        
        this.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e){
                int x = e.getX();
                int y = e.getY();
                int offset = 0;
                int index = -1;
                for (int i=0 ; i<cellImage.length ; ++i){
                    if (x < offset + buffered[i].getWidth()) {
                        index = i;
                        break;
                    }
                    offset = offset + buffered[i].getWidth();
                }
                y = y -(int)(roots[index].getTime()*headPanel.getTimeScale());
                CellLocation cellLoc = cellImage[index].cellAtLocation(x-offset, y);
                if (cellLoc != null){
                    Nucleus firstNuc = cellLoc.firstNuc;
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        TreeMap<Integer,Nucleus> desc = new TreeMap<>();
                        firstNuc.descedentsInCell(desc);
                        int t0 = desc.firstKey();
                        int t1 = desc.lastKey();
                        double f = (y-cellLoc.y0)/(cellLoc.y1-cellLoc.y0);
                        
                        int t = t0 + (int)(f*(t1-t0));
                        Nucleus nuc = desc.get(t);
                        embryo.setSelectedNucleus(nuc);
                    } else if (e.getButton()==MouseEvent.BUTTON3){
                        Nucleus nuc = firstNuc.lastNucleusOfCell();
                        embryo.setSelectedNucleus(nuc);                        
                    }
                }
                
            }
        });

        
        this.addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseMoved(MouseEvent e){
                int[] adj = adjustedLocation(e);
                if (adj == null) return;
                if (cellImage != null){
                   CellLocation cellLoc = cellImage[adj[2]].cellAtLocation(adj[0], adj[1]);
                    if (cellLoc != null){
                        Nucleus nuc = nucleusAtCellLocation(cellLoc,adj[1]);
                        if (nuc != null){
                            headPanel.setNucleus(nuc.getFullName());
 //                           System.out.printf("%s\n",nuc.getName());
                        } else {
 //                           System.out.println("Nucleus null");
                        }
                    }else {
//                        System.out.println("CellLoc null");
                    }  
                }
            }
        });

    }
    public int[] adjustedLocation(MouseEvent e){
        if (cellImage== null) return null;
        int[] ret = new int[3];
        int x = e.getX();
        int y = e.getY();
        int offset = 0;
        int index = -1;
        for (int i=0 ; i<cellImage.length ; ++i){
            if (x < offset + buffered[i].getWidth()) {
                index = i;
                break;
            }
            offset = offset + buffered[i].getWidth();
        }
        if (index == -1){
            return null;
        }
        y = y -(int)(roots[index].getTime()*headPanel.getTimeScale());  
        ret[0] = x-offset;
        ret[1] = y;
        ret[2] = index;
        return ret;
    }
    
    public Nucleus nucleusAtCellLocation(CellLocation cellLoc,int y){
        Nucleus firstNuc = cellLoc.firstNuc;
        TreeMap<Integer,Nucleus> desc = new TreeMap<>();
        firstNuc.descedentsInCell(desc);
        int t0 = desc.firstKey();
        int t1 = desc.lastKey();
        double f = (y-cellLoc.y0)/(cellLoc.y1-cellLoc.y0);

        int t = t0 + (int)(f*(t1-t0));
    //    System.out.printf("Time %d\n",t);
        Nucleus nuc = desc.get(t);   
        return nuc;
    }
    @Override
    public void stateChanged(ChangeEvent e) {
        if (e.getSource() instanceof JTree){
            JTree jTree = (JTree)e.getSource();
            treePaths = jTree.getSelectionPaths();
            if (treePaths == null) {
                return;
            }            
        } 
        if (treePaths  == null) {
            return;
        }

        cellImage = new CellImage[treePaths.length];
        buffered = new BufferedImage[treePaths.length];
        roots = new Nucleus[treePaths.length];
        int W = 0;
        int H = -1;
        for (int i=0 ; i<treePaths.length ; ++i){
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)treePaths[i].getPathComponent(1);
            roots[i] = (Nucleus)node.getUserObject();
            cellImage[i] = new CellImage();
            buffered[i] = cellImage[i].getImage(roots[i],headPanel.getMaxTime(),lut,headPanel.labelNodes(),headPanel.labelLeaves(),
                    headPanel.getTimeScale(),headPanel.getCellWidth());
            W = W + buffered[i].getWidth();
            int h = buffered[i].getHeight();
            if (h > H){
                H = h;
            }
        }
 /*       
        NavigationHeaderPanel headPanel = (NavigationHeaderPanel)e.getSource();
        rootNuc = headPanel.getRoot();
        if (rootNuc == null)return;
        cellImage = new CellImage();
        buffered = cellImage.getImage(rootNuc,headPanel.getMaxTime(),lut,headPanel.labelNodes(),headPanel.labelLeaves(),
                headPanel.getTimeScale(),headPanel.getCellWidth());
        int h = buffered.getHeight();
        int w = buffered.getWidth(); 
*/
        this.setSize(W,H);
        this.setPreferredSize(new Dimension(W,H));

        this.invalidate();
        this.repaint();
    }
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (embryo == null){
            return;
        }
        NucleusFile nucFile = embryo.getNucleusFile();
        if (nucFile == null){
            return;
        }
        if (buffered == null){
            return;
        }
        Graphics2D g2 = (Graphics2D) g;

        Dimension d = this.getSize();
//        double scale = Math.min((double)d.width/(double)w, (double)d.height/(double)h);
  //      System.out.printf("Scale=%f,d.w=%d,d.h=%d,w=%d.h=%d)",scale,d.width,d.height,w,h);
        // clear the panel
        Color save = g2.getColor();
        g2.setColor(Color.white);
 
        g2.fillRect(0,0,d.width,d.height);
        g2.setColor(save);
        
//        AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
        AffineTransform xForm = new AffineTransform();
        int xPos =0;
        for (int i=0 ; i<buffered.length ; ++i){
            g2.drawImage(buffered[i],new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),xPos,(int)(roots[i].getTime()*headPanel.getTimeScale()));    
            xPos = xPos + buffered[i].getWidth();
        }
    }
    public void setHeadPanel(NavigationHeaderPanel headPanel){
        this.headPanel=headPanel;
    }
    TreePath[] treePaths;
    NavigationHeaderPanel headPanel;
    LUT lut;
    ImagedEmbryo embryo;
    BufferedImage[] buffered;
    CellImage[] cellImage;
    Nucleus[] roots;
    String nucName;
    

}
