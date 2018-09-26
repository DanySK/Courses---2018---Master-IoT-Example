package it.unibo.example;

import java.util.stream.Collectors;

public class MeanAggregatorActor extends AggregatorActor<Double> {
    @Override
    protected Double aggregate() {
        log.debug(sensors.asMap().toString());
        return sensors.asMap().values().stream().collect(Collectors.<Double>averagingDouble(x -> x));
    }
}
