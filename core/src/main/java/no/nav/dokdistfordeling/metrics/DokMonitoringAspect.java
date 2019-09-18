/*
 * Copyright 2017 Pivotal Software, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package no.nav.dokdistfordeling.metrics;

import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_ERROR_TYPE;
import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_EXCEPTION_NAME;
import static no.nav.dokdistfordeling.metrics.MetricLabels.LABEL_PROCESS;
import static no.nav.dokdistfordeling.metrics.MetricLabels.TYPE_FUNCTIONAL_EXCEPTION;
import static no.nav.dokdistfordeling.metrics.MetricLabels.TYPE_TECHNICAL_EXCEPTION;

import io.micrometer.core.annotation.Incubating;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistfordeling.exception.functional.AbstractDokdistfordelingFunctionalException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.function.Function;


/**
 * AspectJ aspect for intercepting types or method annotated with @Timed.
 * Changes: Counter for exceptions
 *
 * @author Joakim Bjørnstad, Jbit AS
 */
@Aspect
@Incubating(since = "1.0.0")
@Slf4j
@SuppressWarnings("Duplicates")
public class DokMonitoringAspect {
	private final MeterRegistry registry;
	private final Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint;
	private static final String EXCEPTION_COUNTER = "dok_metric_exception_total";


	public DokMonitoringAspect(MeterRegistry registry) {
		this(registry, pjp ->
				Tags.of("class", pjp.getStaticPart().getSignature().getDeclaringTypeName(),
						"method", pjp.getStaticPart().getSignature().getName())
		);
	}

	private DokMonitoringAspect(MeterRegistry registry, Function<ProceedingJoinPoint, Iterable<Tag>> tagsBasedOnJoinpoint) {
		this.registry = registry;
		this.tagsBasedOnJoinpoint = tagsBasedOnJoinpoint;
	}

	@Around("execution (@no.nav.dokdistfordeling.metrics.Monitor * *.*(..))")
	public Object dokMetrics(ProceedingJoinPoint pjp) throws Throwable {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		Monitor monitor = method.getAnnotation(Monitor.class);

		if (monitor.value().isEmpty()) {
			return pjp.proceed();
		}

		Timer.Sample sample = Timer.start(registry);
		try {
			return pjp.proceed();
		} catch (Exception e) {
			Counter.builder(EXCEPTION_COUNTER)
					.tags(LABEL_ERROR_TYPE, isFunctionalException(e) ? TYPE_FUNCTIONAL_EXCEPTION : TYPE_TECHNICAL_EXCEPTION)
					.tags(LABEL_EXCEPTION_NAME, e.getClass().getSimpleName())
					.tags(LABEL_PROCESS, monitor.process())
					.register(registry)
					.increment();
			throw e;
		} finally {
			sample.stop(Timer.builder(monitor.value())
					.description(monitor.description().isEmpty() ? null : monitor.description())
					.tags(LABEL_PROCESS, monitor.process())
					.tags(monitor.extraTags())
					.tags(tagsBasedOnJoinpoint.apply(pjp))
					.publishPercentileHistogram(monitor.histogram())
					.publishPercentiles(monitor.percentiles().length == 0 ? null : monitor.percentiles())
					.register(registry));
		}
	}

	private boolean isFunctionalException(Exception e) {
		return e instanceof AbstractDokdistfordelingFunctionalException;
	}

}
