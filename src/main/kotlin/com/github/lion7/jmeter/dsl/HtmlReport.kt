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

    private lateinit var reportGenerator: ReportGenerator
    private var sampleCount = 0L

    override fun testStarted() {
        val resultsFile = filename?.let(::File)?.absoluteFile ?: throw IllegalArgumentException("filename is not set")
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
        if (sampleCount > 0) {
            val outputDir =
                outputDirectory?.let(::File)?.absoluteFile ?: throw IllegalArgumentException("outputDirectory is not set")
            outputDir.deleteRecursively()
            outputDir.mkdirs()
            JMeterUtils.setProperty(JMeter.JMETER_REPORT_OUTPUT_DIR_PROPERTY, outputDir.absolutePath)
            reportGenerator.generate()
        }
    }
}
