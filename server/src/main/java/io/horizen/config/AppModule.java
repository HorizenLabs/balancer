package io.horizen.config;

import com.google.inject.AbstractModule;
import io.horizen.services.NscService;
import io.horizen.services.RosettaService;
import io.horizen.services.SnapshotService;
import io.horizen.services.impl.NscServiceImpl;
import io.horizen.services.impl.RosettaServiceImpl;
import io.horizen.services.impl.SnapshotServiceImpl;

public class AppModule extends AbstractModule {
    private final Settings settings;

    public AppModule(Settings settings) {
        this.settings = settings;
    }

    @Override
    protected void configure() {
        bind(Settings.class).toInstance(this.settings);
        bind(RosettaService.class).to(RosettaServiceImpl.class);
        bind(NscService.class).to(NscServiceImpl.class);
        bind(SnapshotService.class).to(SnapshotServiceImpl.class);
    }
}
