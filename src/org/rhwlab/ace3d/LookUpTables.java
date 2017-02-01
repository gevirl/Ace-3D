/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import ij.ImageJ;
import ij.process.LUT;
import java.awt.Color;
import java.util.Set;
import java.util.TreeMap;
import net.imglib2.display.ColorTable;
import net.imglib2.display.ColorTable8;

/**
 *
 * @author gevirl
 */
public class LookUpTables {
    public LookUpTables(){
        luts.put("Blue",LUT.createLutFromColor(Color.BLUE));
        luts.put("Cyan",LUT.createLutFromColor(Color.CYAN));
        luts.put("Green",LUT.createLutFromColor(Color.GREEN));
        luts.put("Magenta",LUT.createLutFromColor(Color.MAGENTA));
        luts.put("Orange",LUT.createLutFromColor(Color.ORANGE));
        luts.put("Pink",LUT.createLutFromColor(Color.PINK));
        luts.put("Red",LUT.createLutFromColor(Color.RED));
        luts.put("Yellow",LUT.createLutFromColor(Color.YELLOW));
//        luts.put("Gray",LUT.createLutFromColor(Color.LIGHT_GRAY));
        luts.put("Gray",grays());
        luts.put("Fire", fire());
        luts.put("Ice",ice());
    }
    public LUT grays() {
                byte[] reds = new byte[256];
        byte[] greens = new byte[256];
        byte[] blues = new byte[256]; 
            for (int i=0; i<256; i++) {
                    reds[i] = (byte)i;
                    greens[i] = (byte)i;
                    blues[i] = (byte)i;
            }
            return new LUT(8,256,reds,greens,blues);
    }    
    public LUT fire() {

        int[] r = {0,0,1,25,49,73,98,122,146,162,173,184,195,207,217,229,240,252,255,255,255,255,255,255,255,255,255,255,255,255,255,255};
        int[] g = {0,0,0,0,0,0,0,0,0,0,0,0,0,14,35,57,79,101,117,133,147,161,175,190,205,219,234,248,255,255,255,255};
        int[] b = {0,61,96,130,165,192,220,227,210,181,151,122,93,64,35,5,0,0,0,0,0,0,0,0,0,0,0,35,98,160,223,255};
        byte[] reds = new byte[r.length];
        byte[] greens = new byte[r.length];
        byte[] blues = new byte[r.length];        
        for (int i=0; i<r.length; i++) {
                reds[i] = (byte)r[i];
                greens[i] = (byte)g[i];
                blues[i] = (byte)b[i];
        }
        return new LUT(8,r.length,reds,greens,blues);
	} 
    public LUT ice() {
        int[] r = {0,0,0,0,0,0,19,29,50,48,79,112,134,158,186,201,217,229,242,250,250,250,250,251,250,250,250,250,251,251,243,230};
        int[] g = {156,165,176,184,190,196,193,184,171,162,146,125,107,93,81,87,92,97,95,93,93,90,85,69,64,54,47,35,19,0,4,0};
        int[] b = {140,147,158,166,170,176,209,220,234,225,236,246,250,251,250,250,245,230,230,222,202,180,163,142,123,114,106,94,84,64,26,27};
        byte[] reds = new byte[r.length];
        byte[] greens = new byte[r.length];
        byte[] blues = new byte[r.length];         
        for (int i=0; i<r.length; i++) {
                reds[i] = (byte)r[i];
                greens[i] = (byte)g[i];
                blues[i] = (byte)b[i];
        }
        return new LUT(8,r.length,reds,greens,blues);
    }
    public Set<String> getLutNames(){
        return this.luts.keySet();
    }
    static public LUT getLUT(String lutName){
        return luts.get(lutName);
    }
    static public ColorTable getColorTable(String lutName){
        return new ColorTable8(luts.get(lutName).getBytes());
    }
    static public void main(String[] args){
        new ImageJ();
        LookUpTables luts = new LookUpTables();
    }
    static TreeMap<String,LUT> luts = new TreeMap<>();
}
