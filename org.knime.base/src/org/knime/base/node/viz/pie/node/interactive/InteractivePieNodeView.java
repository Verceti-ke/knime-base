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
 * History
 *    21.09.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.pie.node.interactive;

import org.knime.base.node.viz.pie.datamodel.interactive.InteractivePieVizModel;
import org.knime.base.node.viz.pie.impl.PiePlotter;
import org.knime.base.node.viz.pie.impl.interactive.InteractivePiePlotter;
import org.knime.base.node.viz.pie.impl.interactive.InteractivePieProperties;
import org.knime.base.node.viz.pie.node.PieNodeView;
import org.knime.core.node.NodeModel;
import org.knime.core.node.property.hilite.HiLiteHandler;


/**
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractivePieNodeView
extends PieNodeView<InteractivePieProperties, InteractivePieVizModel> {

    /**Constructor for class InteractivePieNodeView.
     * @param nodeModel the node model
     */
    protected InteractivePieNodeView(final NodeModel nodeModel) {
        super(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PiePlotter<InteractivePieProperties, InteractivePieVizModel>
    getPlotter(final InteractivePieVizModel vizModel,
            final HiLiteHandler handler) {
        final InteractivePieProperties properties =
            new InteractivePieProperties(vizModel);
        return new InteractivePiePlotter(properties, handler);
    }
}
