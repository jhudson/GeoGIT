package org.geogit.api;

import org.geogit.api.config.RemoteConfigObject;
import org.geogit.test.RepositoryTestCase;

public class ConfigTest extends RepositoryTestCase {

    private GeoGIT ggit;

    @Override
    protected void setUpInternal() throws Exception {
        ggit = new GeoGIT(repo);
        RemoteConfigObject origin = new RemoteConfigObject("origin", "origin", getRepository().getRepositoryHome().getAbsolutePath());
        ggit.getConfig().addRemoteConfigObject(origin);
    }

    public void testReadWriteConfig() {
        assertNotNull("The settings file is null?", ggit.getConfig());

        RemoteConfigObject rs = ggit.getConfig().getRemote("origin");

        assertNotNull("The remote service origin is null?", rs);
        assertNotNull("The remote service origin's name paramater is null?", rs.getName());
        assertNotNull("The remote service origin's fetch paramater is null?", rs.getFetch());
        assertNotNull("The remote service origin's url paramater is null?", rs.getUrl());
    }
}
