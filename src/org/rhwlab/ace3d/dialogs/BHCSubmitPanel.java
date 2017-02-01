/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.rhwlab.dispim.datasource.BoundingBox;

/**
 *
 * @author gevirl
 */
public class BHCSubmitPanel extends JPanel {
    public BHCSubmitPanel(String init, int[] dims){
        this.dims = dims;
        this.setLayout(new GridLayout(15,3));
        
        this.add(new JLabel("Cores to request"));
        this.add(cores);
        this.add(new JLabel("(1-16)"));
        
        this.add(new JLabel("Memory per Core (Gigs)"));
        this.add(memory);
        this.add(new JLabel(""));
        
        this.add(new JLabel("To waterston grid"));
        waterque.setSelected(true);
        this.add(waterque);  
        this.add(new JLabel(""));        
        
        this.add(new JLabel("Force KMeans/BHC"));
        this.add(forceKMeans);  
        this.add(new JLabel(""));
        
        this.add(new JLabel("Force BHC only"));
        this.add(forceBHC);  
        this.add(new JLabel(""));
        
        this.add(new JLabel("Series Directory"));
        seriesDir = new JTextField(init);
        this.add(seriesDir);
        JButton browse = new JButton("Browse");
        browse.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(seriesDir.getText().trim());
                if (chooser.showOpenDialog(BHCSubmitPanel.this) == JFileChooser.APPROVE_OPTION){
                    seriesDir.setText(chooser.getSelectedFile().getPath());
                }  
            }
        });
        this.add(browse);
        
        this.add(new JLabel("Start Time"));
        this.add(startTime);
        this.add(new JLabel(""));
        
        this.add(new JLabel("End Time"));
        this.add(endTime);
        this.add(new JLabel(""));  
        
        this.add(new JLabel("Segmentation Threshold"));
        this.add(segProb);
        this.add(new JLabel("0-100"));
        
        this.add(new JLabel("Concentration parameter"));
        this.add(alpha);
        this.add(new JLabel("1000-1000000"));
        
        this.add(new JLabel("Variance"));
        this.add(variance);
        this.add(new JLabel("5-200"));
        
        this.add(new JLabel("Degrees Freedom"));
        this.add(degrees);
        this.add(new JLabel("4-20"));
        
        this.add(new JLabel("X Range"));
        xMin = new JTextField(Integer.toString((int)(0.05*dims[0])));
        xMax = new JTextField(Integer.toString((int)(.95*dims[0])));
        this.add(xMin);       
        this.add(xMax);

        this.add(new JLabel("Y Range"));
        yMin = new JTextField(Integer.toString((int)(0.05*dims[1])));
        yMax = new JTextField(Integer.toString((int)(.95*dims[1])));
        this.add(yMin);       
        this.add(yMax);
        
        this.add(new JLabel("Z Range"));
        zMin = new JTextField(Integer.toString((int)(0.05*dims[2])));
        zMax = new JTextField(Integer.toString((int)(.95*dims[2])));
        this.add(zMin);       
        this.add(zMax);
        
        
        
    }
    public boolean isForceKMeans(){
        return forceKMeans.isSelected();
    }
    public boolean isForceBHC(){
        return forceBHC.isSelected();
    }
    public String getForce(){
        if (isForceKMeans()){
            return "KMeans";
        } else if (isForceBHC()){
            return "BHC";
        }
        return null;
    }
    
    public int getMemory(){
        try {
            return  Integer.valueOf(memory.getText().trim());
        } catch (Exception exc){
            return 5;
        }
    }
    public File getSeriesDirectory(){
        return new File(seriesDir.getText().trim());
    }
    public int getStartTime(){
        try {
            return  Integer.valueOf(startTime.getText().trim());
        } catch (Exception exc){
            return 1;
        }
    }
    public int getEndTime(){
        try {
            return  Integer.valueOf(endTime.getText().trim());
        } catch (Exception exc){
            return 400;
        }
    } 
    public double getAlpha(){
        try {
            return Double.valueOf(alpha.getText().trim());
        } catch (Exception exc){
            return 10000;
        }
    }
    public double getVariance(){
        try {
            return Double.valueOf(variance.getText().trim());
        } catch (Exception exc){
            return 20;
        }        
    }
    public int getDegrees(){
        try {
            return Integer.valueOf(degrees.getText().trim());
        } catch (Exception exc){
            return 10;
        }
    }
    public int getProb(){
        try {
            return Integer.valueOf(segProb.getText().trim());
        } catch (Exception exc){
            return 50;
        }
    }    
    public int getCores(){
        try {
            return  Integer.valueOf(cores.getText().trim());
        } catch (Exception exc){
            return 4;
        }
    }    
    public BoundingBox getBoundingBox(){
        Double[] mins = new Double[dims.length];
        Double[] maxs = new Double[dims.length];
        
        String s;
        s = xMin.getText().trim();
        mins[0] = null;
        if (!s.equals("")){
            mins[0] = new Double(s);
        }
        s = xMax.getText().trim();
        maxs[0] = null;
        if (!s.equals("")){
            maxs[0] = new Double(s);
        }  
        
        s = yMin.getText().trim();
        mins[1] = null;
        if (!s.equals("")){
            mins[1] = new Double(s);
        }
        s = yMax.getText().trim();
        maxs[1] = null;
        if (!s.equals("")){
            maxs[1] = new Double(s);
        } 

        s = zMin.getText().trim();
        mins[2] = null;
        if (!s.equals("")){
            mins[2] = new Double(s);
        }
        s = zMax.getText().trim();
        maxs[2] = null;
        if (!s.equals("")){
            maxs[2] = new Double(s);
        } 
        return new BoundingBox(mins,maxs);
    }
    public boolean isWaterston(){
        return waterque.isSelected();
    }
    int[] dims;
    JCheckBox forceKMeans = new JCheckBox();
    JCheckBox forceBHC = new JCheckBox();
    JCheckBox waterque = new JCheckBox();
    JTextField cores = new JTextField("4");
    JTextField memory = new JTextField("5");
    JTextField seriesDir;
    JTextField startTime = new JTextField("1");
    JTextField endTime = new JTextField("400");
    JTextField alpha = new JTextField("10000");
    JTextField variance = new JTextField("20");
    JTextField degrees = new JTextField("10");
    JTextField segProb = new JTextField("50");
    JTextField zMax;
    JTextField zMin;
    JTextField xMin;
    JTextField xMax;
    JTextField yMin;
    JTextField yMax;
}
