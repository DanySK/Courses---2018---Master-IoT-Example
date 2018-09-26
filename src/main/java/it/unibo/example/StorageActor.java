package it.unibo.example;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class StorageActor<T> extends SinkActor<T> {
    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    abstract void store(T t);

    abstract Collection<Map.Entry<Instant,T>> contents();

    @Override
    public void preStart() throws Exception {
        super.preStart();
        self().tell(new DoConsume<T>(t -> {
            log.debug("Storing value " + t);
            store(t);
        }), self());
    }

    @Override
    protected Receive moreBehaviour(){
        return ReceiveBuilder.create()
                .match(Messages.Get.class, msg -> sender().tell(contents(), self()))
                .build();
    }

    public static class BasicStorageActor<T> extends StorageActor<T> {
        Map<Instant,T> storage = new HashMap<>();


        @Override
        void store(T t) {
            storage.put(Instant.now(), t);
        }

        @Override
        Collection<Map.Entry<Instant,T>> contents() {
            return storage.entrySet();
        }
    }
}