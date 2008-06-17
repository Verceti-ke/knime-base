/*
 * --------------------------------------------------------------------- *
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
 * --------------------------------------------------------------------- *
 * History: 
 *     03.06.2004 (gabriel) created
 */

package org.knime.base.node.mine.bfn;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowKey;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * Class presents a predictor row for basisfunctions providing method to apply
 * unknown data (compose).
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public abstract class BasisFunctionPredictorRow {
    
    /** The key of this row. */
    private final RowKey m_key;

    /** The class label of this basisfunction. */
    private final DataCell m_classLabel;

    /** The don't know class degree; activation is above this threshold. */
    private final double m_dontKnowDegree;

    /** Number of correctly covered pattern. */
    private int m_correctCovered;

    /** Number of wrong covered pattern. */
    private int m_wrongCovered;

    /** Within-cluster variance. */
    private double m_clusterVariance;

    /**
     * Creates new predictor row.
     * 
     * @param key the key of this row
     * @param classLabel class label of the target attribute
     * @param dontKnowDegree don't know probability
     */
    protected BasisFunctionPredictorRow(final RowKey key,
            final DataCell classLabel, final double dontKnowDegree) {
        m_key = key;
        m_classLabel = classLabel;
        m_dontKnowDegree = dontKnowDegree;
        m_correctCovered = 0;
        m_wrongCovered = 0;
        m_clusterVariance = 0;
    }

    /**
     * Creates new predictor row on model content.
     * 
     * @param pp the model content to read the new predictor row from
     * @throws InvalidSettingsException if the model content is invalid
     */
    public BasisFunctionPredictorRow(final ModelContentRO pp)
            throws InvalidSettingsException {
        RowKey key;
        try {
            // load key before 2.0
            key = new RowKey(pp.getDataCell("row_id").toString());
        } catch (InvalidSettingsException ise) {
            key = new RowKey(pp.getString("row_id"));
        }
        m_key = key;
        m_classLabel = pp.getDataCell("class_label");
        m_dontKnowDegree = pp.getDouble("dont_know_class");
        m_correctCovered = pp.getInt("correct_covered");
        m_wrongCovered = pp.getInt("wrong_covered");
        m_clusterVariance = pp.getDouble("within-cluster_variance", 0);
    }
    
    /**
     * @param row to compute distance with
     * @return computes the distance between this row and the anchor
     */
    public abstract double computeDistance(final DataRow row);
    
    /**
     * Returns a value for the spread of this rule.
     * 
     * @return rule spread value
     */
    public abstract double computeSpread();
    
    /**
     * If the same class as this basisfunction is assigned to, the number of
     * correctly covered pattern is increased, otherwise the number of wrong
     * covered ones.
     * @param row to cover
     * @param classLabel a pattern of the given class has to be covered
     */
    final void cover(final DataRow row, final DataCell classLabel) {
        if (m_classLabel.equals(classLabel)) {
            m_correctCovered++;
        } else {
            m_wrongCovered++;
        }
        double d = computeDistance(row);
        m_clusterVariance += d * d;
    }
    
    /**
     * @return with-in cluster variance
     */
    public final double getVariance() {
        if (m_clusterVariance > 0) {
            return m_clusterVariance / getNumAllCoveredPattern();
        } else {
            return 0;
        }
    }
    
    /**
     * Computes the activation based on the given row for this basisfunction.
     * @param row compute activation for
     * @return activation between 0 and 1
     */
    public abstract double computeActivation(final DataRow row);

    /**
     * Composes the activation of the given array and of the calculated one
     * based on the given row. All values itself have to be between
     * <code>0</code> and <code>1</code>.
     * 
     * @param row combine activation with this pattern
     * @param act activation to combine with
     * @return the new activation compromising the given activation
     */
    public abstract double compose(DataRow row, double act);
    
    /**
     * @return number of features that have been shrunken
     */
    public abstract int getNrUsedFeatures();

    /**
     * @return <i>don't know</i> class probability
     */
    public final double getDontKnowClassDegree() {
        return m_dontKnowDegree;
    }

    /**
     * @return class label
     */
    public final DataCell getClassLabel() {
        return m_classLabel;
    }

    /**
     * Returns the number of covered input pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumAllCoveredPattern() {
        return m_correctCovered + m_wrongCovered;
    }

    /**
     * Returns the number of correctly covered data pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumCorrectCoveredPattern() {
        return m_correctCovered;
    }

    /**
     * Returns the number of wrong covered data pattern.
     * 
     * @return the current number of covered input pattern
     */
    public final int getNumWrongCoveredPattern() {
        return m_wrongCovered;
    }

    /**
     * Resets all covered pattern. Called by the learner only.
     */
    final void resetCoveredPattern() {
        m_correctCovered = 0;
        m_wrongCovered = 0;
        m_clusterVariance = 0;
    }

    /**
     * @return row key for this row
     */
    public final RowKey getId() {
        return m_key;
    }

    /**
     * Saves this row into a model content.
     * 
     * @param pp the model content to save this row to
     */
    public void save(final ModelContentWO pp) {
        pp.addString("row_id", m_key.getString());
        pp.addDataCell("class_label", m_classLabel);
        pp.addDouble("dont_know_class", m_dontKnowDegree);
        pp.addInt("correct_covered", m_correctCovered);
        pp.addInt("wrong_covered", m_wrongCovered);
        pp.addDouble("within-cluster_variance", m_clusterVariance);
    }
}
