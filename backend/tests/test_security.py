from datetime import UTC, datetime, timedelta
from uuid import uuid4

import jwt
import pytest
from jwt import InvalidTokenError
from pydantic import ValidationError

from app.core.config import Settings
from app.core.security import AuthSecurity


@pytest.fixture
def settings():
    return Settings(
        _env_file=None,
        database_url="postgresql+psycopg://test:test@127.0.0.1:55432/liuxueba_test",
        jwt_secret_key="a-test-key-that-is-long-enough-and-only-used-in-tests",
    )


@pytest.fixture
def security(settings):
    return AuthSecurity(settings)


def test_hash_is_salted_and_only_matching_passwords_verify(security):
    first_hash = security.hash_password(" study2026 ")
    second_hash = security.hash_password(" study2026 ")
    assert first_hash != second_hash
    assert first_hash.startswith("$argon2id$")
    assert "study2026" not in first_hash
    assert security.verify_password(" study2026 ", first_hash)
    assert not security.verify_password("study2026", first_hash)
    assert not security.verify_password("wrong-password", first_hash)


def test_invalid_stored_hash_is_rejected(security):
    assert not security.verify_password("study2026", "corrupted-hash")


def test_access_token_round_trip_has_required_claims(security, settings):
    user_id = uuid4()
    token = security.create_access_token(user_id)
    assert security.decode_access_token(token) == user_id
    claims = jwt.decode(
        token,
        settings.jwt_secret_key.get_secret_value(),
        algorithms=["HS256"],
        issuer="liuxueba-api",
        audience="liuxueba-app",
    )
    assert claims["exp"] - claims["iat"] == 3600


@pytest.mark.parametrize(
    "change", ["expired", "future", "issuer", "audience", "missing", "subject"]
)
def test_invalid_claims_are_rejected(security, settings, change):
    now = datetime.now(UTC)
    claims = {
        "sub": str(uuid4()),
        "iat": now,
        "nbf": now,
        "exp": now + timedelta(minutes=10),
        "iss": "liuxueba-api",
        "aud": "liuxueba-app",
    }
    if change == "expired":
        claims["exp"] = now - timedelta(seconds=10)
    elif change == "future":
        claims["nbf"] = now + timedelta(minutes=10)
    elif change == "issuer":
        claims["iss"] = "another-api"
    elif change == "audience":
        claims["aud"] = "another-app"
    elif change == "missing":
        del claims["exp"]
    else:
        claims["sub"] = "invalid-user-id"
    token = jwt.encode(claims, settings.jwt_secret_key.get_secret_value(), algorithm="HS256")
    with pytest.raises(InvalidTokenError):
        security.decode_access_token(token)


def test_wrong_signing_key_is_rejected(security):
    other_settings = Settings(
        _env_file=None,
        database_url="postgresql+psycopg://test:test@127.0.0.1:55432/liuxueba_test",
        jwt_secret_key="another-test-key-that-is-long-enough-for-hs256",
    )
    token = AuthSecurity(other_settings).create_access_token(uuid4())
    with pytest.raises(InvalidTokenError):
        security.decode_access_token(token)


def test_unsigned_token_is_rejected(security):
    token = jwt.encode({"sub": str(uuid4())}, key="", algorithm="none")
    with pytest.raises(InvalidTokenError):
        security.decode_access_token(token)


@pytest.mark.parametrize("secret", ["", "short", " " * 32])
def test_configuration_rejects_empty_or_weak_signing_keys(secret):
    with pytest.raises(ValidationError):
        Settings(
            _env_file=None,
            database_url="postgresql+psycopg://test:test@127.0.0.1:55432/liuxueba_test",
            jwt_secret_key=secret,
        )


def test_configuration_requires_postgresql():
    with pytest.raises(ValidationError):
        Settings(
            _env_file=None,
            database_url="sqlite:///temporary.db",
            jwt_secret_key="a-test-key-that-is-long-enough-and-only-used-in-tests",
        )
