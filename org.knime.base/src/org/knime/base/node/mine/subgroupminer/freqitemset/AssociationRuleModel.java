/*
 * ------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 * 
 * History
 *   14.07.2006 (Fabian Dill): created
 */
package org.knime.base.node.mine.subgroupminer.freqitemset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;


/**
 * 
 * @author Fabian Dill, University of Konstanz
 */
public class AssociationRuleModel {
    private static final String ASSOCIATION_RULES = "associationRules";

    private static final String ASSOCIATION_RULE = "associationRule";

    private static final String CONSEQUENT = "consequent";

    private static final String ANTECEDENT = "antecedent";

    private static final String ANTECEDENT_SIZE = "antecedent size";

    private static final String CONFIDENCE = "confidence";

    private static final String SUPPORT = "support";

    private static final String TYPE = "type";

    private static final String ITEM = "item";

    private static final String NR_RULES = "number of rules";

    private static final String NAME_MAPPING = "nameMapping";

    private Collection<AssociationRule> m_rules;

    private List<String> m_nameMapping;

    /**
     * 
     * @param model model containing the association rules.
     * @throws InvalidSettingsException if a
     */
    public void loadFromModelContent(final ModelContentRO model)
            throws InvalidSettingsException {
        String type = model.getString(TYPE, "");
        if (!type.equals(ASSOCIATION_RULES)) {
            throw new InvalidSettingsException("Model is not of type "
                    + ASSOCIATION_RULES);
        }
        m_nameMapping = Arrays.asList(model.getStringArray(NAME_MAPPING));
        int nrRules = model.getInt(NR_RULES);
        for (int i = 0; i < nrRules; i++) {
            ModelContentRO ruleModel = model.getModelContent(ASSOCIATION_RULE
                    + i);
            double support = ruleModel.getDouble(SUPPORT);
            double confidence = ruleModel.getDouble(CONFIDENCE);
            String consequentName = ruleModel.getString(CONSEQUENT);
            int consequent;
            if (m_nameMapping != null) {
                consequent = m_nameMapping.indexOf(consequentName);
            } else {
                consequent = Integer.parseInt(consequentName.substring(4,
                        consequentName.length()));
            }
            List<Integer> antecedent = new ArrayList<Integer>();
            ModelContentRO antecedentModel = ruleModel
                    .getModelContent(ANTECEDENT);
            int antecedentSize = antecedentModel.getInt(ANTECEDENT_SIZE);
            for (int j = 0; j < antecedentSize; j++) {
                String itemName = ruleModel.getString(CONSEQUENT);
                int item;
                if (m_nameMapping != null) {
                    item = m_nameMapping.indexOf(itemName);
                } else {
                    item = Integer.parseInt(itemName.substring(4, itemName
                            .length()));
                }
                antecedent.add(item);
            }
            AssociationRule rule = new AssociationRule(consequent, antecedent,
                    confidence, support);
            m_rules.add(rule);
        }

    }

    /**
     * @param model the model the association rules are saved to
     */
    public void saveToModelContent(final ModelContentWO model) {
        ModelContentWO associationRulesModel = model
                .addModelContent(ASSOCIATION_RULES);
        associationRulesModel.addString(TYPE, ASSOCIATION_RULES);
        String[] mappingArray = new String[m_nameMapping.size()];
        m_nameMapping.toArray(mappingArray);
        associationRulesModel.addStringArray(NAME_MAPPING, mappingArray);
        int counter = 0;
        associationRulesModel.addInt(NR_RULES, m_rules.size());
        for (AssociationRule rule : m_rules) {
            ModelContentWO ruleModel = associationRulesModel
                    .addModelContent(ASSOCIATION_RULE + counter++);
            ruleModel.addDouble(SUPPORT, rule.getSupport());
            ruleModel.addDouble(CONFIDENCE, rule.getConfidence());
            String name;
            if (m_nameMapping != null) {
                name = m_nameMapping.get(rule.getConsequent());
            } else {
                name = "item" + rule.getConsequent();
            }
            ruleModel.addString(CONSEQUENT, name);
            int antecedentSize = rule.getAntecedent().size();
            ModelContentWO antecedentModel = ruleModel
                    .addModelContent(ANTECEDENT);
            antecedentModel.addInt(ANTECEDENT_SIZE, antecedentSize);
            int itemCounter = 0;
            for (Integer item : rule.getAntecedent()) {
                if (m_nameMapping != null) {
                    name = m_nameMapping.get(item);
                } else {
                    name = "item" + item;
                }
                antecedentModel.addString(ITEM + itemCounter++, name);
            }
        }
    }

    /**
     * @param nameMapping the index to name mapping for the items
     */
    public void setNameMapping(final List<String> nameMapping) {
        m_nameMapping = nameMapping;
    }

    /**
     * @param rules sets the rule for the model
     */
    public void setAssociationRules(final Collection<AssociationRule> rules) {
        m_rules = rules;
    }

    /**
     * @return the loaded association rules
     */
    public Collection<AssociationRule> getAssociationRules() {
        return m_rules;
    }
}
