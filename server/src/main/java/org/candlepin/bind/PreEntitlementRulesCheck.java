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

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import org.candlepin.model.Pool;
import org.candlepin.model.PoolQuantity;
import org.candlepin.policy.EntitlementRefusedException;
import org.candlepin.policy.ValidationResult;
import org.candlepin.policy.js.entitlement.Enforcer;
import org.candlepin.policy.js.entitlement.Enforcer.CallerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This operation is responsible for validating if a bind request is permissible.
 * Uses rules implemented in both Java and Javascript.
 */
public class PreEntitlementRulesCheck implements BindOperation {

    private Enforcer enforcer;
    private CallerType callerType;
    private Map<String, ValidationResult> results;
    private static Logger log = LoggerFactory.getLogger(PreEntitlementRulesCheck.class);

    @Inject
    public PreEntitlementRulesCheck(Enforcer enforcer,
        @Assisted CallerType callerType) {
        this.enforcer = enforcer;
        this.callerType = callerType;
    }

    /**
     * The java script portion of the rules can be run before the locks have been placed.
     * if there is a failure, we stop the chain here.
     * @param context
     * @param chain
     */
    @Override
    public void preProcess(BindContext context, BindChain chain) {

        // fetch pools
        context.getPoolQuantities();
        if (context.isQuantityRequested()) {
            log.info("Running pre-entitlement rules.");
            // XXX preEntitlement is run twice for new entitlement creation
            results = enforcer.preEntitlement(context.getConsumer(),
                context.getPoolQuantities().values(), callerType);

            EntitlementRefusedException exception = checkResults();
            if (exception != null) {
                context.setException(exception, Thread.currentThread().getStackTrace());
                return;
            }
        }

        chain.preProcess(context);
    }

    /**
     * The pool's quantity might have changed since we last fetched it,
     * so ensure that the pool still has enough quantity left.
     * @param context
     * @param chain
     */
    @Override
    public void execute(BindContext context, BindChain chain) {

        if (context.isQuantityRequested()) {
            for (PoolQuantity poolQuantity: context.getPoolQuantities().values()) {
                Pool pool = poolQuantity.getPool();
                enforcer.finishValidation(results.get(pool.getId()),
                    pool, context.getPoolQuantities().get(pool.getId()).getQuantity());
            }
            EntitlementRefusedException exception = checkResults();
            if (exception != null) {
                context.setException(exception, Thread.currentThread().getStackTrace());
                return;
            }
        }

        chain.execute(context);
    }

    private EntitlementRefusedException checkResults() {
        boolean success = true;
        for (Map.Entry<String, ValidationResult> entry : results.entrySet()) {
            ValidationResult result = entry.getValue();
            if (!result.isSuccessful()) {
                log.warn("Entitlement not granted: {} for pool: {}",
                    result.getErrors().toString(), entry.getKey());
                success = false;
            }
        }
        if (!success) {
            return new EntitlementRefusedException(results);
        }
        return null;
    }
}
