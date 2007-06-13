/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 */
package org.knime.base.node.mine.bfn.radial;

import org.knime.base.node.mine.bfn.BasisFunctionFactory;
import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.ModelContent;

/**
 * Basic interface for all basis function algorithms. Provides the function
 * getNewBasisFunction(.) to initialize a new prototype. This interface is
 * needed in order to create new prototypes in the general BasisFunctionLearner.
 * Hence a BasisFunctionLearner would be initialized with an object of type
 * BasisFunctionFactory. It is used as inter-class to init BasisFunction(s). One
 * implementation of the BasisFunctionFactory; here represents the
 * RadialBasisFunctionFactory object.
 * 
 * @author Thomas Gabriel, University of Konstanz
 * 
 * @see RadialBasisFunctionLearnerRow
 * @see #commit(RowKey, DataCell, DataRow, int)
 */
class RadialBasisFunctionFactory extends BasisFunctionFactory {
    
    /** theta minus value. */
    private final double m_thetaMinus;

    /** theta plus value. */
    private final double m_thetaPlus;

    /**
     * Creates a new factory for a radial basis function learner.
     * 
     * @param thetaMinus the upper bound activation for conflicting instances
     * @param thetaPlus the lower bound activation for non-conflicting instances
     * @param distance the choice of distance function
     * @param spec the input data to learn from
     * @param target the class info column in the data
     */
    RadialBasisFunctionFactory(final double thetaMinus, final double thetaPlus,
            final int distance, final DataTableSpec spec, final String target) {
        this(thetaMinus, thetaPlus, distance, spec, target, false);
    }

    /**
     * Creates a new factory for a radial basis function learner.
     * 
     * @param thetaMinus the upper bound activation for conflicting instances
     * @param thetaPlus the lower bound activation for non-conflicting instances
     * @param distance the choice of distance function
     * @param spec the input data to learn from
     * @param target the class info column in the data
     * @param isHierarchical If the radial rule is hierarchical nature. 
     */
    RadialBasisFunctionFactory(final double thetaMinus, final double thetaPlus,
            final int distance, final DataTableSpec spec, final String target,
            final boolean isHierarchical) {
        super(spec, target, DoubleCell.TYPE, distance, isHierarchical);
        m_thetaMinus = thetaMinus;
        m_thetaPlus = thetaPlus;
    }

    /**
     * Creates and returns a new {@link RadialBasisFunctionLearnerRow}
     * initialized with a center vector and a class label.
     * 
     * @param key this row's key
     * @param row the initial center vector
     * @param classInfo the class info
     * @param numPat The overall number of pattern used for training.
     * @return A new basisfunction.
     */
    @Override
    public final BasisFunctionLearnerRow commit(final RowKey key,
            final DataCell classInfo, final DataRow row, final int numPat) {
        return new RadialBasisFunctionLearnerRow(key, classInfo, row,
                m_thetaMinus, m_thetaPlus, super.getDistance(), numPat, 
                super.isHierarchical());
    }

    /**
     * Returns the upper bound for conflicting instances.
     * 
     * @return the upper bound for activation
     */
    final double getThetaMinus() {
        return m_thetaMinus;
    }

    /**
     * Returns the lower bound for non-conflicting instances.
     * 
     * @return the lower bound for activation
     */
    final double getThetaPlus() {
        return m_thetaPlus;
    }

    /** Key of theta minus. */
    static final String THETA_MINUS = "theta_minus";

    /** Key of theta plus. */
    static final String THETA_PLUS = "theta_plus";

    /**
     * @see BasisFunctionFactory
     *      #save(org.knime.core.node.ModelContent)
     */
    @Override
    public void save(final ModelContent pp) {
        super.save(pp);
        pp.addDouble(THETA_MINUS, m_thetaMinus);
        pp.addDouble(THETA_PLUS, m_thetaPlus);
    }
}
