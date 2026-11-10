from functools import lru_cache
from pathlib import Path

from pydantic import Field, SecretStr, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict
from sqlalchemy.engine import make_url
from sqlalchemy.exc import ArgumentError

BACKEND_DIR = Path(__file__).resolve().parents[2]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_prefix="LIUXUEBA_",
        env_file=BACKEND_DIR / ".env",
        env_file_encoding="utf-8",
        extra="ignore",
        frozen=True,
    )

    database_url: SecretStr
    jwt_secret_key: SecretStr
    jwt_issuer: str = Field(default="liuxueba-api", min_length=1)
    jwt_audience: str = Field(default="liuxueba-app", min_length=1)
    access_token_expire_minutes: int = Field(default=60, ge=1, le=1440)

    @field_validator("database_url")
    @classmethod
    def validate_database_url(cls, value: SecretStr) -> SecretStr:
        try:
            url = make_url(value.get_secret_value())
        except ArgumentError as error:
            raise ValueError("数据库连接地址格式不正确") from error
        if url.drivername != "postgresql+psycopg" or not url.database:
            raise ValueError("请配置 postgresql+psycopg 格式的 PostgreSQL 数据库连接")
        return value

    @field_validator("jwt_secret_key")
    @classmethod
    def validate_signing_key(cls, value: SecretStr) -> SecretStr:
        if len(value.get_secret_value().strip()) < 32:
            raise ValueError("JWT 密钥至少需要 32 个非空字符，请生成独立随机密钥")
        return value


@lru_cache
def get_settings() -> Settings:
    return Settings()
