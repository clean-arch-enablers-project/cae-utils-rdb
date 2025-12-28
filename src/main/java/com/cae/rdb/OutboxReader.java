package com.cae.rdb;

import com.cae.rdb.tables.OutboxItem;
import com.cae.scheduler.DefaultScheduler;
import com.cae.scheduler.DefaultSchedulerSetup;
import com.cae.scheduler.SchedulerEvent;
import com.cae.scheduler.SchedulerEvents;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public abstract class OutboxReader<E extends OutboxItem<I>, I> extends DefaultScheduler {

    protected OutboxReader(
            DefaultBasicOutboxOperations<E, I> dao,
            OutboxStreamer outboxStreamer,
            int batchSize) {
        this.dao = dao;
        this.outboxStreamer = outboxStreamer;
        this.batchSize = batchSize;
    }

    private final DefaultBasicOutboxOperations<E, I> dao;
    private final OutboxStreamer outboxStreamer;
    private final int batchSize;

    @Override
    protected DefaultSchedulerSetup provideSetup() {
        return DefaultSchedulerSetup.builder()
                .fixedRate(false)
                .initialDelay(500)
                .intervals(100)
                .timeUnit(TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public void run() {
        this.streamNextBatch();
    }

    public void streamNextBatch(){
        try {
            var currentBatch = this.dao.getAvailableBatch(this.batchSize);
            var problemsPerBatch = new ArrayList<StreamingProblem>();
            for (var item : currentBatch){
                try{
                    this.outboxStreamer.stream(item);
                    this.dao.deleteById(item.getPrimaryKey());
                } catch (Exception exception){
                    problemsPerBatch.add(new StreamingProblem(exception, item.getPrimaryKey()));
                }
            }
            if (!currentBatch.isEmpty())
                SchedulerEvents.SINGLETON.emit(SchedulerEvent.of("Success streaming " + (currentBatch.size() - problemsPerBatch.size()) + " out of " + currentBatch.size() + " events"));
            problemsPerBatch.forEach(problem -> {
                var exception = problem.exception;
                var wholeMessage = "Couldn't successfully process event with ID '" + problem.id + "' | Caught: " + exception.getClass().getSimpleName() + " | Details: " + exception.getMessage();
                SchedulerEvents.SINGLETON.emit(SchedulerEvent.of(wholeMessage));
            });
        } catch (Exception exception){
            SchedulerEvents.SINGLETON.emit(SchedulerEvent.of("Couldn't start the streaming process | Caught: " + exception.getClass().getSimpleName() + " | Details: " + exception.getMessage()));
        }
    }

    @Builder
    @Getter
    public static class StreamingProblem {

        private final Exception exception;
        private final Object id;

    }

}
