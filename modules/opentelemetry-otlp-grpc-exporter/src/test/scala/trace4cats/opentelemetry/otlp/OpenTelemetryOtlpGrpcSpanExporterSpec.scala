package trace4cats.opentelemetry.otlp

import java.time.Instant

import cats.effect.IO
import fs2.Chunk
import trace4cats.SemanticTags
import trace4cats.model.{Batch, TraceProcess, TraceState}
import trace4cats.test.jaeger.BaseJaegerSpec

class OpenTelemetryOtlpGrpcSpanExporterSpec extends BaseJaegerSpec {
  it should "Send a batch of spans to jaeger" in forAll { (batch: Batch[Chunk], process: TraceProcess) =>
    val updatedBatch =
      Batch(
        batch.spans.map(span =>
          span.copy(
            serviceName = process.serviceName,
            attributes = (process.attributes ++ span.attributes)
              .filterNot { case (key, _) =>
                excludedTagKeys.contains(key)
              },
            start = Instant.now(),
            end = Instant.now(),
            context = span.context.copy(traceState = TraceState.empty)
          )
        )
      )

    testExporter(
      OpenTelemetryOtlpGrpcSpanExporter[IO, Chunk]("localhost", 4317),
      updatedBatch,
      batchToJaegerResponse(updatedBatch, process, SemanticTags.kindTags, statusTags, processTags, additionalTags)
    )
  }
}
