/* 
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2006
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
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
 *   28.09.2005 (User): created
 */
package org.knime.base.node.mine.decisiontree.predictor.decisiontree;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import javax.swing.tree.TreeNode;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.w3c.dom.Node;

import org.knime.base.data.util.DataCellStringMapper;

/**
 * An abstract implementation of an inner node of a decision tree, i.e. one that
 * is not a leaf. It mostly holds information about children.
 * 
 * @author Michael Berthold, University of Konstanz
 */
public abstract class DecisionTreeNodeSplit extends DecisionTreeNode {
    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(DecisionTreeNodeSplitContinuous.class);

    private String m_splitAttr;

    private DecisionTreeNode[] m_child;

    private int[] m_childIndex;

    /**
     * Empty Constructor visible only within package.
     */
    DecisionTreeNodeSplit() {
    }

    /**
     * Constructor of derived class. Read all type-specific information from XML
     * File.
     * 
     * @param xmlNode XML node info
     * @param mapper map translating column names to {@link DataCell}s and vice
     *            versa
     */
    public DecisionTreeNodeSplit(final Node xmlNode,
            final DataCellStringMapper mapper) {
        super(xmlNode, mapper); // let super read all type-invariant info
        makeRoomForKids(2);
        // now read information related to a split on a continuous attribute
        Node splitNode = xmlNode.getChildNodes().item(3);
        assert splitNode.getNodeName().equals("SPLIT");
        String splitAttr = splitNode.getAttributes().getNamedItem("attribute")
                .getNodeValue();
        m_splitAttr = mapper.stringToOrigString(splitAttr);
    }

    /**
     * Return name of attribute this node splits on.
     * 
     * @return string the name of the column used for the split
     */
    public String getSplitAttr() {
        return m_splitAttr;
    }

    /**
     * Reserve space for specific number of kids.
     * 
     * @param nrKids number of children attached to this node
     */
    protected void makeRoomForKids(final int nrKids) {
        m_childIndex = new int[nrKids];
        m_child = new DecisionTreeNode[nrKids];
    }

    /**
     * Mark index of child node at a specific branch.
     * 
     * @param pos position of branch at this node
     * @param index index of child node
     */
    protected void setChildNodeIndex(final int pos, final int index) {
        assert m_childIndex != null;
        assert pos >= 0 && pos < m_childIndex.length;
        m_childIndex[pos] = index;
    }

    /**
     * Return DecisionTreeNode at specific branch.
     * 
     * @param pos position of branch
     * @return node attached to this branch
     */
    protected DecisionTreeNode getChildNodeAt(final int pos) {
        assert m_child != null;
        assert pos >= 0 && pos < m_child.length;
        return m_child[pos];
    }

    /**
     * Add a new node to the tree structure based on a depth-first indexing
     * strategy.
     * 
     * @param node node to be inserted
     * @param ix index of this node in depth first traversal order
     * @return true only if the node was successfully inserted
     */
    @Override
    public boolean addNodeToTreeDepthFirst(final DecisionTreeNode node,
            final int ix) {
        assert m_childIndex != null;
        if (ix < m_childIndex[0]) {
            // can't be true, all lower indices should already have been used
            return false;
        }
        int kidpos = 0;
        while (kidpos < m_childIndex.length) {
            if (ix == m_childIndex[kidpos]) {
                // node is (hopefully first and only) child for this position
                if (m_child[kidpos] != null) {
                    return false;
                }
                m_child[kidpos] = node;
                m_childIndex[kidpos] = ix;
                m_child[kidpos].setParent(this);
                return true;
            }
            if ((kidpos < m_childIndex.length - 1)
                    && (ix < m_childIndex[kidpos + 1])) {
                // node's index is below next kid's, has to fit into this branch
                if (m_child[kidpos] == null) {
                    return false;
                }
                return m_child[kidpos].addNodeToTreeDepthFirst(node, ix);
            }
            kidpos++;
        }
        int rightMostKid = m_childIndex.length - 1;
        assert (ix > m_childIndex[rightMostKid]);
        // node must fit into the rightmost branch
        if (m_child[rightMostKid] == null) {
            return false;
        }
        return m_child[rightMostKid].addNodeToTreeDepthFirst(node, ix);
    }

    // remember prev table spec and index used for classification to save time
    private transient DataTableSpec m_previousSpec = null;

    private transient int m_previousIndex = -1;

    /**
     * @see DecisionTreeNode
     *      #getClassCounts(org.knime.core.data.DataRow,
     *      org.knime.core.data.DataTableSpec)
     */
    @Override
    public final HashMap<DataCell, Double> getClassCounts(final DataRow row,
            final DataTableSpec spec) throws Exception {
        assert (spec != null);
        if (m_splitAttr != null) {
            if (spec != m_previousSpec) {
                m_previousIndex = spec.findColumnIndex(m_splitAttr);
                if (m_previousIndex == -1) {
                    LOGGER.error(spec.toString());
                    throw new Exception("Decision Tree Prediction failed."
                            + " Could not find attribute '"
                            + m_splitAttr.toString() + "'");
                }
                m_previousSpec = spec;
            }
            assert (m_previousIndex != -1);
            DataCell cell = row.getCell(m_previousIndex);
            if (cell.isMissing()) {
                // if we can not determine the split at this node because
                // value is missing, we have to combine all class weights
                // from _all_ branches
                // initialize result HashMap
                HashMap<DataCell, Double> result 
                    = new HashMap<DataCell, Double>();
                // check each branch for it's counts and add them up
                for (DecisionTreeNode nodeIt : m_child) {
                    HashMap<DataCell, Double> thisNodeCounts = nodeIt
                            .getClassCounts();
                    for (DataCell cellIt : thisNodeCounts.keySet()) {
                        // if entry for this class already exist, modify
                        // value, otherwise insert new one
                        if (result.containsKey(cellIt)) {
                            double newCount = thisNodeCounts.get(cellIt)
                                    + result.get(cellIt);
                            result.remove(cellIt);
                            result.put(cellIt, newCount);
                        } else {
                            result.put(cellIt, thisNodeCounts.get(cellIt));
                        }
                    }
                }
                // return result
                return result;
            }
            return getClassCounts(cell, row, spec);
        }
        return getClassCounts(null, row, spec);
    }

    /**
     * Determine class counts for a new pattern given as a row of values.
     * Returns a HashMap listing counts for all classes.
     * 
     * @param cell the call to be used for classification at this node
     * @param row input pattern
     * @param spec the corresponding table spec
     * @return HashMap class/count
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract HashMap<DataCell, Double> getClassCounts(
            final DataCell cell, final DataRow row, final DataTableSpec spec)
            throws Exception;

    /**
     * @see DecisionTreeNode
     *      #addCoveredPattern(org.knime.core.data.DataRow,
     *      org.knime.core.data.DataTableSpec)
     */
    @Override
    public final void addCoveredPattern(final DataRow row,
            final DataTableSpec spec) throws Exception {
        assert (spec != null);
        if (m_splitAttr != null) {
            if (spec != m_previousSpec) {
                m_previousIndex = spec.findColumnIndex(m_splitAttr);
                if (m_previousIndex == -1) {
                    LOGGER.error(spec.toString());
                    throw new Exception("Decision Tree Prediction failed."
                           + " Could not find attribute '" + m_splitAttr + "'");
                }
                m_previousSpec = spec;
            }
            assert (m_previousIndex != -1);
            DataCell cell = row.getCell(m_previousIndex);
            if (cell.isMissing()) {
                // of we can not determine the split at this node because
                // value is missing, we add the row to each child
                for (DecisionTreeNode child : m_child) {
                    child.addCoveredPattern(row, spec);
                }
                return;
            }
            addCoveredPattern(cell, row, spec);
            return;
        }
        addCoveredPattern(null, row, spec);
    }

    /**
     * Add patterns given as a row of values if they fall within a specific
     * node. Usually only Leafs will actually hold a list of RowKeys, all
     * intermediate nodes will collect "their" information recursively.
     * 
     * @param cell the call to be used for classification at this node
     * @param row input pattern
     * @param spec the corresponding table spec
     * @throws Exception if something went wrong (unknown attriubte for example)
     */
    public abstract void addCoveredPattern(DataCell cell, DataRow row,
            DataTableSpec spec) throws Exception;

    /**
     * @see DecisionTreeNode
     *      #coveredPattern()
     */
    @Override
    public abstract Set<DataCell> coveredPattern();

    /**
     * @see DecisionTreeNode
     *      #saveNodeInternalsToPredParams(org.knime.core.node.ModelContentWO)
     */
    @Override
    public final void saveNodeInternalsToPredParams(
            final ModelContentWO pConf) {
        saveNodeSplitInternalsToPredParams(pConf);
        pConf.addString("splitAttribute", m_splitAttr);
        pConf.addInt("nrChildren", m_child.length);
        for (int i = 0; i < m_child.length; i++) {
            ModelContentWO newChildConf = pConf.addModelContent("child" + i);
            newChildConf.addInt("index", m_childIndex[i]);
            m_child[i].saveToPredictorParams(newChildConf);
        }
    }

    /**
     * save internal SplitNode settings to a ModelContent object.
     * 
     * @param pConf configuration object to save decision tree to
     */
    public abstract void saveNodeSplitInternalsToPredParams(
            final ModelContentWO pConf);

    /**
     * @see DecisionTreeNode
     *      #loadNodeInternalsFromPredParams(org.knime.core.node.ModelContentRO)
     */
    @Override
    public final void loadNodeInternalsFromPredParams(
            final ModelContentRO pConf)
            throws InvalidSettingsException {
        loadNodeSplitInternalsFromPredParams(pConf);
        m_splitAttr = pConf.getString("splitAttribute");
        int nrKids = pConf.getInt("nrChildren");
        m_child = new DecisionTreeNode[nrKids];
        m_childIndex = new int[nrKids];
        for (int i = 0; i < nrKids; i++) {
            ModelContentRO newChildConf = pConf.getModelContent("child" + i);
            m_child[i] = DecisionTreeNode.createNodeFromPredictorParams(
                    newChildConf, this);
            int kidIndex = newChildConf.getInt("index");
            if (kidIndex != m_child[i].getOwnIndex()) {
                throw new InvalidSettingsException("DecisionTreeNode: Expected"
                        + " index does not match real index: " + kidIndex
                        + " != " + m_child[i].getOwnIndex());
            }
        }

        // no need to store or load these, since they are only used to
        // speed up subsequent access to the same attribute:
        m_previousSpec = null;
        m_previousIndex = -1;
    }

    /**
     * Load internal SplitNode settings from a ModelContent object.
     * 
     * @param pConf configuration object to load decision tree from.
     * @throws InvalidSettingsException if something goes wrong
     */
    public abstract void loadNodeSplitInternalsFromPredParams(
            final ModelContentRO pConf) throws InvalidSettingsException;

    /**
     * @see javax.swing.tree.TreeNode#getChildCount()
     */
    @Override
    public int getChildCount() {
        if (m_childIndex == null) {
            return 0;
        }
        return m_childIndex.length;
    }

    /**
     * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
     */
    @Override
    public int getIndex(final TreeNode node) {
        if (m_childIndex == null) {
            // no kids!
            return -1;
        }
        for (int i = 0; i < m_child.length; i++) {
            if (node == m_child[i]) {
                // return position as index if node is one of the kids
                return i;
            }
        }
        // node not found:
        return -1;
    }

    /**
     * @see javax.swing.tree.TreeNode#getChildAt(int)
     */
    @Override
    public TreeNode getChildAt(final int pos) {
        assert m_child != null;
        assert pos >= 0 && pos < m_child.length;
        return m_child[pos];
    }

    /**
     * @see javax.swing.tree.TreeNode#isLeaf()
     */
    @Override
    public boolean isLeaf() {
        return m_child == null;
    }

    /**
     * @return enumeration of all children
     */
    @Override
    public Enumeration children() {
        // TODO: fix. JTree doesn't seem to need it, luckily.
        /*
         * if (m_leftChild != null) { children.add(m_leftChild); } if
         * (m_rightChild != null) { children.add(m_rightChild); } return
         * children; Vector children = new Vector();
         */
        return null;
    }

    /**
     * @see javax.swing.tree.TreeNode#getAllowsChildren()
     */
    @Override
    public boolean getAllowsChildren() {
        return m_child != null;
    }
}
