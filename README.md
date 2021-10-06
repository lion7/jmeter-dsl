# JMeter DSL

[![](https://jitpack.io/v/lion7/jmeter-dsl.svg)](https://jitpack.io/#lion7/jmeter-dsl)

## Example usage

```kotlin
import com.github.lion7.jmeter.dsl.JMeterDsl.Companion.jMeter
import org.junit.jupiter.api.Test
import java.net.URI

internal class JMeterDslTest {

    @Test
    fun simpleTestPlan() {
        val testPlan = jMeter {
            testPlan {
                threadGroup {
                    mainController {
                        httpSampler("GET", URI("https://www.google.nl/"))
                    }

                    htmlReport {
                        filename = "jmeter-results.csv"
                        outputDirectory = "jmeter-report/"
                    }
                }
            }
        }
        testPlan.run()
    }
}
```
