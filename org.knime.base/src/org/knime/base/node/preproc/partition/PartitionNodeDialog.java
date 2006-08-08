/* 
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
 */
package org.knime.base.node.preproc.partition;

import javax.swing.BorderFactory;

import org.knime.base.node.preproc.sample.SamplingNodeDialogPanel;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;

/**
 * Dialog that allows to define the partitioning of the input table. It is
 * similar to the Sampling node dialog but differs just in the labels.
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class PartitionNodeDialog extends NodeDialogPane {
    private final SamplingNodeDialogPanel m_panel;

    /**
     * Creates the dialog.
     */
    public PartitionNodeDialog() {
        super();
        m_panel = new SamplingNodeDialogPanel();
        m_panel.setBorder(BorderFactory
                .createTitledBorder("Choose size of first partition"));
        super.addTab("First partition", m_panel);
    }

    /**
     * @see NodeDialogPane#loadSettingsFrom(NodeSettingsRO, DataTableSpec[])
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final DataTableSpec[] specs) throws NotConfigurableException {
        m_panel.loadSettingsFrom(settings);
    }

    /**
     * @see NodeDialogPane#saveSettingsTo(NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings)
            throws InvalidSettingsException {
        m_panel.saveSettingsTo(settings);
    }
}
