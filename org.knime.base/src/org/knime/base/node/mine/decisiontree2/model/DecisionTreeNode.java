/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2007
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   21.07.2005 (mb): created
 */
package org.knime.base.node.mine.decisiontree2.model;

import java.awt.Color;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.tree.TreeNode;

import org.knime.base.data.util.DataCellStringMapper;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.Config;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The base abstract implementations of a node of a decision tree. Separate
 * implementations for a leaf and a split node (abstract) exist.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Christoph Sieb, University of Konstanz
 */
public abstract class DecisionTreeNode implements TreeNode, Serializable {
    
    /** The node logger for this class. */
    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(DecisionTreeNode.class);
    
    private static final String CONFIG_KEY_COLORS = "colors";
    private static final String CONFIG_KEY_COLOR = "color";
    private static final String CONFIG_KEY_RED = "red";
    private static final String CONFIG_KEY_GREEN = "green";
    private static final String CONFIG_KEY_BLUE = "blue";
    private static final String CONFIG_KEY_COUNT = "count";
    
    private HashMap<Color, Double> m_coveredColors 
                = new HashMap<Color, Double>();

    private HashMap<DataCell, Double> m_classCounts;

    private int m_ownIndex = -1;

    private DataCell m_class;

    private double m_ownClassFreq;

    private double m_allClassFreq;

    private DecisionTreeNode m_parent = null;

    private String m_prefix = "root";
    
    private Object m_customData;

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNode() {
    }

    /**
     * Constructor of base class. Read all type-invariant information from XML
     * file.
     * 
     * @param xmlNode XML node object
     * @param mapper map translating column names to {@link DataCell}s and vice
     *            versa
     */
    protected DecisionTreeNode(final Node xmlNode,
            final DataCellStringMapper mapper) {
        // read index of this node
        m_ownIndex =
                Integer.parseInt(xmlNode.getAttributes().getNamedItem("id")
                        .getNodeValue());
        // and also the majority class up to here
        String cls =
                xmlNode.getAttributes().getNamedItem("class").getNodeValue();
        m_class = mapper.stringToDataCell(cls);
        assert m_class != null;
        // Create HashMap for all class-frequency pairs
        NodeList childNodes = xmlNode.getChildNodes();
        // initialize counter for frequencies of all and this node's class
        m_ownClassFreq = 0.0;
        m_allClassFreq = 0.0;
        // init HashTable and write Class-Frequency pairs
        m_classCounts = new HashMap<DataCell, Double>();
        for (int i = 0; i < childNodes.getLength(); i++) {
            if (childNodes.item(i).getNodeName().equals("CLASSES")) {
                NodeList classesNodes = childNodes.item(i).getChildNodes();
                for (int j = 0; j < classesNodes.getLength(); j++) {
                    Node thisClassFreq = classesNodes.item(j);
                    if (thisClassFreq.getNodeName().equals("FREQUENCY")) {
                        NamedNodeMap attr = thisClassFreq.getAttributes();
                        String cl = attr.getNamedItem("class").getNodeValue();
                        DataCell thisClass = mapper.stringToDataCell(cl);
                        String freq = attr.getNamedItem("freq").getNodeValue();
                        // parse double, fail during debug otherwise assume 0.0
                        double dfreq = 0.0;
                        try {
                            dfreq = Double.parseDouble(freq);
                        } catch (Exception e) {
                            assert false;
                        }
                        m_classCounts.put(thisClass, dfreq);
                        if (thisClass.equals(m_class)) {
                            m_ownClassFreq += dfreq;
                        }
                        m_allClassFreq += dfreq;
                    }
                }
            }
        }
        // done with all the non node-invariant information. All other info
        // needs to be extracted by the constructor of the derived classes.
    }

    /**
     * Constructor of base class. The necessary data is provided directly in the
     * constructor.
     * 
     * @param nodeId the id of this node
     * @param majorityClass the majority class of the records in this node
     * @param classCounts the class distribution of the data in this node
     */
    protected DecisionTreeNode(final int nodeId, final DataCell majorityClass,
            final HashMap<DataCell, Double> classCounts) {

        // read index of this node
        m_ownIndex = nodeId;

        // and also the majority class up to here
        m_class = majorityClass;
        assert m_class != null;

        // initialize counter for frequencies of all and this node's class
        m_ownClassFreq = 0.0;
        m_allClassFreq = 0.0;
        // init HashTable and write Class-Frequency pairs
        m_classCounts = classCounts;
        // initialize counter for frequencies of all and this node's class
        m_ownClassFreq = 0.0;
        m_allClassFreq = 0.0;
        for (Entry<DataCell, Double> entry : m_classCounts.entrySet()) {
            // add the count to the overall counter
            m_allClassFreq += entry.getValue();

            // if the class is the "own" class, add to the "own class" counter
            if (entry.getKey().equals(m_class)) {

                m_ownClassFreq += entry.getValue();
            }
        }
        // done with all the non node-invariant information. All other info
        // needs to be extracted by the constructor of the derived classes.
    }

    /**
     * Create new node from XML-information. Note that this constructor only
     * constructs the node itself and does not generate any other nodes
     * connected to it - it will solely read it's children's indices from the
     * XML file. This function serves as a factory for subclasses that are
     * distinguished here - the constructors of these classes are called and
     * will read their individual information themselves. Most "node" type stuff
     * is handled here, however.
     * 
     * @param xmlNode XML Information for this node
     * @param mapper map translating column names to {@link DataCell}s and vice
     *            versa
     * @return new node initialized from XML node or null if type is not
     *         recognized
     */
    public static DecisionTreeNode createNewNode(final Node xmlNode,
            final DataCellStringMapper mapper) {
        String type =
                xmlNode.getAttributes().getNamedItem("type").getNodeValue();
        if (type.equals("Leaf")) {
            return new DecisionTreeNodeLeaf(xmlNode, mapper);
        }
        if (type.equals("Continuous")) {
            return new DecisionTreeNodeSplitContinuous(xmlNode, mapper);
        }
        if (type.equals("Discrete")) {
            return new DecisionTreeNodeSplitNominal(xmlNode, mapper);
        }
        LOGGER.error("c4.5 Tree Builder, can not handle node of type '" + type
                + "'");
        return null;
    }

    /**
     * Add a new node to the tree structure based on a depth-first indexing
     * strategy.
     * 
     * @param node node to be inserted
     * @param ix index of this node in depth first traversal order
     * @return true only if the node was successfully inserted
     */
    public abstract boolean addNodeToTreeDepthFirst(DecisionTreeNode node,
            int ix);

    /**
     * Return majority class of this node.
     * 
     * @return majority class
     */
    public DataCell getMajorityClass() {
        return m_class;
    }

    /**
     * Return class counts, that is how many patterns (also fractions of) for
     * each class were encountered in this branch during training.
     * 
     * @return class counts
     */
    public HashMap<DataCell, Double> getClassCounts() {
        return m_classCounts;
    }

    /**
     * Classify a new pattern given as a row of values. Returns the class with
     * the maximum count.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return class of pattern the decision tree predicts
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public final DataCell classifyPattern(final DataRow row,
            final DataTableSpec spec) throws Exception {
        HashMap<DataCell, Double> classCounts = getClassCounts(row, spec);
        double winnerCount = -1.0;
        DataCell winner = null;
        for (DataCell classCell : classCounts.keySet()) {
            double thisClassCount = classCounts.get(classCell);
            if (thisClassCount >= winnerCount) {
                winnerCount = thisClassCount;
                winner = classCell;
            }
        }
        if (winner == null) {
            assert false;
            return DataType.getMissingCell();
        }
        return winner;
    }

    /**
     * Determine class counts for a new pattern given as a row of values.
     * Returns a HashMap listing counts for all classes.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return HashMap class/count
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract HashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) throws Exception;

    /**
     * Return number of patterns of correct class (= majority class in a
     * non-risk decision tree). Note that there can be fractions of patterns,
     * for example when we have built a fuzzy decision tree or there were
     * missing values.
     * 
     * @return number (and fractions) of patterns of class of this node
     */
    public double getOwnClassCount() {
        return m_ownClassFreq;
    }

    /**
     * Return number of patterns of all classes. Note that there can be
     * fractions of patterns, for example when we have built a fuzzy decision
     * tree or there were missing values.
     * 
     * @return number (and fractions) of patterns of alss classes
     */
    public double getEntireClassCount() {
        return m_allClassFreq;
    }

    /**
     * Add patterns given as a row of values if they fall within a specific
     * node. Usually only Leafs will actually hold a list of RowKeys, all
     * intermediate nodes will collect "their" information recursively.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract void addCoveredPattern(DataRow row, DataTableSpec spec,
            double weight) throws Exception;

    /**
     * Add colors for a row of values if they fall within a specific
     * node/branch. Used if we don't want to (or can't anymore) store the
     * pattern itself. We still want the color pie chart to be correct.
     * 
     * @param row input pattern
     * @param spec the corresponding table spec
     * @param weight the weight of the row (between 0.0 and 1.0)
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract void addCoveredColor(DataRow row, DataTableSpec spec,
            double weight) throws Exception;

    /**
     * @return set of data cells which are the row keys that are covered by all
     *         nodes of this branch
     */
    public abstract Set<DataCell> coveredPattern();

    /**
     * @return list of colors and coverage counts covered by this node
     */
    public final HashMap<Color, Double> coveredColors() {
        return m_coveredColors;
    }
    

    /**
     * @return index of this node itself
     */
    public int getOwnIndex() {
        return m_ownIndex;
    }

    /**
     * Set parent of this node.
     * 
     * @param parent new parent
     */
    public void setParent(final DecisionTreeNode parent) {
        m_parent = parent;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final String toString() {
        return "[" + m_prefix + "]: " + " class '" + m_class + "' ("
                + m_ownClassFreq + " of " + m_allClassFreq + ")";
    }

    /**
     * Set information about this node, e.g. condition this branch needs to
     * fulfill.
     * 
     * @param pf string describing condition
     */
    public void setPrefix(final String pf) {
        m_prefix = pf;
    }

    /**
     * @return string summary of node content (split, leaf info...)
     */
    public abstract String getStringSummary();

    // ////////////////////////////////////
    // Save & Load to/from ModelContent
    // ////////////////////////////////////

    /**
     * Save node to a model content object.
     * 
     * @param predParams configuration object to attach decision tree to
     * @param saveColorsAndKeys whether to save the colors and the row keys
     */
    public void saveToPredictorParams(final ModelContentWO predParams,
            final boolean saveColorsAndKeys) {
        predParams.addDataCell("class", m_class);
        predParams.addDouble("allClassFreq", m_allClassFreq);
        predParams.addDouble("ownClassFreq", m_ownClassFreq);
        predParams.addInt("ownIndex", m_ownIndex);
        predParams.addString("prefix", m_prefix);
        // also write out frequencies of all classes
        DataCell[] keys = new DataCell[m_classCounts.size()];
        double[] values = new double[m_classCounts.size()];
        int i = 0;
        for (Entry<DataCell, Double> mapIt : m_classCounts.entrySet()) {
            keys[i] = mapIt.getKey();
            values[i] = mapIt.getValue();
            i++;
        }
        predParams.addDataCellArray("classes", keys);
        predParams.addDoubleArray("frequencies", values);
        // now store the class name so we know which derived class we
        // need to re-create when loading this node...
        if (this instanceof DecisionTreeNodeLeaf) {
            predParams.addString("className", "LeafNode");
        } else if (this instanceof DecisionTreeNodeSplitContinuous) {
            predParams.addString("className", "ContinuousSplit");
        } else if (this instanceof DecisionTreeNodeSplitNominal) {
            predParams.addString("className", "NominalSplit");
        } else {
            LOGGER.error("DecisionTreeNode.saveToPredictorParams() doesn't"
                    + " know this node type: " + this.getClass().getName());
        }
        // and finally save all internals of the derived class as well
        saveNodeInternalsToPredParams(predParams, saveColorsAndKeys);
        
        // if the keys and colors are supposed to be stored
        if (saveColorsAndKeys) {
            Config colorsConfig = predParams.addConfig(CONFIG_KEY_COLORS);
            int counter = 0;
            for (Entry<Color, Double> entry : m_coveredColors.entrySet()) {
                Config colorConfig = colorsConfig.addConfig(CONFIG_KEY_COLOR 
                        + "_" + counter);
                colorConfig.addInt(CONFIG_KEY_RED, entry.getKey().getRed());
                colorConfig.addInt(CONFIG_KEY_GREEN, entry.getKey().getGreen());
                colorConfig.addInt(CONFIG_KEY_BLUE, entry.getKey().getBlue());
                colorConfig.addDouble(CONFIG_KEY_COUNT, entry.getValue());
                counter++;
            }
        }
    }

    /**
     * Save internal node settings to a model content object.
     * 
     * @param pConf configuration object to attach decision tree to
     * @param saveKeysAndPatterns whether to save the keys and patterns
     */
    public abstract void saveNodeInternalsToPredParams(
            final ModelContentWO pConf, final boolean saveKeysAndPatterns);

    /**
     * Load node from a model content object.
     * 
     * @param predParams configuration object to load decision tree from
     * @throws InvalidSettingsException if something goes wrong
     */
    public void loadFromPredictorParams(final ModelContentRO predParams)
            throws InvalidSettingsException {
        m_class = predParams.getDataCell("class");
        m_allClassFreq = predParams.getDouble("allClassFreq");
        m_ownClassFreq = predParams.getDouble("ownClassFreq");
        m_ownIndex = predParams.getInt("ownIndex");
        m_prefix = predParams.getString("prefix");
        DataCell[] keys = predParams.getDataCellArray("classes");
        double[] values = predParams.getDoubleArray("frequencies");
        if (keys.length != values.length) {
            throw new InvalidSettingsException("DecisionTreeNode: Can't read"
                    + " class frequencies, array-lenghts don't match!");
        }
        m_classCounts = new HashMap<DataCell, Double>();
        for (int i = 0; i < keys.length; i++) {
            m_classCounts.put(keys[i], values[i]);
        }
        // and finally load all internals of the derived class as well
        loadNodeInternalsFromPredParams(predParams);
        
 
        // if the keys and colors are stored load them
        if (predParams.containsKey(CONFIG_KEY_COLORS)) {
            m_coveredColors.clear();
            Config colorsConfig = predParams.getConfig(CONFIG_KEY_COLORS);
            for (String key : colorsConfig) {
                Config colorConfig = colorsConfig.getConfig(key);
                int red = colorConfig.getInt(CONFIG_KEY_RED);
                int green = colorConfig.getInt(CONFIG_KEY_GREEN);
                int blue = colorConfig.getInt(CONFIG_KEY_BLUE);
                double count = colorConfig.getDouble(CONFIG_KEY_COUNT);
                m_coveredColors.put(new Color(red, green, blue), count);
            }
        }
    }

    /**
     * Creates a new DecisionTreeNode (and all it's children!) based on an model
     * content object.
     * 
     * @param predParams configuration object
     * @param parent the parent node (or <code>null</code> if this is the
     *            root)
     * @return newly created DecisionTreeNode
     * @throws InvalidSettingsException if something goes wrong
     */
    public static DecisionTreeNode createNodeFromPredictorParams(
            final ModelContentRO predParams, final DecisionTreeNode parent)
            throws InvalidSettingsException {
        DecisionTreeNode newNode = null;
        String className = predParams.getString("className");
        if (className.equals("LeafNode")) {
            newNode = new DecisionTreeNodeLeaf();
        } else if (className.equals("ContinuousSplit")) {
            newNode = new DecisionTreeNodeSplitContinuous();
        } else if (className.equals("NominalSplit")) {
            // if this is a binary nominal split
            if (predParams.containsKey("childIndices0")) {
                newNode = new DecisionTreeNodeSplitNominalBinary();
            } else {
                newNode = new DecisionTreeNodeSplitNominal();
            }
        }
        if (newNode == null) {
            throw new InvalidSettingsException("Load DecisionTreeNode failed!"
                    + " Unknown type: " + className);
        }
        newNode.m_parent = parent;
        newNode.loadFromPredictorParams(predParams);
        return newNode;
    }

    /**
     * Load internal node settings from a model content object.
     * 
     * @param pConf configuration object to load decision tree from
     * @throws InvalidSettingsException if something goes wrong
     */
    public abstract void loadNodeInternalsFromPredParams(
            final ModelContentRO pConf) throws InvalidSettingsException;

    // /////////////////////////////////
    // TreeNode implementations
    // /////////////////////////////////

    /**
     * @return count of children
     */
    public abstract int getChildCount();

    /**
     * Returns the index of node in the receivers children. If the receiver does
     * not contain node, -1 will be returned.
     * 
     * @param node that supposedly is a child of this one
     * @return index of node (or -1 if not found)
     */
    public abstract int getIndex(TreeNode node);

    /**
     * @param pos position of child
     * @return child node at index
     */
    public abstract TreeNode getChildAt(int pos);

    /**
     * @return parent of node
     */
    public final TreeNode getParent() {
        return m_parent;
    }
    
    /**
     * Returns the count of the subtree.
     * 
     * @return the count of the subtree
     */
    public abstract int getCountOfSubtree();

    /**
     * @return <code>true</code> if node is a leaf
     */
    public abstract boolean isLeaf();

    /**
     * @return enumeration of all children
     */
    public abstract Enumeration<DecisionTreeNode> children();

    /**
     * @return <code>true</code> if the receiver allows children
     */
    public abstract boolean getAllowsChildren();

    /**
     * Returns the prefix of this node representing the condition.
     * 
     * @return the prefix of this node representing the condition
     */
    public String getPrefix() {
        return m_prefix;
    }
    
    /**
     * Adds the given color to the color map.
     * 
     * @param col the color to add
     * @param weight the weight for the color count
     */
    protected void addColorToMap(final Color col, final double weight) {
        if (m_coveredColors.containsKey(col)) {
            Double oldCount = m_coveredColors.get(col);
            m_coveredColors.remove(col);
            m_coveredColors.put(col, new Double(
                    oldCount.doubleValue() + weight));
        } else {
            m_coveredColors.put(col, new Double(weight));
        }
    }

    /**
     * To get the custom data object.
     * 
     * @return the custom data object
     */
    public Object getCustomData() {
        return m_customData;
    }

    /**
     * To set a custom data object.
     * 
     * @param customData the custom data object to set
     */
    public void setCustomData(final Object customData) {
        m_customData = customData;
    }

    /**
     * Sets the covered colors distribution directly.
     * 
     * @param coveredColors the color distribution to set
     */
    public void setCoveredColors(final HashMap<Color, Double> coveredColors) {
        m_coveredColors = coveredColors;
    }
}
