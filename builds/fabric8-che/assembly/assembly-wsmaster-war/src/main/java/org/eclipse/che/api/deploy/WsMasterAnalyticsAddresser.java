/*
 * Copyright (c) 2016-2017 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.deploy;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import org.eclipse.che.commons.lang.NameGenerator;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class WsMasterAnalyticsAddresser {

  private static final Logger LOG = LoggerFactory.getLogger(WsMasterAnalyticsAddresser.class);

  public static final String ID = NameGenerator.generate("CHM", 10);

  @ScheduleRate(period = 1, unit = TimeUnit.HOURS)
  void send() {
    HttpURLConnection connection = null;
    try {
      final URL url = new URL("https://install.codenvycorp.com/che/init/server?id=" + ID);
      connection = (HttpsURLConnection) url.openConnection();
      connection.getResponseCode();
    } catch (Exception e) {
      LOG.debug("Failed to send master analytics", e);
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
  }
}
