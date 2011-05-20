/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 */

package org.knime.base.data.aggregation;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.config.Config;

import java.util.LinkedList;
import java.util.List;


/**
 * Class that defines the {@link AggregationMethod} for a column.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class ColumnAggregator {
    private static final String CNFG_AGGR_COL_SECTION = "aggregationColumn";
    private static final String CNFG_COL_NAMES = "columnNames";
    private static final String CNFG_COL_TYPES = "columnTypes";
    private static final String CNFG_AGGR_METHODS = "aggregationMethod";
    private static final String CNFG_INCL_MISSING_VALS = "inclMissingVals";

    private final DataColumnSpec m_origColSpec;
    private final AggregationMethod m_operatorTemplate;
    private AggregationOperator m_operator;
    private boolean m_inclMissingVals;

    /**Constructor for class ColumnAggregator.
     * @param origColSpec the {@link DataColumnSpec} of the original column
     * @param method the {@link AggregationMethod} to use for the given column
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered during aggregation
     */
    public ColumnAggregator(final DataColumnSpec origColSpec,
            final AggregationMethod method, final boolean inclMissingCells) {
        if (origColSpec == null) {
            throw new NullPointerException("colSpec must not be null");
        }
        if (method == null) {
            throw new NullPointerException("method must not be null");
        }
        m_operatorTemplate = method;
        if (!m_operatorTemplate.isCompatible(origColSpec)) {
            throw new IllegalArgumentException("Aggregation method '"
                    + m_operatorTemplate.getLabel() + "' not valid for column '"
                    + origColSpec.getName() + "'");
        }
        m_inclMissingVals = inclMissingCells;
        m_origColSpec = origColSpec;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ColumnAggregator clone() {
        return new ColumnAggregator(m_origColSpec, m_operatorTemplate,
                m_inclMissingVals);
    }

    /**
     * @return the colName of the original column
     */
    public String getOriginalColName() {
        return m_origColSpec.getName();
    }

    /**
     * @return the {@link DataColumnSpec} of the original column
     */
    public DataColumnSpec getOriginalColSpec() {
        return m_origColSpec;
    }

    /**
     * @return the {@link DataType} of the original column
     */
    public DataType getOriginalDataType() {
        return m_origColSpec.getType();
    }

    /**
     * @param type the type to check for
     * @return <code>true</code> if the column is compatible to the given type
     */
    public boolean isCompatible(final Class<? extends DataValue> type) {
        return getOriginalDataType().isCompatible(type);
    }

    /**
     * @return the {@link AggregationMethods} to use
     */
    public AggregationMethod getMethodTemplate() {
        return m_operatorTemplate;
    }

    /**
     * @return <code>true</code> if the operator supports the alteration of
     * the missing value option
     */
    public boolean supportsMissingValueOption() {
        return m_operatorTemplate.supportsMissingValueOption();
    }

    /**
     * @return <code>true</code> if missing cells should be
     * considered during aggregation
     */
    public boolean inclMissingCells() {
        return m_inclMissingVals;
    }

    /**
     * @param inclMissingCells <code>true</code> if missing cells should be
     * considered during aggregation
     */
    public void setinclMissingCells(final boolean inclMissingCells) {
        m_inclMissingVals = inclMissingCells;
    }

    /**
     * Creates only ones an {@link AggregationOperator} that is always
     * returned by this method.
     *
     * @param globalSettings the maximum number of unique values
     * @return the operator for this column
     */
    public AggregationOperator getOperator(
            final GlobalSettings globalSettings) {
        if (m_operator == null) {
            final OperatorColumnSettings opColSettings =
                new OperatorColumnSettings(m_inclMissingVals, m_origColSpec);
            m_operator = m_operatorTemplate.createOperator(
                    globalSettings, opColSettings);
        }
        return m_operator;
    }

    /**
     * @return the column name that should be used for the operator
     */
    public String getOperatorColName() {
        final String operatorLabel = getMethodTemplate().getColumnLabel();
        if (supportsMissingValueOption() && !inclMissingCells()) {
            //add the star to indicate that missing values are excluded
            //but only if the method supports the changing of this option
            //by the user to be compatible to old methods
            return operatorLabel + "*";
        }
        return operatorLabel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getOriginalColName() + "->" + getMethodTemplate().getLabel()
            + "->incl. missing:" + m_inclMissingVals;
    }

    /**
     * Creates a {@link List} with all {@link ColumnAggregator}s that were
     * stored in the settings.
     *
     * @param settings the settings object to read from
     * @return {@link List} with the {@link ColumnAggregator}s
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static List<ColumnAggregator> loadColumnAggregators(
            final NodeSettingsRO settings)
            throws InvalidSettingsException {
        final Config cnfg = settings.getConfig(CNFG_AGGR_COL_SECTION);
        final String[] colNames = cnfg.getStringArray(CNFG_COL_NAMES);
        final DataType[] colTypes = cnfg.getDataTypeArray(CNFG_COL_TYPES);
        final String[] aggrMethods = cnfg.getStringArray(CNFG_AGGR_METHODS);
        boolean[] inclMissingVals = null;
        try {
            inclMissingVals = cnfg.getBooleanArray(CNFG_INCL_MISSING_VALS);
        } catch (final InvalidSettingsException e) {
            // be compatible to version 2.3 and earlier
        }
        final List<ColumnAggregator> colAggrList =
            new LinkedList<ColumnAggregator>();
        if (aggrMethods.length != colNames.length) {
            throw new InvalidSettingsException("Column name array and "
                    + "aggregation method array should be of equal size");
        }
        for (int i = 0, length = aggrMethods.length; i < length; i++) {
            final AggregationMethod method =
                AggregationMethods.getMethod4Label(aggrMethods[i]);
            final boolean inclMissingVal;
            if (inclMissingVals != null) {
                inclMissingVal = inclMissingVals[i];
            } else {
                //get the default behavior of the method
                inclMissingVal = method.inclMissingCells();
            }
            final DataColumnSpec spec = new DataColumnSpecCreator(
                    colNames[i], colTypes[i]).createSpec();
            colAggrList.add(new ColumnAggregator(spec, method, inclMissingVal));
        }
        return colAggrList;
    }

    /**
     * @param settings the settings object to write to
     * @param cols the {@link ColumnAggregator} objects to save
     */
    public static void saveColumnAggregators(final NodeSettingsWO settings,
            final List<ColumnAggregator> cols) {
        if (settings == null) {
            throw new NullPointerException("settings must not be null");
        }
        if (cols == null) {
            throw new NullPointerException("cols must not be null");
        }
        final String[] colNames = new String[cols.size()];
        final String[] aggrMethods = new String[cols.size()];
        final boolean[] inclMissingVals = new boolean[cols.size()];
        final DataType[] types = new DataType[cols.size()];
        for (int i = 0, length = cols.size(); i < length; i++) {
            final ColumnAggregator aggrCol = cols.get(i);
            colNames[i] = aggrCol.getOriginalColName();
            aggrMethods[i] = aggrCol.getMethodTemplate().getLabel();
            types[i] = aggrCol.getOriginalDataType();
            inclMissingVals[i] = aggrCol.inclMissingCells();
        }
        final Config cnfg = settings.addConfig(CNFG_AGGR_COL_SECTION);
        cnfg.addStringArray(CNFG_COL_NAMES, colNames);
        cnfg.addDataTypeArray(CNFG_COL_TYPES, types);
        cnfg.addStringArray(CNFG_AGGR_METHODS, aggrMethods);
        cnfg.addBooleanArray(CNFG_INCL_MISSING_VALS, inclMissingVals);
    }
}
