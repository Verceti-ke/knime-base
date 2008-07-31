/*
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ---------------------------------------------------------------------
 * 
 * History
 *   Feb 25, 2008 (sellien): created
 */
package org.knime.base.node.viz.condbox;

import org.knime.core.node.GenericNodeFactory;
import org.knime.core.node.GenericNodeView;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeModel;

/**
 * This class creates a ConditionalBoxPlotNode.
 * 
 * @author Stephan Sellien, University of Konstanz
 * 
 */
public class ConditionalBoxPlotNodeFactory 
    extends GenericNodeFactory<NodeModel> {
    /**
     * {@inheritDoc}
     */
    @Override
    protected NodeDialogPane createNodeDialogPane() {
        return new ConditionalBoxPlotNodeDialog();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new ConditionalBoxPlotNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericNodeView<NodeModel> createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        return new ConditionalBoxPlotNodeView(
                (ConditionalBoxPlotNodeModel)nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean hasDialog() {
        return true;
    }

}
