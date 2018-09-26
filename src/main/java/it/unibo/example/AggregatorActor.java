package it.unibo.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.ReceiveBuilder;
import akka.routing.BroadcastRoutingLogic;
import akka.routing.Router;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import it.unibo.patterns.ManyToManyBridge;
import it.unibo.patterns.ObservableAbstractActor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public abstract class AggregatorActor<T> extends AbstractActor {

    protected LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    protected Cache<ActorRef, T> sensors = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(20, TimeUnit.SECONDS)
            .build();

    protected Router router = new Router(new BroadcastRoutingLogic());

    @Override
    public Receive createReceive() {
        scheduleNextAggregation();
        return ReceiveBuilder.create()
                .match(SensorData.class, sd -> sensors.put(sender(), (T)sd.getValue()))
                .match(ManyToManyBridge.ForwardTo.class, f -> router = router.addRoutee(f.getTarget()))
                .match(DoAggregate.class, doIt -> {
                    T aggr = aggregate();
                    log.debug("Aggregating into " + aggr + "; forwarding to " + router.routees());
                    route(aggr);
                    scheduleNextAggregation();
                })
                .build();
    }

    protected abstract T aggregate();

    protected void route(T aggrValue){
        router.route(new ObservableAbstractActor.Notify(aggrValue), self());
    }

    private void scheduleNextAggregation(){
        scheduleNextAggregation(Duration.ofSeconds(5L));
    }

    private void scheduleNextAggregation(Duration fd){
        context().system().scheduler().scheduleOnce(fd, self(), new DoAggregate(), context().system().dispatcher(), self());
    }

    private static class DoAggregate { }

    public static class SensorData<T>{
        private final T value;

        public SensorData(T value){
            this.value = value;
        }

        public T getValue() {
            return value;
        }
    }
}
