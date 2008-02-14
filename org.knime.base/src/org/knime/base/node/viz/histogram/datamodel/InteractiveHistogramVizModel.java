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
 * History
 *    26.02.2007 (Tobias Koetter): created
 */

package org.knime.base.node.viz.histogram.datamodel;

import java.awt.Color;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.knime.base.node.viz.aggregation.AggregationMethod;
import org.knime.base.node.viz.histogram.HistogramLayout;
import org.knime.base.node.viz.histogram.util.BinningUtil;
import org.knime.base.node.viz.histogram.util.ColorColumn;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.NodeLogger;


/**
 * Extends the {@link AbstractHistogramVizModel} to allow hiliting and
 * column changing.
 * @author Tobias Koetter, University of Konstanz
 */
public class InteractiveHistogramVizModel extends AbstractHistogramVizModel {
    private static final NodeLogger LOGGER = NodeLogger
            .getLogger(InteractiveHistogramVizModel.class);

    /**
     * Compares the value on the given column index with the given
     * {@link DataValueComparator} of to rows.
     * @author Tobias Koetter, University of Konstanz
     */
    private class RowComparator implements Comparator<DataRow> {

        private DataValueComparator m_colComparator;

        private int m_colIdx;

        /**Constructor for class InteractiveHistogramVizModel.RowComparator.
         * @param comparator the {@link DataValueComparator} to use
         * @param colIdx the column index to compare
         *
         */
        public RowComparator(final DataValueComparator comparator,
                final int colIdx) {
            if (comparator == null) {
                throw new IllegalArgumentException(
                        "Column comparator must not be null");
            }
            m_colComparator = comparator;
            m_colIdx = colIdx;
        }

        /**
         * @param comparator the new {@link DataValueComparator} to use
         * @param colIdx the new column index to compare
         */
        public void update(final DataValueComparator comparator,
                final int colIdx) {
            m_colIdx = colIdx;
            m_colComparator = comparator;
        }

        /**
         * {@inheritDoc}
         */
        public int compare(final DataRow o1, final DataRow o2) {
            return m_colComparator.compare(o1.getCell(m_colIdx),
                    o2.getCell(m_colIdx));
        }

    }

    private final DataTableSpec m_tableSpec;

    private int m_xColIdx = -1;

    private DataColumnSpec m_xColSpec;

    private Collection<ColorColumn> m_aggrColumns;

    private RowComparator m_rowComparator;

    private final List<DataRow> m_dataRows;

    private boolean m_isSorted = false;

    /**Constructor for class InteractiveHistogramVizModel.
     * @param rowColors all possible colors the user has defined for a row
     * @param noOfBins the number of bins to create
     * @param aggrMethod the {@link AggregationMethod} to use
     * @param layout {@link HistogramLayout} to use
     * @param spec the {@link DataTableSpec}
     * @param rows the {@link DataRow}
     * @param xColSpec the {@link DataColumnSpec} of the selected x column
     * @param aggrColumns the selected aggregation columns
     */
    public InteractiveHistogramVizModel(final SortedSet<Color> rowColors,
            final AggregationMethod aggrMethod, final HistogramLayout layout,
            final DataTableSpec spec,  final List<DataRow> rows,
            final DataColumnSpec xColSpec,
            final Collection<ColorColumn> aggrColumns,
            final int noOfBins) {
        super(rowColors, aggrMethod, layout, noOfBins);
        if (spec == null) {
            throw new IllegalArgumentException(
                    "Table specification must not be null");
        }
        if (xColSpec == null) {
            throw new IllegalArgumentException(
            "No column specification found for selected binning column");
        }
        if (rows == null) {
            throw new IllegalArgumentException("Rows must not be null");
        }
//        if (aggrColumns == null || aggrColumns.size() < 1) {
//            throw new IllegalArgumentException("At least one aggregation "
//                    + "column should be selected");
//
//        }
        if (noOfBins < 1) {
            throw new IllegalArgumentException("Number of bins should be > 0");
        }
        m_tableSpec = spec;
        m_dataRows = rows;
        m_aggrColumns = aggrColumns;
        if (aggrColumns != null && aggrColumns.size() > 1) {
            setShowBarOutline(true);
            setShowBinOutline(true);
        } else {
            setShowBarOutline(false);
            setShowBinOutline(false);
        }
        setXColumn(xColSpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setNoOfBins(final int noOfBins) {
        if (super.setNoOfBins(noOfBins)) {
            createBins();
            return true;
        }
        return false;
    }

    /**
     * @param xColSpec the new x column specification
     * @return <code>true</code> if the variable has changed
     */
    public boolean setXColumn(final DataColumnSpec xColSpec) {
        if (xColSpec == null) {
            throw new IllegalArgumentException(
                    "Binning column specification must not be null");
        }
        final int xColIdx = m_tableSpec.findColumnIndex(xColSpec.getName());
        if (xColIdx < 0) {
            throw new IllegalArgumentException("Binning column not found");
        }
        if (xColIdx == m_xColIdx) {
            return false;
        }
        m_xColSpec = xColSpec;
        m_xColIdx = xColIdx;
        m_isSorted = false;
        final DataType xColType = m_xColSpec.getType();
        if (m_rowComparator == null) {
            m_rowComparator =
                new RowComparator(xColType.getComparator(), m_xColIdx);
        } else {
            m_rowComparator.update(xColType.getComparator(), m_xColIdx);
        }
//        if (BinningUtil.binNominal(m_xColSpec, getNoOfBins())) {
//            setBinNominal(true);
//        } else {
//            final boolean wasNominal = isBinNominal();
//            setBinNominal(false);
//            //if we have binned nominal reset the number of bins to default
//            if (wasNominal) {
//                updateNoOfBins(DEFAULT_NO_OF_BINS);
//            }
//        }
        if (xColType.isCompatible(
                DoubleValue.class)) {
            final boolean wasNominal = isBinNominal();
            setBinNominal(false);
            //if we have binned nominal reset the number of bins to default
            if (wasNominal) {
                updateNoOfBins(DEFAULT_NO_OF_BINS);
            }

        } else {
            setBinNominal(true);
        }
        createBins();
        return true;
    }

    /**
     * @param aggrCols the new aggregation columns
     * @return <code>true</code> if the variable has changed
     */
    public boolean setAggregationColumns(
            final Collection<ColorColumn> aggrCols) {
//        if (aggrCols == null || aggrCols.size() < 1) {
//            throw new IllegalArgumentException(
//                    "Aggregation column must not be null");
//        }
        if (aggrCols == null || aggrCols.size() < 1) {
            //force the aggregation method to be count
            if (!AggregationMethod.COUNT.equals(getAggregationMethod())) {
                setAggregationMethod(AggregationMethod.COUNT);
            }
        }
        if (m_aggrColumns != null && aggrCols != null
                && m_aggrColumns.size() == aggrCols.size()
                && m_aggrColumns.containsAll(aggrCols)) {
            return false;
        }
        if ((m_aggrColumns == null || m_aggrColumns.size() <= 1)
                && (aggrCols != null && aggrCols.size() > 1)) {
            setShowBarOutline(true);
            setShowBinOutline(true);
        } else if (m_aggrColumns != null && m_aggrColumns.size() > 1
                && (aggrCols == null || aggrCols.size() < 2)) {
            setShowBarOutline(false);
            setShowBinOutline(false);
        }
        m_aggrColumns = aggrCols;
//        createBins();
        //reset all bins and add the rows to the cleaned bins
        final boolean showMissingWas = isShowMissingValBin();
        setShowMissingValBin(false);
        final List<BinDataModel> bins = getBins();
        final BinDataModel missingValueBin = getMissingValueBin();
        for (final BinDataModel bin : bins) {
            bin.clear();
        }
        missingValueBin.clear();
        addRows2Bins(bins, missingValueBin);
        setShowMissingValBin(showMissingWas);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXColumnName() {
        return m_xColSpec.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataColumnSpec getXColumnSpec() {
        return m_xColSpec;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ColorColumn> getAggrColumns() {
        return m_aggrColumns;
    }

    /**
     * @return the data rows in ascending order
     */
    private List<DataRow> getSortedRows() {
        if (!m_isSorted) {
            Collections.sort(m_dataRows, m_rowComparator);
        }
        return m_dataRows;
    }

    /**
     * Creates the bins for the currently set binning information
     * and adds all data rows to the corresponding bin.
     */
    private void createBins() {
        LOGGER.debug("Entering createBins() of class HistogramVizModel.");
        final long startBinTimer = System.currentTimeMillis();
        List<InteractiveBinDataModel> bins;
        if (isBinNominal()) {
            bins = BinningUtil.createInteractiveNominalBins(getXColumnSpec());
        } else {
            //create the new bins
            bins = BinningUtil.createInteractiveIntervalBins(getXColumnSpec(),
                    getNoOfBins());
        }
        final BinDataModel missingValBin = new InteractiveBinDataModel(
                AbstractHistogramVizModel.MISSING_VAL_BAR_CAPTION, 0, 0);
        final long startAddRowTimer = System.currentTimeMillis();
        addRows2Bins(bins, missingValBin);
        final long end = System.currentTimeMillis();
        //add the created bins to the super implementation
        setBins(bins, missingValBin);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(" Total time to create " + bins.size() + " bins: "
                    + (end - startBinTimer) + " in ms.\n"
                    + "Time to create bins: "
                    + (startAddRowTimer - startBinTimer)
                    + " in ms.\n"
                    + "Time to add rows: "
                    + (end - startAddRowTimer) + " in ms.");
            LOGGER.debug("Exiting createBins() of class HistogramVizModel.");
        }
    }

    /**
     *This method should loop through all data rows and should add each row
     *to the corresponding bin by calling the
     *{@link #addDataRow2Bin(int, DataCell, Color, DataCell,
     *Collection, DataCell[])} method.
     * @param missingValBin the bin for missing values
     * @param bins the different bins
     */
    private void addRows2Bins(final List<? extends BinDataModel> bins,
            final BinDataModel missingValBin) {
//      add the data rows to the new bins
        int startBin = 0;
        if (m_aggrColumns == null || m_aggrColumns.size() < 1) {
            //if the user hsn't selected a aggregation column
            for (final DataRow row : getSortedRows()) {
                final DataCell xCell = row.getCell(m_xColIdx);
                final Color color =
                    m_tableSpec.getRowColor(row).getColor(false, false);
                final DataCell id = row.getKey().getId();
                try {
                    startBin = BinningUtil.addDataRow2Bin(
                            isBinNominal(), bins, missingValBin, startBin,
                            xCell, color, id, m_aggrColumns,
                            DataType.getMissingCell());
                } catch (final IllegalArgumentException e) {
                    if (!BinningUtil.checkDomainRange(xCell,
                            getXColumnSpec())) {
                        throw new IllegalStateException(
                            "Invalid column domain for column "
                            + m_xColSpec.getName()
                            + ". " + e.getMessage());
                    }
                    throw e;
                }
            }
        } else {
            final DataTableSpec tableSpec = getTableSpec();
            final int aggrSize = m_aggrColumns.size();
            final int[] aggrIdx = new int[aggrSize];
            int i = 0;
            for (final ColorColumn aggrColumn : m_aggrColumns) {
                aggrIdx[i++] = tableSpec.findColumnIndex(
                        aggrColumn.getColumnName());
            }
            for (final DataRow row : getSortedRows()) {
                final DataCell xCell = row.getCell(m_xColIdx);
                final Color color =
                    m_tableSpec.getRowColor(row).getColor(false, false);
                final DataCell id = row.getKey().getId();
                final DataCell[] aggrVals = new DataCell[aggrSize];
                for (int j = 0, length = aggrIdx.length; j < length; j++) {
                    aggrVals[j] = row.getCell(aggrIdx[j]);
                }
                try {
                    startBin = BinningUtil.addDataRow2Bin(
                            isBinNominal(), bins, missingValBin, startBin,
                            xCell, color, id, m_aggrColumns, aggrVals);
                } catch (final IllegalArgumentException e) {
                        if (!BinningUtil.checkDomainRange(xCell,
                                getXColumnSpec())) {
                            throw new IllegalStateException(
                                "Invalid column domain for column "
                                + m_xColSpec.getName()
                                + ". " + e.getMessage());
                        }
                        throw e;
                    }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFixed() {
        return false;
    }

    /**
     * @return the {@link DataTableSpec} of the table on which this
     * histogram based on
     */
    public DataTableSpec getTableSpec() {
        return m_tableSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DataCell> getHilitedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            final Collection<BarDataModel> bars = bin.getBars();
            for (final BarDataModel bar : bars) {
                if (bar.isSelected()) {
                    final Collection<BarElementDataModel>
                    elements = bar.getElements();
                    for (final BarElementDataModel element : elements) {
                        keys.addAll((element).getHilitedKeys());
                    }
                }
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DataCell> getSelectedKeys() {
        final Set<DataCell> keys = new HashSet<DataCell>();
        for (final BinDataModel bin : getBins()) {
            if (bin.isSelected()) {
                final Collection<BarDataModel> bars = bin.getBars();
                for (final BarDataModel bar : bars) {
                    if (bar.isSelected()) {
                        final Collection<BarElementDataModel>
                        elements = bar.getElements();
                        for (final BarElementDataModel element
                                : elements) {
                            if (element.isSelected()) {
                                keys.addAll((element).getKeys());
                            }
                        }
                    }
                }
            }
        }
        return keys;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateHiliteInfo(final Set<DataCell> hilited,
            final boolean hilite) {
        LOGGER.debug("Entering updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
        if (hilited == null || hilited.size() < 1) {
            return;
        }
        final long startTime = System.currentTimeMillis();
        final HistogramHiliteCalculator calculator = getHiliteCalculator();
        for (final BinDataModel bin : getBins()) {
            if (hilite) {
                ((InteractiveBinDataModel)bin).setHilitedKeys(hilited,
                        calculator);
            } else {
                ((InteractiveBinDataModel)bin).removeHilitedKeys(hilited,
                        calculator);
            }
        }
        final long endTime = System.currentTimeMillis();
        final long durationTime = endTime - startTime;
        LOGGER.debug("Time for updateHiliteInfo: " + durationTime + " ms");
        LOGGER.debug("Exiting updateHiliteInfo(hilited, hilite) "
                + "of class InteractiveHistogramVizModel.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unHiliteAll() {
        for (final BinDataModel bin : getBins()) {
            ((InteractiveBinDataModel)bin).clearHilite();
        }
    }
}
