/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.repository.jpa.osgi.hibernate;

import jakarta.persistence.PersistenceException;
import org.hibernate.boot.archive.spi.ArchiveContext;
import org.hibernate.boot.archive.spi.ArchiveDescriptor;
import org.hibernate.boot.archive.spi.ArchiveEntry;
import org.hibernate.boot.archive.spi.InputStreamAccess;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.osgi.framework.Bundle;
import org.osgi.framework.wiring.BundleWiring;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @Author Aristide Cittadino
 * Take from the old hibernate-osgi project but simplfying it.
 */
public class OsgiArchiveDescriptor implements ArchiveDescriptor {
    private static final CoreMessageLogger LOG = CoreLogging.messageLogger(OsgiArchiveDescriptor.class);

    private final Bundle persistenceBundle;
    private final BundleWiring bundleWiring;

    /**
     * Creates a OsgiArchiveDescriptor
     *
     * @param persistenceBundle The bundle being described as an archive
     */
    @SuppressWarnings("RedundantCast")
    public OsgiArchiveDescriptor(Bundle persistenceBundle) {
        this.persistenceBundle = persistenceBundle;
        bundleWiring = persistenceBundle.adapt(BundleWiring.class);
    }

    @Override
    public void visitArchive(ArchiveContext context) {
        final Collection<String> resources = bundleWiring.listResources("/", "*", BundleWiring.LISTRESOURCES_RECURSE);
        for (final String resource : resources) {
            if (!resource.endsWith("/")) {
                try {
                    final InputStreamAccess inputStreamAccess = new InputStreamAccess() {
                        @Override
                        public String getStreamName() {
                            return resource;
                        }

                        @Override
                        public InputStream accessInputStream() {
                            return openInputStream();
                        }

                        private InputStream openInputStream() {
                            try {
                                return persistenceBundle.getResource(resource).openStream();
                            } catch (IOException e) {
                                throw new PersistenceException(
                                        "Unable to open an InputStream on the OSGi Bundle resource!",
                                        e);
                            }
                        }

                    };

                    final ArchiveEntry entry = new ArchiveEntry() {
                        @Override
                        public String getName() {
                            return resource;
                        }

                        @Override
                        public String getNameWithinArchive() {
                            return resource;
                        }

                        @Override
                        public InputStreamAccess getStreamAccess() {
                            return inputStreamAccess;
                        }
                    };

                    context.obtainArchiveEntryHandler(entry).handleEntry(entry, context);
                } catch (Exception e) {
                    LOG.unableToLoadScannedClassOrResource(e);
                }
            }
        }
    }
}
