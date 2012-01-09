package org.mypsycho.netsend;

import java.io.File;

import javax.swing.filechooser.FileFilter;


/**
 * 
 * @author psycho
 * @version 1.0
 */
public class XmlFilter extends FileFilter {

    public static final String EXTENSION = ".xml";
    protected String description; // Localized ?

    public static final String XML_DTD = "NetSend.dtd";
//    public static final String AUTHOR_TOKEN = "Author"; // Technical word => No translate
    public static final String LIST_TOKEN = "List"; // Tech word
    public static final String KEY_TOKEN = "Key"; // Tech word
    public static final String EXPANDED_TOKEN = "expanded"; // Tech word
    public static final String NAME_TOKEN = "name"; // Tech word
    public static final String ID_TOKEN = "machine"; // Tech word
    public static final String PORT_TOKEN = "port"; // Tech word

    public XmlFilter(String d) {
        description = d;
    }

    public boolean accept(File f) {
        return f.isDirectory() || f.getName().endsWith(XmlFilter.EXTENSION);
    }
    public String getDescription() { return description; }
    
    // NOT USED
    public static File ensureExtension(File f) {
        return ((f.getName().endsWith(XmlFilter.EXTENSION)))
                ? f : new File(f.getPath() + EXTENSION);
    }

} // endclass XmlExchange