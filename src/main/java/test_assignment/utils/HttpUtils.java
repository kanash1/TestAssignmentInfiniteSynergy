package test_assignment.utils;

import org.apache.hc.core5.http.impl.EnglishReasonPhraseCatalog;
import rawhttp.core.*;
import rawhttp.core.body.StringBody;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.IntStream;

public final class HttpUtils {
    private final RawHttp http;

    public HttpUtils() {
        http = new RawHttp();
    }

    public RawHttpResponse<?> formResponse(
            final Integer code,
            final Map<String, List<String>> headers,
            final String body
    ) {
        final StringBuilder builder = new StringBuilder();

        builder
                .append(HttpVersion.HTTP_1_1)
                .append(" ")
                .append(code.toString())
                .append(" ")
                .append(EnglishReasonPhraseCatalog.INSTANCE.getReason(code, Locale.ENGLISH))
                .append("\n");

        for (final var header : headers.entrySet()) {
            builder
                    .append(header.getKey())
                    .append(": ");
            final List<String> values = header.getValue();
            IntStream.range(0, values.size())
                    .forEach(idx -> {
                        builder.append(values.get(idx));
                        if (idx + 1 < values.size())
                            builder.append(", ");
                    });
            builder.append("\n");
        }


        RawHttpResponse<?> response = http.parseResponse(builder.toString());
        if (body != null)
            response = response.withBody(new StringBody(body));
        return response;
    }

    public RawHttpResponse<?> formEmptyResponse(final int code) {
        return new RawHttpResponse<Void>(
                null,
                null,
                getStatusLineByCode(code),
                RawHttpHeaders.CONTENT_LENGTH_ZERO,
                null);
    }

    public StatusLine getStatusLineByCode(final int code) {
        return new StatusLine(
                HttpVersion.HTTP_1_1,
                code,
                EnglishReasonPhraseCatalog.INSTANCE.getReason(code, Locale.ENGLISH)
        );
    }
}
