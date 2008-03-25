/*
 * -------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 */
package org.knime.base.node.io.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.DatabasePortObject;
import org.knime.core.node.DatabasePortObjectSpec;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.GenericNodeModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.PortObject;
import org.knime.core.node.PortObjectSpec;
import org.knime.core.node.PortType;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
final class DBConnectionNodeModel extends GenericNodeModel {
    
    private DataTableSpec m_lastSpec = null;
    
    /** Config key to write last processed spec. */
    static final String CFG_SPEC_XML = "spec.xml";
    
    /**
     * Creates a new database connection reader.
     * @param inPorts array of in-port types
     * @param outPorts array of out-port types
     */
    DBConnectionNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws CanceledExecutionException,
            Exception {
        exec.setProgress("Opening database connection...");
        DatabasePortObject dbObj = (DatabasePortObject) inData[0];
        DBQueryConnection conn = new DBQueryConnection();
        conn.loadValidatedConnection(dbObj.getConnectionModel());
        DBReaderConnection load = new DBReaderConnection(conn, conn.getQuery());
        m_lastSpec = load.getDataTableSpec();
        exec.setProgress("Reading data from database...");
        return new BufferedDataTable[]{
                exec.createBufferedDataTable(load, exec)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        File specFile = new File(nodeInternDir, CFG_SPEC_XML);
        if (!specFile.exists()) {
            throw new IOException("Spec file (\"" 
                    + specFile.getAbsolutePath() + "\") does not exist "
                    + "(node may have been saved by an older version!)");
        }
        NodeSettingsRO specSett = 
            NodeSettings.loadFromXML(new FileInputStream(specFile));
        try {
            m_lastSpec = DataTableSpec.load(specSett);
        } catch (InvalidSettingsException ise) {
            IOException ioe = new IOException("Could not read last spec.");
            ioe.initCause(ise);
            throw ioe;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException {
        NodeSettings specSett = new NodeSettings(CFG_SPEC_XML);
        if (m_lastSpec != null) {
            m_lastSpec.save(specSett);
        }
        File specFile = new File(nodeInternDir, CFG_SPEC_XML);
        specSett.saveToXML(new FileOutputStream(specFile));
    }
 
    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) 
            throws InvalidSettingsException {
        if (m_lastSpec != null) {
            return new DataTableSpec[]{m_lastSpec};
        }
        try {
            DatabasePortObjectSpec dbSpec = (DatabasePortObjectSpec) inSpecs[0];
            DBQueryConnection conn = new DBQueryConnection();
            conn.loadValidatedConnection(dbSpec.getConnectionModel());
            DBReaderConnection reader = 
                new DBReaderConnection(conn, conn.getQuery());
            m_lastSpec = reader.getDataTableSpec();
        } catch (SQLException e) {
            throw new InvalidSettingsException(e.getMessage());
        } catch (Exception e) {
            throw new InvalidSettingsException("Could not decrypt password.");
        }
        return new DataTableSpec[]{m_lastSpec};
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        
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
    protected void saveSettingsTo(final NodeSettingsWO settings) {

    }
    
}
