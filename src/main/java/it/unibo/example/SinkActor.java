package it.unibo.example;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import it.unibo.patterns.ObservableAbstractActor;

import java.awt.image.RescaleOp;
import java.util.function.Consumer;

public class SinkActor<T> extends AbstractActor {
    DoConsume consume;

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().match(DoConsume.class, t -> {
            this.consume = t;
            this.context().become(workingReceive().onMessage());
        }).build();
    }

    public Receive workingReceive() {
        return ReceiveBuilder.create().match(ObservableAbstractActor.Notify.class, t -> {
            this.consume.getConsume().accept((T) t.getValue());
        }).build().orElse(moreBehaviour());
    }

    // template method
    protected Receive moreBehaviour(){
        return ReceiveBuilder.create().build();
    }

    public static class DoConsume<T> {
        private final Consumer<T> consume;

        public DoConsume(Consumer<T> consume){

            this.consume = consume;
        }

        public Consumer<T> getConsume() {
            return consume;
        }
    }
}
