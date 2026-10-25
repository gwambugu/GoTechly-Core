package com.dotechly.gotechly.core.pipeline.stages.callback;

import com.dotechly.gotechly.core.pipeline.PipelineContext;

/**
 * What the channel answered when {@link CallbackStage} POSTed to it.
 *
 * <p>Published on the {@link PipelineContext} under {@link CallbackStage#RESPONSE_ATTR} rather than
 * onto {@code Details}: the body can be large, is only of interest to the stage that interprets it,
 * and does not belong in the {@code additional_fields} CLOB that every other stage shares. The
 * persisted breadcrumb is the HTTP status alone, under {@code callback.httpStatus}.
 *
 * @param statusCode  HTTP status; {@link CallbackHttpClient} maps a transport failure to a negative
 *                    value rather than an error signal, so classify on the number
 * @param body        raw response body, exactly as received — never pre-parsed here, since
 *                    {@code callback} does not know which shape the channel speaks
 * @param channelName the channel this answer came from, for logging
 * @param contentType the content type the request was sent as
 */
public record CallbackOutcome(int statusCode, String body, String channelName, String contentType) {
}
