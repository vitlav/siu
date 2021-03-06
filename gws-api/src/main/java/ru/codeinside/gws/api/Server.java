/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * Copyright (c) 2013, MPL CodeInside http://codeinside.ru
 */

package ru.codeinside.gws.api;

import java.net.URL;

public interface Server {

  /**
   * Поддерживаемвя ревизия.
   */
  Revision getRevision();

  /**
   * Описатель сервиса.
   */
  URL getWsdlUrl();

  ServerResponse processRequest(RequestContext requestContext);


  ServerResponse processStatus(String statusMessage, ReceiptContext exchangeContext);


  ServerResponse processResult(String resultMessage, ReceiptContext exchangeContext);

}
