package it.unibo.patterns;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.japi.pf.ReceiveBuilder;
import it.unibo.patterns.ObservableAbstractActor;

import java.util.function.Consumer;

public class ObserverActor<T> extends AbstractActor {
    private final ActorRef obs;
    private final Consumer<T> onNotification;

    public ObserverActor(Consumer<T> onNotification, ActorRef obs){
        this.onNotification = onNotification;
        this.obs = obs;
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create()
                .match(ObservableAbstractActor.Notify.class, v -> onNotification.accept((T)v.getValue()))
                .build();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        obs.tell(new ObservableAbstractActor.AddObserver(self()), self());
    }
}
