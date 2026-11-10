from pathlib import Path

import pytest
from alembic import command
from alembic.config import Config
from fastapi.testclient import TestClient
from pydantic import SecretStr
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy import create_engine, delete
from sqlalchemy.engine import make_url

from app.application import create_app
from app.core.config import Settings
from app.models.user import User

BACKEND_DIR = Path(__file__).resolve().parents[1]


class TestDatabaseSettings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="LIUXUEBA_", env_file=BACKEND_DIR / ".env", extra="ignore"
    )
    test_database_url: SecretStr | None = None


@pytest.fixture(scope="session")
def database_url():
    configured_url = TestDatabaseSettings().test_database_url
    if configured_url is None:
        pytest.fail("请设置指向独立测试库的 LIUXUEBA_TEST_DATABASE_URL")
    value = configured_url.get_secret_value()
    url = make_url(value)
    if url.drivername != "postgresql+psycopg" or not (url.database or "").endswith("_test"):
        pytest.fail("测试仅允许连接名称以 _test 结尾的独立 PostgreSQL 数据库")
    return value


@pytest.fixture(scope="session")
def migration_config(database_url):
    config = Config(str(BACKEND_DIR / "alembic.ini"))
    config.attributes["database_url"] = database_url
    return config


@pytest.fixture(scope="session")
def database_engine(database_url, migration_config):
    command.upgrade(migration_config, "head")
    engine = create_engine(database_url)
    yield engine
    engine.dispose()


@pytest.fixture
def app_settings(database_url):
    return Settings(
        _env_file=None,
        database_url=database_url,
        jwt_secret_key="integration-test-secret-for-liuxueba-only-not-for-production",
    )


@pytest.fixture
def client(database_engine, app_settings):
    with database_engine.begin() as connection:
        connection.execute(delete(User))
    with TestClient(create_app(app_settings)) as client:
        yield client
    with database_engine.begin() as connection:
        connection.execute(delete(User))
