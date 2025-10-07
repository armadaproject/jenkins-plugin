package io.armadaproject.jenkins.plugin.job;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

public class DailyArmadaJobSetStrategy implements ArmadaJobSetStrategy {
    private final String jobSetPrefix;

    public DailyArmadaJobSetStrategy(String jobSetPrefix) {
        this.jobSetPrefix = jobSetPrefix;
    }

    @Override
    public String getCurrentJobSet() {
        return jobSetPrefix + new SimpleDateFormat("-ddMMyyyy").format(new Date());
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DailyArmadaJobSetStrategy)) return false;
        DailyArmadaJobSetStrategy that = (DailyArmadaJobSetStrategy) o;
        return Objects.equals(jobSetPrefix, that.jobSetPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(jobSetPrefix);
    }
}
