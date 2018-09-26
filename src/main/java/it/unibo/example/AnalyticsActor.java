package it.unibo.example;

import akka.japi.pf.ReceiveBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class AnalyticsActor<T> extends SinkActor<T> {
    @Override
    public void preStart() throws Exception {
        super.preStart();
        self().tell(new DoConsume<T>(t -> {
            // TODO: analytics
        }), self());
    }

    @Override
    protected Receive moreBehaviour(){
        return ReceiveBuilder.create()
                .match(Messages.Get.class, msg -> sender().tell(null, self())) // TODO: what analysis to return?
                .build();
    }
}
