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
 * ------------------------------------------------------------------- * 
 */
package org.knime.base.node.mine.regression.polynomial.learner;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.knime.base.data.append.column.AppendedColumnTable;
import org.knime.base.node.util.DataArray;
import org.knime.base.node.util.DefaultDataArray;
import org.knime.base.node.viz.plotter.DataProvider;
import org.knime.base.util.math.MathUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.container.CellFactory;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.ContainerTable;
import org.knime.core.data.container.DataContainer;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * This node performs polynomial regression on an input table with numeric-only
 * columns. The user can choose the maximum degree the built polynomial should
 * have.
 * 
 * @author Thorsten Meinl, University of Konstanz
 */
public class PolyRegLearnerNodeModel extends NodeModel implements DataProvider {
    private final PolyRegLearnerSettings m_settings =
            new PolyRegLearnerSettings();

    private double[] m_betas;

    private double m_squaredError;

    private String[] m_columnNames;

    private DataArray m_rowContainer;

    private double[] m_meanValues;

    /**
     * Creates a new model for the polynomial regression learner node.
     */
    public PolyRegLearnerNodeModel() {
        super(1, 1, 0, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        for (DataColumnSpec dcs : inSpecs[0]) {
            if (!dcs.getType().isCompatible(DoubleValue.class)) {
                throw new InvalidSettingsException("The column '"
                        + dcs.getName()
                        + "' from the input table is not a numeric column.");
            }
        }

        if (m_settings.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
        if (inSpecs[0].findColumnIndex(m_settings.getTargetColumn()) == -1) {
            throw new InvalidSettingsException("Target column '"
                    + m_settings.getTargetColumn() + "' does not exist.");
        }

        DataColumnSpecCreator crea =
                new DataColumnSpecCreator("PolyReg prediction", DoubleCell.TYPE);
        DataColumnSpec col1 = crea.createSpec();

        crea = new DataColumnSpecCreator("Prediction Error", DoubleCell.TYPE);
        DataColumnSpec col2 = crea.createSpec();

        return new DataTableSpec[]{AppendedColumnTable.getTableSpec(inSpecs[0],
                col1, col2)};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        final int independentVariables =
                inData[0].getDataTableSpec().getNumColumns() - 1;
        final int degree = m_settings.getDegree();
        double max = inData[0].getRowCount();
        final int dependantIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.getTargetColumn());

        double[][] xMat =
                new double[inData[0].getRowCount()][1 + independentVariables
                        * degree];
        double[][] yMat = new double[inData[0].getRowCount()][1];

        int rowIndex = 0;
        for (DataRow row : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(0.2 * rowIndex / max);
            xMat[rowIndex][0] = 1;
            int colIndex = 1;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (row.getCell(i).isMissing()) {
                    throw new Exception(
                            "Missing values are not supported by this node.");
                }
                double val = ((DoubleValue)row.getCell(i)).getDoubleValue();

                if (i != dependantIndex) {
                    double poly = val;
                    xMat[rowIndex][colIndex] = poly;
                    colIndex++;

                    for (int d = 2; d <= degree; d++) {
                        poly *= val;
                        xMat[rowIndex][colIndex] = poly;
                        colIndex++;
                    }
                } else {
                    yMat[rowIndex][0] = val;
                }
            }
            rowIndex++;
        }

        // compute X'
        double[][] xTransMat = MathUtils.transpose(xMat);
        exec.setProgress(0.24);
        exec.checkCanceled();
        // compute X'X
        double[][] xxMat = MathUtils.multiply(xTransMat, xMat);
        exec.setProgress(0.28);
        exec.checkCanceled();
        // compute X'Y
        double[][] xyMat = MathUtils.multiply(xTransMat, yMat);
        exec.setProgress(0.32);
        exec.checkCanceled();

        // compute (X'X)^-1
        double[][] xxInverse;
        try {
            xxInverse = MathUtils.inverse(xxMat);
            exec.setProgress(0.36);
            exec.checkCanceled();
        } catch (ArithmeticException ex) {
            throw new ArithmeticException("The attributes of the data samples"
                    + " are not mutually independent.");
        }

        // compute (X'X)^-1 * (X'Y)
        final double[][] betas = MathUtils.multiply(xxInverse, xyMat);
        exec.setProgress(0.4);

        // ColumnRearranger crea =
        // new ColumnRearranger(inData[1].getDataTableSpec());
        // crea.append(createCellFactory(betas, inData));

        m_betas = new double[independentVariables * degree + 1];
        for (int i = 0; i < betas.length; i++) {
            m_betas[i] = betas[i][0];
        }

        m_columnNames = new String[independentVariables];
        int m = 0;
        for (DataColumnSpec dcs : inData[0].getDataTableSpec()) {
            if (!dcs.getName().equals(m_settings.getTargetColumn())) {
                m_columnNames[m++] = dcs.getName();
            }
        }

        m_rowContainer =
                new DefaultDataArray(inData[0], 1, m_settings
                        .getMaxRowsForView());

        m_meanValues = new double[independentVariables];
        for (DataRow row : m_rowContainer) {
            int k = 0;
            for (int i = 0; i < row.getNumCells(); i++) {
                if (i != dependantIndex) {
                    m_meanValues[k++] +=
                            ((DoubleValue)row.getCell(i)).getDoubleValue();
                }
            }
        }
        for (int i = 0; i < m_meanValues.length; i++) {
            m_meanValues[i] /= m_rowContainer.size();
        }

        ColumnRearranger crea =
                new ColumnRearranger(inData[0].getDataTableSpec());
        crea.append(getCellFactory(dependantIndex));

        BufferedDataTable[] bdt =
                new BufferedDataTable[]{exec.createColumnRearrangeTable(
                        inData[0], crea, exec.createSubProgress(0.6))};
        m_squaredError /= inData[0].getRowCount();
        return bdt;
    }

    private CellFactory getCellFactory(final int dependantIndex) {
        final int degree = m_settings.getDegree();

        return new CellFactory() {
            public DataCell[] getCells(final DataRow row) {
                double sum = m_betas[0];
                int betaCount = 1;
                double y = 0;
                for (int col = 0; col < row.getNumCells(); col++) {
                    if (col != dependantIndex) {
                        final double value =
                                ((DoubleValue)row.getCell(col))
                                        .getDoubleValue();
                        double poly = 1;
                        for (int d = 1; d <= degree; d++) {
                            poly *= value;
                            sum += m_betas[betaCount++] * poly;
                        }
                    } else {
                        y = ((DoubleValue)row.getCell(col)).getDoubleValue();
                    }
                }

                double err = Math.abs(sum - y);
                m_squaredError += err * err;

                return new DataCell[]{new DoubleCell(sum), new DoubleCell(err)};
            }

            public DataColumnSpec[] getColumnSpecs() {
                DataColumnSpecCreator crea =
                        new DataColumnSpecCreator("PolyReg prediction",
                                DoubleCell.TYPE);
                DataColumnSpec col1 = crea.createSpec();

                crea =
                        new DataColumnSpecCreator("Prediction Error",
                                DoubleCell.TYPE);
                DataColumnSpec col2 = crea.createSpec();
                return new DataColumnSpec[]{col1, col2};
            }

            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor execMon) {
                // do nothing
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        File f = new File(nodeInternDir, "internals.xml");
        if (f.exists()) {
            NodeSettingsRO internals =
                    NodeSettings.loadFromXML(new BufferedInputStream(
                            new FileInputStream(f)));
            try {
                m_betas = internals.getDoubleArray("betas");
                m_columnNames = internals.getStringArray("columnNames");
                m_squaredError = internals.getDouble("squarredError");
                m_meanValues = internals.getDoubleArray("meanValues");
            } catch (InvalidSettingsException ex) {
                throw new IOException("Old or corrupt internals");
            }
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }

        f = new File(nodeInternDir, "data.zip");
        if (f.exists()) {
            ContainerTable t = DataContainer.readFromZip(f);
            int rowCount = t.getRowCount();
            m_rowContainer = new DefaultDataArray(t, 1, rowCount, exec);
        } else {
            throw new FileNotFoundException("Internals do not exist");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_betas = null;
        m_columnNames = null;
        m_squaredError = 0;
        m_rowContainer = null;
        m_meanValues = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        if (m_betas != null) {
            NodeSettings internals = new NodeSettings("internals");
            internals.addDoubleArray("betas", m_betas);
            internals.addStringArray("columnNames", m_columnNames);
            internals.addDouble("squarredError", m_squaredError);
            internals.addDoubleArray("meanValues", m_meanValues);

            internals.saveToXML(new BufferedOutputStream(new FileOutputStream(
                    new File(nodeInternDir, "internals.xml"))));

            File dataFile = new File(nodeInternDir, "data.zip");
            DataContainer.writeToZip(m_rowContainer, dataFile, exec);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        PolyRegLearnerSettings s = new PolyRegLearnerSettings();
        s.loadSettingsFrom(settings);

        if (s.getTargetColumn() == null) {
            throw new InvalidSettingsException("No target column selected");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveModelContent(final int index,
            final ModelContentWO predParams) throws InvalidSettingsException {
        predParams.addInt("degree", m_settings.getDegree());
        predParams.addStringArray("columnNames", m_columnNames);
        predParams.addDoubleArray("betas", m_betas);
        predParams.addDouble("squaredErrorPerRow", m_squaredError);
    }

    /**
     * Returns the learned beta values.
     * 
     * @return the beta values
     */
    double[] getBetas() {
        return m_betas;
    }

    /**
     * Returns the column names.
     * 
     * @return the column names
     */
    String[] getColumnNames() {
        return m_columnNames;
    }

    /**
     * Returns the total squarred error.
     * 
     * @return the squarred error
     */
    double getSquarredError() {
        return m_squaredError;
    }

    /**
     * Returns the degree of the regression function.
     * 
     * @return the degree
     */
    int getDegree() {
        return m_settings.getDegree();
    }

    /**
     * Returns the target column's name.
     * 
     * @return the target column's name
     */
    String getTargetColumn() {
        return m_settings.getTargetColumn();
    }

    /**
     * Returns the mean value of each input column.
     * 
     * @return the mean values
     */
    double[] getMeanValues() {
        return m_meanValues;
    }

    /**
     * {@inheritDoc}
     */
    public DataArray getDataArray(final int index) {
        return m_rowContainer;
    }
}
