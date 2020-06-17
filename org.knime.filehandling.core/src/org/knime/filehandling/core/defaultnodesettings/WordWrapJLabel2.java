/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.03.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A <code>JPanel</code> that wraps the text of the errors in a textarea and .
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
public final class WordWrapJLabel2 extends JPanel {

    private static final long serialVersionUID = 1L;

    private JLabel m_icon;

    private JTextArea m_textArea;

    /**
     * Creates a <code>JPanel</code> that wraps the text of the errors in a textarea.
     *
     * @param text the text to set
     * @param icon the icon to set
     */
    public WordWrapJLabel2(final String text, final Icon icon) {

        final GBCBuilder gbc = createGBC();
        this.setLayout(new GridBagLayout());

        m_icon = new JLabel();
        m_icon.setIcon(icon);

        initializeTextArea(text);

        this.setPreferredSize(new Dimension(150,100));
        this.setBorder(BorderFactory.createEtchedBorder());

        this.add(m_icon, gbc.fillHorizontal().setWeightX(0).build());
        this.add(m_textArea, gbc.incX().fillBoth().setWeightX(1.0).setWeightY(1.0).build());


//        this.add(m_icon, gbc);
//        gbc.gridx = 1;
//        gbc.weightx = 1;
//        gbc.weighty = 1;
//        //TODO maybe horizontal is sufficent
//        gbc.fill = GridBagConstraints.BOTH;
//        this.add(m_textArea, gbc);

    }

    /**
     * Creates the GridBagConstraints.
     *
     * @return the GridBagConstraints
     */
    private GBCBuilder createGBC() {
        final GBCBuilder gbc = new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
//        final GridBagConstraints gbc = new GridBagConstraints();
//        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
//        gbc.gridx = 0;
//        gbc.gridy = 0;
//        gbc.weightx = 0;
//        gbc.weighty = 0;
//        gbc.fill = GridBagConstraints.HORIZONTAL;

        return gbc;
    }

    /**
     * Initialize a JTextArea with specific settings and a certain text.
     *
     * @param text the text to set in the JTextArea
     */
    private void initializeTextArea(final String text) {
        m_textArea = new JTextArea(0,0);
        //TODO....
//        m_textArea.setMinimumSize(new Dimension(150,20));
//        m_textArea.setMinimumSize(new Dimension(20, 1));

        m_textArea.setPreferredSize(new Dimension(150, 20));
        m_textArea.setText(text);
        m_textArea.setForeground(Color.red);
        m_textArea.setWrapStyleWord(true);
        m_textArea.setLineWrap(true);
        m_textArea.setWrapStyleWord(true);
        m_textArea.setOpaque(false);
        m_textArea.setEditable(false);
        m_textArea.setFocusable(false);
        m_textArea.setFont(UIManager.getFont("Label.font"));
    }

    /**
     * Sets the text of the textarea.
     *
     * @param text the text to set
     */
    public void setText(final String text) {
        m_textArea.setText(text);
    }
    /**
     * Sets the icon.
     *
     * @param icon the icon to set.
     */
    public void setIcon(final Icon icon) {
        m_icon.setIcon(icon);
    }

    /**
     * Returns the JPanel.
     *
     * @return the JPanel
     */
    public JPanel getPanel() {
        return this;
    }

}
