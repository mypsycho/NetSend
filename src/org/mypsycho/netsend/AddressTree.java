package org.mypsycho.netsend;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.mypsycho.swing.tree.CheckTree;
import org.mypsycho.swing.tree.CheckTreeModel;
import org.mypsycho.swing.tree.DefaultCheckTreeModel;
import org.mypsycho.swing.tree.DefaultCheckTreeNode;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 
 * @author psycho
 * @version 1.0
 */
public class AddressTree extends CheckTree {

    /*
     * TODO-Better : Editable tree
     *   add group, add destinary, copy, cut, paste, <edit>, edit comment    
     *   Save
     * 
     * */
    
    protected String author;
    NetSend owner;
    
    public AddressTree(NetSend parent, File file) throws IOException {
        super((CheckTreeModel) null);
        owner = parent;
        
        Element rootXml = readConfig(parent, file);
        setModel(new DefaultCheckTreeModel(buildTree(rootXml)));

        /* Expand nodes */
        AddressTreeNode addressesRoot = (AddressTreeNode) getModel().getRoot();
        if (addressesRoot.isAllExpanded()) {
            int count = 0;
            TreePath pathToExpanded = getPathForRow(count);
            while (pathToExpanded != null) {
                expandPath(pathToExpanded);
                count++;
                pathToExpanded = getPathForRow(count);
            }
        } else {
            expandRequiredNodes(addressesRoot);
        }
        
        getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        
        
    }

    public String getAuthor() { return author; }
    
    protected void expandRequiredNodes(AddressTreeNode node) {
        if (node.isLeaf())
            return;

        if (node.isExpandedAtStart()) {
            expandPath(new TreePath(node.getPath()));
        }
        for (int iChild=0; iChild<node.getChildCount(); iChild++) {
            expandRequiredNodes((AddressTreeNode) node.getChildAt(iChild));
        }
    }
    
    
    protected AddressTreeNode buildTree(Element node) {
        AddressTreeNode result = new AddressTreeNode(node);

        if (!result.isLeaf()) {
            for (node = (Element)node.getFirstChild(); node != null;
                    node = (Element)node.getNextSibling()) {
                AddressTreeNode child = buildTree(node);
                result.add(child);
            }
        }
        return result;
    }



    public Map<String, MessageReceiver> getSelectedTargets() {
        Map<String, MessageReceiver> result = new HashMap<String, MessageReceiver>();
        
        AddressTreeNode root = (AddressTreeNode)getModel().getRoot();
        java.util.List<TreeNode> path = new ArrayList<TreeNode>(root.getDepth());
        path.add(root);
        while (!path.isEmpty()) { // Avoid recursivity
            AddressTreeNode last = (AddressTreeNode)path.get(path.size() - 1);
            if (last.isLeaf()) {
                if (last.isAllSelected()) {
                    int port = last.getPort();
                    String addresse = last.getMachine();
                    if (port > NO_PORT)
                        addresse += ":" + port;
                    else
                        addresse += ":" + NetReceiver.DEFAULT_PORT;
                    
                    if (!result.containsKey(addresse)) {
                        result.put(addresse, new MessageReceiver(last, last.getName(), addresse));
                    }
                }

                path.remove(path.size() - 1);
                last = (AddressTreeNode)last.getNextSibling();
                while ((last == null) && (!path.isEmpty())) {
                    last = (AddressTreeNode)((AddressTreeNode)path.remove(path.size() -
                            1)).getNextSibling();
                }
                if (last != null) {
                    path.add(last);
                }
            } else {
                path.add(last.getFirstChild());
            }
        }

        return result;
    }

    public int getPort() throws NumberFormatException {
        return ((AddressTreeNode) getModel().getRoot()).getPort();
    }
    
    public static final int NO_PORT = 0;

    public static final int NOT_SELECTED = 0;
    public static final int PARTIALLY_SELECTED = -1;
    public static final int FULLY_SELECTED = 1;

    public class AddressTreeNode extends DefaultCheckTreeNode {
        protected int minX = 0;
        protected int maxX = 0;
        protected Element element = null;  // Xml element
        protected int selection = NOT_SELECTED;

        public AddressTreeNode(Element node) {
            super(node);
            element = node;
        }

        public String getName() {
            return isRoot() ? "All" : element.getAttribute(XmlFilter.NAME_TOKEN);
        }

        
        public boolean isLeaf() {
            return element.getTagName().equals(XmlFilter.KEY_TOKEN);
        }
        

        public boolean isExpandedAtStart() {
            return "true".equalsIgnoreCase(element.getAttribute(XmlFilter.EXPANDED_TOKEN));
        }
        
        public boolean isAllExpanded() {
            return "all".equalsIgnoreCase(element.getAttribute(XmlFilter.EXPANDED_TOKEN));
        }

        
        
        public String getMachine() {
            return element.getAttribute(XmlFilter.ID_TOKEN);
        }

        public int getPort() throws NumberFormatException {
            String codedPort = element.getAttribute(XmlFilter.PORT_TOKEN);
            if (codedPort.length()==0)
                return NO_PORT;
            
            int port = NO_PORT;
            try {
                port = Integer.parseInt(codedPort);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(owner.getText(
                        "ErrFormatPort", codedPort ));
            }
            
            if (port < 0)
                throw new NumberFormatException(owner.getText(
                        "ErrFormatPort", codedPort));

            return port;
        }


    } // endclass AddressTreeNode


    public String convertValueToText(Object value, boolean selected, boolean expanded,
            boolean leaf, int row, boolean hasFocus) {
        if (value == null) {
            return "";
        }
        
        if (value instanceof AddressTreeNode) {
            AddressTreeNode address = (AddressTreeNode) value;
            if (leaf) {
                if (address.getPort() > NO_PORT) {
                    return address.getName() + " [" + address.getMachine() + ":"
                            + address.getPort() + "]";
                } else {
                    return address.getName() + " [" + address.getMachine() + "]";
                }
            } else {
                return address.getName();
            }
        }
        return value.toString();
    }



    protected Element readConfig(final NetSend parent, final File file) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setIgnoringComments(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            ErrorHandler errorHandler = new ErrorHandler() {
                protected void stop(SAXParseException e) throws SAXException {
                    /* For technical value, no localisation of number */
                    String errorMessage = owner.getText("ErrFileFormat",  
                            file.getName(), String.valueOf(e.getLineNumber())); 
                    System.err.println(errorMessage);
                    parent.setStatus(errorMessage);
                    throw e;
                }
                public void warning(SAXParseException e) throws SAXException { stop(e); }
                public void error(SAXParseException e) throws SAXException { stop(e); }
                public void fatalError(SAXParseException e) throws SAXException { stop(e); }
            };
            builder.setErrorHandler(errorHandler);


            EntityResolver entityResolver = new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId)
                        throws SAXException, IOException {
                    if (systemId.endsWith(XmlFilter.XML_DTD)) {
                        URL res = NetSend.class.getResource(XmlFilter.XML_DTD);
                        if (res != null)
                            return new InputSource(res.toExternalForm());
                    }
                    return null;
                }
            };
            builder.setEntityResolver(entityResolver);
            Element rootXml = builder.parse(file).getDocumentElement();

            author = rootXml.getAttribute(XmlFilter.NAME_TOKEN);


            return rootXml;
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            // Send a better message
            throw (IOException) new IOException().initCause(e);
        }
    }

    public void clearChecked() {
        getCheckModel().setSelected(getCheckModel().getRoot(), false);
    }
    
} // endClass AddressTree