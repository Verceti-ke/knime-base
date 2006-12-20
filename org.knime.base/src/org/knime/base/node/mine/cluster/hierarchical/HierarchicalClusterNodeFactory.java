/*
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
 */
package org.knime.base.node.mine.cluster.hierarchical;


import org.knime.base.node.viz.plotter.dendrogram.DendrogramPlotter;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * The Factory for the hierarchical clustering node.
 * 
 * @author Christoph Sieb, University of Konstanz
 */
public class HierarchicalClusterNodeFactory extends NodeFactory {

    /**
     * @see org.knime.core.node.NodeFactory#createNodeModel()
     */
    @Override
    public NodeModel createNodeModel() {
        
        return new HierarchicalClusterNodeModel(); // new ManhattanDist());
    }

    /**
     * @see org.knime.core.node.NodeFactory#getNrNodeViews()
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeView(int, NodeModel)
     */
    @Override
    public NodeView createNodeView(final int i, final NodeModel nodeModel) {
        if (i != 0) {
            throw new IllegalArgumentException();
        }
        HierarchicalClusterNodeView view = new HierarchicalClusterNodeView(
                nodeModel, new DendrogramPlotter());
        return view;
    }
    
    /**
     * @return <b>true</b>.
     * @see org.knime.core.node.NodeFactory#hasDialog()
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * @see org.knime.core.node.NodeFactory#createNodeDialogPane()
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new HierarchicalClusterNodeDialog();
    }
}
