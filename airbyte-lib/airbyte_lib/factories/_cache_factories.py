# Copyright (c) 2023 Airbyte, Inc., all rights reserved.


import ulid

from airbyte_protocol.models import ConfiguredAirbyteCatalog

from airbyte_lib.caches.duckdb import DuckDBCache, DuckDBCacheConfig


def get_default_cache(source_catalog: ConfiguredAirbyteCatalog) -> DuckDBCache:
    """Get a local cache for storing data, using the default database path.

    Cache files are stored in the `.cache` directory, relative to the current
    working directory.
    """
    config = DuckDBCacheConfig(
        db_path="./.cache/default_cache_db.duckdb",
    )
    return DuckDBCache(
        config=config,
        source_catalog=source_catalog,
    )


def new_local_cache(
    cache_name: str | None = None,
    *,
    source_catalog: ConfiguredAirbyteCatalog,
) -> DuckDBCache:
    """Get a local cache for storing data, using a name string to seed the path.

    Cache files are stored in the `.cache` directory, relative to the current
    working directory.
    """
    cache_name = cache_name or str(ulid.ULID())
    config = DuckDBCacheConfig(
        db_path=f"./.cache/db_{cache_name}.duckdb",
    )
    return DuckDBCache(
        config=config,
        source_catalog=source_catalog,
    )
