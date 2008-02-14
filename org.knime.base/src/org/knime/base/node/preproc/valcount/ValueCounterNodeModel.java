/* Created on 27.03.2007 14:38:48 by thor
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.preproc.valcount;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.property.hilite.DefaultHiLiteHandler;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * This is the model for the value counter node that does all the work.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class ValueCounterNodeModel extends NodeModel {
    private final ValueCounterSettings m_settings = new ValueCounterSettings();

    private static final DataColumnSpec COL_SPEC =
            new DataColumnSpecCreator("count", IntCell.TYPE).createSpec();

    private static final DataTableSpec TABLE_SPEC = new DataTableSpec(COL_SPEC);

    private final HiLiteTranslator m_translator =
            new HiLiteTranslator(new DefaultHiLiteHandler());

    /**
     * Creates a new value counter model.
     */
    public ValueCounterNodeModel() {
        super(1, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        String colName = m_settings.columnName();
        if (colName == null) {
            throw new InvalidSettingsException("No column selected");
        }
        int index = inSpecs[0].findColumnIndex(colName);
        if (index == -1) {
            if (inSpecs[0].getNumColumns() == 1) {
                index = 0;
                m_settings.columnName(inSpecs[0].getColumnSpec(0).getName());
            } else {
                throw new InvalidSettingsException("Column '" + colName
                        + "' does not exist");
            }
        }

        return new DataTableSpec[]{TABLE_SPEC};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int colIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.columnName());
        final double max = inData[0].getRowCount();
        int rowCount = 0;
        Map<DataCell, Set<DataCell>> map =
                new HashMap<DataCell, Set<DataCell>>();
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(rowCount++ / max, map.size()
                    + " different values found");
            DataCell cell = row.getCell(colIndex);
            Set<DataCell> s = map.get(cell);
            if (s == null) {
                s = new HashSet<DataCell>();
                map.put(cell, s);
            }
            s.add(row.getKey().getId());
        }

        final DataValueComparator comp =
                inData[0].getDataTableSpec().getColumnSpec(colIndex).getType()
                        .getComparator();

        List<Map.Entry<DataCell, Set<DataCell>>> sorted =
                new ArrayList<Map.Entry<DataCell, Set<DataCell>>>(map
                        .entrySet());
        Collections.sort(sorted,
                new Comparator<Map.Entry<DataCell, Set<DataCell>>>() {
                    public int compare(
                            final Map.Entry<DataCell, Set<DataCell>> o1,
                            final Entry<DataCell, Set<DataCell>> o2) {
                        return comp.compare(o1.getKey(), o2.getKey());
                    }
                });

        BufferedDataContainer cont = exec.createDataContainer(TABLE_SPEC);
        for (Map.Entry<DataCell, Set<DataCell>> entry : sorted) {
            cont.addRowToTable(new DefaultRow(entry.getKey(), new int[]{entry
                    .getValue().size()}));
        }
        cont.close();

        if (m_settings.hiliting()) {
            m_translator.setMapper(new DefaultHiLiteMapper(map));
        } else {
            m_translator.setMapper(new DefaultHiLiteMapper(
                    new HashMap<DataCell, Set<DataCell>>()));
        }
        return new BufferedDataTable[]{cont.getTable()};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "Hiliting.conf.gz");
        if (f.exists() && f.canRead()) {
            InputStream in = new GZIPInputStream(new BufferedInputStream(
                    new FileInputStream(f)));
            NodeSettingsRO s = NodeSettings.loadFromXML(in);
            in.close();
            try {
                m_translator.setMapper(DefaultHiLiteMapper.load(s));
            } catch (InvalidSettingsException ex) {
                throw new IOException(ex.getMessage());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_translator.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        NodeSettings s = new NodeSettings("Hiliting");
        ((DefaultHiLiteMapper)m_translator.getMapper()).save(s);
        File f = new File(nodeInternDir, "Hiliting.conf.gz");
        OutputStream out = new GZIPOutputStream(new BufferedOutputStream(
                new FileOutputStream(f)));
        s.saveToXML(out);
        out.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        ValueCounterSettings s = new ValueCounterSettings();
        s.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int id, final HiLiteHandler hdl) {
        assert (id == 0);
        m_translator.removeAllToHiliteHandlers();
        m_translator.addToHiLiteHandler(hdl);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_translator.getFromHiLiteHandler();
    }
}
