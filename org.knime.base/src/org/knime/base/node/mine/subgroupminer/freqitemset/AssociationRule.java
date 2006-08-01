/* 
 * -------------------------------------------------------------------
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
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 * 
 * History
 *   22.02.2006 (dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.List;

/**
 * A data structure to encapsulate an association rule.
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class AssociationRule {
    private Integer m_consequent;

    private List<Integer> m_antecedent;

    private double m_confidence;

    private int m_support;

    /**
     * Creates an association rule with the list of ids of the antecedent and an
     * id as the consequent of this rule.
     * 
     * @param consequent the consequent of the rule
     * @param antecendent the antecedent of the rule
     * @param confidence the confidence of the rule
     * @param support the support of the rule
     */
    public AssociationRule(final Integer consequent,
            final List<Integer> antecendent, final double confidence,
            final int support) {
        m_consequent = consequent;
        m_antecedent = antecendent;
        m_confidence = confidence;
        m_support = support;
    }

    /**
     * @return the support of the rule.
     */
    public int getSupport() {
        return m_support;
    }

    /**
     * @param confidence the confidence to set
     */
    public void setConfidence(final double confidence) {
        m_confidence = confidence;
    }

    /**
     * @return the confidence.
     */
    public double getConfidence() {
        return m_confidence;
    }

    /**
     * @return the antecedent
     */
    public List<Integer> getAntecedent() {
        return m_antecedent;
    }

    /**
     * @param antecedent he antecedent to set
     */
    public void setAntecedent(final List<Integer> antecedent) {
        m_antecedent = antecedent;
    }

    /**
     * @return the consequent
     */
    public Integer getConsequent() {
        return m_consequent;
    }

    /**
     * @param consequent the consequent to set
     */
    public void setConsequent(final Integer consequent) {
        m_consequent = consequent;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "support: " + m_support + " confidence: " + m_confidence
                + " antecedent: " + m_antecedent + " consequent: "
                + m_consequent;
    }
}
