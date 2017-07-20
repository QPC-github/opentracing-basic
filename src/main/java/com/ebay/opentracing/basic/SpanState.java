package com.ebay.opentracing.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import io.opentracing.References;

/**
 * Object which encapsulates the mutable span state.  It also doubles as the implemnentation of the publicly facing
 * {@link SpanData} interface which is used to expose this information at the API surface.
 *
 * @param <T> trace context type
 */
final class SpanState<T> implements SpanData<T>
{

	private final InternalSpanContext<T> spanContext;
	private final TimeUnit startTimeUnit;
	private final long startTimeStamp;
	private final Map<String, List<InternalSpanContext<T>>> references;
	private String operationName;

	@Nullable
	private Map<String, String> tags;

	@Nullable
	private List<LogEvent> logs;

	@Nullable
	private TimeUnit finishTimeUnit;
	private long finishTimeStamp;

	SpanState(
		InternalSpanContext<T> spanContext,
		String operationName,
		TimeUnit startTimeUnit,
		long startTimeStamp,
		@Nullable  Map<String, String> tags,
		Map<String, List<InternalSpanContext<T>>> references
	)
	{
		this.spanContext = TracerPreconditions.checkNotNull(spanContext);
		this.operationName = TracerPreconditions.checkNotNull(operationName);
		this.startTimeUnit = TracerPreconditions.checkNotNull(startTimeUnit);
		this.startTimeStamp = startTimeStamp;
		this.tags = tags;
		this.references = TracerPreconditions.checkNotNull(references);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public InternalSpanContext<T> getSpanContext()
	{
		return spanContext;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getStartTime(TimeUnit timeUnit)
	{
		return timeUnit.convert(startTimeStamp, startTimeUnit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getFinishTime(TimeUnit timeUnit)
	{
		return timeUnit.convert(finishTimeStamp, finishTimeUnit);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getOperationName()
	{
		return operationName;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getTags()
	{
		return (tags == null) ? Collections.<String, String>emptyMap() : Collections.unmodifiableMap(tags);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<? extends InternalSpanContext<T>> getReferences(String referenceType)
	{
		return references.get(referenceType);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<LogEvent> getLogEvents()
	{
		return (logs == null) ? Collections.<LogEvent>emptyList() : logs;
	}

	/**
	 * {@inheritDoc}
	 *
	 * NOTE: This implementation dumps most of the span state but avoid logging values as these may contain
	 *       sensitive information and inadvertently end up in logs.
	 */
	public String toString()
	{
		StringBuilder builder = new StringBuilder("Span{operationName='")
			.append(operationName)
			.append("'");

		List<InternalSpanContext<T>> childOfList = references.get(References.CHILD_OF);
		if (childOfList != null && !childOfList.isEmpty())
		{
			builder.append(",childOf=[");
			for (int i = 0; i < childOfList.size(); i++)
			{
				if (i > 0)
					builder.append(",");
				builder.append(childOfList.get(i));
			}
			builder.append("]");
		}
		List<InternalSpanContext<T>> followsFromList = references.get(References.FOLLOWS_FROM);
		if (followsFromList != null && !followsFromList.isEmpty())
		{
			builder.append(",followsFrom=[");
			for (int i = 0; i < followsFromList.size(); i++)
			{
				if (i > 0)
					builder.append(",");
				builder.append(followsFromList.get(i));
			}
			builder.append("]");
		}

		builder.append(",startTimeMs=")
			.append(TimeUnit.MILLISECONDS.convert(startTimeStamp, startTimeUnit));
		if (finishTimeUnit != null)
		{
			builder.append(",finishTimeMs=")
				.append(TimeUnit.MILLISECONDS.convert(finishTimeStamp, finishTimeUnit));
		}

		if (tags != null)
		{
			Set<String> tagKeys = tags.keySet();
			if (!tags.isEmpty())
			{
				builder.append(",tags=[");
				Iterator<String> tagIterator = tagKeys.iterator();
				while (tagIterator.hasNext())
				{
					builder.append(tagIterator.next());
					if (tagIterator.hasNext())
						builder.append(",");
				}
				builder.append("]");
			}
		}

		return builder.append("}").toString();
	}

	/**
	 * Update the operation name to the provided value.
	 *
	 * @param operationName new operation name
	 */
	void setOperationName(String operationName)
	{
		this.operationName = TracerPreconditions.checkNotNull(operationName, "operationName may not be null");
	}

	/**
	 * Set the span finish time.
	 *
	 * @param finishTimeUnit time unit
	 * @param finishTimeStamp time value
	 */
	void setFinishTime(TimeUnit finishTimeUnit, long finishTimeStamp)
	{
		this.finishTimeUnit = TracerPreconditions.checkNotNull(finishTimeUnit, "finishTimeUnit may not be null");
		this.finishTimeStamp = finishTimeStamp;
	}

	void putTag(String key, String value)
	{
		if (tags == null)
			tags = Collections.synchronizedMap(new HashMap<String, String>());
		tags.put(key, value);

	}

	void addLogEvent(LogEvent logEvent)
	{
		if (logs == null)
			logs = Collections.synchronizedList(new ArrayList<LogEvent>(5));
		logs.add(logEvent);
	}

}
