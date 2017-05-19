/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
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
package org.candlepin.bind;

import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.EntitlementCurator;
import org.candlepin.model.Pool;
import org.candlepin.model.PoolCurator;
import org.candlepin.model.PoolQuantity;

import com.google.inject.Inject;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This bind operation is responsible to create, enrich and persist entitlements.
 * In addition, it also updates pool and consumer consumed counts.
 */
public class HandleEntitlementsOp implements BindOperation {

    private PoolCurator poolCurator;
    private EntitlementCurator entitlementCurator;

    @Inject
    public HandleEntitlementsOp(PoolCurator poolCurator, EntitlementCurator entitlementCurator) {
        this.entitlementCurator = entitlementCurator;
        this.poolCurator = poolCurator;
    }

    /**
     * generates entitlement objects without associating them with pools or consumers.
     * @param context
     * @param chain
     */
    @Override
    public void preProcess(BindContext context, BindChain chain) {
        context.getEntitlementMap();
        chain.preProcess(context);
    }

    /**
     * associates the pools, consumers with entitlements, and persists the entities as needed.
     * also computes all the consumed counts.
     * @param context
     * @param chain
     */
    @Override
    public void execute(BindContext context, BindChain chain) {
        Consumer consumer = context.getLockedConsumer();
        Map<String, Entitlement> entitlementMap = context.getEntitlementMap();
        Map<String, PoolQuantity> lockedPools = context.getPoolQuantities();
        List<Pool> poolsToSave = new LinkedList<Pool>();
        for (Entry<String, PoolQuantity> entry: context.getPoolQuantities().entrySet()) {
            Entitlement ent = entitlementMap.get(entry.getKey());
            Pool pool = lockedPools.get(entry.getKey()).getPool();
            Integer quantity = ent.getQuantity();

            Set<Entitlement> ents = new HashSet<Entitlement>(pool.getEntitlements());
            ents.add(ent);
            pool.setEntitlements(ents);
            ent.setPool(pool);
            ent.setConsumer(consumer);
            ent.setOwner(consumer.getOwner());

            pool.setConsumed(pool.getConsumed() + quantity);
            if (consumer.isManifestDistributor()) {
                pool.setExported(pool.getExported() + quantity);
            }
            else if (consumer.isShare()) {
                pool.setShared(pool.getShared() + quantity);
            }
            consumer.addEntitlement(ent);
            consumer.setEntitlementCount(consumer.getEntitlementCount() + quantity);
            poolsToSave.add(pool);
        }
        entitlementCurator.saveAll(entitlementMap.values(), false, false);
        poolCurator.updateAll(poolsToSave, false, false);
        chain.execute(context);
    }
}
