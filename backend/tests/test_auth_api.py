from datetime import UTC, datetime, timedelta

import jwt
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import delete, select, update

from app.application import create_app
from app.core.config import Settings
from app.models.user import User


def register(client, **overrides):
    return client.post(
        "/auth/register",
        json={
            "username": "留学同学",
            "email": "student@example.com",
            "password": "study2026",
            **overrides,
        },
    )


def login(client, email="student@example.com", password="study2026"):
    return client.post("/auth/login", json={"email": email, "password": password})


def bearer(token):
    return {"Authorization": f"Bearer {token}"}


def test_health_checks_the_real_database(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok", "database": "ok"}


def test_registration_persists_a_regular_user_without_exposing_secrets(client, database_engine):
    response = register(client)
    assert response.status_code == 201
    user = response.json()
    assert user["username"] == "留学同学"
    assert user["nickname"] == "留学同学"
    assert user["role"] == "user"
    assert "password" not in user
    assert "password_hash" not in user
    assert "study2026" not in response.text
    with database_engine.connect() as connection:
        row = connection.execute(select(User.__table__)).mappings().one()
    assert str(row["id"]) == user["id"]
    assert row["password_hash"].startswith("$argon2id$")
    assert row["password_hash"] != "study2026"


@pytest.mark.parametrize("field", ["email", "username"])
def test_duplicate_accounts_are_rejected_case_insensitively(client, field):
    assert register(client, username="Student").status_code == 201
    changes = {"email": "second@example.com", "username": "AnotherStudent"}
    changes[field] = "STUDENT@EXAMPLE.COM" if field == "email" else "STUDENT"
    assert register(client, **changes).status_code == 409
    assert login(client).status_code == 200


@pytest.mark.parametrize(
    "changes",
    [{"email": "invalid"}, {"password": "short"}, {"username": " "}, {"role": "admin"}],
)
def test_invalid_registration_is_rejected_without_echoing_passwords(client, changes):
    response = register(client, **changes)
    assert response.status_code == 422
    assert "study2026" not in response.text
    assert "short" not in response.text
    assert all("input" not in error for error in response.json()["detail"])


def test_login_and_current_user_use_the_bearer_identity(client):
    registered = register(client).json()
    response = login(client, email=" STUDENT@EXAMPLE.COM ")
    assert response.status_code == 200
    token = response.json()
    assert token["token_type"] == "bearer"
    assert token["expires_in"] == 3600
    assert response.headers["cache-control"] == "no-store"
    profile = client.get("/auth/me", headers=bearer(token["access_token"]))
    assert profile.status_code == 200
    assert profile.json() == registered
    assert profile.headers["cache-control"] == "no-store"


@pytest.mark.parametrize(
    ("email", "password"),
    [("student@example.com", "wrongpassword"), ("missing@example.com", "study2026")],
)
def test_wrong_password_and_unknown_account_have_the_same_response(client, email, password):
    register(client)
    response = login(client, email, password)
    assert response.status_code == 401
    assert response.json() == {"detail": "邮箱或密码不正确"}
    assert response.headers["www-authenticate"] == "Bearer"


@pytest.mark.parametrize("authorization", [None, "Basic abc", "Bearer invalid-token"])
def test_current_user_requires_valid_bearer_credentials(client, authorization):
    headers = {"Authorization": authorization} if authorization else {}
    response = client.get("/auth/me", headers=headers)
    assert response.status_code == 401
    assert response.headers["www-authenticate"] == "Bearer"


def test_current_user_rejects_expired_tokens(client, app_settings):
    user = register(client).json()
    now = datetime.now(UTC)
    token = jwt.encode(
        {
            "sub": user["id"],
            "iat": now - timedelta(hours=2),
            "nbf": now - timedelta(hours=2),
            "exp": now - timedelta(hours=1),
            "iss": "liuxueba-api",
            "aud": "liuxueba-app",
        },
        app_settings.jwt_secret_key.get_secret_value(),
        algorithm="HS256",
    )
    assert client.get("/auth/me", headers=bearer(token)).status_code == 401


def test_two_accounts_only_receive_their_own_profiles(client):
    first = register(client).json()
    second = register(client, username="另一位同学", email="second@example.com").json()
    first_token = login(client).json()["access_token"]
    second_token = login(client, "second@example.com").json()["access_token"]
    assert client.get("/auth/me", headers=bearer(first_token)).json()["id"] == first["id"]
    assert client.get("/auth/me", headers=bearer(second_token)).json()["id"] == second["id"]


@pytest.mark.parametrize("action", ["delete", "disable"])
def test_removed_or_disabled_accounts_cannot_use_existing_tokens(client, database_engine, action):
    register(client)
    token = login(client).json()["access_token"]
    with database_engine.begin() as connection:
        if action == "delete":
            connection.execute(delete(User))
        else:
            connection.execute(update(User).values(is_active=False))
    assert client.get("/auth/me", headers=bearer(token)).status_code == 401
    assert login(client).status_code == 401


def test_account_survives_recreating_the_application(client, app_settings):
    user = register(client).json()
    with TestClient(create_app(app_settings)) as restarted_client:
        token = login(restarted_client).json()["access_token"]
        response = restarted_client.get("/auth/me", headers=bearer(token))
    assert response.status_code == 200
    assert response.json()["id"] == user["id"]


def test_database_unavailability_returns_a_safe_service_error(app_settings):
    settings = Settings(
        _env_file=None,
        database_url="postgresql+psycopg://unavailable:secret@127.0.0.1:1/unavailable_test",
        jwt_secret_key=app_settings.jwt_secret_key,
    )
    with TestClient(create_app(settings)) as unavailable_client:
        response = unavailable_client.get("/health")
    assert response.status_code == 503
    assert response.json() == {"detail": "服务暂时不可用，请稍后重试"}
    assert "secret" not in response.text


def test_unpaired_surrogate_is_rejected_before_password_hashing(client):
    response = client.post(
        "/auth/register",
        content=b'{"username":"student","email":"a@example.com","password":"ab\\ud800password"}',
        headers={"Content-Type": "application/json"},
    )
    assert response.status_code == 422
    assert "ud800" not in response.text
    assert all("input" not in error for error in response.json()["detail"])
