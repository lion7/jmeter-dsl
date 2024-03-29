package com.github.lion7.jmeter.dsl

import org.apache.jmeter.assertions.ResponseAssertion
import org.apache.jmeter.assertions.XPath2Assertion
import org.apache.jmeter.assertions.gui.AssertionGui
import org.apache.jmeter.assertions.gui.XPath2AssertionGui
import org.apache.jmeter.control.Controller
import org.apache.jmeter.control.InterleaveControl
import org.apache.jmeter.control.LoopController
import org.apache.jmeter.control.OnceOnlyController
import org.apache.jmeter.control.RandomOrderController
import org.apache.jmeter.control.RunTime
import org.apache.jmeter.control.WhileController
import org.apache.jmeter.control.gui.InterleaveControlGui
import org.apache.jmeter.control.gui.LoopControlPanel
import org.apache.jmeter.control.gui.OnceOnlyControllerGui
import org.apache.jmeter.control.gui.RandomOrderControllerGui
import org.apache.jmeter.control.gui.RunTimeGui
import org.apache.jmeter.control.gui.TestPlanGui
import org.apache.jmeter.control.gui.WhileControllerGui
import org.apache.jmeter.engine.StandardJMeterEngine
import org.apache.jmeter.extractor.XPath2Extractor
import org.apache.jmeter.extractor.gui.XPath2ExtractorGui
import org.apache.jmeter.gui.JMeterGUIComponent
import org.apache.jmeter.gui.TestElementMetadata
import org.apache.jmeter.protocol.http.control.Header
import org.apache.jmeter.protocol.http.control.HeaderManager
import org.apache.jmeter.protocol.http.control.gui.HttpTestSampleGui
import org.apache.jmeter.protocol.http.gui.HeaderPanel
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerBase
import org.apache.jmeter.protocol.http.sampler.HTTPSamplerProxy
import org.apache.jmeter.protocol.http.util.HTTPFileArg
import org.apache.jmeter.reporters.ResultCollector
import org.apache.jmeter.samplers.Sampler
import org.apache.jmeter.save.SaveService
import org.apache.jmeter.testelement.TestElement
import org.apache.jmeter.testelement.TestPlan
import org.apache.jmeter.threads.SetupThreadGroup
import org.apache.jmeter.threads.ThreadGroup
import org.apache.jmeter.threads.gui.SetupThreadGroupGui
import org.apache.jmeter.threads.gui.ThreadGroupGui
import org.apache.jmeter.timers.ConstantTimer
import org.apache.jmeter.timers.GaussianRandomTimer
import org.apache.jmeter.timers.gui.ConstantTimerGui
import org.apache.jmeter.timers.gui.GaussianRandomTimerGui
import org.apache.jmeter.timers.gui.PoissonRandomTimerGui
import org.apache.jmeter.util.JMeterUtils
import org.apache.jmeter.visualizers.ViewResultsFullVisualizer
import org.apache.jorphan.collections.HashTree
import org.apache.jorphan.collections.ListedHashTree
import java.io.File
import java.net.JarURLConnection
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.IdentityHashMap
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.toPath

class JMeterDsl {

    companion object {

        fun jMeter(f: JMeterDsl.() -> Unit): JMeterDsl {
            init()
            return JMeterDsl().apply(f)
        }

        private fun init() {
            if (JMeterUtils.getJMeterHome() != null) return

            // Determine the JMeter home dir
            val jMeterHome: Path =
                System.getProperty("jmeter.outputDir")?.let(Paths::get) ?: Files.createTempDirectory("jmeter-dsl")
                    .also { tempDir ->
                        Runtime.getRuntime().addShutdownHook(Thread { tempDir.toFile().deleteRecursively() })
                    }

            // Unzip bundled resources
            val jmeterProperties = JMeterDsl::class.java.getResource("/jmeter/jmeter.properties")
                ?: throw IllegalStateException("Failed to find bundled resources")
            val jmeterResources: Path = when (val conn = jmeterProperties.openConnection()) {
                is JarURLConnection -> FileSystems.newFileSystem(conn.jarFileURL.toURI().toPath()).getPath("jmeter")
                else -> jmeterProperties.toURI().toPath().resolve("..").normalize()
            }
            val targetDir = jMeterHome.resolve("bin").createDirectories()
            jmeterResources.copyRecursively(targetDir)

            // JMeter initialization (properties, log levels, locale, etc)
            JMeterUtils.setJMeterHome(jMeterHome.toString())
            JMeterUtils.loadJMeterProperties(jMeterHome.resolve("bin/jmeter.properties").toString())
            JMeterUtils.initLocale()

            // force initialization of localhost details
            JMeterUtils.getLocalHostFullName()
        }

        private fun Path.copyRecursively(targetDir: Path) = Files.walk(this).forEachOrdered { source ->
            val path = relativize(source).toString()
            val target = targetDir.resolve(path)
            source.copyTo(target, true)
        }
    }

    // root test tree
    private val tree: HashTree = ListedHashTree()

    // map with subtrees
    private val subTrees = IdentityHashMap<TestElement, HashTree>()

    @JMeterDslMarker
    fun run() {
        val jmeter = StandardJMeterEngine()
        jmeter.configure(tree)
        jmeter.run()
    }

    @JMeterDslMarker
    fun save(file: File) = file.outputStream().use {
        SaveService.saveTree(tree, it)
    }

    @JMeterDslMarker
    fun testPlan(configure: TestPlan.() -> Unit) {
        val testPlan = createTestElementWithGui<TestPlan, TestPlanGui>()
        subTrees[testPlan] = tree.add(testPlan)
        testPlan.configure()
    }

    @JMeterDslMarker
    fun TestPlan.setupThreadGroup(configure: SetupThreadGroup.() -> Unit) =
        testElement<SetupThreadGroup, SetupThreadGroupGui>(this, configure)

    @JMeterDslMarker
    fun TestPlan.threadGroup(configure: ThreadGroup.() -> Unit) = testElement<ThreadGroup, ThreadGroupGui>(this) {
        numThreads = 1
        configure()
    }

    @JMeterDslMarker
    fun ThreadGroup.mainController(configure: LoopController.() -> Unit) {
        schema.mainController.getOrNull(this)?.let(subTrees::remove)
        val controller = testElement<LoopController, LoopControlPanel>(this) {
            name = "Main Controller"
            loops = 1
            setContinueForever(false)
        }
        schema.mainController[this] = controller
        controller.configure()
    }

    @JMeterDslMarker
    fun Controller.loopController(configure: LoopController.() -> Unit) =
        testElement<LoopController, LoopControlPanel>(this, configure)

    @JMeterDslMarker
    fun Controller.onceOnlyController(configure: OnceOnlyController.() -> Unit) =
        testElement<OnceOnlyController, OnceOnlyControllerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.whileController(configure: WhileController.() -> Unit) =
        testElement<WhileController, WhileControllerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.runTimeController(configure: RunTime.() -> Unit) =
        testElement<RunTime, RunTimeGui>(this, configure)

    @JMeterDslMarker
    fun Controller.randomOrderController(configure: RandomOrderController.() -> Unit) =
        testElement<RandomOrderController, RandomOrderControllerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.interleaveController(configure: InterleaveControl.() -> Unit) =
        testElement<InterleaveControl, InterleaveControlGui>(this, configure)

    @JMeterDslMarker
    fun HTTPSamplerBase.headerManager(configure: HeaderManager.() -> Unit) =
        testElement<HeaderManager, HeaderPanel>(this, configure)

    @JMeterDslMarker
    fun Controller.httpSampler(configure: HTTPSamplerProxy.() -> Unit) =
        testElement<HTTPSamplerProxy, HttpTestSampleGui>(this, configure)

    @JMeterDslMarker
    fun Controller.httpSampler(
        method: String,
        uri: URI,
        headers: Map<String, String> = emptyMap(),
        formData: List<Pair<String, String>> = emptyList(),
        files: List<Pair<String, File>> = emptyList()
    ) = httpSampler {
        this.method = method
        this.path = uri.toASCIIString()
        this.headerManager = headerManager {
            headers.map { Header(it.key, it.value) }.forEach(this::add)
        }

        if (formData.isNotEmpty()) {
            formData.forEach { addArgument(it.first, it.second) }
            doMultipart = true
        }

        if (files.isNotEmpty()) {
            // copy content from the provided file and make sure it is POSTed as raw data
            httpFiles =
                files.map { HTTPFileArg(it.second.absolutePath, it.first, Files.probeContentType(it.second.toPath())) }
                    .toTypedArray()
            postBodyRaw = true
        }
    }

    @JMeterDslMarker
    fun Controller.constantTimer(configure: ConstantTimer.() -> Unit) =
        testElement<ConstantTimer, ConstantTimerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.gaussianRandomTimer(configure: GaussianRandomTimer.() -> Unit) =
        testElement<GaussianRandomTimer, GaussianRandomTimerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.preciseThroughputTimer(configure: PreciseThroughputTimer.() -> Unit) =
        testElement<PreciseThroughputTimer, PoissonRandomTimerGui>(this, configure)

    @JMeterDslMarker
    fun Controller.constantThroughputTimer(configure: ConstantThroughputTimer.() -> Unit) =
        testElement<ConstantThroughputTimer, PoissonRandomTimerGui>(this, configure)

    @JMeterDslMarker
    fun Sampler.responseAssertion(configure: ResponseAssertion.() -> Unit) =
        testElement<ResponseAssertion, AssertionGui>(this, configure)

    @JMeterDslMarker
    fun Sampler.responseCodeAssertion(responseCode: Int, configure: ResponseAssertion.() -> Unit = {}) =
        responseAssertion {
            setTestFieldResponseCode()
            setToEqualsType()
            addTestString(responseCode.toString())

            configure()
        }

    @JMeterDslMarker
    fun Sampler.xpath2Assertion(configure: XPath2Assertion.() -> Unit) =
        testElement<XPath2Assertion, XPath2AssertionGui>(this, configure)

    @JMeterDslMarker
    fun Sampler.xpath2Extractor(configure: XPath2Extractor.() -> Unit) =
        testElement<XPath2Extractor, XPath2ExtractorGui>(this, configure)

    @JMeterDslMarker
    fun ThreadGroup.viewResultsTree(configure: ResultCollector.() -> Unit) =
        testElement<ResultCollector, ViewResultsFullVisualizer>(this, configure)

    @JMeterDslMarker
    fun ThreadGroup.htmlReport(configure: HtmlReport.() -> Unit) =
        testElement<HtmlReport, HtmlReportVisualizer>(this, configure)

    private fun <T : TestElement> testElement(
        parent: TestElement,
        element: T,
        configure: T.() -> Unit
    ): T {
        subTrees[element] = subTrees.getValue(parent).add(element)
        element.configure()
        return element
    }

    private inline fun <reified T : TestElement, reified G : JMeterGUIComponent> testElement(
        parent: TestElement,
        noinline configure: T.() -> Unit = {}
    ) = testElement(parent, createTestElementWithGui<T, G>(), configure)

    private inline fun <reified T : TestElement> createTestElement(): T =
        T::class.java.getConstructor().newInstance().apply {
            // defaults
            schema.testClass[this] = T::class.java
            schema.enabled[this] = true
            schema.name[this] = T::class.simpleName
            schema.enabled[this] = true
        }

    private inline fun <reified T : TestElement, reified G : JMeterGUIComponent> createTestElementWithGui(): T =
        createTestElement<T>().apply {
            schema.guiClass[this] = G::class.java
            schema.name[this] =
                G::class.java.getAnnotation(TestElementMetadata::class.java)?.labelResource?.let(JMeterUtils::getResString)
                    ?: G::class.simpleName
        }
}
