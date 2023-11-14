#
# Copyright (c) 2023 Airbyte, Inc., all rights reserved.
#
import logging
from typing import Any, Iterator, List, Mapping, MutableMapping, Optional, Union

from airbyte_cdk.models import AirbyteMessage, AirbyteStateMessage, ConfiguredAirbyteCatalog
from airbyte_cdk.sources import AbstractSource
from airbyte_cdk.sources.concurrent_source.concurrent_source import ConcurrentSource
from airbyte_cdk.sources.streams.concurrent.abstract_stream import AbstractStream
from airbyte_cdk.sources.streams.concurrent.adapters import StreamFacade


class ConcurrentSourceAdapter(AbstractSource):
    def __init__(self, concurrent_source: ConcurrentSource, **kwargs):
        self._concurrent_source = concurrent_source
        super().__init__(**kwargs)

    def read(
        self,
        logger: logging.Logger,
        config: Mapping[str, Any],
        catalog: ConfiguredAirbyteCatalog,
        state: Optional[Union[List[AirbyteStateMessage], MutableMapping[str, Any]]] = None,
    ) -> Iterator[AirbyteMessage]:
        concurrent_streams = self._streams_as_abstract_streams(config, catalog)
        concurrent_stream_names = {stream.name for stream in concurrent_streams}
        configured_catalog_for_regular_streams = ConfiguredAirbyteCatalog(
            streams=[stream for stream in catalog.streams if stream.stream.name not in concurrent_stream_names]
        )
        if concurrent_streams:
            yield from self._concurrent_source.read(concurrent_streams)
        if configured_catalog_for_regular_streams.streams:
            yield from super().read(logger, config, configured_catalog_for_regular_streams, state)

    def _streams_as_abstract_streams(self, config: Mapping[str, Any], configured_catalog: ConfiguredAirbyteCatalog) -> List[AbstractStream]:
        """
        Ensures the streams are StreamFacade and returns the underlying AbstractStream.
        This is necessary because AbstractSource.streams() returns a List[Stream] and not a List[AbstractStream].
        :param config:
        :return:
        """
        all_streams = self.streams(config)
        stream_name_to_instance: Mapping[str, AbstractStream] = {s.name: s for s in all_streams}
        abstract_streams = []
        for configured_stream in configured_catalog.streams:
            stream_instance = stream_name_to_instance.get(configured_stream.stream.name)
            if not stream_instance:
                if not self.raise_exception_on_missing_stream:
                    continue
                raise KeyError(
                    f"The stream {configured_stream.stream.name} no longer exists in the configuration. "
                    f"Refresh the schema in replication settings and remove this stream from future sync attempts."
                )
            if isinstance(stream_instance, StreamFacade):
                abstract_streams.append(stream_instance._abstract_stream)
        return abstract_streams