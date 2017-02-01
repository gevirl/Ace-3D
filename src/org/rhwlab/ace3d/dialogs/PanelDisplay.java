/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.ace3d.dialogs;

import javax.swing.JDialog;
import javax.swing.JPanel;

/**
 *
 * @author gevirl
 */
public class PanelDisplay extends JDialog {
    public PanelDisplay(JPanel panel){
      this.setContentPane(panel);
      this.setSize(750, 500);
      this.setLocation(1000, 1000);
    }
}
