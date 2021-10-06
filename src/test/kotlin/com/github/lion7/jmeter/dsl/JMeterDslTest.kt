package com.github.lion7.jmeter.dsl

import com.github.lion7.jmeter.dsl.JMeterDsl.Companion.jMeter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.net.URI

internal class JMeterDslTest {

    @Test
    fun simpleTestPlan(testInfo: TestInfo) {
        val testPlan = jMeter {
            testPlan {
                threadGroup {
                    mainController {
                        httpSampler("GET", URI("https://www.google.nl/"))
                    }

                    htmlReport {
                        val methodName = testInfo.testMethod.map { it.name }.orElseThrow()
                        filename = "build/jmeter/results/$methodName.csv"
                        outputDirectory = "build/jmeter/reports/$methodName/"
                    }
                }
            }
        }
        testPlan.run()
    }
}
