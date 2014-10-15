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
package org.candlepin.model;

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * PoolFilterBuilder
 *
 * Builds criteria to find pools based upon their attributes and product attributes
 */
public class PoolFilterBuilder extends FilterBuilder {

    private static Logger log = LoggerFactory.getLogger(PoolFilterBuilder.class);

    /**
     * Add filters to search only for pools containing the given text. A number of
     * fields on the pool are searched including it's SKU, SKU product name, and provided
     * (engineering) product IDs and their names.
     *
     * @param containsText Text to search for in various fields on the pool. Basic
     * wildcards are supported for everything or a single character. (* and ? respectively)
     */
    public void addContainsTextFilter(String containsText) {

        Disjunction textOr = Restrictions.disjunction();
        textOr.add(new FilterLikeExpression("productName", containsText, true));
        textOr.add(new FilterLikeExpression("productId", containsText, true));
        textOr.add(new FilterLikeExpression("contractNumber", containsText, true));
        textOr.add(new FilterLikeExpression("orderNumber", containsText, true));
        textOr.add(Subqueries.exists(
                createProvidedProductCriteria(containsText)));
        textOr.add(Subqueries.exists(
                createAttributeCriteria(ProductPoolAttribute.class, "support_level",
                Arrays.asList(containsText))));
        this.otherCriteria.add(textOr);
    }

    private DetachedCriteria createProvidedProductCriteria(String searchString) {

        DetachedCriteria attrMatch = DetachedCriteria.forClass(
            ProvidedProduct.class, "provided");

        List<Criterion> providedOrs = new ArrayList<Criterion>();
        providedOrs.add(new FilterLikeExpression("productId", searchString, true));
        providedOrs.add(new FilterLikeExpression("productName", searchString, true));

        attrMatch.add(Restrictions.or(
            providedOrs.toArray(new Criterion[providedOrs.size()]))
        );

        attrMatch.add(Property.forName("this.id").eqProperty("provided.pool.id"));
        attrMatch.setProjection(Projections.property("provided.id"));

        return attrMatch;
    }

    @Override
    protected Criterion buildCriteriaForKey(String key, List<String> values) {
        DetachedCriteria productPoolAttrMatch =
            createAttributeCriteria(PoolAttribute.class, key, values);

        DetachedCriteria poolAttrMatch =
            createAttributeCriteria(ProductPoolAttribute.class, key, values);

        return Restrictions.or(Subqueries.exists(productPoolAttrMatch),
            Subqueries.exists(poolAttrMatch));
    }

    private DetachedCriteria createAttributeCriteria(
        Class<? extends AbstractPoolAttribute> entityClass, String attributeName,
        List<String> possibleValues) {
        DetachedCriteria attrMatch = DetachedCriteria.forClass(
            entityClass, "attr");
        attrMatch.add(new FilterLikeExpression("name", attributeName, false));

        // It would be nice to be able to use an 'in' restriction here, but
        // hibernate does not support ignoring case with its 'in' restriction.
        // We could probably roll our own, but would involve duplicating some
        // hibernate code to achieve it.
        List<Criterion> attrOrs = new ArrayList<Criterion>();
        for (String val : possibleValues) {
            // Setting an attribute value as '' may end up being set to null,
            // so we check both.
            if (val == null || val.isEmpty()) {
                attrOrs.add(Restrictions.isNull("value"));
                attrOrs.add(Restrictions.eq("value", ""));
            }
            else {
                attrOrs.add(new FilterLikeExpression("value", val, true));
            }
        }
        attrMatch.add(Restrictions.or(
            attrOrs.toArray(new Criterion[attrOrs.size()]))
        );

        attrMatch.add(Property.forName("this.id").eqProperty("attr.pool.id"));
        attrMatch.setProjection(Projections.property("attr.id"));

        // We don't want to match Product Attributes that have been overridden
        if (entityClass == ProductPoolAttribute.class) {
            DetachedCriteria overridden =
                DetachedCriteria.forClass(PoolAttribute.class, "pattr")
                // If we're using wildcards in the name, we should block exact matches
                    .add(Restrictions.eqProperty("name", "attr.name"))
                    .setProjection(Projections.property("pattr.pool.id"));
            attrMatch.add(Subqueries.propertyNotIn("attr.pool.id", overridden));
        }
        return attrMatch;
    }
}
