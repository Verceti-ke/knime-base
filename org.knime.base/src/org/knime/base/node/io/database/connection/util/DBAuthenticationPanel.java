/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 * ---------------------------------------------------------------------
 *
 * History
 *   08.05.2014 (thor): created
 */
package org.knime.base.node.io.database.connection.util;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.workflow.CredentialsProvider;

/**
 * A panel for database authentication settings. The panel has a {@link GridBagLayout} and uses the protected
 * {@link #m_c} {@link GridBagConstraints} for layouting. You should re-use the constraints when extending this panel.
 *
 * @param <T> a subclass of {@link DatabaseConnectionSettings}
 * @author Thorsten Meinl, KNIME.com, Zurich, Switzerland
 * @since 2.10
 */
public class DBAuthenticationPanel<T extends DatabaseConnectionSettings> extends JPanel {
    private static final long serialVersionUID = -1251640708440701579L;

    /**
     * The settings object from which and to which the panel read/writes the settings.
     */
    protected final T m_settings;

    /**
     * Checkbox whether credentials should be used or not.
     */
    protected final JRadioButton m_useCredentials = new JRadioButton("Use credentials");

    /**
     * Checkbox whether username&password should be used.
     */
    protected final JRadioButton m_usePassword = new JRadioButton("Use username & password");

    /**
     * List for the available workflow credentials.
     */
    protected final JComboBox<String> m_credentials = new JComboBox<>();

    /**
     * Text field for the username.
     */
    protected final JTextField m_username = new JTextField();

    /**
     * Text field for the password.
     */
    protected final JPasswordField m_password = new JPasswordField();

    /**
     * Gridbag constraints object used for layouting the panel.
     */
    protected final GridBagConstraints m_c = new GridBagConstraints();

    /**
     * Creates a new authentication panel.
     *
     * @param settings the settings object the panel should use
     */
    public DBAuthenticationPanel(final T settings) {
        super(new GridBagLayout());
        m_settings = settings;

        m_c.gridx = 0;
        m_c.gridy = 0;
        m_c.gridwidth = 2;
        m_c.insets = new Insets(2, 2, 2, 2);
        m_c.anchor = GridBagConstraints.WEST;

        add(m_useCredentials, m_c);

        m_c.gridy++;
        m_c.insets = new Insets(2, 25, 2, 2);
        m_c.fill = GridBagConstraints.HORIZONTAL;
        m_c.weightx = 1;
        add(m_credentials, m_c);

        m_c.gridy++;
        m_c.insets = new Insets(2, 2, 2, 2);
        add(m_usePassword, m_c);

        m_c.gridy++;
        m_c.insets = new Insets(2, 25, 2, 2);
        m_c.gridwidth = 1;
        m_c.weightx = 0;
        final JLabel usernameLabel = new JLabel("Username   ");
        add(usernameLabel, m_c);
        m_c.gridx = 1;
        m_c.weightx = 1;
        add(m_username, m_c);

        m_c.gridx = 0;
        m_c.gridy++;
        m_c.weightx = 0;
        final JLabel passwordLabel = new JLabel("Password   ");
        add(passwordLabel, m_c);
        m_c.gridx = 1;
        m_c.weightx = 1;
        add(m_password, m_c);

        ButtonGroup bg = new ButtonGroup();
        bg.add(m_useCredentials);
        bg.add(m_usePassword);

        m_useCredentials.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                m_credentials.setEnabled(m_useCredentials.isSelected());
                usernameLabel.setEnabled(!m_useCredentials.isSelected());
                m_username.setEnabled(!m_useCredentials.isSelected());
                passwordLabel.setEnabled(!m_useCredentials.isSelected());
                m_password.setEnabled(!m_useCredentials.isSelected());
            }
        });

        m_useCredentials.doClick();

        setBorder(BorderFactory.createTitledBorder("Authentication"));
    }

    /**
     * Loads the settings into the dialog components.
     *
     * @param specs the incoming port specs.
     * @param credentialProvider a credential provider
     * @throws NotConfigurableException if the dialog should not open because necessary information is missing
     */
    public void loadSettings(final PortObjectSpec[] specs, final CredentialsProvider credentialProvider)
        throws NotConfigurableException {
        m_credentials.removeAllItems();
        for (String s : credentialProvider.listNames()) {
            m_credentials.addItem(s);
        }

        if (m_settings.getCredentialName() == null) {
            m_usePassword.doClick();
            m_username.setText(m_settings.getUserName(credentialProvider));
            m_password.setText(m_settings.getPassword(credentialProvider));
        } else {
            m_useCredentials.doClick();
            m_credentials.setSelectedItem(m_settings.getCredentialName());
        }
    }

    /**
     * Saves the component values into the settings object.
     *
     * @throws InvalidSettingsException if some settings are invalid
     */
    public void saveSettings() throws InvalidSettingsException {
        if (m_useCredentials.isSelected()) {
            m_settings.setCredentialName((String)m_credentials.getSelectedItem());
        } else {
            m_settings.setUserName(m_username.getText());
            m_settings.setPassword(new String(m_password.getPassword()));
        }
    }
}