package io.airbyte.integrations.source.mssql.initialsync;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.cdk.integrations.source.relationaldb.models.OrderedColumnLoadStatus;
import io.airbyte.integrations.source.mssql.initialsync.MssqlInitialReadUtil.OrderedColumnInfo;
import io.airbyte.protocol.models.AirbyteStreamNameNamespacePair;
import io.airbyte.protocol.models.v0.AirbyteStateMessage;
import java.util.HashMap;
import java.util.Map;

public interface MssqlInitialLoadStateManager {
  long MSSQL_STATE_VERSION = 2;
  String STATE_TYPE_KEY = "state_type";
  String ORDERED_COL_STATE_TYPE = "ordered_column";

  // Returns an intermediate state message for the initial sync.
  AirbyteStateMessage createIntermediateStateMessage(final AirbyteStreamNameNamespacePair pair, final OrderedColumnLoadStatus ocLoadStatus);

  // Updates the {@link OrderedColumnLoadStatus} for the state associated with the given pair
  void updateOrderedColumnLoadState(final AirbyteStreamNameNamespacePair pair, final OrderedColumnLoadStatus ocLoadStatus);

  // Returns the final state message for the initial sync.
  AirbyteStateMessage createFinalStateMessage(final AirbyteStreamNameNamespacePair pair, final JsonNode streamStateForIncrementalRun);

  // Returns the previous state emitted, represented as a {@link OrderedColumnLoadStatus} associated with
  // the stream.
  OrderedColumnLoadStatus getOrderedColumnLoadStatus(final AirbyteStreamNameNamespacePair pair);

  // Returns the current {@OrderedColumnInfo}, associated with the stream. This includes the data type &
  // the column name associated with the stream.
  OrderedColumnInfo getOrderedColumnInfo(final AirbyteStreamNameNamespacePair pair);

  static Map<AirbyteStreamNameNamespacePair, OrderedColumnLoadStatus> initPairToOrderedColumnLoadStatusMap(
      final Map<io.airbyte.protocol.models.v0.AirbyteStreamNameNamespacePair, OrderedColumnLoadStatus> pairToOcStatus) {
    final Map<AirbyteStreamNameNamespacePair, OrderedColumnLoadStatus> map = new HashMap<>();
    pairToOcStatus.forEach((pair, ocStatus) -> {
      final AirbyteStreamNameNamespacePair updatedPair = new AirbyteStreamNameNamespacePair(pair.getName(), pair.getNamespace());
      map.put(updatedPair, ocStatus);
    });
    return map;
  }
}
