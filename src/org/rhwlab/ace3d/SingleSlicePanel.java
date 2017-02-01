/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.rhwlab.dispim.CompositeTimePointImage;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.TimePointImage;
import org.rhwlab.dispim.nucleus.Nucleus;
import org.rhwlab.dispim.nucleus.NucleusData;
import org.rhwlab.dispim.nucleus.NucleusFile;

/**
 *
 * @author gevirl
 */
public class SingleSlicePanel extends JPanel implements ChangeListener {
    public SingleSlicePanel(String ttl,int d,final SynchronizedMultipleSlicePanel par){
        this.parent = par;
        this.title = ttl;
        this.dim =d;
        this.setLayout(new BorderLayout());
        this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED) );
        slider = new JSlider();
        slider.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                SingleSlicePanel.this.setSlice(slider.getValue());
                 if (parent != null && timePointImage != null) parent.changePosition(dim,slice);                
            }
        });
        this.add(slider,BorderLayout.SOUTH);
        slicePanel = new JPanel(){
            
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                Graphics2D panelg2 = (Graphics2D)g;
                Dimension panelSize = this.getSize();
//                System.out.printf("panel(%d,%d)\n",panelSize.width,panelSize.height);

                if (timePointImage != null){                    
 //                   Graphics2D g2 = (Graphics2D) g;
                    buffered = timePointImage.getBufferedImage(dim,slice);
                    if (buffered == null){
                        return;
                    }
                    Graphics2D g2 = buffered.createGraphics();
                    if (buffered == null){
                        return ;
                    }
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);                   
                    bufH = buffered.getHeight();
                    bufW = buffered.getWidth();

                    double rw,rh;
                    if (dim ==0){
                        rw = (double)panelSize.width/(double)bufH;
                        rh = (double)panelSize.height/(double)bufW;                       
                    }else {
                         rw = (double)panelSize.width/(double)bufW;
                        rh = (double)panelSize.height/(double)bufH;                        
                    }

                    scale = Math.max(1.0, Math.min(rw, rh));

//                    AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
//                    g2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);

                    // draw lines showing the current position
                    if (Ace3D_Frame.locationIndicated()){
                        long xPos = screenX(imagePosition);
                        long yPos = screenY(imagePosition);
                        Stroke dotted = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {3}, 0);
                        Graphics2D g2copy = (Graphics2D) g2.create();
                        g2copy.setStroke(dotted);
                        g2copy.setColor(new Color(1f,1f,1f,.5f));
                        g2copy.drawLine(0, (int)(yPos), (int)(bufW),(int)(yPos));
                        g2copy.drawLine((int)(xPos), 0,(int)(xPos), (int)(bufH));
                        g2copy.dispose();                      
                    }
                    // add an axis identifyer
                    g2.drawLine(10,5,20,5);  //horizontal
                    g2.drawString(horizontalAxis(), 25,10);
                    g2.drawLine(10,5,10,15);  //vertical
                    g2.drawString(verticalAxis(),10, 25);
                    
                    
                   // draw any nuclei
                   if (Ace3D_Frame.nucleiIndicated()){
                       drawNuclei(g2);
//                       g2.setColor(save);
                   }

                   if (Ace3D_Frame.sistersIndicated()){
                        g2.setColor(Color.GREEN);
                        drawSisters(g2);  // draw the sister indicator   
//                        g2.setColor(save);
                   }     
                   if (Ace3D_Frame.divisionsIndicated()){
                        g2.setColor(Color.GREEN);
                        drawDivisions(g2);  // draw the sister indicator   
//                        g2.setColor(save);
                   }                    
                   if (Ace3D_Frame.labelNuclei()){
                        g2.setColor(Color.GREEN);
                        labelAllNuclei(g2);
//                        g2.setColor(save);
                   }
                   if (Ace3D_Frame.labelSelectedNucleus()){
                       g2.setColor(Color.RED);
                       labelSelectedNucleus(g2);
//                       g2.setColor(save);
                   }
                   g2.setColor(Color.BLUE);
                   labelMarkedNuclei(g2);
                  
                   
                    // clear the panel
                    Color save = panelg2.getColor();
                    panelg2.setColor(Color.white);
                    Dimension d = this.getSize();
                    panelg2.fillRect(0,0,d.width,d.height);                   
                    AffineTransform xForm = AffineTransform.getScaleInstance(scale, scale);
                    AffineTransform flip = new AffineTransform(0.0,1.0,1.0,0.0,0.0,0.0);
                    if (dim == 0) {
                        xForm.concatenate(flip);
                    }
                    panelg2.drawImage(buffered,new AffineTransformOp(xForm,AffineTransformOp.TYPE_NEAREST_NEIGHBOR),0,0);   
                    panelg2.setColor(save);
                }
            }
        };

        this.add(slicePanel,BorderLayout.CENTER);
        slicePanel.addKeyListener(new KeyAdapter(){
            @Override
            public void keyPressed(KeyEvent e){
                char k = e.getKeyChar();
                int kCode = e.getKeyCode();
                if (kCode == KeyEvent.VK_F2){
                    rightClick(mousePosition,false);
                } else if (kCode == KeyEvent.VK_F1){
                    rightClick(mousePosition,true);
                }else if (kCode == KeyEvent.VK_F4){
                    embryo.formLink();
                } else if (kCode == KeyEvent.VK_F5){
                    Nucleus.intersect(embryo.getNucleusFile().getSelected(),embryo.getMarked());
                }
                else {
//                int mask = KeyEvent.SHIFT_DOWN_MASK;
//                int modifier = e.getModifiersEx();
 //               if ((modifier&mask) == mask){
                    long pos[];
                    switch (k) {
/*                        
                    case 'z':
                        parent.changeRadiusSelectedNucleus(-1);
                        break;
                    case 'x':
                        parent.changeRadiusSelectedNucleus(1);
                        break;
                    case 'A':
                        parent.moveSelectedNucleus(imageXDirection(), -1);
                        break;
                    case 'D':
                        parent.moveSelectedNucleus(imageXDirection(), 1);
                        break;
                    case 'S':
                        parent.moveSelectedNucleus(imageYDirection(), 1);
                        break;
                    case 'W':
                        parent.moveSelectedNucleus(imageYDirection(), -1);
                        break;
*/                        
                    case 'w':
                        pos = parent.getPosition();
                        pos[dim] = pos[dim] - 1;
                        setPosition(pos);
                        break;  
                    case 's':
                        pos = parent.getPosition();
                        pos[dim] = pos[dim] + 1;
                        setPosition(pos);
                        break; 
                    case 'a':
                        parent.decrementTime();
                        break;
                    case 'd':
                        parent.incrementTime();
                        break;
                    case 'Q':
                        try {
                            embryo.joinSelectedNucleus();
                        } catch (Exception exc){
                            exc.printStackTrace();
                        }
                        break;
                    case 'E':
                        try {
                            embryo.splitSelectedNucleus();
                        } catch(Exception exc){
                            exc.printStackTrace();
                        }
                        break;
                    case 'r':
                        try {
                            embryo.setSelectedNucleus(null);
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                        break;
                    case 't':
                        try {
                            if (embryo.selectedNucleus() != null) {
                                embryo.getNucleusFile().unlinkNucleus(embryo.selectedNucleus(),true);
                            }
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                        break;
                    case 'f':
                        try {
                            if (embryo.selectedNucleus() != null) {
                                Nucleus sis = embryo.selectedNucleus().getSisterNucleus();
                                embryo.setSelectedNucleus(sis);
                            }
                        } catch (Exception exc) {
                            exc.printStackTrace();
                        }
                        break;
                    }
                }
            }
        });
        
        this.addMouseWheelListener(new MouseAdapter(){
            @Override
            public void mouseWheelMoved(MouseWheelEvent e){
                slider.setValue(slider.getValue()+e.getWheelRotation());
            }            
        });
        
        this.addMouseMotionListener(new MouseMotionAdapter(){
            @Override
            public void mouseMoved(MouseEvent e){
                slicePanel.requestFocusInWindow();
               
                if (dim == 0){
                    mousePosition = imageCoordinates(e.getY(),e.getX());
                }else {
                    mousePosition = imageCoordinates(e.getX(),e.getY());
                }                
            }            
        });
        
        this.addMouseListener(new MouseAdapter(){

            @Override
            public void mouseEntered(MouseEvent e){
                parent.getAce3D_Frame().toFront();
                parent.getAce3D_Frame().requestFocus();

            }
          
            @Override
            public void mouseClicked(MouseEvent e){
                long [] pos;
                if (dim == 0){
                    pos = imageCoordinates(e.getY(),e.getX());
                }else {
                    pos = imageCoordinates(e.getX(),e.getY());
                }
                if (e.getButton() == MouseEvent.BUTTON1){
                    int mask = MouseEvent.SHIFT_DOWN_MASK;
                    if (( e.getModifiersEx()&mask) == mask){
                        // making a new nucleus  - shift left button
                        long[] parentPos = parent.getPosition();
                        double[] center = new double[parentPos.length];
                        double radius = 0.0;
                        for (int d=0 ; d<pos.length ; ++d){
                            center[d] = parentPos[d];     //(image coordinates)
                            long l = pos[d]-parentPos[d];
                            radius = radius + l*l;  // (image coordinates)
                        }
                        radius = Math.sqrt(radius);
                        embryo.addNucleus(new Nucleus(new NucleusData(timePointImage.getTime(),center,radius)));
                        parent.repaint();                        
                    }
                    else if (parent != null ){
                        parent.changePosition(pos);  // left button moves to location
                    }                 
                } 
                else if (e.getButton() == MouseEvent.BUTTON3){
                    int mask = MouseEvent.SHIFT_DOWN_MASK;
                    rightClick(pos,( e.getModifiersEx()&mask) == mask);
                }
            }
        });        
    }

    private void rightClick(long[] pos,boolean shift){
        // finding the closest nucleus
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        double min = Double.MAX_VALUE;
        Nucleus closest = null;
        for (Nucleus nuc : nucs){
            double d = nuc.distanceSqaured(pos);
            if (d < min){
                closest = nuc;
                min = d;
            }
        }                    
        if (shift) {   // right shift toggles the marked status of the nucleus
            Nucleus mark = embryo.getMarked();
            if (mark!=null && mark.equals(closest)) {
                embryo.setMarked(null);
            } else {
                embryo.setMarked(closest);
            }

        }
        else {  // right mouse button selects a nucleus
            embryo.setSelectedNucleus(closest);

        }
        parent.repaint();        
    }
    public int getDimension(){
        return dim;
    }
    // set the position (image coordinates)
    public void setPosition(long[] pos){

        imagePosition = pos;
        setSlice(pos[dim]);
        slider.setValue((int)pos[dim]);
        this.repaint();
    }
/*    
    // return the screen x coordinate given image coordinates
    public int screenX(long[] p){
        if (dim==0){
            return (int)((p[1]-timePointImage.minPosition(1))*scale*bufW/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[0]-timePointImage.minPosition(0))*scale*bufW/(timePointImage.maxPosition(0)-timePointImage.minPosition(0)));
        }
    }

    // return the screen y coordinate given image coordinates
    public int screenY(long[] p){
        if (dim==2){
            return (int)((p[1]-timePointImage.minPosition(1))*scale*bufH/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[2]-timePointImage.minPosition(2))*scale*bufH/(timePointImage.maxPosition(2)-timePointImage.minPosition(2)));
        }
    } 
*/    
    public int screenX(double[] p){
        if (dim==0){
            return (int)((p[1]-timePointImage.minPosition(1))*bufW/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[0]-timePointImage.minPosition(0))*bufW/(timePointImage.maxPosition(0)-timePointImage.minPosition(0)));
        }
    }
    public int screenX(long[] p){
        if (dim==0){
            return (int)((p[1]-timePointImage.minPosition(1))*bufW/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[0]-timePointImage.minPosition(0))*bufW/(timePointImage.maxPosition(0)-timePointImage.minPosition(0)));
        }
    }    
    static public int screenX(long[] p,int dim,int bufW){
 //       System.out.printf("screenX: p =(%d,%d,%d),dim=%d,bufH=%d\n",p[0],p[1],p[2],dim,bufW);
        if (dim==0){
            return (int)((p[1]-TimePointImage.getMinPosition(1))*bufW/(TimePointImage.getMaxPosition(1)-TimePointImage.getMinPosition(1)));
        } else {
            return (int)((p[0]-TimePointImage.getMinPosition(0))*bufW/(TimePointImage.getMaxPosition(0)-TimePointImage.getMinPosition(0)));
        }
    }

    // return the screen y coordinate given image coordinates
    public int screenY(double[] p){
        if (dim==2){
            return (int)((p[1]-timePointImage.minPosition(1))*bufH/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[2]-timePointImage.minPosition(2))*bufH/(timePointImage.maxPosition(2)-timePointImage.minPosition(2)));
        }
    }  
    public int screenY(long[] p){
        if (dim==2){
            return (int)((p[1]-timePointImage.minPosition(1))*bufH/(timePointImage.maxPosition(1)-timePointImage.minPosition(1)));
        } else {
            return (int)((p[2]-timePointImage.minPosition(2))*bufH/(timePointImage.maxPosition(2)-timePointImage.minPosition(2)));
        }
    }      
    static public int screenY(long[] p,int dim,int bufH){
 //       System.out.printf("screenY: p =(%d,%d,%d),dim=%d,bufH=%d\n",p[0],p[1],p[2],dim,bufH);
        if (dim==2){
            return (int)((p[1]-TimePointImage.getMinPosition(1))*bufH/(TimePointImage.getMaxPosition(1)-TimePointImage.getMinPosition(1)));
        } else {
            return (int)((p[2]-TimePointImage.getMinPosition(2))*bufH/(TimePointImage.getMaxPosition(2)-TimePointImage.getMinPosition(2)));
        }
    }     
    public long[] imageCoordinates(int screenX,int screenY){
        if (timePointImage == null) return null;
        
        long[] pos = new long[timePointImage.numDimensions()];
        pos[dim] = SingleSlicePanel.this.slice;
        double bufX = screenX/scale/bufW;
        double bufY = screenY/scale/bufH;
        int ix = imageXDirection();
        int iy = imageYDirection();
        
        pos[ix] =(long)( timePointImage.minPosition(ix) + bufX*(timePointImage.maxPosition(ix)-timePointImage.minPosition(ix)));
        pos[imageYDirection()] = (long)( timePointImage.minPosition(iy) + bufY*(timePointImage.maxPosition(iy)-timePointImage.minPosition(iy)));;
        
        return pos;
    }    
    public int imageXDirection(){
        if (dim==0){
            return 1;
        }
        return 0;
    }    
    public int imageYDirection(){
        if (dim==2){
            return 1;
        }
        return 2;
    }  
    private boolean visible(Nucleus nuc){
        return nuc.isVisible(slice, dim);
       
    }
    final private void setSlice(long p){
        slice = p;

    }
    public long getSlice(){
        return this.slice;
    }
    
    final public void setImage(CompositeTimePointImage tpi,long[] pos){
        this.timePointImage = tpi;
        this.setPosition(pos);
        this.repaint();
    }
    public void setExtent(double min,double max) {
        slider.setMaximum((int)max);
        slider.setMinimum((int)min);
        slider.setValue((int)((min+max)/2));        
    
}
    private String horizontalAxis(){
        if (dim == 0){
            return "y";
        } 
        return "x";
    }
    private String verticalAxis(){
        if (dim==2){
            return "y";
        }
        return "z";
    }
    private void drawNuclei(Graphics2D g2){
       Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
       for (Nucleus nuc : nucs){

           Shape nucShape = nuc.getShape(slice, dim, bufW, bufH);   
           if (nucShape != null){
               if (nuc == this.embryo.getMarked()){
                    g2.setColor(Color.CYAN);
                }
                else if (nuc == this.embryo.selectedNucleus()){
                    g2.setColor(Color.RED);
                }
                else {
                    g2.setColor(Color.GREEN);
                }
                g2.draw(nucShape);               
           }
           
           
/*           
           
            long[] center = nuc.getCenter();  // image corrdinates
            double r = nuc.getRadius();   // image corrdinates
            double delta = Math.abs(slice-center[dim]);   // image corrdinates
            if (nuc.isVisible(slice, dim)){
                double rad = Math.sqrt(r*r-delta*delta);  //image coordinates
                int ix = imageXDirection();
                int iy = imageYDirection(); 
                long[] low = new long[center.length];
                long[] high = new long[center.length];
                low[dim] = slice;
                high[dim] = slice;
                low[ix] = center[ix] - (long)rad;
                low[iy] = center[iy] - (long)rad;
                high[ix] = center[ix] + (long)rad;
                high[iy] = center[iy] + (long)rad;  
                int scrX = screenX(low);
                int scrY = screenY(low);
                int scrHighX = screenX(high);
                int scrHighY = screenY(high);
                Ellipse2D.Double ellipse = new Ellipse2D.Double(scrX,scrY,scrHighX-scrX,scrHighY-scrY);
                


                
                if (nuc == this.embryo.selectedNucleus()){
                    g2.setColor(Color.RED);
                }else {
                    g2.setColor(Color.GREEN);
                }
                g2.draw(ellipse);
               
            }
            
*/            
            
       }        
    }
    private void drawSisters(Graphics2D g2){
        NucleusFile nucFile = parent.getEmbryo().getNucleusFile();
        
        TreeMap<Nucleus,Nucleus> sisterPairs = new TreeMap<>();
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
           Nucleus sisterNuc = nucFile.sister(nuc);
           if (sisterNuc != null){
               if (sisterPairs.get(sisterNuc)==null){
                   sisterPairs.put(nuc,sisterNuc);
               }
           }
        } 
        
        for (Entry<Nucleus,Nucleus> entry : sisterPairs.entrySet()){
            Nucleus nuc1 = entry.getKey();
            Nucleus nuc2 = entry.getValue();
            if (visible(nuc1)||visible(nuc2)){
                g2.drawLine(screenX(nuc1.getCenter()),screenY(nuc1.getCenter()),screenX(nuc2.getCenter()),screenY(nuc2.getCenter()));
            }
        }
    }
    private void drawDivisions(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if (nuc.equals(this.embryo.selectedNucleus())){
                Nucleus sister = nuc.getSisterNucleus();
                if (sister != null){
                    Graphics2D g3 = (Graphics2D) g2;
                    g3.setStroke(new BasicStroke(3));
                    g3.draw(new Line2D.Float(screenX(nuc.getCenter()),screenY(nuc.getCenter()),screenX(sister.getCenter()),screenY(sister.getCenter())));                    
                }
            }
        }
    }
    private void labelMarkedNuclei(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if (nuc.getLabeled()){
                labelNucleus(g2,nuc);
            }
        }
        
    }
    private void labelSelectedNucleus(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if (nuc == embryo.selectedNucleus()){
                labelNucleus(g2,nuc);
            }
        }        
    }
    // label all visible nuclei 
    private void labelAllNuclei(Graphics2D g2){
        Set<Nucleus> nucs = embryo.getNuclei(timePointImage.getTime());
        for (Nucleus nuc : nucs){
            if(visible(nuc)){
                labelNucleus(g2,nuc);
            }
        }        
    }
    private void labelNucleus(Graphics g2,Nucleus nuc){
        g2.drawString(nuc.getName(),screenX(nuc.getCenter()),screenY(nuc.getCenter()));
    }
    public void setEmbryo(ImagedEmbryo em){
        this.embryo = em;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        this.slicePanel.repaint();
    }
    
    SynchronizedMultipleSlicePanel parent;
    JPanel slicePanel;
    double scale=1.0;
    int bufW;
    int bufH;
    long slice;
    BufferedImage buffered;
    long bufferedSlice=Long.MAX_VALUE;
    int bufferedTime = Integer.MAX_VALUE;
    DataSetProperties bufferedProps;
    int dim;
    JSlider slider;
    final String title;
    CompositeTimePointImage timePointImage;
    ImagedEmbryo embryo;
    long[] imagePosition;
    long[] mousePosition;

}
