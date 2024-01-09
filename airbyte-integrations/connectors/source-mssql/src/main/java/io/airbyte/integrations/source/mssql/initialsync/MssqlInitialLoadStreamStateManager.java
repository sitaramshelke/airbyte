package io.airbyte.integrations.source.mssql.initialsync;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.cdk.integrations.source.relationaldb.models.OrderedColumnLoadStatus;
import io.airbyte.commons.json.Jsons;
import io.airbyte.integrations.source.mssql.initialsync.MssqlInitialReadUtil.InitialLoadStreams;
import io.airbyte.integrations.source.mssql.initialsync.MssqlInitialReadUtil.OrderedColumnInfo;
import io.airbyte.protocol.models.AirbyteStreamNameNamespacePair;
import io.airbyte.protocol.models.v0.AirbyteStateMessage;
import io.airbyte.protocol.models.v0.AirbyteStateMessage.AirbyteStateType;
import io.airbyte.protocol.models.v0.AirbyteStreamState;
import io.airbyte.protocol.models.v0.ConfiguredAirbyteCatalog;
import io.airbyte.protocol.models.v0.StreamDescriptor;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This state manager extends the StreamStateManager to enable writing the state_type and version
 * keys to the stream state when they're going through the iterator Once we have verified that
 * expanding StreamStateManager itself to include this functionality, this class will be removed
 */
public class MssqlInitialLoadStreamStateManager implements MssqlInitialLoadStateManager{
  private static final Logger LOGGER = LoggerFactory.getLogger(MssqlInitialLoadStateManager.class);
  private final Map<AirbyteStreamNameNamespacePair, OrderedColumnLoadStatus> pairToOrderedColLoadStatus;

  private final Map<AirbyteStreamNameNamespacePair, OrderedColumnInfo> pairToOrderedColInfo;

  public MssqlInitialLoadStreamStateManager(final ConfiguredAirbyteCatalog catalog,
      final InitialLoadStreams initialLoadStreams,
      final Map<AirbyteStreamNameNamespacePair, OrderedColumnInfo> pairToOrderedColInfo) {
    this.pairToOrderedColInfo = pairToOrderedColInfo;
    this.pairToOrderedColLoadStatus = MssqlInitialLoadStateManager.initPairToOrderedColumnLoadStatusMap(initialLoadStreams.pairToInitialLoadStatus());
  }

  @Override
  public void updateOrderedColumnLoadState(final AirbyteStreamNameNamespacePair pair, final OrderedColumnLoadStatus ocLoadStatus) {
    pairToOrderedColLoadStatus.put(pair, ocLoadStatus);
  }

  @Override
  public AirbyteStateMessage createFinalStateMessage(final AirbyteStreamNameNamespacePair pair, final JsonNode streamStateForIncrementalRun) {
    return new AirbyteStateMessage()
        .withType(AirbyteStateType.STREAM)
        .withStream(getAirbyteStreamState(pair, streamStateForIncrementalRun));
  }

  @Override
  public OrderedColumnLoadStatus getOrderedColumnLoadStatus(final AirbyteStreamNameNamespacePair pair) {
    return pairToOrderedColLoadStatus.get(pair);
  }

  @Override
  public OrderedColumnInfo getOrderedColumnInfo(final AirbyteStreamNameNamespacePair pair) {
    return pairToOrderedColInfo.get(pair);
  }

  @Override
  public AirbyteStateMessage createIntermediateStateMessage(final AirbyteStreamNameNamespacePair pair, final OrderedColumnLoadStatus ocLoadStatus) {
    return new AirbyteStateMessage()
        .withType(AirbyteStateType.STREAM)
        .withStream(getAirbyteStreamState(pair, Jsons.jsonNode(ocLoadStatus)));
  }

  private AirbyteStreamState getAirbyteStreamState(final io.airbyte.protocol.models.AirbyteStreamNameNamespacePair pair, final JsonNode stateData) {
    LOGGER.info("STATE DATA FOR {}: {}", pair.getNamespace().concat("_").concat(pair.getName()), stateData);
    assert Objects.nonNull(pair.getName());
    assert Objects.nonNull(pair.getNamespace());

    return new AirbyteStreamState()
        .withStreamDescriptor(
            new StreamDescriptor().withName(pair.getName()).withNamespace(pair.getNamespace()))
        .withStreamState(stateData);
  }

}
