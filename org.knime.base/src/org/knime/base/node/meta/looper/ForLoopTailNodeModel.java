/* ------------------------------------------------------------------
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
 *   13.02.2008 (thor): created
 */
package org.knime.base.node.meta.looper;

import java.io.File;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnRow;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.workflow.LoopEndNode;
import org.knime.core.node.workflow.ScopeVariable;

/**
 *T his model is the tail node of a for loop.
 *
 * @author Thorsten Meinl, University of Konstanz
 */
public class ForLoopTailNodeModel extends NodeModel implements LoopEndNode {
    private BufferedDataContainer m_resultContainer;

    /**
     * Creates a new model.
     */
    public ForLoopTailNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        return new DataTableSpec[]{createSpec(inSpecs[0])};
    }

    private static DataTableSpec createSpec(final DataTableSpec inSpec) {
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator(DataTableSpec.getUniqueColumnName(
                        inSpec, "Iteration"), IntCell.TYPE);
        DataTableSpec newSpec = new DataTableSpec(crea.createSpec());

        return new DataTableSpec(inSpec, newSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        
        // retrieve variables from the stack which the head of this
        // loop hopefully put there:
        ScopeVariable countVar = peekScopeVariable("LOOP_COUNT");
        int count = countVar.getIntValue();
        ScopeVariable maxCountVar = peekScopeVariable("LOOP_MAXCOUNT");
        int maxCount = maxCountVar.getIntValue();

        if (count == 1) {
            m_resultContainer =
                    exec.createDataContainer(createSpec(inData[0]
                            .getDataTableSpec()));
        }

        IntCell currIterCell = new IntCell(count);
        for (DataRow row : inData[0]) {
            AppendedColumnRow newRow =
                    new AppendedColumnRow(new DefaultRow(new RowKey(row.getKey()
                    + "#" + count), row), currIterCell);
            m_resultContainer.addRowToTable(newRow);
        }

        if (count == maxCount) {
            m_resultContainer.close();
            return new BufferedDataTable[]{m_resultContainer.getTable()};
        } else {
            continueLoop();
            return new BufferedDataTable[1];
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_resultContainer = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    }
}
