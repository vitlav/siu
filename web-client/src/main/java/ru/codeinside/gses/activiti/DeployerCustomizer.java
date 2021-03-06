/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.activiti;

import org.activiti.engine.impl.bpmn.deployer.BpmnDeployer;
import org.activiti.engine.impl.bpmn.parser.BpmnParser;
import org.activiti.engine.impl.bpmn.parser.CustomBpmnParser;
import org.activiti.engine.impl.persistence.deploy.Deployer;

import java.util.Collection;

final public class DeployerCustomizer {

    public static Collection<? extends Deployer> customize(final Collection<? extends Deployer> deployers) {
        for (final Deployer deployer : deployers) {
            if (deployer instanceof BpmnDeployer) {
                final BpmnDeployer bpmnDeployer = (BpmnDeployer) deployer;
                final BpmnParser oldParser = bpmnDeployer.getBpmnParser();
                final CustomBpmnParser newParser = new CustomBpmnParser(oldParser.getExpressionManager());
                newParser.getParseListeners().addAll(oldParser.getParseListeners());
                bpmnDeployer.setBpmnParser(newParser);
            }
        }
        return deployers;
    }
}
