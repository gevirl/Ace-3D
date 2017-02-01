/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.BorderLayout;
import java.awt.Container;
import java.util.TreeMap;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;



/**
 *
 * @author gevirl
 */
public class DataSetsDialog extends JFrame {
    public DataSetsDialog(Ace3D_Frame owner,int sliderMin,int sliderMax){
        super("DataSets");
        this.owner = owner;
        this.min = sliderMin;
        this.max = sliderMax;
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(owner);
        Container content = this.getContentPane();
        content.setLayout(new BorderLayout());
        
        contrastPanel = new JPanel();
        contrastPanel .setLayout(new BoxLayout(contrastPanel,BoxLayout.Y_AXIS));
        for (String dataset : Ace3D_Frame.getAllDatsets()){
            addDataSet(dataset);
        }
        content.add(contrastPanel,BorderLayout.CENTER);
        this.pack();
        this.setLocation(1544, 495);
        this.setSize(380, 396);
    }
    public void addDataSet(String dataset){
        DataSetPropertyPanel dspp = new DataSetPropertyPanel(owner,dataset,min,max); 
        contrastPanel.add(dspp);
        map.put(dataset, dspp);
        this.pack();
        this.setVisible(true);
    }
    public void setProperties(String dataset,DataSetProperties props){
        DataSetPropertyPanel p = map.get(dataset);
        if (p != null){
            p.setProperties(props);
        }
    }
    Ace3D_Frame owner;
    JPanel contrastPanel;
    int min;
    int max;
    TreeMap<String,DataSetPropertyPanel> map = new TreeMap<>();
}
