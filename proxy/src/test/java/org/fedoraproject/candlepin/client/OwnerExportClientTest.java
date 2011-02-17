/**
 * Copyright (c) 2009 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.fedoraproject.candlepin.client;

//import static org.junit.Assert.assertNotNull;

import org.fedoraproject.candlepin.model.Owner;

import org.jboss.resteasy.client.ProxyFactory;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.junit.Test;


/**
 * OwnerExportClientTest
 */
public class OwnerExportClientTest {

    @Test
    public void exportOwner() {
        RegisterBuiltin.register(ResteasyProviderFactory.getInstance());
        OwnerExportClient oec = ProxyFactory.create(OwnerExportClient.class, "http://localhost:8443/candlepin/");
        //Owner o = oec.exportOwner("8a8b64a32cc73ee5012cc73f01d40009");
        //assertNotNull(o);
        //System.out.println(o);
    }
}
