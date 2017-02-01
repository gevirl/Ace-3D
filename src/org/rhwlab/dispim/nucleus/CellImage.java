/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import ij.process.LUT;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author gevirl
 */
public class CellImage {
    public BufferedImage getImage(Nucleus firstNuc,int maxTime,LUT lut,boolean nodes,boolean leaves,double timeScale,double cellWidth){
        
        this.lut = lut;
        this.maxTime = maxTime;
        this.labelNodes = nodes;
        this.labelLeaves = leaves;
        this.timeScale = timeScale;
        this.cellWidth = cellWidth;
        int h = (int)(timeScale*(maxTime - firstNuc.getTime() +1));
        HashSet<Nucleus> leafNucs = new HashSet<>();
        firstNuc.findLeaves(leafNucs);
        int w = (int)(cellWidth*(leafNucs.size()+1));
        BufferedImage image = new BufferedImage((int)w+20,(int)h,BufferedImage.TYPE_INT_ARGB);
        g2 = image.createGraphics();
        BasicStroke stroke = new BasicStroke(strokeWidth);
        g2.setStroke(stroke);
        locations.clear();
        drawCell(0,w,0,firstNuc);

        return image;
    }
    // draw a cell and its descendents into a portion of the bufferedimage
    // xLeft,width, and yStart determine the portion of the bufferimage to use
    // returns the x position where the cell was drawn
    private double drawCell(int xLeft,int width,double yStart,Nucleus firstNuc){
        double xStart = xLeft+width/2; // cell is drawn in middle if no children are drawn
        double c0 = 0;
        double c1 = 0;
        boolean hasChildren = false;
        Nucleus lastNuc = firstNuc.lastNucleusOfCell();
        // ?? draw children
        if (this.maxTime >= lastNuc.getTime()){
            
            double childY0  = yStart + (timeScale*(1+lastNuc.getTime()-firstNuc.getTime()));
            if (lastNuc.isDividing()){
                Nucleus[] nextNucs = lastNuc.nextNuclei();
                //draw the children
                Set<Nucleus> child0Leaves = new HashSet<>();
                nextNucs[0].findLeaves(child0Leaves);
                int n0 = child0Leaves.size();                
                Set<Nucleus> child1Leaves = new HashSet<>();
                nextNucs[1].findLeaves(child1Leaves);
                int n1 = child1Leaves.size();
                
                double f = (double)n0/(double)(n0+n1);
                int w0 = (int)(f*(double)width);
                int w1 = width - w0;
                c0 = drawCell(xLeft   ,w0,childY0,nextNucs[0]);
                c1 = drawCell(xLeft+w0,w1,childY0,nextNucs[1]);
                xStart = (c0+c1)/2;  // draw the parent in the middle of the children
                hasChildren = true;
            }            
        }

        // drawing the vertical line
        double y0 = yStart;
        double x0 = xStart;
        double yend = 0;
        int endt = Math.min(lastNuc.getTime(), this.maxTime);
        Nucleus currentNuc = firstNuc;
        for (int t = firstNuc.getTime() ; t <=endt ; ++t){
            double y1 = y0 + timeScale;
            Line2D.Double line = new Line2D.Double(x0,y0,x0,y1);

            int exp = (int)currentNuc.getExpression();
            int rgb = lut.getRGB(exp);
            Color c = new Color(rgb);
            g2.setColor(c);
            g2.draw(line);
            y0 = y1;
            yend = y0;
            currentNuc = currentNuc.getChild1();
        }
        CellLocation loc = new CellLocation(firstNuc,xStart,yStart,yend);
        locations.add(loc);
        if(hasChildren){
             // drawing the horizontal line
            Line2D.Double horiz = new Line2D.Double(c0, yend, c1, yend);
            g2.draw(horiz);
           
  //          System.out.printf("Horiz %s c0=%f,c1=%f,yend=%f\n", cell.getName(),c0,c1,yend);
        }
 //       System.out.printf("Vertical %s x=%f,y0=%f,y1=%f\n", cell.getName(),xStart,yStart,yend);
        if (lastNuc.isLeaf()){
            if (labelLeaves){
                labelCell(firstNuc.getCellName(),x0,(yStart+yend)/2.0);
            }
        } else if (labelNodes){
            labelCell(firstNuc.getCellName(),x0,(yStart+yend)/2.0);
        }
        return xStart;
    }
    private void labelCell(String label, double x,double y){
        g2.setColor(Color.BLACK);
        AffineTransform save = g2.getTransform();
        AffineTransform xform = (AffineTransform)save.clone();
       
        xform.translate(5.0+x,y);
        xform.rotate(-Math.PI/4.0);
        g2.setTransform(xform);
        float zero = (float)0.0;
        g2.drawString(label,zero,zero);
        g2.setTransform(save);
    }
    public CellLocation cellAtLocation(int x,int y){
        CellLocation ret = null;
        double dMin = Double.MAX_VALUE;
        for (CellLocation loc : locations){
            if (loc.y0<=y && y<=loc.y1){
                double d = Math.abs(loc.x-x);
                if (d <dMin){
                    dMin = d;
                    ret = loc;
                }
            }
        }
        return ret;
    }
    public class CellLocation {
        public CellLocation(Nucleus firstNuc,double x,double y0,double y1){
            this.firstNuc = firstNuc;
            this.x = x;
            this.y0 = y0;
            this.y1 = y1;
        }
        public Nucleus firstNuc;
        public double x;
        public double y0;
        public double y1;
    }
    int maxTime;
    boolean labelNodes;
    boolean labelLeaves;
    LUT lut;
    Graphics2D g2;
    static int strokeWidth=3;
    double timeScale;
    double cellWidth;
    Set<CellLocation> locations = new HashSet<>();
}
