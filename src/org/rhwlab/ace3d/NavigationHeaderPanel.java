/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;

/**
 *
 * @author gevirl
 */
public class NavigationHeaderPanel extends JPanel {
    public NavigationHeaderPanel(){
        
        nucleus = new JLabel("Nucleus:                  ");
        this.add(nucleus);
        
        this.add(new JLabel("Max Time:"));
        maxTime = new JTextField();
        maxTime.setText("100");
        maxTime.setColumns(10);
        maxTime.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });
        this.add(maxTime);
        
        labelNodes = new JCheckBox("Label Nodes", true);
        labelNodes.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });        
        this.add(labelNodes);
        
        labelLeaves = new JCheckBox("Label Leaves", true);
        labelLeaves.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });        
        this.add(labelLeaves);
        
        this.add(new JLabel("Time Scale:"));
        timeScale = new JTextField("3.0");
        timeScale.setColumns(10);
        timeScale.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });
        this.add(timeScale);  
        
        this.add(new JLabel("Cell Width:"));
        cellWidth = new JTextField("20.0");
        cellWidth.setColumns(10);
        cellWidth.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                treePanel.stateChanged(new ChangeEvent(NavigationHeaderPanel.this));
            }
        });
        this.add(cellWidth);
    }

    public void setTreePanel(NavigationTreePanel treePanel){
        this.treePanel = treePanel;
    }
    public int getMaxTime(){
        return Integer.valueOf(maxTime.getText().trim());
    }
    public boolean labelLeaves(){
        return labelLeaves.isSelected();
    }
    public boolean labelNodes(){
        return labelNodes.isSelected();
    }
    public double getTimeScale(){
        return Double.valueOf(timeScale.getText().trim());
    }
    public double getCellWidth(){
        return Double.valueOf(cellWidth.getText().trim());
    }
    public void setNucleus(String name){
        nucleus.setText("Nucleus: "+name);
    }

    NavigationTreePanel treePanel;
    JTextField maxTime;
    JCheckBox labelNodes;
    JCheckBox labelLeaves;
    JTextField timeScale;
    JTextField cellWidth;
    JLabel nucleus;
}
