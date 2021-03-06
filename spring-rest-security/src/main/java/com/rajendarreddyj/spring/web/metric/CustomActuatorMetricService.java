package com.rajendarreddyj.spring.web.metric;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CustomActuatorMetricService implements ICustomActuatorMetricService {

    private MetricRepository repo;

    @Autowired
    private CounterService counter;

    private final List<ArrayList<Integer>> statusMetricsByMinute;
    private final List<String> statusList;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public CustomActuatorMetricService() {
        super();
        this.repo = new InMemoryMetricRepository();
        this.statusMetricsByMinute = new ArrayList<>();
        this.statusList = new ArrayList<>();
    }

    // API

    @Override
    public void increaseCount(final int status) {
        this.counter.increment("status." + status);
        if (!this.statusList.contains("counter.status." + status)) {
            this.statusList.add("counter.status." + status);
        }
    }

    @Override
    public Object[][] getGraphData() {
        final Date current = new Date();
        final int colCount = this.statusList.size() + 1;
        final int rowCount = this.statusMetricsByMinute.size() + 1;
        final Object[][] result = new Object[rowCount][colCount];
        result[0][0] = "Time";

        int j = 1;
        for (final String status : this.statusList) {
            result[0][j] = status;
            j++;
        }

        for (int i = 1; i < rowCount; i++) {
            result[i][0] = dateFormat.format(new Date(current.getTime() - (60000 * (rowCount - i))));
        }

        List<Integer> minuteOfStatuses;
        for (int i = 1; i < rowCount; i++) {
            minuteOfStatuses = this.statusMetricsByMinute.get(i - 1);
            for (j = 1; j <= minuteOfStatuses.size(); j++) {
                result[i][j] = minuteOfStatuses.get(j - 1);
            }
            while (j < colCount) {
                result[i][j] = 0;
                j++;
            }
        }
        return result;
    }

    // Non - API

    @Scheduled(fixedDelay = 60000)
    private void exportMetrics() {
        Metric<?> metric;
        final ArrayList<Integer> statusCount = new ArrayList<>();
        for (final String status : this.statusList) {
            metric = this.repo.findOne(status);
            if (metric != null) {
                statusCount.add(metric.getValue().intValue());
                this.repo.reset(status);
            } else {
                statusCount.add(0);
            }

        }
        this.statusMetricsByMinute.add(statusCount);
    }
}