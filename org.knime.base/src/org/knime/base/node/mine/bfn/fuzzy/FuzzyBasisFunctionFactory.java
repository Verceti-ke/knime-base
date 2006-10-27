/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.fuzzy;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.FuzzyIntervalCell;
import org.knime.core.node.ModelContent;

/**
 * Basic interface for all basis function algorithms. Provides the function
 * getNewBasisFunction(.) to initialize a new prototype. This interface is
 * needed in order to create new prototypes in the general BasisFunctionLearner.
 * Hence a BasisFunctionLearner would be initialized with an object of type
 * BasisFunctionFactory. It is used as inter-class to init BasisFunction(s). One
 * implementation of the BasisFunctionFactory; here represents the
 * FuzzyBasisFunctionFactory object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see FuzzyBasisFunctionLearnerRow
 * 
 * @see #commit(RowKey, DataCell, DataRow, int)
 */
public final class FuzzyBasisFunctionFactory extends BasisFunctionFactory {
    
    /** The choice of fuzzy norm. */
    private final int m_norm;

    /** The choice of shrink procedure. */
    private final int m_shrink;

    /**
     * Creates a new factory fuzzy basisfunction along with a {@link Norm} and a
     * {@link Shrink} function.
     * 
     * @param norm the choice of fuzzy norm
     * @param shrink the choice of shrink procedure
     * @param spec the data to retrieve all columns and class info from
     * @param target the class info column in the data
     * @param distance the choice of distance function
     */
    public FuzzyBasisFunctionFactory(final int norm, final int shrink,
            final DataTableSpec spec, final String target, final int distance) {
        this(norm, shrink, spec, target, distance, false);
    }

    /**
     * Creates a new factory fuzzy basisfunction along with a {@link Norm} and a
     * {@link Shrink} function.
     * 
     * @param norm the choice of fuzzy norm
     * @param shrink the choice of shrink procedure
     * @param spec the data to retrieve all columns and class info from
     * @param target the class info column in the data
     * @param distance the choice of distance function
     * @param isHierarchical If hierarchical rules have to be commited.
     */
    public FuzzyBasisFunctionFactory(final int norm, final int shrink,
            final DataTableSpec spec, final String target, final int distance,
            final boolean isHierarchical) {
        super(spec, target, FuzzyIntervalCell.TYPE, distance, isHierarchical);
        m_norm = norm;
        m_shrink = shrink;
    }

    /**
     * Creates and returns a new row initialized with a class label and a center
     * vector.
     * 
     * @param key the key for this row
     * @param row the initial center vector
     * @param classInfo the class info
     * @param numPat The overall number of pattern used for training.
     * @return A new basisfunction 
     */
    @Override
    public BasisFunctionLearnerRow commit(final RowKey key,
            final DataCell classInfo, final DataRow row, final int numPat) {
        return new FuzzyBasisFunctionLearnerRow(key, classInfo, row, m_norm,
                m_shrink, getMinimums(), getMaximums(), numPat, 
                isHierarchical());
    }

    /**
     * Returns the upper bound for conflicting instances.
     * 
     * @return the upper bound for activation
     */
    final int getNorm() {
        return m_norm;
    }

    /**
     * Returns the lower bound for non-conflicting instances.
     * 
     * @return the lower bound for activation
     */
    final int getShrink() {
        return m_shrink;
    }

    /**
     * @see BasisFunctionFactory
     *      #save(org.knime.core.node.ModelContent)
     */
    @Override
    public void save(final ModelContent pp) {
        super.save(pp);
        pp.addInt(Norm.NORM_KEY, m_norm);
        pp.addInt(Shrink.SHRINK_KEY, m_shrink);
    }
}
