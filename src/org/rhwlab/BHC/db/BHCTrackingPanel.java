/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.BHC.db;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import javax.swing.JMenuItem;
import org.rhwlab.dispim.ImagedEmbryo;
import org.rhwlab.dispim.nucleus.BHCDirectory;
import org.rhwlab.dispim.nucleus.LinkedNucleusFile;
import org.rhwlab.dispim.nucleus.NucleusFile;
import org.rhwlab.spreadsheet.TrackingPanel;

/**
 *
 * @author gevirl
 */
public class BHCTrackingPanel extends TrackingPanel implements ActionListener {
    
    public BHCTrackingPanel() {
        super(true, inits);
        buildMenu(); 
    }
    public void buildMenu() {
        
        super.getMenu();
        menu.setText("BHC");    
        
        JMenuItem sage = new JMenuItem(submitSage);
        sage.addActionListener(this);
        menu.add(sage);    
        
        JMenuItem water = new JMenuItem(submitWater);
        water.addActionListener(this);
        menu.add(water); 

        JMenuItem local = new JMenuItem(runLocal);
        local.addActionListener(this);
        menu.add(local); 
        
        JMenuItem sync = new JMenuItem(resync);
        sync.addActionListener(this);
        menu.add(sync);         
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(resync)){
            try {
                resync();
            }catch (Exception exc){
                exc.printStackTrace();
            }
        }
    }
    
    public void resync()throws Exception {
        this.getModel().updateDb();
        this.getModel().setRowCount(0);
        LinkedNucleusFile nucFile = (LinkedNucleusFile)embryo.getNucleusFile();
        for (int time : bhc.getTimes()){
            Integer probUsed = nucFile.getThresholdProb(time);
            for (int prob : bhc.getThresholdProbs(time)){
                File file = bhc.getTreeFile(time, prob);
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();
                String[] tokens= line.split(" ");
                int row = this.getModel().addEmptyRow();
                this.getModel().setValue(row,"Name" , diSpimName, true);
                this.getModel().setValue(row,"Time",Integer.toString(time), true);
                this.getModel().setValue(row,"SegProb",Integer.toString(prob), true);
                if (probUsed != null && prob == probUsed){
                    this.getModel().setValue(row,"Tracking" , "Active", true);
                } else {
                    this.getModel().setValue(row,"Tracking" , "Complete", true);
                }
                for (int i=0 ; i<tokens.length; ++i){
                    if (tokens[i].startsWith("nu=")){
                        String[] values = tokens[i].split("\"");
                        this.getModel().setValue(row,"DegreesFreedom" ,values[1], true);
                    }
                    else if (tokens[i].startsWith("alpha")){
                        String[] values = tokens[i].split("\"");
                        Integer logConc = new Integer((int)Math.log10(Double.valueOf(values[1])));
                        this.getModel().setValue(row,"Log Conc" ,logConc.toString() , true);                        
                    }
                    else if (tokens[i].startsWith("s=")){
                        this.getModel().setValue(row,"Variance" ,tokens[i+1] , true);  
                    }
                }
            }
            
            
        }
        
    }
    public void setEmbryo(BHCDirectory bhc,ImagedEmbryo embryo)throws Exception {
        this.embryo = embryo;
        this.bhc = bhc;
        diSpimName = bhc.getDirectory().getParentFile().getName();
        String whereClause = String.format("diSPIMName = \'%s\'", diSpimName);
        loadWithTracking(whereClause);
    }
    ImagedEmbryo embryo;
    BHCDirectory bhc;
    String diSpimName;
    static String[] inits = {"None"};
    static String submitSage = "Submit to Sage cluster";
    static String submitWater = "Submit to Waterston cluster";
    static String runLocal = "Run on Local Machine";
    static String resync = "Resync";
}
