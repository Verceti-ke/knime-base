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

import org.knime.base.node.mine.bfn.BasisFunctionLearnerRow;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowKey;

/**
 * This class extends the general
 * {@link BasisFunctionLearnerRow} in order to
 * use radial basis function prototypes for training. This prototype keeps an
 * Gaussian functions is internal representation. This function is created
 * infinity which means cover the entry domain. During training the function is
 * shrunk if new conflicting instances are omitted. Therefore two parameters
 * have been introduced. One is <code>m_thetaMinus</code> which is used to
 * describe an upper bound of conflicting instances; and 
 * <code>m_thetaPlus</code>, to lower bound for non-conflicting instances.
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class RadialBasisFunctionLearnerRow extends BasisFunctionLearnerRow {
    
    /** The upper bound for conflicting instances. */
    private final double m_thetaMinus;
    
    private final double m_thetaMinusSqrtMinusLog;
    private final double m_thetaPlusSqrtMinusLog;

    /** The lower bound for non-conflicting instances. */
    private final double m_thetaPlus;

    /** Row used to predict unknown instances. */
    private final RadialBasisFunctionPredictorRow m_predRow;

    /**
     * Creates a new radial basisfunction using the center vector as the anchor
     * of the Gaussian function and also assigns class label for this prototype.
     * The Gaussian function is be initialized infinity covering the entire
     * domain.
     * 
     * @param key the key of this row
     * @param classInfo the class info assigned to this basisfunction
     * @param center the initial center vector
     * @param thetaMinus upper bound for conflicting instances
     * @param thetaPlus lower bound for non-conflicting instances
     * @param distance choice of the distance function between patterns.
     */
    protected RadialBasisFunctionLearnerRow(final RowKey key, 
            final DataCell classInfo, final DataRow center, 
            final double thetaMinus, final double thetaPlus, 
            final int distance) {
        super(key, center, classInfo);
        m_thetaMinus = thetaMinus;
        m_thetaMinusSqrtMinusLog = Math.sqrt(-Math.log(m_thetaMinus));
        assert (m_thetaMinus >= 0.0 && m_thetaMinus <= 1.0);
        m_thetaPlus = thetaPlus;
        m_thetaPlusSqrtMinusLog = Math.sqrt(-Math.log(m_thetaPlus));
        assert (m_thetaPlus >= 0.0 && m_thetaPlus <= 1.0);
        m_predRow = new RadialBasisFunctionPredictorRow(key.getId(), center,
                classInfo, m_thetaMinus, distance);
        addCovered(center, classInfo);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RadialBasisFunctionPredictorRow getPredictorRow() {
        return m_predRow;
    }

    /**
     * Returns the missing double value for the given dimension.
     * @param col the column index.
     * @return The centroid value at the given dimension.
     */
    @Override
    public DoubleValue getMissingValue(final int col) {
        return (DoubleValue)getAnchor().getCell(col);
    }

    /**
     * Checks if the given row is covered by this basis function. If this basis
     * function has not been shrunk before, it will return <b>true</b>
     * immediately, otherwise it checks if the activation is greater theta
     * minus.
     * 
     * @param row the input row to check coverage for
     * @return <code>true</code> if the given row is covered other
     *         <code>false</code>
     * @throws NullPointerException if the given row is <code>null</code>
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public
    final boolean covers(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        return (m_predRow.getStdDev()
            >= m_predRow.computeDistance(row) / m_thetaPlusSqrtMinusLog);
    }

    /**
     * Checks if the given row is explained by this basisfunction. If this basis
     * function has not been shrunk before, it will return <b>true</b>
     * immediately, otherwise it checks if the activation is greater or equal
     * theta plus.
     * 
     * @param row the input row to check coverage for
     * @return <code>true</code> if the given row is explained other
     *         <code>false</code>
     * @throws NullPointerException if the given row is <code>null</code>
     * 
     * @see #computeActivation(DataRow)
     */
    @Override
    public
    final boolean explains(final DataRow row) {
        if (m_predRow.isNotShrunk()) {
            return true;
        }
        return (computeActivation(row) >= m_thetaPlus);
    }

    /**
     * Computes the overlapping based on the standard deviation of both radial
     * basisfunctions.
     * 
     * @param symmetric if the result is proportional to both basis functions,
     *            and thus symmetric, or if it is proportional to the area of 
     *            the basis function on which the function is called.
     * @param bf the other radial basisfunction to compute the overlap with
     * @return <code>true</code> if both radial basisfunctions overlap
     */
    @Override
    public double overlap(final BasisFunctionLearnerRow bf,
            final boolean symmetric) {
        assert (bf instanceof RadialBasisFunctionLearnerRow);
        RadialBasisFunctionLearnerRow rbf = (RadialBasisFunctionLearnerRow)bf;
        assert (this.getAnchor().getNumCells() 
                == rbf.getAnchor().getNumCells());
        double overlap = 1.0;
        for (int i = 0; i < this.getAnchor().getNumCells(); i++) {
            if (this.getAnchor().getCell(i).isMissing()
                    || rbf.getAnchor().getCell(i).isMissing()) {
                continue;
            }
            double a = ((DoubleValue)this.getAnchor().getCell(i))
                    .getDoubleValue();
            double b = ((DoubleValue)rbf.getAnchor().getCell(i))
                    .getDoubleValue();
            double overlapping = overlapping(a - m_predRow.getStdDev(), a
                    + m_predRow.getStdDev(), b - rbf.m_predRow.getStdDev(), b
                    + rbf.m_predRow.getStdDev(), symmetric);
            if (overlapping == 0.0) {
                return 0.0;
            } else {
                overlap *= overlapping;
            }
        }
        return overlap;
    }

    /**
     * Returns the standard deviation of this radial basisfunction.
     * 
     * @return the standard deviation
     */
    @Override
    public double computeSpread() {
        return m_predRow.getStdDev();
    }

    /**
     * Compares this basis function with the other one by its standard deviation
     * if the number of covered pattern is equal otherwise use this
     * identification.
     * 
     * @param best the basisfunction with the highest coverage so far
     * @param row the row on which to coverage need to be compared
     * @return <code>true</code> if the coverage of <code>this</code> object
     *         is better than the of the other
     * @throws ClassCastException if the other cell is not a
     *             {@link RadialBasisFunctionLearnerRow}
     */
    @Override
    public
    final boolean compareCoverage(
            final BasisFunctionLearnerRow best, final DataRow row) {
        RadialBasisFunctionLearnerRow rbf 
            = (RadialBasisFunctionLearnerRow) best;
        return m_predRow.getStdDev() > rbf.m_predRow.getStdDev();
    }

    /**
     * Called if a new {@link BasisFunctionLearnerRow} has to be adjusted.
     * 
     * @param row conflicting pattern.
     * @return a value greater zero if a conflict has to be solved. The value
     *         indicates relative loss in coverage for this basisfunction.
     */
    @Override
    public
    final boolean getShrinkValue(final DataRow row) {
        return shrinkIt(row, false);
    }

    /**
     * Basis functions need to be adjusted if they conflict with other ones.
     * Therefore this shrink method computes the new standard deviation based on
     * the <code>m_thetaMinus</code>.
     * 
     * @param row the input row to shrink this basisfunction on
     * @return <code>true</code> if the standard deviation changed due to the
     *         method which happens when either this basisfunction has not be
     *         shrunk before or the new radius is smaller than the old one,
     *         other wise this function return <code>false</code>
     */
    @Override
    public
    final boolean shrink(final DataRow row) {
        return shrinkIt(row, true);
    }

    /**
     * If <code>shrinkIt</code> is true the shrink will be executed otherwise
     * the shrink value is only returned.
     * 
     * @param row the input pattern for shrinking
     * @return 0 if no shrink needed otherwise a value greater zero
     */
    private boolean shrinkIt(final DataRow row, final boolean shrinkIt) {
        // if std dev is max or new std dev less that current
        // compute distance between centroid and given row
        double dist = m_predRow.computeDistance(row);
        //if (m_predRow.isNotShrunk() || (newStdDev < m_predRow.getStdDev())) {
        if (m_predRow.isNotShrunk() 
                || m_predRow.getStdDev() > dist / m_thetaMinusSqrtMinusLog) {
            // remembers old standard deviation for shrink value
            double oldStdDev = m_predRow.getStdDev();
            // new std dev for theta minus
            double newStdDev = dist / m_thetaMinusSqrtMinusLog;
            if (shrinkIt) {
                // set current to new std dev for theta minus
                // std dev was affected, set
                m_predRow.shrinkIt(newStdDev);
            }
            return oldStdDev != newStdDev;
        } else {
            // otherwise no shrink performed
            return false;
        }
    }
    
    /**
     * Method is empty.
     */
    @Override
    public
    final void reset() {
        // empty
    }

    /**
     * Method is empty.
     * 
     * @param row Ignored.
     */
    @Override
    public
    final void cover(final DataRow row) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_predRow.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataCell getFinalCell(final int index) {
        return super.getAnchor().getCell(index);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double computeActivation(final DataRow row) {
        return m_predRow.computeActivation(row);
    }
    
    /**
     * @return theta minus
     */
    public final double getThetaMinus() {
        return m_thetaMinus;
    }

    /**
     * @return theta plus
     */
    public final double getThetaPlus() {
        return m_thetaPlus;
    }
    
}
