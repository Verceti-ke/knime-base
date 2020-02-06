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
 *   18.12.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.connections.base;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.knime.filehandling.core.connections.base.attributes.AttributesCache;
import org.knime.filehandling.core.connections.base.attributes.BaseAttributesCache;
import org.knime.filehandling.core.connections.base.attributes.FSFileAttributes;
import org.knime.filehandling.core.connections.base.attributes.NoOpAttributesCache;

/**
 * Base implementation of {@FileSystem}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public abstract class BaseFileSystem extends FileSystem {

    private final BaseFileSystemProvider m_fileSystemProvider;

    private final URI m_uri;

    private final String m_name;

    private final String m_type;

    private final AttributesCache m_cache;

    private final Map<Integer, Closeable> m_closeables = Collections.synchronizedMap(new HashMap<>());

    /**
     * Constructs {@FileSystem} with the given file system provider, identifying uri and name an type of the file
     * system.
     *
     * @param fileSystemProvider the provider that the file system belongs to
     * @param uri the uri identifying the file system
     * @param name the human readable name of the file system
     * @param type readable type information
     * @param cacheTTL the time to live for cached elements in milliseconds. A value of 0 or smaller indicates no
     *            caching.
     */
    public BaseFileSystem(final BaseFileSystemProvider fileSystemProvider, final URI uri, final String name,
        final String type, final long cacheTTL) {
        Validate.notNull(fileSystemProvider, "File system provider must not be null.");
        Validate.notNull(uri, "URI must not be null.");
        Validate.notNull(name, "Name must not be null.");
        Validate.notNull(type, "Type must not be null.");

        m_fileSystemProvider = fileSystemProvider;
        m_uri = uri;
        m_name = name;
        m_type = type;
        if (cacheTTL > 0) {
            m_cache = new BaseAttributesCache(cacheTTL);
        } else {
            m_cache = new NoOpAttributesCache();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FileSystemProvider provider() {
        return m_fileSystemProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        try {
            prepareClose();

        } finally {

            final ArrayList<Closeable> valuesCopy = new ArrayList<>(m_closeables.values());
            for (final Closeable closeable : valuesCopy) {
                try {
                    closeable.close();
                } catch (final IOException ex) {
                    //Nothing we could do here.
                }
            }
            m_cache.clearCache();
            m_fileSystemProvider.removeFileSystem(m_uri);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isReadOnly() {
        return false;
    }

    /**
     * This method is called in the {@link #close} method before the file system is removed from the list of file
     * systems in the provider. The method should ensure to close all open channels, directory-streams, and other
     * closeable objects associated with this file system.
     */
    protected abstract void prepareClose();

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOpen() {
        return m_fileSystemProvider.isOpen(m_uri);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Iterable<FileStore> getFileStores() {
        return (Iterable<FileStore>)Collections.singletonList(new BaseFileStore(m_type, m_name)).iterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> supportedFileAttributeViews() {
        final Set<String> supportedViews = new HashSet<>();
        supportedViews.add("basic");
        supportedViews.add("posix");
        return supportedViews;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PathMatcher getPathMatcher(final String syntaxAndPattern) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WatchService newWatchService() throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Stores an attribute for the path with the given URI in the attribute cache.
     *
     * @param path the path as String
     * @param attributes the attributes object to store
     */
    public void addToAttributeCache(final String path, final FSFileAttributes attributes) {
        m_cache.storeAttributes(path, attributes);
    }

    /**
     * Returns an Optional containing the cached file-attributes for a path if present.
     *
     * @param path the path as String
     * @return optional file attributes from cache
     */
    public Optional<FSFileAttributes> getCachedAttributes(final String path) {
        return m_cache.getAttributes(path);
    }

    /**
     * If a valid cache entry for this path with the given URI in the provider cache.
     *
     * @param path the path as String
     * @return whether a valid entry is in the cache
     */
    public boolean hasCachedAttributes(final String path) {
        return m_cache.getAttributes(path).isPresent();
    }

    /**
     * Informs the file system, that the corresponding closeable was closed and does not need to be tracked anymore.
     *
     * @param closeable the closeable
     */
    public void notifyClosed(final Closeable closeable) {
        m_closeables.remove(closeable.hashCode());
    }

    /**
     * Adds a {@link Closeable} for tracking, so it can be closed on file system close.
     *
     * @param closeable the closeable
     */
    public void addCloseable(final Closeable closeable) {
        m_closeables.put(closeable.hashCode(), closeable);
    }

}