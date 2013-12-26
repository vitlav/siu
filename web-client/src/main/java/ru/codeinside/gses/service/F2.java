/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gses.service;

import org.activiti.engine.ProcessEngine;

import java.io.Serializable;

public interface F2<R, A1, A2> extends Serializable {
  R apply(ProcessEngine engine, A1 arg1, A2 arg2);
}