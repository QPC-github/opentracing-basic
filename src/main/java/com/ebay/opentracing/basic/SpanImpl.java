package com.ebay.opentracing.basic;

import java.util.concurrent.TimeUnit;

import io.opentracing.Span;

final class SpanImpl<T> extends BaseSpanImpl<Span, T> implements Span
{
	private final SpanState<T> spanState;
	private final SpanFinisher<T> spanFinisher;

	SpanImpl(SpanState<T> spanState, SpanFinisher<T> spanFinisher)
	{
		super(spanState);
		this.spanState = spanState;
		this.spanFinisher = spanFinisher;
	}

	@Override
	public void finish()
	{
		spanFinisher.finish(spanState);
	}

	@Override
	public void finish(long finishMicros)
	{
		spanFinisher.finish(spanState, TimeUnit.MICROSECONDS, finishMicros);
	}

}
