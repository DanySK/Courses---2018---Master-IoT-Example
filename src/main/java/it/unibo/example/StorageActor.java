package it.unibo.example;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class StorageActor<T> extends SinkActor<T> {
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    Map<Instant,T> storage = new HashMap<>();

    @Override
    public void preStart() throws Exception {
        super.preStart();
        self().tell(new DoConsume<T>(t -> {
            log.debug("Storing value " + t);
            storage.put(Instant.now(), t);
        }), self());
    }

    @Override
    protected Receive moreBehaviour(){
        return ReceiveBuilder.create()
                .match(Messages.Get.class, msg -> sender().tell(storage.entrySet(), self()))
                .build();
    }
}