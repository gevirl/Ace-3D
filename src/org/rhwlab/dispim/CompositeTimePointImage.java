/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.rhwlab.ace3d.Ace3D_Frame;
import org.rhwlab.ace3d.DataSetProperties;

/**
 *
 * @author gevirl
 */
public class CompositeTimePointImage  {
    public CompositeTimePointImage(int time){
        this.time = time;
    }

    public BufferedImage getBufferedImage(int dim,long slice){
        List<String> datasets = Ace3D_Frame.datasetsSelected();
        if (datasets.isEmpty()) return null;
        
        // find an image to start with at this time
        TimePointImage tpi = null;
        for (int i=0 ; i<datasets.size() ; ++i){
            tpi = TimePointImage.getSingleImage(datasets.get(i),time); 
            if (tpi != null){
                break;
            }
        }
        if (tpi == null){
            return null; // no images at this time point
        }
        IntervalView iv = Views.hyperSlice(tpi.getImage(), dim, slice);
        Cursor cursor = iv.localizingCursor();
        cursor.fwd();
        int x0 = (int)iv.min(0);  // images min x position
        int y0 = (int)iv.min(1);  // images min y position
        int w = (int)iv.dimension(0);
        int h = (int)iv.dimension(1);
        BufferedImage ret = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB_PRE);   
        
        // get the properties of each dataset in the composite image
        RandomAccess[] access = new RandomAccess[datasets.size()];
        double[] Rfactor = new double[datasets.size()];
        double[] Gfactor = new double[datasets.size()];
        double[] Bfactor = new double[datasets.size()];
        double[] pixMin = new double[datasets.size()];
        for (int i=0 ; i<datasets.size() ; ++i){
            DataSetProperties ps = Ace3D_Frame.getProperties(datasets.get(i));
            double red = ps.color.getRed();
            double green = ps.color.getGreen();
            double blue = ps.color.getBlue();
            double alpha = ps.color.getAlpha();
            double range = ps.max-ps.min;
            if (ps.autoContrast){
                range = tpi.getMax()-tpi.getMin();
            }
            pixMin[i] = ps.min;
            double f = 255.0/(alpha*range*datasets.size());
            Rfactor[i] = red*f;
            Gfactor[i] = green*f;
            Bfactor[i] = blue*f;
            if (i > 0){
                tpi = TimePointImage.getSingleImage(datasets.get(i),time); 
                if (tpi != null){
                    access[i] = Views.hyperSlice(TimePointImage.getSingleImage(datasets.get(i),time).getImage(), dim, slice).randomAccess();
                } else {
                    access[i] = null;
                }
            }
        }
        while (cursor.hasNext()){
 //           UnsignedShortType pix = (UnsignedShortType)cursor.get();
            AbstractIntegerType pix = (AbstractIntegerType)cursor.get();
            int[] cursorPos = new int[2];
            cursor.localize(cursorPos);        
            double v = Math.max(0.0, pix.getRealDouble() - pixMin[0]);
            double r = Rfactor[0]*v;
            double g = Gfactor[0]*v;
            double b = Bfactor[0]*v;
            int x = cursorPos[0]-x0;   // BufferedImage x position
            int y = cursorPos[1]-y0;    // BufferedImage y position
            
            // fuse over all the images the current cursor location
            for (int i=1 ; i<datasets.size() ; ++i){
                if (access[i]!=null) {
                    access[i].setPosition(cursorPos);
    //                pix = (UnsignedShortType)access[i].get();
                    pix = (AbstractIntegerType)access[i].get();
                    v = Math.max(0.0,pix.getRealDouble() - pixMin[i]);
                    r = r + Rfactor[i]*v;
                    g = g + Gfactor[i]*v;
                    b = b + Bfactor[i]*v;
                }
            }
            Color c = new Color(Math.min(255,(int)r),Math.min(255,(int)g),Math.min(255,(int)b),255);
            ret.setRGB(x,y,c.getRGB());

            cursor.fwd();
        }
        return ret;  
    }
    public int getTime(){
        return time;
    }
    public double minPosition(int d){
        TimePointImage.initCache();
        return TimePointImage.timePointCache.get(0).minPosition(d);
    }
    public double maxPosition(int d){
        TimePointImage.initCache();
        return TimePointImage.timePointCache.get(0).maxPosition(d);
    } 
    public int numDimensions(){
        TimePointImage.initCache();
        return TimePointImage.timePointCache.get(0).getDims().length;
    }
    public double[] getMinPosition(){
        TimePointImage.initCache();
        return TimePointImage.timePointCache.get(0).getMinPosition();
    }
    public double[] getMaxPosition(){
        TimePointImage.initCache();
        return TimePointImage.timePointCache.get(0).getMaxPosition();
    }
    int time;

//    List<TimePointImage> images;
}
