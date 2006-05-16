/* @(#)$RCSfile$ 
 * $Revision$ $Date$ $Author$
 * 
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 * 
 * Copyright, 2003 - 2006
 * Universitaet Konstanz, Germany.
 * Lehrstuhl fuer Angewandte Informatik
 * Prof. Dr. Michael R. Berthold
 * 
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner.
 * -------------------------------------------------------------------
 * 
 */
package de.unikn.knime.base.data.append.row;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import de.unikn.knime.core.data.DataCell;
import de.unikn.knime.core.data.DataCellComparator;
import de.unikn.knime.core.data.DataColumnDomain;
import de.unikn.knime.core.data.DataColumnSpec;
import de.unikn.knime.core.data.DataColumnSpecCreator;
import de.unikn.knime.core.data.DataTable;
import de.unikn.knime.core.data.DataTableSpec;
import de.unikn.knime.core.data.DataType;
import de.unikn.knime.core.data.RowIterator;
import de.unikn.knime.core.data.def.DefaultDataColumnDomain;
import de.unikn.knime.core.node.NodeLogger;

/**
 * <code>DataTable</code> that concatenates a set of <code>DataTable</code>
 * objects. All tables should have the same columns (identified by their column
 * name). Missing columns in tables are tried to be filled with missing cells
 * (where it is possible - so far for string, double, int). Non matching column
 * types are adjusted such that they match (if not possible, the column's type
 * is <code>DataCell</code>).
 * 
 * <p>
 * The order of colums of this table is determined by the order of the top table
 * (the first array argument in the constructor)
 * 
 * @author Bernd Wiswedel, University of Konstanz
 */
public class AppendedRowsTable implements DataTable {

    private static final NodeLogger LOGGER = 
        NodeLogger.getLogger(AppendedRowsTable.class);

    /**
     * Tables that make up this table. The first entry contains the the top part
     * and the second, third and subsequent table the rest. The order of columns
     * is defined by the top table.
     */
    private final DataTable[] m_tables;

    /**
     * The DataTableSpec for this table (defined mainly be the order of the top
     * table (apart from column that only occur in other tables).
     */
    private final DataTableSpec m_spec;

    /**
     * Suffix to append or null to skip subsequent occurences of a row key.
     */
    private final String m_suffix;

    /**
     * Concatenates a set of tables. Duplicate entries are skip.
     * 
     * @param tables Containing all tables to be appended.
     * @throws NullPointerException If argument is <code>null</code> or
     *             contains <code>null</code> elements.
     */
    public AppendedRowsTable(final DataTable... tables) {
        this(null, tables);
    } // AppendedRowsTable(DataTable[])

    /**
     * Concatenates a set of tables. Duplicate keys may be skipped 
     * (<code>suffix</code> argument is <code>null</code>) or may be 
     * avoided by appending a fixed suffix to any subsequent occurence 
     * of a duplicate.
     * 
     * @param tables Containing all tables to be appended.
     * @param suffix Suffix to append to duplicate keys or <code>null</code>
     *            to skip them
     * @throws NullPointerException If argument is <code>null</code> or
     *             contains <code>null</code> elements.
     * @throws IllegalArgumentException If suffix is empty (null is ok)
     */
    public AppendedRowsTable(final String suffix, final DataTable... tables) {
        m_spec = generateDataTableSpec(tables);
        if (suffix != null && suffix.equals("")) {
            throw new IllegalArgumentException(
                    "Suffix must not be an empty string.");
        }
        m_suffix = suffix;
        m_tables = new DataTable[tables.length];
        System.arraycopy(tables, 0, m_tables, 0, m_tables.length);
    } // AppendedRowsTable(DataTable[],String,boolean)

    /**
     * @see de.unikn.knime.core.data.DataTable#getDataTableSpec()
     */
    public DataTableSpec getDataTableSpec() {
        return m_spec;
    }

    /**
     * @see de.unikn.knime.core.data.DataTable#iterator()
     */
    public RowIterator iterator() {
        return new AppendedRowsIterator(m_tables, m_spec, m_suffix);
    }

    /**
     * Factory method that determines the final DataTableSpec given the tables.
     * 
     * @param tableSpecs The table specs as in the constructor
     * @return The outcoming <code>DataTableSpec</code>
     * @see #AppendedRowsTable(DataTable[])
     */
    public static final DataTableSpec generateDataTableSpec(
            final DataTableSpec... tableSpecs) {
        LinkedHashMap<DataCell, DataType> columnSet = 
            new LinkedHashMap<DataCell, DataType>();
        LinkedHashMap<DataCell, DataColumnDomain> domainSet = 
            new LinkedHashMap<DataCell, DataColumnDomain>();

        // create final data table spec
        for (int i = 0; i < tableSpecs.length; i++) {
            DataTableSpec cur = tableSpecs[i];
            for (int c = 0; c < cur.getNumColumns(); c++) {
                DataColumnSpec colSpec = cur.getColumnSpec(c);
                DataCell colName = colSpec.getName();
                DataType colType = colSpec.getType();
                DataColumnDomain colDomain = colSpec.getDomain();

                // duplicates are welcome - but only if they match the type
                if (columnSet.containsKey(colName)) {
                    DataType oldType = columnSet.get(colName);
                    DataColumnDomain oldDomain = domainSet.get(colName);
                    // the base type they share
                    DataType type = DataType.getCommonSuperType(oldType,
                            colType);
                    assert type.isASuperTypeOf(oldType);
                    assert type.isASuperTypeOf(colType);
                    // that shouldn't happen though, eh: shit happens.
                    if (!oldType.equals(type)) {
                        LOGGER.info("Confusing data types for column \""
                                + colName + "\": " + oldType.toString()
                                + " vs. " + colType.toString() + "\n"
                                + "Using common base type " + type.toString());
                        // that must not change the order.
                        columnSet.put(colName, type);
                    }
                    DataColumnDomain newDomain = merge(
                            oldDomain, colDomain, type.getComparator());
                    domainSet.put(colName, newDomain);
                } else { // doesn't contain the key
                    columnSet.put(colName, colType);
                    domainSet.put(colName, colDomain);
                }
            } // for all columns in the current table spec
        } // for all tables

        DataColumnSpec[] colSpecs = new DataColumnSpec[columnSet.size()];
        int i = 0;
        for (Map.Entry<DataCell, DataType> entry : columnSet.entrySet()) {
            DataCell name = entry.getKey();
            DataType type = entry.getValue();
            // domain is null, if we did not remember it (e.g. "keepDomain" was
            // false)
            DataColumnDomain domain = domainSet.get(name);
            DataColumnSpecCreator specCreator = 
                new DataColumnSpecCreator(name, type);
            specCreator.setDomain(domain);
            colSpecs[i++] = specCreator.createSpec();
        }
        return new DataTableSpec(colSpecs);
    }

    /**
     * Convenience method that extracts the table specs and calls
     * generateDataTableSpec(DataTableSpec[]).
     */
    private static final DataTableSpec generateDataTableSpec(
            final DataTable... tables) {
        DataTableSpec[] tableSpecs = new DataTableSpec[tables.length];
        for (int i = 0; i < tableSpecs.length; i++) {
            tableSpecs[i] = tables[i].getDataTableSpec();
        }
        return generateDataTableSpec(tableSpecs);
    }
    
    /** Merges two domains of the same column, i.e. emerging from different
     * tables, min max will be updated (if possible) and the possible value
     * set. 
     */
    private static final DataColumnDomain merge(final DataColumnDomain d1, 
            final DataColumnDomain d2, final DataCellComparator comp) {
        final DataCell d1Min = d1.getLowerBound();
        final DataCell d1Max = d1.getUpperBound();
        final DataCell d2Min = d2.getLowerBound();
        final DataCell d2Max = d2.getUpperBound();
        final Set<DataCell> d1Poss = d1.getValues(); 
        final Set<DataCell> d2Poss = d2.getValues(); 
        final DataCell newMin;
        if (d1Min == null || d2Min == null) {
            newMin = null;
        } else {
            newMin = comp.compare(d1Min, d2Min) < 0 ? d1Min : d2Min;
        }
        final DataCell newMax;
        if (d1Max == null || d2Max == null) {
            newMax = null;
        } else {
            newMax = comp.compare(d1Max, d2Max) < 0 ? d1Max : d2Max;
        }
        final Set<DataCell> newPoss;
        if (d1Poss == null || d2Poss == null) {
            newPoss = null;
        } else {
            newPoss = new LinkedHashSet<DataCell>(d1Poss);
            newPoss.addAll(d2Poss);
        }
        return new DefaultDataColumnDomain(newPoss, newMin, newMax);
    }

}
