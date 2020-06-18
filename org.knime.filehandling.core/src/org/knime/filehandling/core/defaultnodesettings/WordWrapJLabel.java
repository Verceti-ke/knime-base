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

import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.regex.Pattern;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.lang3.RegExUtils;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A <code>JLabel</code> that wraps the text in a fixed size HTML paragraph to ensure word wrapping.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class WordWrapJLabel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_WIDTH = 700;

    private String m_html;

    private JLabel m_label;

    /**
     * Creates a <code>JLabel</code> that wraps the text in a fixed size HTML paragraph to ensure word wrapping.
     *
     * @param text the text to set
     */
    public WordWrapJLabel(final String text) {
        this(text, DEFAULT_WIDTH);
    }

    /**
     * Creates a <code>JLabel</code> that wraps the text in a fixed size HTML paragraph to ensure word wrapping.
     *
     * @param text the initial text
     * @param widthInPixel label width in pixels
     */
    public WordWrapJLabel(final String text, final int widthInPixel) {
        super(new GridBagLayout());
        final GBCBuilder gbc = createGBC();
        m_label = new JLabel(text);
        this.add(m_label, gbc.setWeightY(1).setWeightX(0).fillVertical().build());
        m_html = createHtmlTemplate(widthInPixel);
    }

    private GBCBuilder createGBC() {
        return new GBCBuilder(new Insets(5, 5, 5, 5)).resetX().resetY();
    }

    private static String createHtmlTemplate(final int widthInPixel) {
        return "<html><body WIDTH=%d><div><p>%s</p></div></body></html>";
    }

    /**
     * @param widthInPixel
     * @param text
     */
    public void setText(final String text) {
        if (m_html == null) {
            // only happens during the call of the super constructor
            m_label.setText(text);
        } else {
            m_label.setText(String.format(m_html, DEFAULT_WIDTH, addWordBreakHints(text)));
        }

        this.revalidate();
    }

    public void setText(final String text, final int widthInPixel) {
        m_html = createHtmlTemplate(widthInPixel);
        if (m_html == null) {
            // only happens during the call of the super constructor
            m_label.setText(text);
        } else {
            m_label.setText(String.format(m_html, widthInPixel, addWordBreakHints(text)));
        }
        m_label.revalidate();
        m_label.repaint();
    }

    public void setIcon(final Icon icon) {
        m_label.setIcon(icon);
    }

    public JLabel getLabel() {
        return m_label;
    }

    private static String addWordBreakHints(final String text) {
        final String wordBreaksText = text.replaceAll("\\\\", "\\\\<wbr>");
        return RegExUtils.replaceAll(wordBreaksText, Pattern.compile("((?<!<)\\/)"), "<wbr>/");
    }
}
