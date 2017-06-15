package org.rhwlab.dispim.nucleus;

import java.io.File;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;

/**
 *
 * @author gevirl
 */
public class BHCNucleusFile extends BHCNucleusSet {

    // open a tgmm nucleus file , adding nuclei to the Ace3dNucleusFile
    public BHCNucleusFile(File file)throws Exception{
        this.file = file;
        SAXBuilder saxBuilder = new SAXBuilder();
        Document doc = saxBuilder.build(file); 
        Element document = doc.getRootElement(); 
        init(document);
    } 
    File file;
}
