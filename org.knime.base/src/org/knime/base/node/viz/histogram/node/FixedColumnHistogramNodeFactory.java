/*
 * ------------------------------------------------------------------
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
 *   11.06.2006 (Tobias Koetter): created
 */
package org.knime.base.node.viz.histogram.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;

/**
 * Factory class of the histogram.
 * @author Tobias Koetter, University of Konstanz
 */
public class FixedColumnHistogramNodeFactory extends NodeFactory {
    /**
     * {@inheritDoc}
     */
    @Override
    public NodeModel createNodeModel() {
        return new FixedColumnHistogramNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView createNodeView(final int viewIndex,
            final NodeModel nodeModel) {
        assert viewIndex == 0;
        return new FixedColumnHistogramNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new FixedColumnHistogramDialogPane();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }
}
