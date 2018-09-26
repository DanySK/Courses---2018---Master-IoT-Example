package it.unibo.example;

import akka.japi.pf.ReceiveBuilder;
import it.unibo.patterns.ObservableAbstractActor;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;


public class SensorActor<T> extends ObservableAbstractActor<T> {
    private Function<Optional<T>,T> underlyingSensor;
    private final Duration interval;
    private Optional<T> value = Optional.empty();

    @Override
    public Receive createReceive() {
        return IdleBehaviour();
    }

    private static class SampleUnderlyingSensor { }
    public static class Start { }
    public static class Stop { }

    private Receive IdleBehaviour(){
        return ReceiveBuilder.create().
                match(Start.class, cmd -> {
                    context().become(WorkingBehaviour().orElse(ObservableBehaviour()).onMessage());
                    scheduleNextSensorSample();
                }).build()
                .orElse(ObservableBehaviour());
    }

    private Receive WorkingBehaviour(){
        return ReceiveBuilder.create().
                match(SampleUnderlyingSensor.class, cmd -> {
                    value = Optional.of(underlyingSensor.apply(value));
                    notificationBroadcast(value.get());
                    scheduleNextSensorSample();
                })
                .match(Stop.class, cmd -> context().become(IdleBehaviour().onMessage())).build();
    }

    public SensorActor(Function<Optional<T>,T> underlyingSensor){
        this(underlyingSensor, Duration.ofSeconds(1));
    }

    public SensorActor(Function<Optional<T>,T> underlyingSensor, Duration interval){
        this.underlyingSensor = underlyingSensor;
        this.interval = interval;
    }

    public void scheduleNextSensorSample(Duration fd){
        context().system().scheduler().scheduleOnce(fd, self(), new SampleUnderlyingSensor(), context().system().dispatcher(), self());
    }

    public void scheduleNextSensorSample(){
        scheduleNextSensorSample(this.interval);
    }
}
