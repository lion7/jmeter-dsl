package com.github.lion7.jmeter.dsl

import org.apache.jmeter.testelement.property.DoubleProperty
import org.apache.jmeter.timers.ConstantThroughputTimer

class ConstantThroughputTimer : ConstantThroughputTimer() {

    var calcMode: Mode
        get() = Mode.values()[getPropertyAsInt("calcMode")]
        set(value) {
            super.setCalcMode(value.ordinal)
            setProperty("calcMode", value.ordinal)
        }

    override fun getThroughput(): Double = getPropertyAsDouble("throughput")
    override fun setThroughput(throughput: Double) {
        super.setThroughput(throughput)
        setProperty(DoubleProperty("throughput", throughput))
    }
}
