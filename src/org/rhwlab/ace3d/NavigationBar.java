/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author gevirl
 */
public class NavigationBar extends JPanel {
    public NavigationBar(SynchronizedMultipleSlicePanel syncPanel){
        this.syncPanel = syncPanel;
        this.setLayout(new FlowLayout());
        this.add(new JLabel("Time: "));
        this.add(timeField);
        timeField.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int t = Integer.valueOf(timeField.getText().trim());
                    syncPanel.changeTime(t);
                } catch (Exception exc){
                    
                }
                
            }
        });
        this.add(location);
        this.add(nucleus);
        
    }
    
    public void update(int time,long[] pos,org.rhwlab.dispim.nucleus.Nucleus nuc){
        timeField.setText(String.format("%4d",time));
        location.setText(String.format("(%d,%d,%d)",pos[0],pos[1],pos[2]));
        if (nuc == null){
            nucleus.setText(noNuc);
        }else {
            nucleus.setText(nuc.getFullName());
        }
    }
    static String noNuc = "Nucleus: none selected";
    SynchronizedMultipleSlicePanel syncPanel;
    JLabel location = new JLabel("(-,-,-)");
    JLabel nucleus = new JLabel(noNuc);
    JTextField timeField = new JTextField("0000",5);
}
