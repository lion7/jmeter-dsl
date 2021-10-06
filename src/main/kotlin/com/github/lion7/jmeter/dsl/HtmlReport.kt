package com.github.lion7.jmeter.dsl

import org.apache.jmeter.JMeter
import org.apache.jmeter.report.dashboard.ReportGenerator
import org.apache.jmeter.reporters.ResultCollector
import org.apache.jmeter.samplers.SampleEvent
import org.apache.jmeter.util.JMeterUtils
import java.io.File

class HtmlReport : ResultCollector() {

    var outputDirectory: String?
        get() = getPropertyAsString("outputDirectory")
        set(value) {
            setProperty("outputDirectory", value)
        }

    lateinit var resultsFile: File
    lateinit var reportGenerator: ReportGenerator
    var sampleCount = 0L

    override fun testStarted() {
        resultsFile = filename?.let(::File) ?: throw IllegalArgumentException("filename is not set")
        resultsFile.delete()
        reportGenerator = ReportGenerator(resultsFile.absolutePath, this)
        sampleCount = 0
        super.testStarted()
    }

    override fun sampleOccurred(event: SampleEvent) {
        super.sampleOccurred(event)
        sampleCount++
    }

    override fun testEnded() {
        super.testEnded()
        val outputDir = outputDirectory?.let(::File) ?: throw IllegalArgumentException("outputDirectory is not set")
        outputDir.deleteRecursively()
        if (sampleCount > 0) {
            outputDir.parentFile.mkdirs()
            JMeterUtils.setProperty(JMeter.JMETER_REPORT_OUTPUT_DIR_PROPERTY, outputDir.absolutePath)
            reportGenerator.generate()
        }
    }
}
