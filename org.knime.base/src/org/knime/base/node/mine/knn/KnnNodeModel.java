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
package org.knime.base.node.mine.knn;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.base.util.kdtree.KDTree;
import org.knime.base.util.kdtree.KDTreeBuilder;
import org.knime.base.util.kdtree.NearestNeighbour;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.MutableDouble;

/**
 * This is the model for the k Nearest Neighbor node. In contrast to most
 * learner/predictor combinations this is "all in one" since the model here
 * really stores all of the training data.
 * 
 * @author Michael Berthold, University of Konstanz
 * @author Thorsten Meinl, University of Konstanz
 */
public class KnnNodeModel extends NodeModel {
    private KnnSettings m_settings = new KnnSettings();

    /**
     * Creates a new model for the kNN node.
     */
    public KnnNodeModel() {
        super(2, 1);
    }

    /**
     * Checks if the two input tables are correct and fills the last two
     * arguments with sensible values.
     * 
     * @param inSpecs the input tables' specs
     * @param featureColumns a list that gets filled with the feature columns'
     *            indices; all columns with {@link DoubleValue}s are used as
     *            features
     * @param firstToSecond a map that afterwards maps the indices of the
     *            feature columns in the first table to the corresponding
     *            columns from the second table
     * @throws InvalidSettingsException if the two tables are not compatible
     */
    private void checkInputTables(final DataTableSpec[] inSpecs,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond)
            throws InvalidSettingsException {
        if (!inSpecs[0].containsCompatibleType(DoubleValue.class)) {
            throw new InvalidSettingsException(
                    "First input table does not contain a numeric column.");
        }
        if (!inSpecs[0].containsCompatibleType(StringValue.class)) {
            throw new InvalidSettingsException(
                    "First input table does not contain a class column of type "
                            + "string.");
        }

        int i = 0;
        for (DataColumnSpec cs : inSpecs[0]) {
            if (cs.getType().isCompatible(DoubleValue.class)) {
                featureColumns.add(i);
            } else if (!cs.getName().equals(m_settings.classColumn())) {
                setWarningMessage("Input table contains more than one "
                        + "non-numeric column; they will be ignored.");
            }
            i++;
        }

        for (int k : featureColumns) {
            DataColumnSpec cs = inSpecs[0].getColumnSpec(k);
            int secondColIndex = inSpecs[1].findColumnIndex(cs.getName());
            if (secondColIndex == -1) {
                throw new InvalidSettingsException(
                        "Second input table does not"
                                + " contain a column named '" + cs.getName()
                                + "'");
            }

            if (inSpecs[1].getColumnSpec(secondColIndex).equalStructure(cs)) {
                firstToSecond.put(k, secondColIndex);
            } else {
                throw new InvalidSettingsException("Column '" + cs.getName()
                        + "' from the second table is not compatible with the "
                        + "corresponding column from the first table.");
            }
        }
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #configure(org.knime.core.data.DataTableSpec[])
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        int classColIndex =
                inSpecs[0].findColumnIndex(m_settings.classColumn());
        if (classColIndex == -1) {
            DataColumnSpec colSpec = null;
            for (DataColumnSpec cs : inSpecs[0]) {
                if (cs.getType().isCompatible(NominalValue.class)) {
                    if (colSpec != null) {
                        throw new InvalidSettingsException(
                                "Please choose a valid class column");
                    }
                    colSpec = cs;
                }
            }

            if (colSpec == null) {
                throw new InvalidSettingsException(
                        "Please choose a valid class column.");
            }
            m_settings.classColumn(colSpec.getName());
            setWarningMessage("Auto-selected column '" + colSpec.getName()
                    + "' as class column.");
        }

        List<Integer> featureColumns = new ArrayList<Integer>();
        Map<Integer, Integer> secondIndex = new HashMap<Integer, Integer>();
        checkInputTables(inSpecs, featureColumns, secondIndex);

        DataColumnSpec[] colSpecs =
                new DataColumnSpec[inSpecs[1].getNumColumns() + 1];
        for (int i = 0; i < colSpecs.length - 1; i++) {
            colSpecs[i] = inSpecs[1].getColumnSpec(i);
        }
        DataColumnSpecCreator crea =
                new DataColumnSpecCreator(inSpecs[0]
                        .getColumnSpec(classColIndex));
        crea.setName("Class [kNN]");
        colSpecs[colSpecs.length - 1] = crea.createSpec();

        return new DataTableSpec[]{new DataTableSpec(colSpecs)};
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #execute(org.knime.core.node.BufferedDataTable[],
     *      org.knime.core.node.ExecutionContext)
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        int classColIndex =
                inData[0].getDataTableSpec().findColumnIndex(
                        m_settings.classColumn());
        if (classColIndex == -1) {
            throw new InvalidSettingsException("Invalid class column chosen.");
        }

        List<Integer> featureColumns = new ArrayList<Integer>();
        Map<Integer, Integer> firstToSecond = new HashMap<Integer, Integer>();
        checkInputTables(new DataTableSpec[]{inData[0].getDataTableSpec(),
                inData[1].getDataTableSpec()}, featureColumns,
                firstToSecond);

        KDTreeBuilder<DataCell> treeBuilder =
                new KDTreeBuilder<DataCell>(featureColumns.size());
        int count = 0;
        for (DataRow currentRow : inData[0]) {
            exec.checkCanceled();
            exec.setProgress(0.1 * count * inData[0].getRowCount(),
                    "Reading row " + currentRow.getKey());

            double[] features = createFeatureVector(currentRow, featureColumns);
            DataCell thisClassCell = currentRow.getCell(classColIndex);
            // and finally add data
            treeBuilder.addPattern(features, thisClassCell);
        }

        // and now use it to classify the test data...
        DataTableSpec inSpec = inData[1].getDataTableSpec();
        DataColumnSpec classColumnSpec =
                inData[0].getDataTableSpec().getColumnSpec(classColIndex);

        exec.setMessage("Building kd-tree");
        KDTree<DataCell> tree =
                treeBuilder.buildTree(exec.createSubProgress(0.3));
        exec.setMessage("Classifying");
        ColumnRearranger c =
                createRearranger(inSpec, classColumnSpec, featureColumns,
                        firstToSecond, tree, inData[1].getRowCount());
        BufferedDataTable out =
                exec.createColumnRearrangeTable(inData[1], c, exec
                        .createSubProgress(0.6));
        return new BufferedDataTable[]{out};
    }

    /**
     * @see org.knime.core.node.NodeModel#reset()
     */
    @Override
    protected void reset() {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #saveSettingsTo(org.knime.core.node.NodeSettingsWO)
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #validateSettings(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        (new KnnSettings()).loadSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel
     *      #loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_settings.loadSettings(settings);
    }

    /**
     * @see org.knime.core.node.NodeModel#saveInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    /**
     * @see org.knime.core.node.NodeModel#loadInternals(java.io.File,
     *      org.knime.core.node.ExecutionMonitor)
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec) throws IOException,
            CanceledExecutionException {
        // nothing to do
    }

    private ColumnRearranger createRearranger(final DataTableSpec in,
            final DataColumnSpec classColumnSpec,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond,
            final KDTree<DataCell> tree, final double maxRows) {
        ColumnRearranger c = new ColumnRearranger(in);
        String newName = "Class [kNN]";
        while (in.containsName(newName)) {
            newName += " ";
        }

        DataColumnSpecCreator crea = new DataColumnSpecCreator(classColumnSpec);
        crea.setName(newName);

        c.append(new SingleCellFactory(crea.createSpec()) {
            /**
             * @see org.knime.core.data.container.SingleCellFactory
             *      #getCell(org.knime.core.data.DataRow)
             */
            @Override
            public DataCell getCell(final DataRow row) {
                return classify(row, tree, featureColumns, firstToSecond);
            }

            /**
             * @see org.knime.core.data.container.SingleCellFactory
             *      #setProgress(int, int, org.knime.core.data.RowKey,
             *      org.knime.core.node.ExecutionMonitor)
             */
            @Override
            public void setProgress(final int curRowNr, final int rowCount,
                    final RowKey lastKey, final ExecutionMonitor exec) {
                exec.setProgress(curRowNr / maxRows, "Classifying row "
                        + lastKey);
            }
        });
        return c;
    }

    private DataCell classify(final DataRow row, final KDTree<DataCell> tree,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond) {
        double[] features =
                createQueryVector(row, featureColumns, firstToSecond);
        if (featureColumns == null) {
            return DataType.getMissingCell();
        }

        HashMap<DataCell, MutableDouble> classWeights =
                new HashMap<DataCell, MutableDouble>();
        List<NearestNeighbour<DataCell>> nearestN =
                tree.getKNearestNeighbours(features, m_settings.k());

        for (NearestNeighbour<DataCell> n : nearestN) {
            MutableDouble count = classWeights.get(n.getData());
            if (count == null) {
                count = new MutableDouble(0);
                classWeights.put(n.getData(), count);
            }
            if (m_settings.weightByDistance()) {
                count.add(1 / n.getDistance());
            } else {
                count.inc();
            }
        }

        double winnerWeight = 0;
        DataCell winnerCell = DataType.getMissingCell();
        for (Map.Entry<DataCell, MutableDouble> e : classWeights.entrySet()) {
            double weight = e.getValue().doubleValue();
            if (weight > winnerWeight) {
                winnerWeight = weight;
                winnerCell = e.getKey();
            }
        }
        return winnerCell;

    }

    /**
     * Creates a double array with the features of one data row.
     * 
     * @param row the row
     * @param featureColumns the indices of the column with the features to use
     * @return a double array with the features' values
     */
    private double[] createFeatureVector(final DataRow row,
            final List<Integer> featureColumns) {
        double[] features = new double[featureColumns.size()];
        int currentIndex = 0;
        for (int i : featureColumns) {
            DataCell thisCell = row.getCell(i);

            if (!thisCell.isMissing()) {
                features[currentIndex] =
                        ((DoubleValue)thisCell).getDoubleValue();
            } else {
                return null;
            }
            currentIndex++;
        }
        return features;
    }

    /**
     * Creates a double array with the features of one data row used for
     * querying the tree.
     * 
     * @param row the row
     * @param featureColumns the indices of the column with the features to use
     * @param firstToSecond a map that maps the indices of the feature columns
     *            from the first table to the columns in the second table
     * @return a double array with the features' values
     */
    private double[] createQueryVector(final DataRow row,
            final List<Integer> featureColumns,
            final Map<Integer, Integer> firstToSecond) {
        double[] features = new double[featureColumns.size()];
        int currentIndex = 0;
        for (Integer i : featureColumns) {
            DataCell thisCell = row.getCell(firstToSecond.get(i));

            if (!thisCell.isMissing()) {
                features[currentIndex] =
                        ((DoubleValue)thisCell).getDoubleValue();
            } else {
                return null;
            }
            currentIndex++;
        }
        return features;
    }
}
