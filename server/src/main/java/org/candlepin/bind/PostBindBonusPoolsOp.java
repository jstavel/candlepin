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

import org.candlepin.controller.PoolManager;
import org.candlepin.model.Consumer;
import org.candlepin.model.Entitlement;
import org.candlepin.model.PoolQuantity;

import com.google.inject.Inject;

import java.util.Map;

/**
 * This Operation will be replaced shortly in an upcoming PR, where we
 * will split the implementation into pre-process and execute steps.
 */
public class PostBindBonusPoolsOp implements BindOperation {

    private PoolManager poolManager;

    @Inject
    public PostBindBonusPoolsOp(PoolManager poolManager) {
        this.poolManager = poolManager;
    }

    @Override
    public void preProcess(BindContext context, BindChain chain) {
        chain.preProcess(context);
    }

    @Override
    public void execute(BindContext context, BindChain chain) {
        Consumer consumer = context.getLockedConsumer();
        Map<String, Entitlement> entitlements = context.getEntitlementMap();
        Map<String, PoolQuantity> poolQuantities = context.getPoolQuantities();

        poolManager.handlePostEntitlement(poolManager, consumer, entitlements, poolQuantities);
        // we might have changed the bonus pool quantities, lets revoke ents if needed.
        poolManager.checkBonusPoolQuantities(consumer.getOwner(), entitlements);
        chain.execute(context);
    }

    @Override
    public String getName() {
        return "PostBindBonusPoolsOp";
    }
}
