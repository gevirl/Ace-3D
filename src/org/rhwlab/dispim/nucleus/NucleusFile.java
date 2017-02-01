/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.rhwlab.dispim.nucleus;

import java.io.File;
import java.util.List;;
import java.util.Set;
import org.jdom2.Element;

/**
 *
 * @author gevirl
 */
public interface NucleusFile extends javafx.beans.Observable{

    public Set<Nucleus> getNuclei(int time);
    public File getFile();
    public void setFile(File f);
    public Set<Nucleus> getRoots(int time);
    public Set<Nucleus> getLeaves(int time);
    public List<Nucleus> linkedForward(Nucleus nuc);
    public Nucleus linkedBack(Nucleus nuc);
    public Nucleus sister(Nucleus nuc);
    public void addNucleus(Nucleus nuc);
    public Set<Integer> getAllTimes();
    public void setSelected(Nucleus nuc);
    public void setMarked(Nucleus nuc);
    public Nucleus getSelected();
    public Nucleus getMarked();
    public BHCDirectory getTreeDirectory();
    public void setBHCTreeDirectory(BHCDirectory bhc);
    public void addNuclei(BHCNucleusSet bhcToAdd,boolean curated);
    public void addSelectionOberver(javafx.beans.value.ChangeListener obs);
    public Element toXML();
    public void fromXML(Element ele);
    public void removeNucleus(Nucleus nuc,boolean notify);
    public void unlinkNucleus(Nucleus nuc,boolean notify);
    public boolean isCurated(int time);
    public void removeCell(Nucleus nuc,boolean notify);
}
