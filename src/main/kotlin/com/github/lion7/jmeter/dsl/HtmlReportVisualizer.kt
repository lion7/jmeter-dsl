package com.github.lion7.jmeter.dsl

import org.apache.jmeter.gui.util.FilePanelEntry
import org.apache.jmeter.gui.util.VerticalPanel
import org.apache.jmeter.samplers.SampleResult
import org.apache.jmeter.testelement.TestElement
import org.apache.jmeter.visualizers.gui.AbstractVisualizer
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.border.Border
import javax.swing.border.EmptyBorder
import javax.swing.event.ChangeEvent

class HtmlReportVisualizer : AbstractVisualizer() {

    private val outputDirectoryPanel: FilePanelEntry

    init {
        this.collector = HtmlReport()
        this.layout = BorderLayout()

        outputDirectoryPanel = FilePanelEntry("Output directory", true)
        outputDirectoryPanel.addChangeListener(this)

        // MAIN PANEL
        val mainPanel: JPanel = VerticalPanel()
        val margin: Border = EmptyBorder(5, 10, 5, 10)
        this.border = margin

        val titlePanel = makeTitlePanel()
        titlePanel.add(outputDirectoryPanel, 0)
        mainPanel.add(titlePanel)

        this.add(mainPanel, BorderLayout.CENTER)
    }

    override fun add(sample: SampleResult) {
        // do nothing
    }

    override fun getLabelResource(): String =
        "html_report"

    override fun clearData() {
        collector.clearData()
    }

    override fun configure(el: TestElement) {
        super.configure(el)
        if (el is HtmlReport) {
            collector = el
            outputDirectoryPanel.filename = el.outputDirectory
        }
    }

    override fun clearGui() {
        super.clearGui()
        outputDirectoryPanel.clearGui()
    }

    override fun stateChanged(e: ChangeEvent) {
        val el = collector
        if (e.source == outputDirectoryPanel && el is HtmlReport) {
            el.outputDirectory = outputDirectoryPanel.filename
        }
    }
}
