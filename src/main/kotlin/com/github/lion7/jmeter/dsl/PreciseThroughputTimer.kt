package com.github.lion7.jmeter.dsl

import org.apache.jmeter.testelement.property.DoubleProperty
import org.apache.jmeter.timers.poissonarrivals.PreciseThroughputTimer

class PreciseThroughputTimer : PreciseThroughputTimer() {

    override fun getThroughput(): Double = getPropertyAsDouble("throughput")
    override fun setThroughput(throughput: Double) {
        super.setThroughput(throughput)
        setProperty(DoubleProperty("throughput", throughput))
    }

    override fun getThroughputPeriod(): Int = getPropertyAsInt("throughputPeriod")
    override fun setThroughputPeriod(throughputPeriod: Int) {
        super.setThroughputPeriod(throughputPeriod)
        setProperty("throughputPeriod", throughputPeriod)
    }

    override fun getDuration(): Long = getPropertyAsLong("duration")
    override fun setDuration(duration: Long) {
        super.setDuration(duration)
        setProperty("duration", duration)
    }
}
