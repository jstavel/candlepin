/**
 * Copyright (c) 2009 - 2017 Red Hat, Inc.
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

import org.candlepin.controller.CandlepinPoolManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Holds and represents the binding chain of responsibility.
 * Inspired by the servlet filter interfaces.
 */
public class BindChain {
    private List<BindOperation> operations;
    private int preProcessIndex = -1;
    private int executeIndex = -1;
    private Date lastTime = new Date();
    private static Logger log = LoggerFactory.getLogger(BindChain.class);
    public BindChain() {
        operations = new ArrayList<BindOperation>();
    }

    public void preProcess(BindContext context) {
        preProcessIndex++;
        if (preProcessIndex < operations.size()) {
            Date date = new Date();
            long diff = (date).getTime() - lastTime.getTime();
            log.error("Vritant starting preprocess "+operations.get(preProcessIndex).getName()+ "after"+
                diff);
            lastTime = date;
            operations.get(preProcessIndex).preProcess(context, this);

        }
    }

    public void execute(BindContext context) {
        executeIndex++;

        if (executeIndex < operations.size()) {

            Date date = new Date();
            long diff = (date).getTime() - lastTime.getTime();
            log.error("Vritant starting execute "+ operations.get(executeIndex).getName()+ "after"+
                diff);
            lastTime = date;

            operations.get(executeIndex).execute(context, this);
        }
    }

    public void addOperation(BindOperation operation) {
        operations.add(operation);
    }
}
